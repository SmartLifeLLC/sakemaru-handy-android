# 配送コース選択画面 UIリデザイン 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`（本タスクでのAPI使用なし）
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- タスク詳細: `prompts/P20-Redesign-0223/P20-Redesign-0223.md`

### 完了済みの作業・現在の状況
- コードベース確認済み（PickingTasksScreen.kt / PickingTask model / PickingTasksViewModel）
- 現在の実装: Compose, 2カラムグリッド既存, MaterialThemeベースの色設定
- 倉庫名: ViewModel非公開・要調査（P1で解決）

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | ナビゲーション確認・倉庫名調査 | ヘッダーの倉庫名取得方法の特定 | 倉庫名の取得方針が決定 |
| P2 | UIリデザイン実装 | PickingTasksScreen.kt の変更 | 全デザイン仕様が実装済み |
| P3 | ビルド確認 | Gradle ビルド成功確認 | ビルドエラーなし |

---

## P1: ナビゲーション確認・倉庫名調査

### 目的
ヘッダーに表示する倉庫名の取得手段を確認する。
PickingTasksViewModel には倉庫名が公開されておらず、代替手段を特定する。

### 調査手順

1. **ナビゲーション引数の確認**
   - `app/src/main/java/.../navigation/` 配下のナビゲーション定義を読む
   - `PickingTasksScreen` の呼び出し箇所を探し、引数として渡せるか確認

   ```bash
   # 呼び出し箇所の検索
   grep -r "PickingTasksScreen" --include="*.kt" .
   grep -r "picking_list" --include="*.kt" .
   ```

2. **MainScreen での倉庫名確認**
   - `feature/main/.../MainScreen.kt` を読み、倉庫名の保持方法を確認
   - MainViewModel で倉庫名を持っているか確認

3. **倉庫マスタキャッシュの確認**
   - WarehouseRepository やローカルキャッシュに倉庫名があるか確認

### 採用方針の決定（いずれか選択）

| 選択肢 | 方法 | ViewModel変更 |
|--------|------|--------------|
| A | ナビゲーション引数で倉庫名を渡す | 不要（推奨） |
| B | PickingTasksState に warehouseName 追加 | **禁止（不可）** |
| C | 倉庫名なし（ヘッダーに倉庫名未表示） | 不要 |
| D | Hilt で Repository を Screen に直接注入 | 不要（非推奨だが可） |

> **注意**: ViewModel の変更は絶対禁止。選択肢 A または C が現実的。

### 完了条件
- 倉庫名の取得方針が決定している

---

## P2: UIリデザイン実装

### 目的・変更内容
`PickingTasksScreen.kt` のみを変更し、以下のデザインを実現する。

### 修正対象ファイル
```
feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/tasks/PickingTasksScreen.kt
```

### 実装仕様

#### Step 1: 画面全体の背景色
```kotlin
// Scaffold の containerColor を設定
Scaffold(
    containerColor = Color(0xFFFDFBF2),  // クリーム/薄ベージュ
    ...
)
```

#### Step 2: ヘッダーデザイン

```
[←]  [🚚 アイコン]  [配送コース選択]  [│]  [倉庫名]
```

```kotlin
TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.White,  // または透明
    ),
    navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = Color(0xFFC0392B),  // 주황빛 적색
                modifier = Modifier.size(24.dp)
            )
        }
    },
    title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalShipping,  // 🚚 トラックアイコン
                contentDescription = null,
                tint = Color(0xFFE67E22),  // 주황
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "配送コース選択",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC0392B)
            )
            // 倉庫名が取得できる場合のみ表示
            // ｜区切り + 倉庫名
            if (warehouseName != null) {
                Text(
                    text = " │ ",
                    color = Color(0xFFCCCCCC),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Text(
                    text = warehouseName,
                    fontSize = 16.sp,
                    color = Color(0xFFE67E22)
                )
            }
        }
    }
)
```

> **TopAppBar の下線除去**: `WindowInsets` の調整や `Divider` を追加しないことで自然に除去。
> Material3 の TopAppBar はデフォルトで shadow/border なし。

#### Step 3: 案内文言エリア

カードリストの上に案内テキストを追加（既存テキストがあれば維持、なければ空でも可）。

```kotlin
Text(
    text = "配送コースを選択してください",  // 既存文言があれば維持
    fontSize = 14.sp,
    color = Color(0xFF555555),
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp, start = 16.dp, bottom = 12.dp)
)
```

#### Step 4: カードグリッドレイアウト（既存実装を維持・調整）

現在すでに `LazyVerticalGrid(columns = GridCells.Fixed(2))` が実装済み。
パディング・スペーシングを仕様に合わせて調整する。

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
)
```

> **홀수(奇数)カード対応**: LazyVerticalGrid は自動的に最後のカードを左に配置する。
> ただし `span` 指定が必要な場合はカード数が奇数の時に `GridItemSpan(1)` で対応。

#### Step 5 & 6: カードデザイン（CourseCardColors 実装）

```kotlin
data class CourseCardColors(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val backgroundPressed: Color,
    val borderPressed: Color
)

fun courseCardColors(task: PickingTask): CourseCardColors = when {
    task.isCompleted -> CourseCardColors(
        background      = Color(0xFFF5F5F5),
        border          = Color(0xFFBDBDBD),
        titleColor      = Color(0xFF757575),
        backgroundPressed = Color(0xFFEEEEEE),
        borderPressed   = Color(0xFF9E9E9E)
    )
    task.isInProgress -> CourseCardColors(
        background      = Color(0xFFE8F5E9),
        border          = Color(0xFF4CAF50),
        titleColor      = Color(0xFF2E7D32),
        backgroundPressed = Color(0xFFC8E6C9),
        borderPressed   = Color(0xFF388E3C)
    )
    else -> CourseCardColors(  // 未着手
        background      = Color(0xFFFFFDE7),
        border          = Color(0xFFF9A825),
        titleColor      = Color(0xFFE67E22),
        backgroundPressed = Color(0xFFFFF9C4),
        borderPressed   = Color(0xFFF57F17)
    )
}
```

#### Step 5: カード共通仕様

```kotlin
@Composable
private fun PickingTaskCard(
    task: PickingTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = courseCardColors(task)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed) colors.backgroundPressed else colors.background
        ),
        border = BorderStroke(
            2.dp,
            if (isPressed) colors.borderPressed else colors.border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {},  // ← 無効化（clickable で制御）
    ) {
        // Card を clickable で包む（interactionSource を渡す）
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // [1行] 🚚 アイコン + コース名
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = colors.titleColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = task.courseName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // [2行] エリア説明
            Text(
                text = task.pickingAreaName,
                fontSize = 13.sp,
                color = Color(0xFF555555)
            )
            // [3行] 出荷指示: X件　検品済: X件
            Text(
                text = "出荷指示: ${task.totalItems}件　検品済: ${task.registeredCount}件",
                fontSize = 13.sp,
                color = Color(0xFF555555)
            )
        }
    }
}
```

> **注意**: `Card` の `onClick` と外の `clickable` を二重にしないこと。
> `Card(onClick = onClick)` の場合は `interactionSource` を `Card` に渡す方法も検討。
> 実装は `Card(onClick = onClick, interactionSource = interactionSource)` でもよい。

### 修正禁止箇所（厳守）

```
□ onTaskClick → onNavigateToDataInput / onNavigateToHistory のロジック
□ ViewModel の selectTask() / refresh() / loadMyAreaTasks()
□ PickingTasksState / TaskListState
□ ナビゲーション定義
□ PickingTask ドメインモデル
```

### 完了条件
- PickingTasksScreen.kt のみ変更されている
- 背景色 #FDFBF2 が全画面に適用されている
- ヘッダーに🚚アイコン + 「配送コース選択」が赤橙色で表示される
- 3状態カード（未着手/着手中/着手完了）が正しい色で表示される
- カードタップ → 既存の選択処理が正常動作する
- ビルドエラーがない

---

## P3: ビルド確認

### 目的
変更後のコードがビルドエラーなしでコンパイルされることを確認する。

### 手順

1. Gradle ビルドを実行:
   ```bash
   ./gradlew :feature:outbound:assembleDebug
   ```
   または Android Studio の Build → Make Project

2. エラーがある場合:
   - 未解決のインポートがないか確認
   - `Color`, `BorderStroke`, `MutableInteractionSource` 等の import が揃っているか確認
   - `Icons.Filled.LocalShipping` が存在するか確認（代替: `Icons.Outlined.LocalShipping` / 絵文字テキスト）

3. エラーなし → 完了

### 完了条件
- `./gradlew :feature:outbound:assembleDebug` がエラーなし終了

---

## 制約（厳守）

- ViewModel・State・ドメインモデル・ナビゲーション定義は変更しない
- 変更対象は `PickingTasksScreen.kt` 1ファイルのみ
- `local.properties` の認証情報はコミットしない
- 新規画面の追加はないため `pages.md` の更新不要

---

## 全体完了条件

- PickingTasksScreen.kt のみに変更が加えられている
- デザイン仕様（Step 1〜6）が全て実装されている
- ビルド成功（エラーなし）
- カードタップの既存動作が維持されている
