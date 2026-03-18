# Work Plan: completed-tasks-list

- **ID**: completed-tasks-list
- **作成日**: 2026-03-18
- **最終更新**: 2026-03-18
- **ステータス**: 完了
- **ディレクトリ**: prompts/20260318/20260318-102122-completed-tasks-list/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（20260318-102122-completed-tasks-list-boot.md）
2. 20260318-102122-completed-tasks-list-plan.md を読む（作業計画の全体像）
3. 下記「進捗」テーブルで現在のPhaseを確認
4. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
5. 「作業中コンテキスト」セクションで途中データを確認
6. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P20 コース選択画面にタブ切替を導入し、「作業中」タブと「完了」タブを分離。完了タスクをタップで P21/P22 に遷移して修正可能にする。作業完了後は P20 に自動で戻る。

## 重要な設計制約

- API 変更なし（クライアント側のみ）
- 既存の作業中タスクの動作に影響を与えないこと
- 完了タスクの修正は既存の cancel → re-register フローを使用
- 完了リストはコース名順でソート
- タブ構成: 将来「応援リスト」タブ追加予定のため拡張性を考慮

## 対象ファイル

### 新規作成
なし

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt`

### 参照のみ（変更禁止）
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: State 変更 | 完了 | 2026-03-18 | 既に実装済み（TaskTab, selectedTab, completedTasks） |
| P2: ViewModel 変更 | 完了 | 2026-03-18 | 既に実装済み（フィルタリング、selectTab） |
| P3: Screen タブ UI | 完了 | 2026-03-18 | TabRow追加、作業中/完了タブ切替、件数バッジ表示 |
| P4: 完了後 P20 復帰 | 完了 | 2026-03-18 | onTaskCompletedでP20まで直接popBack |
| P5: ビルド確認 & Preview | 完了 | 2026-03-18 | 全モジュールビルド成功 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 設計決定（P1 開始前）
- タブ構成: 「作業中」「完了」の2タブ（将来「応援」タブ追加予定）
- 完了リスト並び順: コース名順
- 完了タスクタップ時: 既存の selectTask ナビゲーションロジックで P21 に遷移（isFullyProcessed → onNavigateToDataInput）
- 作業完了後: P20 に自動復帰

### Git ブランチ
- 作業ブランチ: feature/design-upgrade（既存）
- ベースブランチ: master

---

## Phase完了記録

### P1: State 変更
- 完了日: 2026-03-18
- 実績:
  - 既に実装済み（TaskTab enum, selectedTab, completedTasks in Success）

### P2: ViewModel 変更
- 完了日: 2026-03-18
- 実績:
  - 既に実装済み（observeRepositoryTasks/loadMyAreaTasksでフィルタリング、selectTabメソッド）

### P3: Screen タブ UI
- 完了日: 2026-03-18
- 実績:
  - TabRow + Tab追加（作業中/完了、件数バッジ付き）
  - タブ選択に応じたリスト切替
  - TabRowDefaults.SecondaryIndicator使用

### P4: 完了後 P20 復帰
- 完了日: 2026-03-18
- 実績:
  - onTaskCompletedでpopBackStack(Routes.PickingList.route, inclusive = false)に変更

### P5: ビルド確認 & Preview
- 完了日: 2026-03-18
- 実績:
  - feature:outbound, app 両モジュールビルド成功
