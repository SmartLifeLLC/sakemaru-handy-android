# Android Plan: P20-Redesign-0223

- **ID**: P20-Redesign-0223
- **作成日**: 2026-02-23
- **最終更新**: 2026-02-23
- **ステータス**: 完了
- **ディレクトリ**: prompts/P20-Redesign-0223/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 「作業中コンテキスト」セクションで途中データを確認
7. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

---

## 概要

P20（配送コース選択画面 = PickingTasksScreen）のUIリデザイン。
**ロジック・API・ViewModel・ナビゲーションは一切変更しない。UIファイルのみ変更。**

---

## 使用API

なし（純粋なUIリデザイン。APIへのアクセスなし）

---

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi, portrait対応
- API Level 33
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold + TopAppBar + （FunctionKeyBarは本画面では未使用）
- **ViewModel・ロジック・ナビゲーション変更禁止**
- UIファイルのみ変更対象

---

## コードベース確認済み内容

### UI技術
- Compose（XMLではない）

### 対象ファイル
- UI: `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt`
- State: `PickingTasksState.kt` ← 変更禁止
- ViewModel: `PickingTasksViewModel.kt` ← 変更禁止

### カードデータモデルフィールド（PickingTask）
- コース名フィールド: `task.courseName`
- エリア説明フィールド: `task.pickingAreaName`
- 出荷指示件数フィールド: `task.totalItems`（`items.size`）
- 検品済件数フィールド: `task.registeredCount`（PENDINGでないアイテム数）
- 進捗テキスト: `task.progressText` = "$registeredCount/$totalItems"

### 状態判定（PickingTask の legacy プロパティ）
- 미착수（未着手）: `!task.isCompleted && !task.isInProgress`
- 착수중（着手中）: `task.isInProgress`
- 착수완료（着手完了）: `task.isCompleted`

### 倉庫名表示の問題点（解決済み）
- PickingTasksViewModel は倉庫名を State に公開していない
- ナビゲーション定義変更も禁止のため、倉庫名はヘッダーに非表示とした

---

## 対象ファイル

### 既存変更（1ファイルのみ）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt` ✅

### 参照のみ（変更禁止 / 変更なし確認済み）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksViewModel.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`

---

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照
（本タスクはAPIアクセスなし）

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: コードベース確認（倉庫名問題解決） | 完了 | 2026-02-23 | 倉庫名非表示と決定 |
| P2: UIリデザイン実装 | 完了 | 2026-02-23 | PickingTasksScreen.kt 変更完了 |
| P3: ビルド確認 | 完了 | 2026-02-23 | BUILD SUCCESSFUL (16s) |

---

## 作業中コンテキスト

### 倉庫名取得手段（P1 完了後に記入）
- 調査結果: PickingTasksScreen はナビゲーション引数なし。TokenManager も倉庫名なし（IDのみ）。ナビゲーション定義変更禁止。
- 採用方針: 倉庫名はヘッダーに非表示（選択肢C）

### ビルド状態（P3 完了後に記入）
- ビルド結果: BUILD SUCCESSFUL in 16s
- 警告: `hiltViewModel` deprecated 警告（既存コードからの継承、エラーなし）

### Git ブランチ
- 作業ブランチ: `feature/design-upgrade`
- ベースブランチ: `master`

---

## Phase完了記録

### P1: コードベース確認（倉庫名問題解決）
- 完了日: 2026-02-23
- 実績:
  - HandyNavHost.kt を確認。PickingTasksScreen への引数はナビゲーション引数なし
  - PickingTasksViewModel / TokenManager に倉庫名なし（warehouseId のみ）
  - ナビゲーション定義変更禁止のため、倉庫名非表示（選択肢C）で決定

### P2: UIリデザイン実装
- 完了日: 2026-02-23
- 実績:
  - `PickingTasksScreen.kt` 1ファイルのみ変更
  - 背景色: `BackgroundCream = Color(0xFFFDFBF2)` を Scaffold に適用
  - ヘッダー: 白背景 + ArrowBack(赤) + LocalShipping アイコン(橙) + 「配送コース選択」(赤橙・18sp Bold)
  - カードデザイン: `CourseCardColors` data class + `courseCardColors()` 関数で3状態を実装
  - カード: `OutlinedCard` + `BorderStroke(2.dp)` + `RoundedCornerShape(16.dp)` + `defaultMinSize(120.dp)` + `elevation 2dp`
  - カード内: [🚚+コース名] [エリア説明] [出荷指示:X件　検品済:X件] の3行構成
  - インタラクション: `MutableInteractionSource` + `collectIsPressedAsState()` でpress時の色変化
  - 案内文言: カードリスト上部に「配送コースを選択してください」
  - StatusChip は削除（カードデザインで状態を色で表現するため不要）

### P3: ビルド確認
- 完了日: 2026-02-23
- 実績:
  - `./gradlew :feature:outbound:assembleDebug` → BUILD SUCCESSFUL in 16s
  - コンパイルエラーなし
  - 警告: `hiltViewModel` deprecated（既存コードから継承、問題なし）
