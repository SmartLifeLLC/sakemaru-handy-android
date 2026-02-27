# Claude Code 作業指示プロンプト
## WMS メインメニュー画面 — Android アプリ実装

---

## 🎯 タスク概要

添付の `main.html` をリファレンスデザインとして、**Android Studio (Kotlin / Jetpack Compose)** で同等の UI を実装してください。既存プロジェクトへの組み込みを想定しています。

---

## 📐 実装対象画面の仕様

### レイアウト構成

画面は **縦方向に固定（横画面対応不要）** で、以下の 3 エリアで構成されます。

```
┌─────────────────────────────────────────────┐
│ Header (高さ固定: 50dp)                      │
├────────────┬────────────────────────────────┤
│            │  メニューグリッド               │
│  サイドバー │  Row1: [入庫(span2)] [出庫(span2)] │
│  (幅固定   │  Row2: [移動] [棚卸] [ロケ検索] │
│   280dp)   │                                │
└────────────┴────────────────────────────────┘
```

---

### 1. ヘッダー

| 項目 | 仕様 |
|------|------|
| 背景色 | `#1A2634` |
| 高さ | `50dp` |
| 左側 | 戻るアイコン（`<`） ＋ テキスト「メインメニュー」|
| 右側 | 担当者名表示（例: `担当: 山田 太郎`）＋ ログアウトアイコン |
| テキスト色 | White |

---

### 2. サイドバー

背景色: White、角丸: `12dp`、パディング: `16dp`

表示内容（上から順）:
1. **日付表示** — `yyyy/MM/dd(曜)` 形式、太字、`18sp`
2. **倉庫名ボックス** — 背景 `#EEF4FF`、ボーダー `#D0E0FF`、角丸 `6dp`、📍アイコン付き、テキスト色 `#1A56CC`
3. **倉庫変更ボタン** — ボーダーあり白ボタン、全幅、クリックで `Toast` 表示
4. **システム情報** — 下部に `Ver: 1.0.2` / `URL: api.system-server.com` を小テキストで表示（color: `#999`）

---

### 3. メニューグリッド（カード）

カード共通仕様:
- 背景: White
- 角丸: `15dp`
- 影: `elevation = 4dp`
- アイコン円: 直径 `72dp`、円形、背景色＋アイコン色は下表参照
- アイコンサイズ: `28sp`
- タイトル: 太字 `15sp`
- クリック: `ripple effect` ＋ コールバック（後述）

#### カード一覧

| カード名 | サイズ | ボトムボーダー色 | 円背景色 | アイコン色 | アイコン |
|----------|--------|-----------------|----------|-----------|------|
| 入庫処理 (00) | 大（Row1 左半分） | `#1976D2` | `#E3F2FD` | `#1976D2` | ↓ |
| 出庫処理 (00) | 大（Row1 右半分） | `#D32F2F` | `#FFEBEE` | `#D32F2F` | ↑ |
| 移動処理 | 小（Row2 左） | `#388E3C` | `#E8F5E9` | `#388E3C` | ⇄ |
| 棚卸処理 (00) | 小（Row2 中） | `#F57C00` | `#FFF3E0` | `#F57C00` | 📋 |
| ロケ検索 | 小（Row2 右） | `#7B1FA2` | `#F3E5F5` | `#7B1FA2` | 📍 |

> **補足**: 「大カード」= Row1 を 2 分割、「小カード」= Row2 を 3 等分

---

## 🛠 実装方針・技術スタック

```
言語       : Kotlin
UI         : Jetpack Compose (Material3)
最小 SDK   : API 26 (Android 8.0)
ターゲット : API 34
```

---

## 📁 作成・編集ファイル

以下のファイルを **新規作成** してください。既存ファイルは変更不要です。

```
app/src/main/java/com/example/<パッケージ名>/
├── ui/
│   ├── screen/
│   │   └── MainMenuScreen.kt      ← メイン画面 Composable
│   ├── component/
│   │   ├── MenuCard.kt            ← カード共通コンポーネント
│   │   ├── SidebarPanel.kt        ← サイドバーコンポーネント
│   │   └── AppHeader.kt           ← ヘッダーコンポーネント
│   └── theme/
│       └── WmsColor.kt            ← カラー定数定義
└── model/
    └── MenuItem.kt                ← カードデータモデル
```

---

## 🔌 コールバック・インターフェース

`MainMenuScreen` は以下のラムダを引数で受け取る形にしてください。

```kotlin
@Composable
fun MainMenuScreen(
    currentDate: String,               // "2025/12/19(金)" 形式の文字列
    warehouseName: String,             // 倉庫名
    operatorName: String,              // 担当者名
    onInboundClick: () -> Unit,        // 入庫処理
    onOutboundClick: () -> Unit,       // 出庫処理
    onTransferClick: () -> Unit,       // 移動処理
    onInventoryClick: () -> Unit,      // 棚卸処理
    onLocationSearchClick: () -> Unit, // ロケ検索
    onChangeWarehouseClick: () -> Unit,// 倉庫変更
    onBackClick: () -> Unit,           // ヘッダー戻る
    onLogoutClick: () -> Unit          // ログアウト
)
```

---

## ✅ 実装チェックリスト

実装完了後、以下をすべて満たしているか確認してください。

- [ ] `MainMenuScreen.kt` がプレビュー（`@Preview`）で正しく表示される
- [ ] カラー値が `WmsColor.kt` に集約されており、ハードコードされていない
- [ ] `MenuItem.kt` の data class を使ってカードリストが生成されている
- [ ] 各カードのボトムボーダーが正しい色で実装されている
- [ ] サイドバーの高さがメニューグリッドと揃っている（`fillMaxHeight`）
- [ ] `Modifier.weight` を使いカードの大小サイズが比率で制御されている
- [ ] クリック時に ripple エフェクトが表示される
- [ ] `@Preview` アノテーションに `showBackground = true` が付与されている

---

## 💡 実装ヒント

- **ボトムボーダー**: `Modifier.drawBehind { ... }` または `border` ＋ `clip` を使う
- **アイコン円**: `Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(color))`
- **グリッドレイアウト**: `Row` ＋ `Modifier.weight()` の組み合わせで HTML の `span` を再現する
- **サイドバー幅**: `width(280.dp)` で固定
- **日本語フォント**: システムフォントで自動対応されるため追加設定不要

---

## 📎 参考リソース

| ファイル | 用途 |
|---------|------|
| `main.html` | ピクセル精度のリファレンスデザイン（色・レイアウト・テキスト） |

---

