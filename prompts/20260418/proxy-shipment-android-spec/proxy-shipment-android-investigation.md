# 横持ち出荷 Android 調査結果

- 作成日: 2026-04-19
- 対象リポジトリ: `sakemaru-handy-android`

## 1. 調査目的

通常出荷の Android 実装を基準に、横持ち出荷をどの単位で切り出し、どこを再利用し、どこを分離すべきかを明確にする。

## 2. 参照仕様の整理

### 2.1 正式な API ソース

2026-04-19 作成の `prompts/20260418/proxy-shipment-api-specification.md` を API 契約の正式版として扱う。

### 2.2 補助資料

- 2026-04-18 `20260418-proxy-shipment-api-handy-v2-design.md`
- 2026-04-18 `shipping-and-proxy-shipment-spec.md`

### 2.3 差分の要点

| 観点 | `shipping-and-proxy-shipment-spec.md` | `proxy-shipment-api-specification.md` | 判断 |
| --- | --- | --- | --- |
| 一覧対象ステータス | `PENDING`, `PICKING` | `RESERVED`, `PICKING` | 最新仕様の `RESERVED`, `PICKING` を採用 |
| 一覧クエリ名 | `date` | `shipment_date` | 最新仕様の `shipment_date` を採用 |
| 完了後の扱い | ロジック検討中心 | API 契約が明文化済み | 最新仕様を採用 |

## 3. 現行通常出荷の実装構造

### 3.1 入口と共有セッション

通常出荷の入口は P03 メイン画面で、以下の共有状態を `TokenManager` が保持している。

| 項目 | 保存場所 | 現在の用途 |
| --- | --- | --- |
| ピッカー ID | `TokenManager` | API の `picker_id` |
| デフォルト倉庫 ID | `TokenManager` | 出荷一覧の `warehouse_id` |
| 出荷日 | `TokenManager` | 通常出荷一覧の `shipping_date` |

確認結果:

- `MainViewModel` は出荷日を `yyyy/MM/dd` 文字列として保存している。
- `MainScreen` の DatePicker も `yyyy/MM/dd` 前提で動いている。
- この日付形式は横持ち出荷 API の `YYYY-MM-DD` と一致しない。

### 3.2 画面遷移

現行の通常出荷は 3 画面で構成される。

```text
P03 メイン
  -> P20 PickingTasksScreen
  -> P21 OutboundPickingScreen
  -> P22 PickingHistoryScreen
```

ルート定義:

- `picking_list`
- `outbound_picking/{taskId}?editItemId={editItemId}`
- `picking_history/{taskId}`

### 3.3 Repository の責務

`PickingTaskRepository` は通常出荷の単一真実源になっている。

| 機能 | 実装 |
| --- | --- |
| 一覧保持 | `tasksFlow: StateFlow<List<PickingTask>>` |
| 単一タスク監視 | `taskFlow(taskId)` |
| 一覧取得 | `getMyAreaTasks()`, `getAllTasks()` |
| 開始 | `startTask()` |
| 数量更新 | `updatePickingItem()` |
| 完了 | `completeTask()` |
| キャンセル | `cancelPickingItem()` |
| 再取得 | `refreshTask()` |

重要なのは、P20/P21/P22 が同じ `tasksFlow` を監視している点である。これにより、P21 で数量更新した結果が P20 の件数や P22 の履歴に自動反映される。

### 3.4 P20 一覧画面の特徴

`PickingTasksViewModel` / `PickingTasksScreen` の特徴は以下。

1. `warehouse_id` と `picker_id` は `TokenManager` から読む
2. 表示区分は `PENDING`, `ACTIVE`, `COMPLETED`, `SUPPORT`
3. カードタップ時に `POST /api/picking/tasks/{id}/start` を先に呼ぶ
4. 遷移先の `task` 本体は API 詳細取得ではなく `selectedTask` として ViewModel に保持する

この構造は通常出荷では機能するが、横持ち出荷では制約がある。

### 3.5 P21 入力画面の特徴

`OutboundPickingViewModel` / `OutboundPickingScreen` は通常出荷の中心で、以下の前提に立っている。

1. 1 タスク内に複数商品が含まれる
2. 同一商品を `itemId` ごとにグルーピングして 1 ページにする
3. 得意先ごとの CASE / PIECE 入力を持つ
4. 「登録」で各 `wms_picking_item_result_id` に対して複数回 `update` を呼ぶ
5. 全商品が登録済みになると自動で `completeTask()` を呼ぶ

つまり P21 は「複数商品」「複数得意先」「CASE/PIECE の複合入力」に強く依存している。

### 3.6 P22 履歴画面の特徴

`PickingHistoryViewModel` / `PickingHistoryScreen` の特徴は以下。

1. 履歴対象は `status != PENDING` の登録済み明細
2. `cancel` で明細単位の差し戻しができる
3. `confirmAll` で task 全体を確定する
4. 編集時は P21 に `editItemId` を渡して戻る

横持ち出荷 API には履歴一覧 API や明細キャンセル API が存在しないため、この画面構成をそのまま移植する必要はない。

## 4. 横持ち出荷 API の性質

最新仕様から読み取れる前提は以下。

| 観点 | 内容 |
| --- | --- |
| 一覧単位 | `allocation_id` 単位 |
| 商品粒度 | 1 allocation = 1 商品 |
| 詳細取得 | `candidate_locations`, `shortage_detail` を取得するため必須 |
| 入力単位 | `picked_qty` 1 フィールド |
| 数量単位 | `CASE`, `PIECE`, `CARTON` |
| 状態 | `RESERVED`, `PICKING`, 完了時 `FULFILLED`, `SHORTAGE` |
| 完了後レスポンス | `stock_transfer_queue_id` を返す |
| 再送性 | `start`, `update`, `complete` が再送安全 |

## 5. 通常出荷と横持ち出荷の差分

| 観点 | 通常出荷 | 横持ち出荷 |
| --- | --- | --- |
| 元データ | `wms_picking_tasks` | `wms_shortage_allocations` |
| 一覧単位 | 1 task = 複数商品 | 1 allocation = 1 商品 |
| 詳細取得 | 現行 Android は未使用 | 必須 |
| 入力 UI | 得意先別 CASE / PIECE 配分 | 単一 `picked_qty` |
| 履歴画面 | 必要 | API 契約上は不要 |
| 完了後画面 | P20 に戻る | 結果画面で `stock_transfer_queue_id` を見せる必要あり |
| ロケーション | 明細に固定ロケーション | 候補ロケーション配列 |
| JAN | 単一代表 JAN 前提の箇所あり | `jan_codes[]` 全件照合が必要 |
| 完了後の一覧 | 完了タブに残せる | 一覧対象外になるため消える |

## 6. Android 実装への示唆

### 6.1 分離すべきもの

以下は通常出荷から分離する方が安全である。

1. 画面ルート
2. ViewModel State
3. Repository interface
4. Retrofit API interface
5. Network / domain model

理由:

- 通常出荷の `PickingTask` モデルは複数商品タスク前提
- `selectedTask` ベースの画面初期化は、横持ち出荷の詳細 API 必須要件と合わない
- 履歴画面の存在前提が異なる

### 6.2 再利用できるもの

以下は横持ち出荷でも再利用価値が高い。

1. Compose の画面トーンとヘッダーパターン
2. 画面回転制御の実装パターン
3. `JanCodeScannerDialog`
4. `ErrorMapper` / `NetworkException`
5. Repository を StateFlow の単一真実源にする考え方
6. `IncomingRepository.getWarehouses()` を使う倉庫名取得パターン

### 6.3 直接流用しない方がよいもの

1. `OutboundPickingState` の grouped item 構造
2. `PickingHistoryScreen`
3. `PickingTasksViewModel.selectedTask` を使う遷移
4. `TokenManager.shippingDate` を横持ち出荷の API 日付としてそのまま使うこと

## 7. 調査結論

1. 横持ち出荷は通常出荷 API の派生画面ではなく、Android 側でも独立した 3 画面フローとして設計すべきである。
2. ただし UI モジュールは新設せず、`feature/outbound` 配下に `proxyshipment` パッケージを切れば、既存の出荷デザインと実装資産を再利用できる。
3. 最新仕様に合わせるなら、対象ステータスは 2026-04-19 時点で `RESERVED`, `PICKING` で固定する。
4. 日付は通常出荷の共有文字列を流用せず、横持ち出荷側で API 用 ISO 形式を独立管理した方が安全である。
