# P22 履歴画面リデザイン 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`

### 完了済みの作業・現在の状況
- P21（OutboundPickingScreen）は P20 スタイルに統一済み（feature/design-upgrade ブランチ）
- P22（PickingHistoryScreen）は MaterialTheme デフォルト色のまま
- Step 0 分析済み（boot.md に差分一覧あり）

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | UIリデザイン実装 | PickingHistoryScreen.kt を P21スタイルに統一 | ファイル変更完了 |
| P2 | ビルド確認 | assembleDebug でビルド成功 | BUILD SUCCESSFUL |

---

## P1: UIリデザイン実装

### 目的
`PickingHistoryScreen.kt` の本文（ヘッダー以外）を P21 スタイルに統一する。

### 変更禁止
- `TopAppBar`（ヘッダー）: `title = { Text("出庫履歴") }` + 戻るボタン → そのまま
- ViewModel・ロジック・ナビゲーション

### 色定数の追加（ファイル先頭）
以下を import の後に追加する：

```kotlin
// ===== P21/P22 共通カラー =====
private val TitleRed     = Color(0xFFC0392B)
private val AccentOrange = Color(0xFFE67E22)
private val BodyBg       = Color.White
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)
private val BorderGray   = Color(0xFFCCCCCC)
private val ReadonlyText = Color(0xFF888888)
private val BadgeGreen   = Color(0xFF27AE60)
```

### 変更箇所詳細

#### 1. Scaffold
```kotlin
// Before
Scaffold(
    topBar = { ... }

// After
Scaffold(
    containerColor = BodyBg,
    topBar = { ... }
```

#### 2. HistoryListContent — コース情報カード
```kotlin
// Before
Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = state.task.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = "フロア: ...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // 確定済バッジ
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
            Text(text = "確定済み（参照のみ）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, ...)
        }

// After
OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(2.dp, AccentOrange), shape = RoundedCornerShape(12.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = state.task.courseName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
        Text(text = "フロア: ...", fontSize = 14.sp, color = TextSecond)
        // 確定済バッジ
        Surface(color = BadgeGreen, shape = RoundedCornerShape(20.dp)) {
            Text(text = "確定済み（参照のみ）", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
```

#### 3. HistoryItemCard
```kotlin
// Before
Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = item.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider()
        ...
        OutlinedButton(onClick = onDeleteClick, modifier = Modifier.fillMaxWidth()) {
            Text("削除(F3)")
        }

// After
OutlinedCard(modifier = modifier.fillMaxWidth(), border = BorderStroke(1.dp, BorderGray), shape = RoundedCornerShape(12.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = item.itemName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 2.dp))
        ...
        OutlinedButton(onClick = onDeleteClick, modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, AccentOrange)) {
            Text("削除(F3)", color = AccentOrange)
        }
```

#### 4. InfoRow
```kotlin
// Before
Text(text = "$label:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

// After
Text(text = "$label:", fontSize = 13.sp, color = TextSecond)
Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
```

#### 5. StatusBadge
```kotlin
// Before（MaterialTheme色）
PENDING  → error       (赤)
PICKING  → tertiary    (青紫)
COMPLETED → primary    (青)
SHORTAGE → error       (赤)

// After（P21色）
PENDING  → TitleRed    (0xFFC0392B)
PICKING  → AccentOrange (0xFFE67E22)
COMPLETED → BadgeGreen (0xFF27AE60)
SHORTAGE → TitleRed    (0xFFC0392B)
```
Surface の shape も `MaterialTheme.shapes.small` → `RoundedCornerShape(12.dp)` に変更。

#### 6. HistoryBottomBar — 確定ボタン
```kotlin
// Before
Button(onClick = onConfirmAllClick, enabled = canConfirm, modifier = Modifier.widthIn(min = 200.dp)) {
    Text("確定(F4)")
}

// After
Button(onClick = onConfirmAllClick, enabled = canConfirm,
    shape = RoundedCornerShape(6.dp),
    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
    modifier = Modifier.widthIn(min = 200.dp)) {
    Text("確定(F4)")
}
```

#### 7. ReadOnlyModeContent
```kotlin
// Before
Icon(tint = MaterialTheme.colorScheme.primary, ...)
Text("Aコース...", color = MaterialTheme.colorScheme.onSurfaceVariant)
Text("履歴は参照のみ...", color = MaterialTheme.colorScheme.onSurfaceVariant)

// After
Icon(tint = AccentOrange, ...)
Text("Aコース...", fontSize = 16.sp, color = TextSecond)
Text("履歴は参照のみ...", fontSize = 14.sp, color = ReadonlyText)
```

#### 8. 空状態テキスト
```kotlin
// Before
Text(text = "出庫履歴がありません", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

// After
Text(text = "出庫履歴がありません", fontSize = 16.sp, color = ReadonlyText)
```

#### 9. ConfirmAllDialog — 確定ボタン
```kotlin
// Before
Button(onClick = onConfirm, enabled = !isConfirming) {

// After
Button(onClick = onConfirm, enabled = !isConfirming,
    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) {
```

#### 10. DeleteConfirmationDialog — 削除ボタン
```kotlin
// Before
Button(onClick = onConfirm) { Text("削除") }

// After
Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = TitleRed)) { Text("削除") }
```

### 必要な import 追加
```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
```

### 削除できる import（使用されなくなる場合）
- `MaterialTheme.colorScheme` を使わなくなったら関連 import を削除
- ただし import は必要なものだけ削除し、未使用 import は残してもビルドには影響なし

### 完了条件
- PickingHistoryScreen.kt の変更が完了
- ヘッダー TopAppBar は変更されていないこと
- ViewModel・State・ナビゲーション関連コードは変更されていないこと

---

## P2: ビルド確認

### 手順
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :feature:outbound:assembleDebug
```

### 完了条件
- `BUILD SUCCESSFUL` が表示されること
- コンパイルエラーがないこと（deprecation警告は許容）

---

## 制約（厳守）

- `TopAppBar`（ヘッダー）変更禁止
- ViewModel・ロジック・ナビゲーション変更禁止
- `local.properties` はコミットしない
- UIファイル `PickingHistoryScreen.kt` のみ変更対象

## 全体完了条件

- BUILD SUCCESSFUL
- PickingHistoryScreen の色・スタイルが P21 と統一されている
- ヘッダーが変更されていない
