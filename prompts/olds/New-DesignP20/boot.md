# Android Plan: New-DesignP20

- **ID**: New-DesignP20
- **作成日**: 2026-02-28
- **最終更新**: 2026-02-28
- **ステータス**: 進行中
- **ディレクトリ**: prompts/New-DesignP20/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 「作業中コンテキスト」セクションで途中データを確認
7. error.log があればサーバ側エラーの履歴を確認
8. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P21（出荷データ入力画面）とP22（出荷履歴画面）のボディレイアウトを変更する。
P21の右側履歴を削除し、受注数/出荷数を右側へ移動。P22のボディにP21から削除した履歴リストを配置。

## 使用API

| メソッド | エンドポイント | 用途 |
|----------|---------------|------|
| なし | - | UIレイアウト変更のみ。既存APIをそのまま使用 |

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi
- portrait/landscape対応
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold + TopAppBar + FunctionKeyBar
- ヘッダーは変更しない（固定）
- ボディ部分のみレイアウト変更

## 対象ファイル

### 新規作成
なし

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt` — P21レイアウト変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryScreen.kt` — P22レイアウト変更

### 参照のみ（変更禁止）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryState.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: P21レイアウト変更 | 完了 | 2026-02-28 | ロケ/伝票2行化、ケース/バラ2列+受注数インライン |
| P2: P22レイアウト変更 | 完了 | 2026-02-28 | グリッド→LazyColumnリスト形式に変更 |
| P3: ビルド確認・動作テスト | 完了 | 2026-02-28 | assembleDebug成功 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 現在のP21レイアウト構造
- 2ペイン構成（左50%: 商品情報+受注数/出荷数+ボタン、右50%: 履歴リスト）
- 左ペイン: 商品名/JAN/容量、ロケーション/伝票番号、受注数(読取専用)/出荷数(入力)、前へ/登録/次へボタン
- 右ペイン: 履歴リスト（HistoryItemRow）

### 現在のP22レイアウト構造
- 読取専用モード: 完了アイコン+メッセージ
- 編集モード: 2列グリッド（LazyVerticalGrid）でカード表示
- 各カード: 商品名/伝票番号/規格/JAN/予定数量/出荷数量

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: P21レイアウト変更
- 完了日: 2026-02-28
- 実績:
  - ロケーション・伝票番号を2行表示に変更（ラベル+値を横並び×2行）
  - 右ペイン: 受注数/出荷数セクションを削除し、ケース（受注数：XX）/バラ（受注数：XX）2列レイアウトに変更
  - 入力欄を右ペイン最上部に配置

### P2: P22レイアウト変更
- 完了日: 2026-02-28
- 実績:
  - LazyVerticalGrid(2列)をLazyColumn(縦リスト)に変更
  - 各カード: 商品名+削除ボタン / JAN・伝票番号 / 出荷数・受注数 の3行構成
  - imports更新（grid→lazy column）

### P3: ビルド確認・動作テスト
- 完了日: 2026-02-28
- 実績:
  - `./gradlew assembleDebug` ビルド成功
  - Kotlinコンパイルエラーなし（既存の非推奨警告のみ）
