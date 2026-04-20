# P20 完了タスク表示 & タブ切替 作業計画

## 前提

- P20（PickingTasksScreen）は現在 `isFullyProcessed` タスクをフィルタで除外
- 完了タスクのカード色分け（グレー）は既に `courseCardColors` に定義済み
- ナビゲーションロジックで `isFullyProcessed` → `onNavigateToDataInput` は既に実装済み
- 将来「応援リスト」タブ追加予定のためタブ構成で実装

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | State 変更 | TaskListState.Success に completedTasks 追加、タブ状態追加 | ビルド成功 |
| P2 | ViewModel 変更 | フィルタリング変更、タブ切替メソッド | ビルド成功 |
| P3 | Screen タブ UI | タブ切替 UI + 完了リスト表示 | ビルド成功 |
| P4 | 完了後 P20 復帰 | P22 確定完了後に P20 に戻る | ビルド成功 |
| P5 | ビルド確認 & Preview | 全体ビルド、Preview 更新 | ビルドエラーなし |

---

## P1: State 変更

### 目的

`TaskListState.Success` に完了タスクリストを追加し、タブの選択状態を管理する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksState.kt`

### 修正方針

1. `TaskListState.Success` に `completedTasks: List<PickingTask>` を追加

```kotlin
data class Success(
    val tasks: List<PickingTask>,
    val completedTasks: List<PickingTask> = emptyList()
) : TaskListState
```

2. `PickingTasksState` にタブ選択状態を追加

```kotlin
data class PickingTasksState(
    val tasksState: TaskListState = TaskListState.Loading,
    val errorMessage: String? = null,
    val selectedTask: PickingTask? = null,
    val warehouseName: String = "",
    val selectedTab: TaskTab = TaskTab.ACTIVE
)

enum class TaskTab { ACTIVE, COMPLETED }
```

### 完了条件

- ビルド成功（コンパイルエラーなし）

---

## P2: ViewModel 変更

### 目的

フィルタリングロジックを変更して完了タスクも保持し、タブ切替メソッドを追加する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksViewModel.kt`

### 修正方針

1. `observeRepositoryTasks()` のフィルタリングを変更:

```kotlin
val activeTasks = tasks.filter { !it.isFullyProcessed }
val completedTasks = tasks.filter { it.isFullyProcessed }
    .sortedBy { it.courseName }  // コース名順

val newState = if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
    TaskListState.Empty
} else {
    TaskListState.Success(activeTasks, completedTasks)
}
```

2. `loadMyAreaTasks()` も同様に変更

3. タブ切替メソッド追加:

```kotlin
fun selectTab(tab: TaskTab) {
    _state.update { it.copy(selectedTab = tab) }
}
```

### 完了条件

- ビルド成功

---

## P3: Screen タブ UI

### 目的

P20 にタブ切替 UI を追加し、「作業中」と「完了」を切り替えて表示する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt`

### 修正方針

1. ヘッダー下にタブバーを追加:
   - 「作業中」タブ（件数バッジ付き）
   - 「完了」タブ（件数バッジ付き）
   - Material3 `TabRow` + `Tab` を使用

2. タブ選択に応じてリスト表示を切替:
   - `ACTIVE` タブ: 既存の `activeTasks` を表示
   - `COMPLETED` タブ: `completedTasks` を表示（コース名順）

3. 完了タブのカード:
   - 既存の `courseCardColors(task)` でグレー表示（`isFullyProcessed` 判定済み）
   - タップで `selectTask()` → 既存ナビゲーションロジックで P21 に遷移

4. PickingTasksScreen composable に `onSelectTab` コールバック追加

### 完了条件

- ビルド成功
- 作業中タブ: 既存と同じ表示
- 完了タブ: 完了タスクがグレーカードで表示される

---

## P4: 完了後 P20 復帰

### 目的

P22 で全アイテム確定後、P20（コース選択画面）に自動で戻るようにする。

### 修正対象ファイル

- `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/navigation/HandyNavHost.kt`

### 修正方針

`PickingHistoryScreen` の `onHistoryConfirmed` コールバックを確認。現在:

```kotlin
onHistoryConfirmed = {
    restoreLandscape()
    pickingTasksViewModel.clearSelectedTask()
    pickingTasksViewModel.refresh()
    navController.popBackStack()
}
```

これは P22 → P21 に popBack し、P21 でタスクが完了状態のため完了メッセージ表示。
P20 まで戻すには `popBackStack(Routes.PickingList.route, inclusive = false)` を使用。

```kotlin
onHistoryConfirmed = {
    restoreLandscape()
    pickingTasksViewModel.clearSelectedTask()
    pickingTasksViewModel.refresh()
    navController.popBackStack(Routes.PickingList.route, inclusive = false)
}
```

### 完了条件

- P22 で確定後、P20 に直接戻る
- P20 で完了タブにタスクが表示される

---

## P5: ビルド確認 & Preview

### 目的

全体ビルドが成功し、Preview が正しく表示されることを確認する。

### 修正対象ファイル

- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt`（Preview 更新）

### 修正方針

1. 全モジュールビルド: `./gradlew :feature:outbound:compileDebugKotlin :app:compileDebugKotlin`
2. Preview に完了タスクを含むモックデータを追加
3. タブ UI の Preview 追加

### 完了条件

- ビルドエラーなし
- Preview が正常表示

---

## 制約（厳守）

- API 変更なし（クライアント側フィルタリングのみ）
- 既存の作業中タスク動作に影響を与えない
- `PickingTask` ドメインモデルは変更禁止（参照のみ）
- 完了リストはコース名順ソート

## 全体完了条件

1. P20 に「作業中」「完了」タブが表示される
2. 完了タブに完了タスクがグレーカードで表示される
3. 完了タスクタップで P21 に遷移できる
4. P22 確定完了後に P20 に自動復帰する
5. 全モジュールビルド成功
