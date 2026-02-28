# P03 メイン画面 新デザイン実装 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`
- デザイン指示書: `prompts/P03-New-Design-0220/P03-new-design-0220.md`

### 完了済みの作業・現在の状況
- P03 既存実装: `feature/main/.../MainScreen.kt` に Scaffold + TopAppBar + メニューボタン（上下 2 段）が実装済み
- 直前の変更: 縦横向き対応（portrait = 1列, landscape = 2行グリッド）を追加済み（このコードは今回の新デザインで完全に置き換える）

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | WmsColor.kt 作成 | カラー定数を集約したファイルを新規作成 | ファイルが存在し、全カラーが定義されている |
| P2 | MainScreen.kt 新デザイン実装 | 新デザインに沿った完全再実装 | ビルド成功・@Preview で正しく表示 |
| P3 | Preview 確認 | 全 @Preview アノテーションで表示確認 | デザイン仕様と一致している |

---

## P1: WmsColor.kt 作成

### 目的
カラー定数を `WmsColor.kt` に集約し、`MainScreen.kt` 内でハードコードしない。

### 作成ファイル
`feature/main/src/main/java/biz/smt_life/android/feature/main/WmsColor.kt`

### 定義するカラー定数

```kotlin
package biz.smt_life.android.feature.main

import androidx.compose.ui.graphics.Color

object WmsColor {
    // ヘッダー背景
    val HeaderBackground = Color(0xFF1A2634)

    // 入庫カード
    val InboundBorder   = Color(0xFF1976D2)
    val InboundCircleBg = Color(0xFFE3F2FD)
    val InboundIcon     = Color(0xFF1976D2)

    // 出庫カード
    val OutboundBorder   = Color(0xFFD32F2F)
    val OutboundCircleBg = Color(0xFFFFEBEE)
    val OutboundIcon     = Color(0xFFD32F2F)

    // 移動カード
    val MoveBorder   = Color(0xFF388E3C)
    val MoveCircleBg = Color(0xFFE8F5E9)
    val MoveIcon     = Color(0xFF388E3C)

    // 棚卸カード
    val InventoryBorder   = Color(0xFFF57C00)
    val InventoryCircleBg = Color(0xFFFFF3E0)
    val InventoryIcon     = Color(0xFFF57C00)

    // ロケ検索カード
    val LocationBorder   = Color(0xFF7B1FA2)
    val LocationCircleBg = Color(0xFFF3E5F5)
    val LocationIcon     = Color(0xFF7B1FA2)

    // サイドバー倉庫ボックス
    val WarehouseBoxBg     = Color(0xFFEEF4FF)
    val WarehouseBoxBorder = Color(0xFFD0E0FF)
    val WarehouseBoxText   = Color(0xFF1A56CC)

    // フッターテキスト
    val SystemInfoText = Color(0xFF999999)
}
```

### 完了条件
- `WmsColor.kt` が上記パスに存在すること
- ビルドエラーがないこと

---

## P2: MainScreen.kt 新デザイン実装

### 目的
既存の `MainScreen.kt` を新デザイン仕様に合わせて完全に再実装する。

### デザイン仕様要約

```
┌─────────────────────────────────────────────┐
│ Header (高さ 50dp)  #1A2634                  │
│  [<] メインメニュー       担当: 山田太郎 [→|] │
├────────────┬────────────────────────────────┤
│            │  Row1: [入庫処理] [出庫処理]    │
│  サイドバー │  Row2: [移動] [棚卸] [ロケ検索] │
│  280dp     │                                │
│  ・日付     │                                │
│  ・倉庫名   │                                │
│  ・変更ボタン│                                │
│  ・Ver/URL  │                                │
└────────────┴────────────────────────────────┘
```

### ヘッダー仕様
- 背景色: `WmsColor.HeaderBackground` (`#1A2634`)
- 高さ: 50dp
- 左側: 戻るアイコン（`Icons.AutoMirrored.Filled.ArrowBack`）+ Text "メインメニュー"（White）
- 右側: "担当: {pickerName}"（White）+ ログアウトアイコン（`ExitToApp`、White）
- ログアウトはダイアログ確認後に実行（既存ロジック流用）

### サイドバー仕様
- 幅: 280dp、背景: White、角丸: 12dp、padding: 16dp
- 1. 日付: `yyyy/MM/dd(曜)` 形式、Bold、18sp、Black
- 2. 倉庫名ボックス:
  - 背景 `WmsColor.WarehouseBoxBg`、ボーダー `WmsColor.WarehouseBoxBorder`、角丸 6dp
  - 📍アイコン + 倉庫名テキスト（`WmsColor.WarehouseBoxText`）
- 3. 倉庫変更ボタン: OutlinedButton、全幅、`onClick = onNavigateToWarehouseSettings`
- 4. システム情報（下部）: "Ver: {appVersion}" / "URL: {hostUrl}"、小テキスト、`WmsColor.SystemInfoText`

### メニューカード仕様（共通）
- 背景: White、角丸: 15dp、elevation: 4dp
- アイコン円: `size(72.dp).clip(CircleShape).background(circleBg)`
- アイコンサイズ: 28sp（`MaterialTheme.typography.headlineMedium`）
- タイトル: Bold 15sp（`MaterialTheme.typography.titleMedium`）
- ボトムボーダー: `Modifier.drawBehind` で下辺に 4dp の色線を描画
- Ripple: `Modifier.clickable(onClick = ...)`

#### カード一覧

| カード | サイズ | ボトムボーダー色 | 円背景 | アイコン色 | アイコン |
|--------|--------|-----------------|--------|-----------|---------|
| 入庫処理 | Row1 左半分（weight=1f） | `InboundBorder` | `InboundCircleBg` | `InboundIcon` | `Icons.Default.Download` |
| 出庫処理 | Row1 右半分（weight=1f） | `OutboundBorder` | `OutboundCircleBg` | `OutboundIcon` | `Icons.Default.Upload` |
| 移動処理 | Row2 1/3（weight=1f） | `MoveBorder` | `MoveCircleBg` | `MoveIcon` | `Icons.Default.SwapHoriz` |
| 棚卸処理 | Row2 1/3（weight=1f） | `InventoryBorder` | `InventoryCircleBg` | `InventoryIcon` | `Icons.Default.Inventory` または `ListAlt` |
| ロケ検索 | Row2 1/3（weight=1f） | `LocationBorder` | `LocationCircleBg` | `LocationIcon` | `Icons.Default.LocationOn` |

> アイコンは `material-icons-extended` に依存しない標準アイコンを優先。なければ絵文字テキストで代替可。

### レイアウト構造（Kotlin/Compose 疑似コード）

```
Box(fillMaxSize) {
    Column {
        // ヘッダー（50dp）
        MainHeader(pickerName, onLogout)

        // ボディ（Row）
        Row(Modifier.weight(1f).fillMaxWidth()) {
            // サイドバー
            SidebarPanel(
                width = 280.dp,
                currentDate, warehouseName, appVersion, hostUrl,
                onChangeWarehouse
            )

            // メニューグリッド
            Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                // Row1
                Row(Modifier.weight(1f)) {
                    MenuCard(入庫処理, weight=1f)
                    MenuCard(出庫処理, weight=1f)
                }
                Spacer(8.dp)
                // Row2
                Row(Modifier.weight(1f)) {
                    MenuCard(移動処理, weight=1f)
                    MenuCard(棚卸処理, weight=1f)
                    MenuCard(ロケ検索, weight=1f)
                }
            }
        }
    }

    // ログアウト確認ダイアログ（既存ロジック流用）
}
```

### 修正対象ファイル
- `feature/main/src/main/java/biz/smt_life/android/feature/main/MainScreen.kt`

### 変更禁止
- `MainRoute` Composable のシグネチャ（呼び出し元 `MainActivity.kt` への影響を避ける）
- `MainScreen` Composable のシグネチャ
- ViewModel・State・Action 系ファイル

### 実装注意点
- `import androidx.compose.ui.graphics.drawscope.drawBehind` が必要（ボトムボーダー用）
- 不要になったインポート（`Scaffold`, `TopAppBar`, `LocalConfiguration`, `Configuration` など）は削除する
- portrait 固定のため orientation 分岐は不要（P2 実装で削除）
- `@Preview` は portrait 固定で設定: `widthDp = 400, heightDp = 700`

### 完了条件
- Android Studio でビルドエラーなし
- `@Preview` で新デザインが正しく表示される
- 既存の `MainRoute` → `MainScreen` 呼び出しが壊れていない

---

## P3: Preview 確認

### 目的
全 `@Preview` アノテーションで表示確認し、デザイン仕様との齟齬を修正する。

### 確認項目
- [ ] ヘッダー背景色が `#1A2634` になっている
- [ ] サイドバーが 280dp 固定幅で表示されている
- [ ] 倉庫名ボックスに青色ボーダー・背景がついている
- [ ] Row1 に 2 カード（入庫・出庫）が等幅で表示されている
- [ ] Row2 に 3 カード（移動・棚卸・ロケ検索）が等幅で表示されている
- [ ] 各カードにボトムボーダーが表示されている
- [ ] アイコン円が各カードに正しく表示されている
- [ ] ログアウトダイアログが正常に動作する

### 完了条件
- 全チェック項目を満たしていること

---

## 制約（厳守）

- デバイス解像度 1080x2400 / 420dpi / portrait 固定を前提にUI設計
- `local.properties` の認証情報はコミットしない
- `MainRoute` / `MainScreen` のシグネチャを変更しない
- `MainViewModel` / `MainUiState` / `MainAction` を変更しない
- カラー値は `WmsColor.kt` に集約し、`MainScreen.kt` 内でハードコードしない

## 全体完了条件

- ビルド成功
- `@Preview` で新デザインが正しく表示
- 既存ナビゲーション（MainActivity → MainScreen）が壊れていない
