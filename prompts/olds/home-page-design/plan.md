# P03 メイン画面 カード上部色変更 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`

### 完了済みの作業・現在の状況
- MainScreen は実装済み
- `MenuButton` コンポーザブルが `topBorderColor` パラメータを持ち、カード上部に色付きストライプを表示
- 現在は入荷=青、出荷=ピンク、移動=紫、棚卸=オレンジ、ロケ検索=青グレーと各ボタンで色が異なる
- 全ボタンのストライプ色を緑（`Color(0xFF4CAF50)`）に統一する

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | カード上部色を緑に変更 | MainScreen.kt の全 MenuButton の topBorderColor を緑に変更 | ビルド成功・全ボタンの上部が緑になっていること |

---

## P1: カード上部色を緑に変更

### 目的・問題

P03（メイン画面）の各機能メニューカードの上部ストライプが異なる色になっている。
デザイン要件に従い、全カードの上部色を緑（`Color(0xFF4CAF50)`）に統一する。

**対象ファイル**:
`feature/main/src/main/java/biz/smt_life/android/feature/main/MainScreen.kt`

### 問題箇所

`ReadyContent` コンポーザブル内の `MenuButton` 呼び出し箇所（5か所）:

```kotlin
// Row 1
MenuButton(label = "入荷",    topBorderColor = Color(0xFF2196F3), ...) // Blue
MenuButton(label = "出荷",    topBorderColor = Color(0xFFE91E63), ...) // Pink/Red
MenuButton(label = "移動",    topBorderColor = Color(0xFF9C27B0), ...) // Purple
// Row 2
MenuButton(label = "棚卸",    topBorderColor = Color(0xFFFF9800), ...) // Orange
MenuButton(label = "ロケ検索", topBorderColor = Color(0xFF607D8B), ...) // Blue Grey
```

### 修正方針

全 5 か所の `topBorderColor` を `Color(0xFF4CAF50)` (Material Green 500) に変更する。
`MenuButton` コンポーザブル自体の変更は不要。呼び出し側の引数のみ変更。

### 修正内容

**変更前:**
```kotlin
MenuButton(label = "入荷",    topBorderColor = Color(0xFF2196F3), ...)
MenuButton(label = "出荷",    topBorderColor = Color(0xFFE91E63), ...)
MenuButton(label = "移動",    topBorderColor = Color(0xFF9C27B0), ...)
MenuButton(label = "棚卸",    topBorderColor = Color(0xFFFF9800), ...)
MenuButton(label = "ロケ検索", topBorderColor = Color(0xFF607D8B), ...)
```

**変更後:**
```kotlin
MenuButton(label = "入荷",    topBorderColor = Color(0xFF4CAF50), ...)
MenuButton(label = "出荷",    topBorderColor = Color(0xFF4CAF50), ...)
MenuButton(label = "移動",    topBorderColor = Color(0xFF4CAF50), ...)
MenuButton(label = "棚卸",    topBorderColor = Color(0xFF4CAF50), ...)
MenuButton(label = "ロケ検索", topBorderColor = Color(0xFF4CAF50), ...)
```

### 修正対象ファイル

| ファイル | 変更内容 |
|---------|---------|
| `feature/main/.../MainScreen.kt` | 全5つの MenuButton の `topBorderColor` を `Color(0xFF4CAF50)` に変更 |

### 完了条件

- ビルドが成功すること
- 全5つのメニューカード（入荷・出荷・移動・棚卸・ロケ検索）の上部ストライプが緑になっていること

---

## 制約（厳守）

- `local.properties` の認証情報はコミットしない
- デザインパターン（Scaffold + TopAppBar）を崩さない
- `MenuButton` コンポーザブルの構造は変更しない（呼び出し引数のみ変更）

## 全体完了条件

- P1 の修正が完了し、ビルド成功
- 全カードの上部色が緑に統一されていること
