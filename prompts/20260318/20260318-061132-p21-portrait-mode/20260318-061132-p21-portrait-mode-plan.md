# P21 Portrait モード対応 作業計画

## 前提

- P21 OutboundPickingScreen は現在 Landscape 固定の左右2ペインレイアウト
- AndroidManifest.xml で `screenOrientation="landscape"` が設定済み
- OutboundPickingBody（L340-692）が Row で左ペイン（商品情報）・右ペイン（数量入力）を配置
- ユーザーの要望: ボタンで Portrait に切り替え可能にし、縦レイアウト（上3:下7）を提供

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | コンポーネント分離 | 左右ペインを独立 Composable に抽出 | ProductInfoSection, QuantityInputSection が独立し、既存 Landscape レイアウトが変更なく動作 |
| P2 | 向き制御 & 切り替えボタン | requestedOrientation の動的制御 + DataStore 永続化 | TopAppBar に回転ボタン表示、タップで向き切り替え、P21離脱で復元、設定永続化 |
| P3 | Portrait レイアウト | 縦レイアウト（3:7）の実装 | Portrait 時に上部商品情報・下部数量入力が正しく表示 |
| P4 | CompletionCard Portrait 対応 | 完了画面のレイアウト調整 | Portrait 時も CompletionCard が適切に表示 |
| P5 | Preview & 動作確認 | Portrait Preview 追加、ビルド確認 | Portrait 用 Preview が表示、ビルド成功 |

---

## P1: コンポーネント分離

### 目的

`OutboundPickingBody` 内にインラインで書かれている左ペイン（商品情報）と右ペイン（数量入力+ボタン）を、独立した `@Composable` 関数に抽出する。これにより Portrait/Landscape で同じコンポーネントを異なるレイアウトに配置可能になる。

### 修正対象ファイル

- `OutboundPickingScreen.kt`

### 修正方針

#### 1. `ProductInfoSection` の抽出（L358-501 の左ペイン内容）

```kotlin
@Composable
private fun ProductInfoSection(
    currentItem: PickingTaskItem,
    hasImages: Boolean,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // (a) Product info header - 既存の L376-438 のコード
        // (b) Location & Slip number - 既存の L441-498 のコード
    }
}
```

#### 2. `QuantityInputSection` の抽出（L503-691 の右ペイン内容）

```kotlin
@Composable
private fun QuantityInputSection(
    state: OutboundPickingState,
    onPickedQtyChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // (c) ケース & バラ入力 - 既存の L519-635 のコード
        // Spacer + (e) Action buttons - 既存の L637-688 のコード
    }
}
```

#### 3. `OutboundPickingBody` の更新

抽出したコンポーネントを呼び出すように書き換え:

```kotlin
@Composable
private fun OutboundPickingBody(...) {
    Row(modifier = modifier.fillMaxSize().padding(6.dp), ...) {
        Surface(modifier = Modifier.weight(1f).fillMaxHeight(), ...) {
            ProductInfoSection(
                currentItem = currentItem,
                hasImages = state.hasImages,
                onImageClick = onImageClick
            )
        }
        Surface(modifier = Modifier.weight(1f).fillMaxHeight(), ...) {
            QuantityInputSection(
                state = state,
                onPickedQtyChange = onPickedQtyChange,
                onRegisterClick = onRegisterClick,
                onHistoryClick = onHistoryClick
            )
        }
    }
}
```

### 完了条件

- `ProductInfoSection` と `QuantityInputSection` が独立 Composable として存在
- 既存の Landscape レイアウトが変更なく動作（見た目に差異なし）
- ビルドエラーなし

---

## P2: 向き制御 & 切り替えボタン

### 目的

P21 画面でボタンにより Portrait ⇔ Landscape を切り替え可能にし、前回の設定を DataStore に永続化する。

### 修正対象ファイル

- `OutboundPickingScreen.kt`

### 修正方針

#### 1. DataStore による設定永続化

既存の TokenManager が DataStore を使用しているか確認し、同様のパターンで向き設定を永続化する。
簡易的に `SharedPreferences` で実装しても可（DataStore が既に使われていればそれを使用）。

```kotlin
// OutboundPickingScreen.kt 内
private const val PREF_KEY_IS_PORTRAIT = "p21_is_portrait"

@Composable
private fun rememberOrientationPreference(): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("p21_prefs", Context.MODE_PRIVATE)
    return remember {
        mutableStateOf(prefs.getBoolean(PREF_KEY_IS_PORTRAIT, false))
    }
}
```

#### 2. 向き制御ロジック

```kotlin
// OutboundPickingScreen composable 内
val context = LocalContext.current
val activity = context as? Activity
var isPortrait = rememberOrientationPreference()

// 向き適用
LaunchedEffect(isPortrait.value) {
    activity?.requestedOrientation = if (isPortrait.value) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

// P21 離脱時に Landscape に戻す
DisposableEffect(Unit) {
    onDispose {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

fun toggleOrientation() {
    isPortrait.value = !isPortrait.value
    context.getSharedPreferences("p21_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_KEY_IS_PORTRAIT, isPortrait.value).apply()
}
```

#### 3. TopAppBar に切り替えボタン追加

既存の `actions` ブロック（L201-208）に回転ボタンを追加:

```kotlin
actions = {
    // 回転ボタン（新規追加）
    IconButton(onClick = { toggleOrientation() }) {
        Icon(
            imageVector = Icons.Default.ScreenRotation,
            contentDescription = "画面回転",
            tint = AccentOrange
        )
    }
    // ホームボタン（既存）
    IconButton(onClick = onNavigateToMain) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "ホーム(F8)",
            tint = TitleRed
        )
    }
},
```

**注意**: `Icons.Default.ScreenRotation` が Material Icons に含まれていない場合は `Icons.Default.Refresh` や `Icons.Default.RotateRight` 等で代替。

#### 4. `isPortrait` を Body に伝搬

```kotlin
OutboundPickingBody(
    state = state,
    isPortrait = isPortrait.value,
    // ... other params
)
```

### 必要な import 追加

```kotlin
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.ScreenRotation  // or alternative
```

### 完了条件

- TopAppBar 右側に回転アイコンボタンが表示される
- ボタンタップで画面が Portrait ⇔ Landscape に切り替わる
- P21 離脱時（戻る/ホーム）に Landscape に自動復元
- 次回 P21 表示時に前回の向き設定が復元される
- ビルドエラーなし

---

## P3: Portrait レイアウト

### 目的

Portrait 時に `OutboundPickingBody` を縦レイアウト（上部:商品情報 30% / 下部:数量入力 70%）で表示する。

### 修正対象ファイル

- `OutboundPickingScreen.kt`

### 修正方針

`OutboundPickingBody` に `isPortrait` パラメータを追加し、レイアウトを分岐:

```kotlin
@Composable
private fun OutboundPickingBody(
    state: OutboundPickingState,
    isPortrait: Boolean,
    onPickedQtyChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem = state.currentItem!!

    if (isPortrait) {
        // ===== Portrait: 縦レイアウト（3:7） =====
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 上部: 商品情報 (30%)
            Surface(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                ProductInfoSection(
                    currentItem = currentItem,
                    hasImages = state.hasImages,
                    onImageClick = onImageClick
                )
            }
            // 下部: 数量入力 (70%)
            Surface(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                QuantityInputSection(
                    state = state,
                    onPickedQtyChange = onPickedQtyChange,
                    onRegisterClick = onRegisterClick,
                    onHistoryClick = onHistoryClick
                )
            }
        }
    } else {
        // ===== Landscape: 既存の横レイアウト =====
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(modifier = Modifier.weight(1f).fillMaxHeight(), ...) {
                ProductInfoSection(...)
            }
            Surface(modifier = Modifier.weight(1f).fillMaxHeight(), ...) {
                QuantityInputSection(...)
            }
        }
    }
}
```

### Portrait 時の UI 調整

- `ProductInfoSection`: 横幅フルのため、情報が1行に収まりやすい。`verticalScroll` 維持でスクロール対応。
- `QuantityInputSection`: 70%の縦スペースなので余裕あり。ケース/バラ入力は横並び（Row）を維持。
- ボタン: `Spacer(weight(1f))` で下部に押し下げ → Portrait でも同じ配置になる。

### 完了条件

- Portrait 時に上部30%に商品情報、下部70%に数量入力が表示される
- Landscape 時は既存レイアウトと変更なし
- 入力フィールド・ボタンが操作可能
- スクロールが正常に動作

---

## P4: CompletionCard Portrait 対応

### 目的

全アイテム登録完了後に表示される CompletionCard（L241-334）が Portrait 時にも適切に表示されるよう対応する。

### 修正対象ファイル

- `OutboundPickingScreen.kt`

### 修正方針

CompletionCard は `Box(contentAlignment = Alignment.Center)` + `OutlinedCard` でセンタリングされているため、基本的には Portrait でもそのまま表示可能。

ただし以下を確認・調整:
1. `OutlinedCard` の `padding(32.dp)` が Portrait の狭い横幅で収まるか → 必要に応じて縮小
2. ボタンの `Row` 配置が横幅に収まるか → 必要に応じて `Column` に変更
3. `isPortrait` フラグを渡して微調整

```kotlin
// CompletionCard 部分（L241-334）
else -> {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Neutral200),
            modifier = Modifier.widthIn(max = if (isPortrait) 320.dp else Dp.Unspecified)
        ) {
            // 内容は基本同じ — ボタン幅を調整
        }
    }
}
```

### 完了条件

- Portrait 時に CompletionCard が画面内に適切に収まる
- 「確定」「キャンセル」ボタンが操作可能
- Landscape 時の表示に影響なし

---

## P5: Preview & 動作確認

### 目的

Portrait 用の Preview を追加し、ビルドが成功することを確認する。

### 修正対象ファイル

- `OutboundPickingScreen.kt`

### 修正方針

既存の `PreviewOutboundPickingBody`（L919-1018）を参考に、Portrait 用の Preview を追加:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "P21 - Portrait Mode",
    showBackground = true,
    widthDp = 420,
    heightDp = 800
)
@Composable
private fun PreviewOutboundPickingBodyPortrait() {
    // 既存と同じサンプルデータ
    // OutboundPickingBody(isPortrait = true, ...)
}
```

### 確認手順

1. `./gradlew :feature:outbound:compileDebugKotlin` でビルド成功を確認
2. Preview でレイアウトを目視確認
3. 可能であれば実機で以下を確認:
   - 回転ボタンで Portrait ⇔ Landscape が切り替わること
   - 入力データが切り替え時に保持されること
   - P21 離脱で Landscape に戻ること
   - 次回 P21 表示で前回の向きが復元されること

### 完了条件

- Portrait 用 Preview が表示される（widthDp=420, heightDp=800）
- `./gradlew :feature:outbound:compileDebugKotlin` が成功
- Landscape 用 Preview も引き続き正常

---

## 制約（厳守）

1. **P21 限定**: Portrait 対応は OutboundPickingScreen のみ。他画面・AndroidManifest は変更しない
2. **Landscape 変更禁止**: 既存の Landscape レイアウトを変更しない
3. **ViewModel 変更禁止**: OutboundPickingViewModel, OutboundPickingState は変更しない
4. **状態保持**: 向き切り替え時に `pickedQtyInput` と `currentIndex` が失われないこと
5. **離脱時復元**: P21 離脱時に必ず `SCREEN_ORIENTATION_LANDSCAPE` に復元
6. **イマーシブモード維持**: Portrait でもステータスバー/ナビバー非表示を維持

## 全体完了条件

- P21 で回転ボタンによる Portrait ⇔ Landscape 切り替えが動作する
- Portrait レイアウト: 上部30%商品情報 / 下部70%数量入力
- 前回の向き設定が DataStore/SharedPreferences に保存・復元される
- CompletionCard が Portrait 時も正しく表示される
- P21 離脱で Landscape に復元される
- 他画面に影響がない
- ビルド成功
