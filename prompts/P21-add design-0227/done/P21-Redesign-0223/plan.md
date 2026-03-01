# P21 出庫データ入力 UIリデザイン 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`

### 完了済みの作業・現在の状況
- Step 0 完了: 現行実装の調査済み（boot.md に記録）
- 変更対象: `OutboundPickingScreen.kt` のみ（1ファイル）
- ViewModel・ロジック・ナビゲーション変更禁止

---

## Step 0 調査結果サマリー（参照用）

### 現行レイアウト構造
```
Scaffold(
  TopAppBar("出庫データ入力")  ← 標準スタイル、装飾なし
  BottomBar(商品の画像 | コース変更 | 履歴 | 前へ | 登録 | 次へ)
  Body → Row {
    左カラム (weight=1): CourseHeaderCard + ItemInformationCard
    右カラム (weight=1): QuantityInputCard
  }
)
```

### 実装済みデータフィールド（利用可能）
| フィールド | 変数 |
|-----------|------|
| コース名 | `state.task?.courseName` |
| エリア名 | `state.task?.pickingAreaName` |
| 進捗 | `state.registeredCount` / `state.totalCount` |
| 商品名 | `state.currentItem?.itemName` |
| 商品規格 | `.volume`, `.capacityCase`, `.janCode`, `.slipNumber` |
| 出庫数量（入力） | `state.pickedQtyInput` |
| 出荷数量（受注） | `state.currentItem?.plannedQty` |
| 数量タイプ | `state.quantityTypeLabel` |
| 更新中 | `state.isUpdating` |
| 登録可否 | `state.canRegister` |
| 画像あり | `state.hasImages` |

### 未実装（UIに追加しない）
- バーコード入力・スキャン
- 거래처 드롭다운
- ロケーション表示
- 履歴パネル
- 取消ボタン
- ヘッダーのコース名バッジ・日付・設定アイコン

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | UIリデザイン実装 | OutboundPickingScreen.kt の全面書き換え | コンパイルエラーなし、既存ロジック維持 |
| P2 | ビルド確認 | `assembleDebug` でエラーなし確認 | BUILD SUCCESSFUL |

---

## P1: UIリデザイン実装

### 目的
P20（配送コース選択）と同一の色・ヘッダースタイルを `OutboundPickingScreen.kt` に適用する。
ロジック・API・ViewModel・ナビゲーションは一切変更しない。

### 色定数（P20と共通）
```kotlin
private val TitleRed     = Color(0xFFC0392B)   // タイトル・アイコン
private val AccentOrange = Color(0xFFE67E22)   // 強調色
private val DividerGold  = Color(0xFFF9A825)   // ヘッダー下仕切り
private val BodyBg       = Color.White          // ボディ背景
private val HeaderBg     = Color(0xFFFDFBF2)   // ヘッダー背景（クリーム）
private val TextPrimary  = Color(0xFF212529)   // 本文テキスト
private val TextSecond   = Color(0xFF555555)   // 補助テキスト
private val BorderGray   = Color(0xFFCCCCCC)   // 通常ボーダー
private val ReadonlyBg   = Color(0xFFF5F5F5)   // 読み取り専用フィールド背景
private val ReadonlyText = Color(0xFF888888)   // 読み取り専用テキスト
```

### 1-A. ヘッダー（TopAppBar）

変更前:
```kotlin
TopAppBar(
    title = { Text("出庫データ入力") },
    navigationIcon = { /* ArrowBack */ },
    actions = { /* Home */ }
)
```

変更後:
```kotlin
Column {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Inventory2,  // 📦
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "出庫",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleRed
                )
                // コース名・日付・倉庫名は未実装なので表示しない
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る",
                    tint = TitleRed
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToMain) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "ホーム",
                    tint = TitleRed
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HeaderBg
        )
    )
    HorizontalDivider(thickness = 2.dp, color = DividerGold)
}
```

### 1-B. ボディ背景

`OutboundPickingContent` の Modifier に背景色を追加:
```kotlin
modifier = modifier
    .fillMaxSize()
    .background(BodyBg)
```

### 1-C. CourseHeaderCard のリデザイン

現行: 標準 `Card` (Material テーマ色)
変更後:

```kotlin
@Composable
private fun CourseHeaderCard(...) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, AccentOrange),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), ...) {
            // Row: コース名ラベル左 / 進捗カウンタ右
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 「コース」ラベル
                Text(
                    text = "コース",
                    fontSize = 12.sp,
                    color = TextSecond
                )
                // 進捗バッジ: "5 / 10"
                Surface(
                    color = AccentOrange,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$registeredCount / $totalCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // コース名（大）
            Text(
                text = courseName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TitleRed
            )
            // エリア名（小）
            Text(
                text = pickingAreaName,
                fontSize = 13.sp,
                color = TextSecond
            )
            Spacer(Modifier.height(8.dp))
            // プログレスバー（オレンジ）
            LinearProgressIndicator(
                progress = { if (totalCount > 0) registeredCount.toFloat() / totalCount.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = AccentOrange,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}
```

### 1-D. ItemInformationCard のリデザイン

現行: 標準 `Card`
変更後:

```kotlin
@Composable
private fun ItemInformationCard(...) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, BorderGray),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), ...) {
            // 商品名（太字・大）
            Text(
                text = item.itemName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 8.dp))
            // 各 InfoRow: ラベルは TextSecond、値は TextPrimary
            InfoRow(label = "伝票番号", value = slipNumber)
            if (item.volume != null) InfoRow(label = "容量", value = item.volume!!)
            else InfoRow(label = "容量", value = "—")
            if (item.capacityCase != null) InfoRow(label = "入数", value = "${item.capacityCase} 個/ケース")
            else InfoRow(label = "入数", value = "—")
            if (item.janCode != null) InfoRow(label = "JAN", value = item.janCode!!)
            else InfoRow(label = "JAN", value = "—")
        }
    }
}
```

`InfoRow` も色を更新:
```kotlin
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$label:", fontSize = 13.sp, color = TextSecond)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
```

### 1-E. QuantityInputCard のリデザイン

現行: Card + OutlinedCard + OutlinedTextField
変更後:

```kotlin
@Composable
private fun QuantityInputCard(...) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, AccentOrange),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // タイトル「数量」
            Text(text = "数量", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleRed)

            // --- 出荷数量（受注・読み取り専用）---
            Column {
                Text(text = "出荷数量（受注）", fontSize = 12.sp, color = TextSecond)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReadonlyBg, RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = String.format("%.1f %s", plannedQty, quantityType),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReadonlyText
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // --- 出庫数量（入力）---
            Column {
                Text(text = "出庫数量", fontSize = 12.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = pickedQtyInput,
                    onValueChange = onPickedQtyChange,
                    label = { Text("出庫数量 ($quantityType)", color = AccentOrange) },
                    enabled = !isUpdating,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        focusedLabelColor = AccentOrange,
                        unfocusedBorderColor = BorderGray
                    ),
                    supportingText = {
                        Text("数量を入力してください。不足の場合は0を入力。", fontSize = 11.sp, color = TextSecond)
                    }
                )
            }
        }
    }
}
```

### 1-F. BottomBar スタイル調整

既存ロジック維持。`登録` Button のみ色を `AccentOrange` に統一:
```kotlin
Button(
    onClick = onRegisterClick,
    enabled = state.canRegister,
    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
    ...
)
```

### 1-G. Scaffold背景色

```kotlin
Scaffold(
    containerColor = BodyBg,   // 追加
    topBar = { ... }
)
```

### 修正対象ファイル（1ファイルのみ）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`

### 必要なimport追加
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
```

### 完了条件
- コンパイルエラーなし
- 既存の ViewModel / ロジック / ナビゲーションの呼び出しはそのまま
- P20 と同一の色セット（クリームヘッダー、オレンジ強調、赤タイトル）が適用されている
- 履歴パネル・バーコード入力・ロケーション等の未実装機能がUIに追加されていない

---

## P2: ビルド確認

### 目的
ビルドエラーがないことを確認する。

### 手順
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :feature:outbound:assembleDebug 2>&1 | tail -20
```

### 完了条件
- `BUILD SUCCESSFUL` が出力されること
- エラー・未解決参照がないこと

---

## 制約（厳守）

- `OutboundPickingScreen.kt` のみを変更する
- `OutboundPickingState.kt` / `OutboundPickingViewModel.kt` は変更禁止
- ナビゲーション（HandyNavHost.kt 等）は変更禁止
- 未実装機能（バーコード・ロケーション・履歴パネル・取消ボタン）はUIに追加しない
- P20 と同一色セットを使用する
- BottomBar の既存ボタン構成（6ボタン）を維持する

## 全体完了条件

- P1: UIリデザイン実装完了（コンパイルエラーなし）
- P2: `BUILD SUCCESSFUL` 確認
