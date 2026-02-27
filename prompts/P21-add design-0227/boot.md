# Android Plan: P21-add-design-0227

- **ID**: P21-add-design-0227
- **作成日**: 2026-02-27
- **最終更新**: 2026-02-27
- **ステータス**: 完了
- **ディレクトリ**: prompts/P21-add design-0227/

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

P21（出庫データ入力画面）に2つの機能を追加する:
1. 全商品登録完了時に「すべての商品が登録されました。確定を押下してください。」メッセージと確定ボタンを表示
2. ヘッダーのコース名バッジをタップするとコースリスト（P20）へ遷移

参考デザイン: `C:\Users\ninpe\Downloads\ht_inventory_yoko_standalone (1).html`

## 使用API

| メソッド | エンドポイント | 用途 |
|----------|---------------|------|
| POST | `/api/picking/tasks/{id}/complete` | ピッキングタスク完了（確定ボタン） |

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi
- Landscape/portrait 対応
- テーマ: NoActionBar + Compose TopAppBar
- P21の既存レイアウト（2ペイン横分割）を維持
- 参考HTML: `ht_inventory_yoko_standalone (1).html`のシッピング画面を参照

## 対象ファイル

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt` — UI変更（完了メッセージ表示、ヘッダーコース名クリック）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt` — 状態追加（allCompleted判定）

### 参照のみ（変更禁止）
- `prompts/api.md`
- `prompts/design.md`
- `prompts/pages.md`
- `C:\Users\ninpe\Downloads\ht_inventory_yoko_standalone (1).html` — 参考デザイン

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: 全商品登録完了メッセージ | 完了 | 2026-02-27 | OutlinedCard + CheckCircle + 確定ボタン |
| P2: ヘッダーコース名タップ→P20遷移 | 完了 | 2026-02-27 | Surface onClick追加 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 現在のP21画面構造
- P21は`OutboundPickingScreen`で、`Scaffold` + `TopAppBar` + コンテンツの構成
- コンテンツは `when` 分岐で表示:
  - `state.isLoading` → CircularProgressIndicator
  - `state.currentItem != null` → OutboundPickingBody（2ペイン横分割）
  - `else` → 「商品がありません」テキスト（← ここを変更する）
- ヘッダーにコース名バッジ（緑色 BadgeGreen）が表示されるが、現在タップイベントなし
- `onNavigateToCourseList: () -> Unit` パラメータは既に存在

### 参考HTMLの完了画面デザイン
- check-circle アイコン (emerald-500, 48dp相当)
- 「すべての商品が登録されました。」(16sp, bold, neutral-800)
- 「確定を押下してください。」(14sp, neutral-500)
- 「確定」ボタン (amber-600 背景, 白文字, bold)

### 参考HTMLのヘッダーコースクリック
- コース名バッジ `<button @click="ship.selectedCourse = null">` → コース選択画面に戻る
- 現在のP21ヘッダーでは `Surface` コンポーネントで表示（クリックイベントなし）

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: 全商品登録完了メッセージ
- 完了日: 2026-02-27
- 実績:
  - OutboundPickingScreen.kt の `else` 分岐を変更
  - CheckCircle アイコン(48dp, BadgeGreen) + メッセージ + 確定ボタン表示
  - 確定ボタン → `viewModel.showCompletionDialog()` 呼び出し
  - Icons.Filled.CheckCircle import 追加

### P2: ヘッダーコース名タップ→P20遷移
- 完了日: 2026-02-27
- 実績:
  - ヘッダーのコース名バッジ `Surface` に `onClick = onNavigateToCourseList` 追加
  - 既存の「▼」マークでタップ可能であることが視覚的に示されている
