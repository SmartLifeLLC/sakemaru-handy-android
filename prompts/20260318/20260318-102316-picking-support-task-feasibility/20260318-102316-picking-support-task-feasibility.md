# 作業応援機能 - 対応可否調査・設計提案

- **作成日**: 2026-03-18
- **ステータス**: ドラフト
- **ディレクトリ**: prompts/20260318/20260318-102316-picking-support-task-feasibility/

## 背景・目的

現在のピッキングシステムでは、各ピッカーは自分に割り当てられたタスクのみを処理する。
自分の作業が全て完了したピッカーが、まだ作業が残っている他のピッカーの作業を「応援」（代行）できるようにしたい。

これにより、倉庫全体のピッキング効率を向上させ、出荷遅延を防ぐことが目的。

### 要件の整理

1. **応援リストの取得**: 他のピッカーのタスク一覧を閲覧できるようにする
2. **未開始タスクの担当変更**: まだ作業開始前（全アイテムが PENDING）のタスクは、応援者が開始することで担当が変わる
3. **作業中タスクの部分応援**: 作業中（一部アイテムが PICKING/COMPLETED）のタスクについて、残りのアイテムをリストの後ろから応援者がピッキングできるか？

## 現状の実装

### データモデル

**PickingTask** (`core/domain/.../model/PickingTask.kt`)
- `taskId`, `waveId`, `courseName`, `courseCode`, `pickingAreaName`, `pickingAreaCode`
- `items: List<PickingTaskItem>`
- **担当者（picker）フィールドは存在しない** — タスクの所有権はAPIクエリの `picker_id` パラメータで暗黙的にフィルタリング

**PickingTaskItem** のステータス（`ItemStatus` enum）:
- `PENDING` → 未登録
- `PICKING` → 登録済み・編集可能
- `COMPLETED` → 完了
- `SHORTAGE` → 欠品

### API構造

```
GET /api/picking/tasks?warehouse_id={id}&picker_id={id}&shipping_date={date}
```
- `picker_id` で担当者フィルタ
- `picker_id=0` で全タスク取得可能（既存実装: `getAllTasks()` で使用中）

### タスクライフサイクル API

| エンドポイント | 説明 |
|---|---|
| `POST /api/picking/tasks/{id}/start` | タスク開始 |
| `POST /api/picking/tasks/{result_id}/update` | アイテム数量更新 |
| `POST /api/picking/tasks/{id}/complete` | タスク完了 |
| `POST /api/picking/tasks/{result_id}/cancel` | アイテムキャンセル（PENDINGに戻す） |

### 現在のタスク取得フロー

1. `PickingTasksViewModel.loadMyAreaTasks()` → `repository.getMyAreaTasks(warehouseId, pickerId, shippingDate)`
2. APIが `picker_id` でフィルタしたタスクを返却
3. `isFullyProcessed` のタスクは表示から除外

### 重要な制約

- **タスク担当変更API は存在しない**
- **PickingTaskのドメインモデルに担当者情報がない**
- タスクの所有権は完全にサーバーサイドで管理されている

## 対応可否の調査結果

### 要件1: 応援リスト（他人のタスク）の取得

**対応可否: Android側は対応可能 / API確認必要**

**Android側の実装方針:**
- 既存の `getAllTasks(warehouseId, shippingDate)` で `picker_id=0` を使い全タスク取得は可能
- ただし、現在のAPIレスポンスに **担当者名・担当者ID が含まれていない**
- 応援リストとして「誰のタスクか」を表示するには、APIレスポンスに picker 情報の追加が必要

**API側の確認事項:**
- [ ] `GET /api/picking/tasks` のレスポンスに `picker_id`, `picker_name` を含められるか？
- [ ] 全タスク取得時に、自分のタスクと他人のタスクを区別するフィルタは可能か？
- [ ] タスクのステータス（未開始/作業中/完了）をタスク単位で返却できるか？（現在はアイテム単位のステータスのみ）

### 要件2: 未開始タスクの担当変更

**対応可否: API新規エンドポイントが必要**

**分析:**
- 現在「タスク開始」(`POST /tasks/{id}/start`) はあるが、これは担当者を変更する機能ではない
- 応援者が他人の未開始タスクを「引き受ける」には、担当者を変更するAPIが必要

**提案するAPI:**
```
POST /api/picking/tasks/{id}/reassign
Body: { "new_picker_id": 123 }
```

**Android側の実装方針:**
- 応援リスト画面で未開始タスクをタップ → reassign API → start API → 通常のピッキングフロー
- `PickingTaskRepository` に `reassignTask(taskId, newPickerId)` メソッド追加
- 担当変更後は自分のタスクとして通常通り処理可能

### 要件3: 作業中タスクの部分応援（リスト後方からのピッキング）

**対応可否: システム構造上、重大な課題あり**

#### 課題の詳細

**1. 同時編集の競合問題**
- 現在のシステムは「1タスク = 1ピッカー」を前提に設計
- 同じタスクの異なるアイテムを2人が同時に処理すると:
  - `updatePickingItem` の冪等性キー（`X-Idempotency-Key`）がピッカーごとに異なる
  - `completeTask` をどちらが実行するか不明
  - `cancelPickingItem` で相手の作業を巻き戻すリスク

**2. 進捗管理の複雑化**
- `PickingTask.registeredCount` / `pendingCount` の計算が2人分の作業を反映する必要
- どちらのピッカーの画面にもリアルタイムで最新状態を反映する必要がある
- 現在の `StateFlow<List<PickingTask>>` は1ピッカー分のみ想定

**3. UI/UXの問題**
- 「リストの後ろから」ピッキングする場合、元の担当者は「前から」作業中
- 両者が中央で出会ったとき、残りアイテムの分割ロジックが必要
- アイテムのロック機構がないため、同じアイテムを2人が同時に処理する可能性

#### 対応方法の選択肢

**選択肢A: タスク分割方式（推奨）**

作業中タスクを2つに分割し、後半を応援者に割り当てる。

```
POST /api/picking/tasks/{id}/split
Body: { "split_from_index": 5, "new_picker_id": 123 }
```

- サーバーがPENDINGアイテムを歩行順（walkingOrder）の後方から分割
- 新しいタスクIDを発行し、応援者に割り当て
- 元のタスクは残りのアイテムのみに縮小
- **メリット**: 1タスク=1ピッカーの原則を維持、競合なし
- **デメリット**: API側の実装コストが高い

**選択肢B: アイテム単位のロック方式**

各アイテムにロック（担当者）を設定し、複数ピッカーが同一タスクを処理。

- アイテムごとに `locked_by_picker_id` を管理
- `updatePickingItem` 時にロック確認
- **メリット**: タスク分割不要
- **デメリット**: 排他制御が複雑、リアルタイム同期が必要、既存設計の大幅変更

**選択肢C: 未開始タスクの担当変更のみ対応（フェーズ1）**

作業中タスクの部分応援は見送り、未開始タスクの担当変更のみを先行実装。

- **メリット**: 実装コスト最小、リスク低
- **デメリット**: 作業中タスクの応援ができない

## 変更内容（フェーズ1: 選択肢C の場合）

### 概要

未開始タスクの応援リスト表示と担当変更機能のみを先行実装する。

### 詳細設計

#### API変更（サーバー側）

1. **レスポンス拡張**: `GET /api/picking/tasks` に `picker_id`, `picker_name` を追加
2. **新規エンドポイント**: `POST /api/picking/tasks/{id}/reassign` で担当者変更
3. **バリデーション**: 未開始タスク（全アイテムPENDING）のみ reassign 可能

#### モデル変更（Android側）

- `PickingTask` に `pickerId: Int?`, `pickerName: String?` フィールド追加
- `PickingTaskResponse` に対応するフィールド追加
- `PickingTask` に `isNotStarted: Boolean` 算出プロパティ追加（全アイテムがPENDING）

#### Repository変更

- `PickingTaskRepository` に追加:
  - `getSupportableTasks(warehouseId, shippingDate): Flow<List<PickingTask>>` — 他人の未完了タスク取得
  - `reassignTask(taskId: Int, newPickerId: Int): Result<Unit>` — 担当変更

#### API変更（Android側）

- `PickingApi` に追加:
  - `reassignTask(taskId, body)` — POST `/api/picking/tasks/{id}/reassign`

#### UI変更

- 出庫処理画面（PickingTasksScreen）にタブまたはボタンを追加: 「応援」
- 新規画面: `SupportTasksScreen` — 応援可能なタスク一覧
  - 他人のタスクをコース別に表示
  - 担当者名、進捗状況を表示
  - 未開始タスクのみ「引き受ける」ボタンを表示
  - 作業中タスクはグレーアウト（フェーズ2で対応予定と表示）

### 影響範囲

| ファイル | 影響 |
|---|---|
| `PickingTask.kt` | フィールド追加 |
| `PickingModels.kt` | レスポンスモデル変更 |
| `PickingApi.kt` | エンドポイント追加 |
| `PickingTaskRepository.kt` | メソッド追加 |
| `PickingTaskRepositoryImpl.kt` | 実装追加 |
| `PickingTasksViewModel.kt` | 応援リスト取得ロジック追加 |
| `HandyNavHost.kt` | 応援画面のルート追加 |

## 対象ファイル

### 新規作成
- `feature/outbound/src/main/java/.../outbound/support/SupportTasksScreen.kt`
- `feature/outbound/src/main/java/.../outbound/support/SupportTasksViewModel.kt`
- `feature/outbound/src/main/java/.../outbound/support/SupportTasksState.kt`

### 既存変更
- `core/domain/src/main/java/.../model/PickingTask.kt` — picker情報フィールド追加
- `core/domain/src/main/java/.../repository/PickingTaskRepository.kt` — メソッド追加
- `core/network/src/main/java/.../api/PickingApi.kt` — reassign エンドポイント追加
- `core/network/src/main/java/.../model/PickingModels.kt` — レスポンスモデル拡張
- `core/network/src/main/java/.../repository/PickingTaskRepositoryImpl.kt` — 実装追加
- `feature/outbound/src/main/java/.../tasks/PickingTasksScreen.kt` — 応援ボタン/タブ追加
- `app/src/main/java/.../navigation/HandyNavHost.kt` — ルート追加

### 参照のみ
- `core/ui/src/main/java/.../TokenManager.kt` — pickerId取得
- `feature/outbound/src/main/java/.../tasks/PickingTasksViewModel.kt` — 既存パターン参照

## 確認事項

### API側への確認が必要な事項

1. **タスクのレスポンスに担当者情報を含められるか？**
   - `picker_id`, `picker_name` をレスポンスに追加可能か
   - 全タスク取得時（`picker_id=0`）にも担当者情報が返るか

2. **担当変更API（reassign）の実装可否**
   - `POST /api/picking/tasks/{id}/reassign` は実装可能か
   - 未開始タスクのみに制限するバリデーションは可能か
   - reassign 時に元担当者への通知は必要か

3. **タスク分割API（フェーズ2向け）の実現可能性**
   - 作業中タスクを歩行順で分割する機能は、DB構造上可能か
   - `wms_picking_task_id` の採番ルールに制約はあるか
   - 分割後のタスクの `wms_wave_id` は元と同じでよいか

4. **排他制御の要件**
   - 応援者がタスクを引き受ける操作と、元担当者がタスクを開始する操作が同時に発生した場合の優先順位
   - 楽観的ロック（バージョン番号）の導入は検討するか

### 設計判断が必要な事項

5. **フェーズ分けの確認**
   - フェーズ1（未開始タスクの担当変更のみ）で先行リリースしてよいか
   - フェーズ2（作業中タスクの分割応援）のタイムラインは？

6. **応援リストの表示方法**
   - 新規画面として追加 or 既存の出庫処理画面にタブ追加？
   - ピッカー別にグループ化して表示するか、フラットリストか？
