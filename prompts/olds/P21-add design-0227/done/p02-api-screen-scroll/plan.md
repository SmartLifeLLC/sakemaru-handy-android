# P02 設定画面 スクロール対応 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`

### 完了済みの作業・現在の状況
- SettingsScreen は実装済み
- プリセットURL選択ラジオボタン・カスタムURL入力・保存ボタン・注意事項カードが縦に並ぶ構成
- Column に `verticalScroll` がないため、コンテンツ量によっては保存ボタンが画面外に隠れる

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | スクロール対応修正 | Column に verticalScroll を追加 | ビルド成功・全コンテンツ表示確認 |

---

## P1: スクロール対応修正

### 目的・問題

P02（設定画面）で、カスタムURL選択時などに保存ボタンが画面下部に隠れてスクロールで表示できない。

**対象ファイル**:
`feature/settings/src/main/java/biz/smt_life/android/feature/settings/SettingsScreen.kt`

### 問題箇所

`SettingsContent` コンポーザブル内の `Scaffold` の content ブロック（87〜238行目）:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(24.dp),
    ...
)
```

`verticalScroll` がないため、コンテンツが画面高を超えるとスクロールできない。

### 修正方針

1. `rememberScrollState()` を追加
2. Column の modifier に `.verticalScroll(scrollState)` を追加
3. `fillMaxSize()` は維持したまま `verticalScroll` を追加する
   - Compose では `fillMaxSize()` + `verticalScroll()` の組み合わせは動作しない（高さが無限大になる）
   - そのため `fillMaxWidth()` に変更し、縦方向はコンテンツの高さに任せる

### 修正内容

**変更前:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(24.dp),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Top
)
```

**変更後:**
```kotlin
val scrollState = rememberScrollState()
Column(
    modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(padding)
        .padding(24.dp),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.Top
)
```

### 必要なimport追加

```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

### 修正対象ファイル

| ファイル | 変更内容 |
|---------|---------|
| `feature/settings/.../SettingsScreen.kt` | Column に `verticalScroll` 追加、`fillMaxSize` → `fillMaxWidth` に変更、import 追加 |

### 完了条件

- ビルドが成功すること
- 設定画面を開いたとき、コンテンツを下スクロールすると保存ボタン・注意事項カードが表示されること
- カスタムURL選択時にテキストフィールドが表示されてもスクロールで保存ボタンに到達できること

---

## 制約（厳守）

- `local.properties` の認証情報はコミットしない
- デザインパターン（Scaffold + TopAppBar）を崩さない
- FunctionKeyBar はこの画面には不要（P02 の仕様通り）

## 全体完了条件

- P1 の修正が完了し、ビルド成功
- 保存ボタンがスクロールで常に到達可能であること
