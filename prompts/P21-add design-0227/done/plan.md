# P21 出庫データ入力画面 追加機能 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- 参考デザイン: `C:\Users\ninpe\Downloads\ht_inventory_yoko_standalone (1).html`

### 完了済みの作業・現在の状況
- P21 OutboundPickingScreen は動作中
- 2ペイン横分割レイアウト（左: 履歴リスト、右: 商品情報+入力）
- 全商品登録完了時は `else` 分岐で「商品がありません」テキストのみ表示
- ヘッダーにコース名バッジ（緑色）は表示されるがタップ不可
- `onNavigateToCourseList` パラメータは既に定義済み

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | 全商品登録完了メッセージ | 全PENDING商品が登録された後に完了メッセージ+確定ボタン表示 | 全商品登録後に参考HTMLと同等の完了画面が表示される |
| P2 | ヘッダーコース名タップ→P20遷移 | ヘッダーのコース名バッジをタップするとP20に遷移 | コース名バッジタップでP20に遷移する |

---

## P1: 全商品登録完了メッセージ

### 目的
全てのPENDING商品が登録された後、現在の「商品がありません」テキストの代わりに、参考HTMLと同じデザインの完了メッセージと確定ボタンを表示する。

### 参考HTMLのデザイン
```
┌─────────────────────────────────┐
│                                 │
│     ✅ (check-circle, 緑)       │
│                                 │
│ すべての商品が登録されました。    │
│ 確定を押下してください。         │
│                                 │
│        [ 確定 ]                  │
│                                 │
└─────────────────────────────────┘
```

- アイコン: `Icons.Filled.CheckCircle`, 48dp, emerald/green (#10B981 or BadgeGreen)
- メインテキスト: 「すべての商品が登録されました。」16sp, Bold, TextPrimary
- サブテキスト: 「確定を押下してください。」14sp, Neutral500
- 確定ボタン: AccentOrange背景, 白文字, Bold, RoundedCornerShape(8.dp)
- 全体: 中央配置、白背景のカード内

### 修正対象ファイル

1. **`OutboundPickingScreen.kt`** (lines 248-257)
   - `else` 分岐を変更: 「商品がありません」→ 完了メッセージ+確定ボタン
   - 確定ボタンの `onClick` → `viewModel.showCompletionDialog()` または直接 `onNavigateToHistory()`
   - 参考HTML参照: `!ship.item && ship.allCompleted` テンプレートと同等

### 修正方針

`OutboundPickingScreen.kt`の `when` 分岐の `else` ブロックを以下に変更:

```kotlin
else -> {
    // 全商品登録完了メッセージ
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Neutral200),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BadgeGreen,  // emerald-500相当
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("すべての商品が登録されました。", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("確定を押下してください。", fontSize = 14.sp, color = Neutral500)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showCompletionDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(40.dp).widthIn(min = 120.dp)
                ) {
                    Text("確定", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
```

### 完了条件
- 全PENDING商品がPICKING/COMPLETED/SHORTAGEになった後、完了メッセージが表示される
- 確定ボタンを押すと `CompletionConfirmationDialog` が表示される
- デザインが参考HTMLに準拠している

---

## P2: ヘッダーコース名タップ→P20遷移

### 目的
ヘッダーのコース名バッジ（緑色）をタップすると、コースリスト画面（P20）に遷移する。参考HTMLでは `ship.selectedCourse = null` でコース選択画面に戻る。

### 現在のコード (OutboundPickingScreen.kt lines 147-173)
```kotlin
val courseName = state.originalTask?.courseName
if (!courseName.isNullOrBlank()) {
    Spacer(Modifier.width(8.dp))
    Surface(
        color = BadgeGreen,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(...) {
            Text(text = courseName, ...)
            Text(text = "▼", ...)
        }
    }
}
```

### 修正方針

`Surface` に `onClick` を追加して `onNavigateToCourseList()` を呼び出す:

```kotlin
Surface(
    onClick = onNavigateToCourseList,  // ← 追加
    color = BadgeGreen,
    shape = RoundedCornerShape(12.dp)
) {
    // ... 既存のRow内容はそのまま
}
```

### 修正対象ファイル
- **`OutboundPickingScreen.kt`** — ヘッダーの `Surface` に `onClick` 追加

### 完了条件
- コース名バッジをタップするとP20画面に遷移する
- 「▼」マークが表示されており、タップ可能であることが視覚的にわかる

---

## 制約（厳守）

- 画面解像度 1080x2400 / 420dpi / Landscape・portrait対応を前提にUI設計
- NoActionBar テーマ + Compose TopAppBar パターンを踏襲
- P21の既存2ペインレイアウトを維持
- `local.properties` の認証情報はコミットしない
- サーバ側エラーは `error.log` に記録する

## 全体完了条件

- 全商品登録完了時に参考HTMLと同等のメッセージが表示される
- 確定ボタンが正常に動作する
- ヘッダーコース名タップでP20に遷移する
- ビルド成功
