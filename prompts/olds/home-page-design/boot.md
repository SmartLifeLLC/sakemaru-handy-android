# Android Plan: home-page-design

- **ID**: home-page-design
- **作成日**: 2026-02-19
- **最終更新**: 2026-02-19
- **ステータス**: 進行中
- **ディレクトリ**: prompts/home-page-design/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P03 メイン画面（MainScreen）の各メニューカード（入庫・出庫・移動・棚卸・ロケ検索）の上部ストライプ色をすべて緑に統一する。

## 使用API

なし（純粋なUI修正）

## 重要な設計制約

- 画面解像度: 1080 x 2400 / 420dpi（portrait固定）
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold（FunctionKeyBar なし / P03 はメイン画面）
- `MenuButton` の `topBorderColor` パラメータを変更するだけ（ロジック変更なし）

## 対象ファイル

### 既存変更
- `feature/main/src/main/java/biz/smt_life/android/feature/main/MainScreen.kt`

### 参照のみ（変更禁止）
- `prompts/design.md`
- `prompts/pages.md`

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: カード上部色を緑に変更 | 未着手 | 2026-02-19 | 全5ボタンの topBorderColor を緑に |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。

### 現在の色定義（変更前）

| ボタン | 現在色 | カラーコード |
|--------|--------|-------------|
| 入庫 | Blue | `Color(0xFF2196F3)` |
| 出庫 | Pink/Red | `Color(0xFFE91E63)` |
| 移動 | Purple | `Color(0xFF9C27B0)` |
| 棚卸 | Orange | `Color(0xFFFF9800)` |
| ロケ検索 | Blue Grey | `Color(0xFF607D8B)` |

### 変更後の色

全ボタン共通: `Color(0xFF4CAF50)` (Material Green 500)

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: カード上部色を緑に変更
- 完了日: -
- 実績:
  - (完了後に記入)
