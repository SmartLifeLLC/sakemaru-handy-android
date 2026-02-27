# Android Plan: p02-api-screen-scroll

- **ID**: p02-api-screen-scroll
- **作成日**: 2026-02-19
- **最終更新**: 2026-02-19
- **ステータス**: 進行中
- **ディレクトリ**: prompts/p02-api-screen-scroll/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P02 設定画面（SettingsScreen）で、コンテンツが画面高を超えた際に保存ボタンが見えなくなる問題を修正する。Column に verticalScroll を追加してスクロール可能にする。

## 使用API

なし（純粋なUI修正）

## 重要な設計制約

- 画面解像度: 1080 x 2400 / 420dpi（portrait固定）
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold + TopAppBar（FunctionKeyBar なし / P02 は設定画面）
- `verticalScroll` 追加時は `fillMaxSize()` → `fillMaxWidth()` + `wrapContentHeight()` に変更が必要（Columnがスクロール対応の場合、高さを制限するとスクロールが効かない）

## 対象ファイル

### 既存変更
- `feature/settings/src/main/java/biz/smt_life/android/feature/settings/SettingsScreen.kt`

### 参照のみ（変更禁止）
- `prompts/design.md`
- `prompts/pages.md`

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: スクロール対応修正 | 完了 | 2026-02-19 | Column に verticalScroll 追加 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: スクロール対応修正
- 完了日: 2026-02-19
- 実績:
  - `rememberScrollState` / `verticalScroll` import 追加
  - Column modifier: `fillMaxSize()` → `fillMaxWidth()` + `.verticalScroll(scrollState)` に変更
  - `:feature:settings:compileDebugKotlin` BUILD SUCCESSFUL 確認済み
