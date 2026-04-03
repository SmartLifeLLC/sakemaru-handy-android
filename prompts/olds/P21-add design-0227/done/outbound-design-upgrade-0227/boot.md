# Android Plan: outbound-design-upgrade

- **ID**: outbound-design-upgrade
- **作成日**: 2026-02-27
- **最終更新**: 2026-02-27
- **ステータス**: 完了
- **ディレクトリ**: prompts/outbound-design-upgrade/

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

出荷処理3画面（P20コース選択、P21データ入力、P22履歴）のUIデザインを、HTMLリファレンス（03_menu02_01〜03.html）に合わせてアップグレードする。機能・API連携は実装済みのため、UIスタイリングのみ変更。

## 使用API

| メソッド | エンドポイント | 用途 |
|----------|---------------|------|
| GET | `/api/picking/tasks` | コース一覧取得（実装済み） |
| POST | `/api/picking/tasks/{id}/start` | タスク開始（実装済み） |
| POST | `/api/picking/tasks/{id}/update` | ピッキング結果更新（実装済み） |
| POST | `/api/picking/tasks/{id}/complete` | タスク完了（実装済み） |
| POST | `/api/picking/tasks/{id}/cancel` | 結果キャンセル（実装済み） |

> **注意**: API連携は全て実装・テスト済み。本タスクはUI変更のみ。

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi
- portrait/landscape対応
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold + TopAppBar + FunctionKeyBar
- **デザインシステム変更**: HTMLリファレンスのカラー・ボタンスタイルに統一
  - ヘッダー背景: `#1a233a`（ダークネイビー）
  - ボタン: 3Dグラデーション + border-bottom立体感
  - Fキーヒント付きボタン
  - カラー体系: 登録系(青#2196f3)、履歴/戻る系(グレー#607d8b)、選択系(オレンジ#ff9800)、次へ(緑#4caf50)

## HTMLリファレンスファイル

| 画面 | HTMLファイル |
|------|-------------|
| P20 コース選択 | `C:\Users\ninpe\Desktop\00.쭈작업\02_01.SmartLife\08.Handy\html\Denso02\03_menu02_01.html` |
| P21 データ入力 | `C:\Users\ninpe\Desktop\00.쭈작업\02_01.SmartLife\08.Handy\html\Denso02\03_menu02_02.html` |
| P22 出荷履歴 | `C:\Users\ninpe\Desktop\00.쭈작업\02_01.SmartLife\08.Handy\html\Denso02\03_menu02_03.html` |
| 参考: 入荷入力 | `C:\Users\ninpe\Desktop\00.쭈작업\02_01.SmartLife\08.Handy\html\Denso02\03_menu01_01.html` |

## 対象ファイル

### 既存変更（UI変更のみ）
- `feature/outbound/tasks/PickingTasksScreen.kt` — P20 コース選択画面
- `feature/outbound/picking/OutboundPickingScreen.kt` — P21 データ入力画面
- `feature/outbound/picking/PickingHistoryScreen.kt` — P22 出荷履歴画面

### 共通コンポーネント（新規作成 or 既存変更）
- 出荷用共通UIコンポーネント（NavBar、FooterButton、3Dボタンスタイル等）

### 参照のみ（変更禁止）
- `feature/outbound/tasks/PickingTasksViewModel.kt` — ビジネスロジック変更なし
- `feature/outbound/picking/OutboundPickingViewModel.kt` — ビジネスロジック変更なし
- `feature/outbound/picking/PickingHistoryViewModel.kt` — ビジネスロジック変更なし
- `core/network/` — API層変更なし
- `core/domain/` — ドメイン層変更なし

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: 現状分析・共通スタイル定義 | 完了 | 2026-02-27 | OutboundDesignSystem.kt新規作成 |
| P2: P20コース選択画面デザイン変更 | 完了 | 2026-02-27 | PickingTasksScreen.kt全面書換 |
| P3: P21データ入力画面デザイン変更 | 完了 | 2026-02-27 | OutboundPickingScreen.kt全面書換 |
| P4: P22出荷履歴画面デザイン変更 | 完了 | 2026-02-27 | PickingHistoryScreen.kt全面書換 |
| P5: 結合確認 | 完了 | 2026-02-27 | compileDebugKotlin BUILD SUCCESSFUL |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 既存実装の状態
- P20-P22は機能完全実装済み（API連携、ナビゲーション、ViewModel全て動作）
- 現在のデザイン: Material Design系（2カラムグリッド、スプリットペイン等）
- 変更目標: HTMLリファレンスの3Dボタンスタイル、ダークネイビーヘッダー、カラーコーディング

### API テスト結果
- 不要（既存API連携は全て動作確認済み）

### サーバエラー
- なし

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: 現状分析・共通スタイル定義
- 完了日: 2026-02-27
- 実績:
  - `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/components/OutboundDesignSystem.kt` 新規作成
  - OutboundColors: ScreenBg, NavBg, TextPrimary, Register/History/Select/Next/Image/Delete等のカラー定義
  - ButtonGradient / CompletedGradient ブラシ定義
  - OutboundNavBar: ダークネイビーヘッダー + 戻る[F4] / メイン[F8]ボタン
  - NavBarButton: 3Dグラデーション + press animation
  - OutboundFooterBar: フッターアクションバー
  - FooterButton: カラーコーディング底線付き3Dボタン
  - SmallDeleteButton: 削除[F5]ボタン
  - CourseButton: 完了状態対応コースボタン（緑グラデーション）

### P2: P20コース選択画面デザイン変更
- 完了日: 2026-02-27
- 実績:
  - `PickingTasksScreen.kt` 全面書換
  - 2カラムLazyVerticalGrid → 1カラムLazyColumn
  - Material TopAppBar → OutboundNavBar（ダークネイビー）
  - CourseButton使用（3D効果、完了時緑グラデーション）
  - onNavigateToMain追加（メイン[F8]ボタン対応）
  - 背景色 OutboundColors.ScreenBg (#F0F2F5)

### P3: P21データ入力画面デザイン変更
- 完了日: 2026-02-27
- 実績:
  - `OutboundPickingScreen.kt` 全面書換
  - Row(スプリットペイン) → 縦Column レイアウト
  - ProductInfoCard: コース名、商品名、JAN/容量/入数表示
  - FloatingLabelField: ロケーション、伝票番号
  - QuantityTable: 3カラムグリッド（ラベル58dp｜指示｜実績）
  - OutboundPickingFooter: 6ボタン（登録/履歴/選択/次へ/画像/削除）カラーコーディング
  - HandyNavHost.kt更新: onNavigateToMain対応

### P4: P22出荷履歴画面デザイン変更
- 完了日: 2026-02-27
- 実績:
  - `PickingHistoryScreen.kt` 全面書換
  - 2カラムグリッド → シングルカードビュー + ページネーション
  - 件数表示（"01件"）追加
  - HistoryCard: 3D底線効果(6dp)、InfoTable(#8DA9C4/#D9E3F0)
  - QuantityInfoTable: ケース/バラ + 指示数量
  - HistoryFooter: 3ボタン（前へF2/次へF3/確定F1）
  - onNavigateToMain追加、HandyNavHost.kt更新

### P5: 結合確認
- 完了日: 2026-02-27
- 実績:
  - `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL (8s)
  - コンパイルエラーなし（deprecation warning: hiltViewModel importのみ）
  - OutboundPickingScreenのcontentPadding問題を修正済み
  - 全画面のナビゲーション整合性確認済み
