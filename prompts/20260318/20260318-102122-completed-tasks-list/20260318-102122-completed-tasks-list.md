# P20 コース選択画面 — 完了タスク表示 & 修正機能

- **作成日**: 2026-03-18
- **ステータス**: ドラフト
- **ディレクトリ**: prompts/20260318/20260318-102122-completed-tasks-list/

## 背景・目的

現在、P20（コース選択画面 = PickingTasksScreen）では `isFullyProcessed` のタスク（全アイテムが COMPLETED/SHORTAGE）をフィルタで除外しており、作業完了後にリストから消えてしまう。

**問題点:**
1. 完了したタスクの確認ができない
2. 完了後に出荷数量の修正が必要になった場合、再アクセスする手段がない

**目的:**
- 完了タスクを「完了リスト」として別セクションに表示する
- 完了リストからタスクをタップして P21（データ入力画面）に遷移し、修正できるようにする

## 現状の実装

### フィルタリングロジック（PickingTasksViewModel.kt）

```kotlin
// Line 66, 154: isFullyProcessed のタスクを除外
val activeTasks = tasks.filter { !it.isFullyProcessed }
```

### PickingTask ドメインモデル（PickingTask.kt）

```kotlin
val isFullyProcessed: Boolean
    get() = pendingCount == 0 && pickingCount == 0 && completedOrShortageCount == totalItems
```

### タスク選択ナビゲーション（PickingTasksViewModel.kt:197-214）

```kotlin
when {
    task.hasUnregisteredItems -> onNavigateToDataInput(task)   // PENDING → P21
    task.hasPickingItems -> onNavigateToHistory(task)           // PICKING → P22
    task.isFullyProcessed -> onNavigateToDataInput(task)       // 完了 → P21（完了メッセージ表示）
}
```

### カード色分け（PickingTasksScreen.kt:74-96）

- **未着手**: 黄色（`#FFFDE7` / `#F9A825`）
- **作業中**: 緑色（`#E8F5E9` / `#4CAF50`）
- **完了**: グレー（`#F5F5F5` / `#BDBDBD`）— 現在は表示されない

### State（PickingTasksState.kt）

```kotlin
data class PickingTasksState(
    val tasksState: TaskListState = TaskListState.Loading,
    val errorMessage: String? = null,
    val selectedTask: PickingTask? = null,
    val warehouseName: String = ""
)

sealed interface TaskListState {
    data object Loading : TaskListState
    data object Empty : TaskListState
    data class Success(val tasks: List<PickingTask>) : TaskListState
    data class Error(val message: String) : TaskListState
}
```

## 変更内容

### 概要

P20 のタスクリストを「作業中リスト」と「完了リスト」の2セクションに分割。完了タスクはグレーカードで表示し、タップで P21 に遷移して修正可能にする。

### 詳細設計

#### DB変更

なし（API・ドメインモデルの変更不要）

#### モデル変更

**PickingTasksState.kt** — `TaskListState.Success` に `completedTasks` を追加:

```kotlin
data class Success(
    val tasks: List<PickingTask>,           // 作業中タスク（!isFullyProcessed）
    val completedTasks: List<PickingTask>   // 完了タスク（isFullyProcessed）
) : TaskListState
```

#### ViewModel 変更

**PickingTasksViewModel.kt** — フィルタリングロジックを変更:

```kotlin
// Before:
val activeTasks = tasks.filter { !it.isFullyProcessed }

// After:
val activeTasks = tasks.filter { !it.isFullyProcessed }
val completedTasks = tasks.filter { it.isFullyProcessed }
val newState = if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
    TaskListState.Empty
} else {
    TaskListState.Success(activeTasks, completedTasks)
}
```

- `observeRepositoryTasks()` と `loadMyAreaTasks()` の両方を更新
- タスク選択ナビゲーション: 完了タスクタップ時も `onNavigateToDataInput(task)` で P21 に遷移（既存ロジックで対応済み）

#### UI変更

**PickingTasksScreen.kt** — リスト表示を2セクションに分割:

1. **作業中セクション**（既存のリスト）
   - セクションヘッダー: 「作業中」ラベル + 件数バッジ
   - 未着手（黄色）・作業中（緑色）のカードを表示

2. **完了セクション**（新規）
   - セクションヘッダー: 「完了」ラベル + 件数バッジ（グレー）
   - 完了（グレー）カードを表示
   - カードに「完了」バッジ表示（既存の `courseCardColors` で対応済み）
   - タップで P21 に遷移

### 影響範囲

- `PickingTasksState.kt` — `TaskListState.Success` のプロパティ追加
- `PickingTasksViewModel.kt` — フィルタリングロジック変更（2箇所）
- `PickingTasksScreen.kt` — リスト表示の2セクション化
- `OutboundPickingScreen.kt` — 完了タスクから遷移した場合の修正フロー（既存の完了メッセージ表示から修正可能に変更が必要な場合）

## 制約

- API変更なし（クライアント側のフィルタリング変更のみ）
- 完了タスクの修正可否はサーバー側のステータス管理に依存（COMPLETED/SHORTAGE → PICKING に戻せるかは API 仕様次第）
- 既存の作業中タスクの動作に影響を与えないこと

## 対象ファイル

### 新規作成
なし

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt`

### 参照のみ
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

## 確認事項

1. **修正フロー**: 完了タスクをタップして P21 に遷移した場合、現在は「作業が完了しました」メッセージが表示されるだけ。ここから数量修正を可能にするには、P21 側で完了タスクの再編集 UI を追加する必要があるか？
作業完了したあと、P20にもどるのはどうか？
2. **API 制約**: COMPLETED/SHORTAGE ステータスのアイテムに対して `/update` API を呼べるか？呼べない場合は先に `/cancel` で PENDING に戻す必要がある？
   COMPLETED/SHORTAGE　捨てたーすのアイテムに変更可能にする。また、出荷確定になった物に関してはAPIで対応できないように修正する。これはAPI側の対応をまとめて欲しい。対応を進める。
3. **完了リストの並び順**: コース名順？完了日時順？
コース名基準
4. **完了リストの折りたたみ**: 完了リストはデフォルト展開？折りたたみ可能にするか？
タブを分けたい。今後完了リストとは別に応援リスト（他のピッカーの作業リスト）を用意する予定。