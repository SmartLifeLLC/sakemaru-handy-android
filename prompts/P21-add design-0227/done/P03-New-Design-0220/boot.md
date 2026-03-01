# Android Plan: P03-New-Design-0220

- **ID**: P03-New-Design-0220
- **作成日**: 2026-02-20
- **最終更新**: 2026-02-20
- **ステータス**: 完了
- **ディレクトリ**: prompts/P03-New-Design-0220/

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（boot.md）
2. プロジェクト仕様を読む: `prompts/api.md`, `prompts/design.md`, `prompts/pages.md`
3. plan.md を読む（作業計画の全体像）
4. 下記「進捗」テーブルで現在のPhaseを確認
5. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
6. 「作業中コンテキスト」セクションで途中データを確認
7. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P03 メイン画面を新デザイン仕様（`P03-new-design-0220.md`）に基づいて再実装する。
左サイドバー＋右メニューグリッドのレイアウトに変更し、カード型メニューボタン、カラーアクセント付きヘッダーを導入する。

## 使用API

APIの変更はなし。既存 MainViewModel が保持するデータ（倉庫名・担当者名・日付・バージョン・ホストURL）をそのまま使用する。

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi
- portrait 固定（新デザイン仕様より）
- テーマ: NoActionBar + Compose 独自ヘッダー
- 画面構成: Scaffold なし → Row（サイドバー + コンテンツ）
- 既存の `MainViewModel` / `MainUiState` / `MainAction` は変更禁止
- `feature/main/.../MainScreen.kt` を直接修正する（新規ファイルは WmsColor のみ）

## 対象ファイル

### 新規作成
- `feature/main/src/main/java/biz/smt_life/android/feature/main/WmsColor.kt` — カラー定数

### 既存変更
- `feature/main/src/main/java/biz/smt_life/android/feature/main/MainScreen.kt` — P03 新デザイン適用

### 参照のみ（変更禁止）
- `feature/main/src/main/java/biz/smt_life/android/feature/main/MainViewModel.kt`
- `feature/main/src/main/java/biz/smt_life/android/feature/main/MainUiState.kt`
- `feature/main/src/main/java/biz/smt_life/android/feature/main/MainAction.kt`

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: WmsColor.kt 作成 | 完了 | 2026-02-20 | WmsColor.kt 新規作成、全カラー定数定義済み |
| P2: MainScreen.kt 新デザイン実装 | 完了 | 2026-02-20 | ヘッダー・サイドバー・メニューグリッド完全再実装 |
| P3: Preview 確認 | 完了 | 2026-02-20 | コードレビューで全チェック項目確認済み |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

### P1: WmsColor.kt 作成
- 完了日: 2026-02-20
- 実績:
  - `feature/main/src/main/java/biz/smt_life/android/feature/main/WmsColor.kt` 新規作成
  - 18色の定数定義（ヘッダー・各カード・サイドバー・フッター）

### P2: MainScreen.kt 新デザイン実装
- 完了日: 2026-02-20
- 実績:
  - `ReadyContent` を完全再実装（ヘッダー50dp + サイドバー280dp + メニューグリッド）
  - `MenuCard` composable 新規実装（CircleShape アイコン、ボトムボーダー4dp、elevation4dp）
  - `MainRoute` / `MainScreen` シグネチャ維持（ナビゲーション影響なし）
  - 不要コード削除（orientation分岐、Scaffold、TopAppBar、MenuButton）

### P3: Preview 確認
- 完了日: 2026-02-20
- 実績:
  - 全8チェック項目をコードレビューで確認・全項目合格
  - @Preview 3種類（Loading/Ready/Error）を widthDp=400, heightDp=700 で設定
