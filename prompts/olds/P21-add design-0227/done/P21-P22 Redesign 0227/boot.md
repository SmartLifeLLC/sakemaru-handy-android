# Android Plan: p21-p22-redesign-0227

- **ID**: p21-p22-redesign-0227
- **作成日**: 2026-02-27
- **最終更新**: 2026-02-27
- **ステータス**: 完了
- **ディレクトリ**: prompts/p21-p22-redesign-0227/

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

P21（出荷データ入力）のBody部分を、HTMLリファレンス（ht_inventory_yoko_standalone.html）の出荷入力画面デザインに合わせてリデザイン。ヘッダーは現行を維持し、Body（コンテンツ領域）のみ変更する。

## HTMLリファレンスデザインの要点

### レイアウト変更（現行 → HTML準拠）
- **現行**: 左ペイン(商品情報カード) + 右ペイン(数量入力カード) + BottomBar(6ボタン)
- **HTML**: 左ペイン(商品情報+ロケ/伝票+受注数/出荷数+ボタン) + 右ペイン(履歴リスト)

### 左ペイン（w-1/2）
- 白背景カード(rounded-xl, shadow-sm, border neutral-200)
- 商品名(16sp extrabold) + JAN/容量/入数(14sp neutral-500) + 得意先名
- 画像ボタン(amber-50 bg, amber-200 border, amber-600 icon)
- ロケーション・伝票番号: 2カラムグリッド(amber-50 bg, amber-300 border, font-mono)
- 受注数（readonly, neutral-50 bg）: ケース/バラ行
- 出荷数（editable, neutral-300 border）: ケース/バラ行
- ボタン行: 前へ(disabled style) / 登録(amber-600 bg) / 次へ(amber-50 bg, amber-300 border)

### 右ペイン（w-1/2）
- "履歴" ラベル(13sp bold neutral-700)
- 白背景カード(rounded-xl, shadow-sm, border neutral-200)
- 空状態: アイコン + "履歴はありません"
- 履歴アイテム: amber-50 bg, amber-200 border, rounded-lg
  - 商品名(16sp bold) + コード/ロケ(11sp) + 出荷/受注数(11sp) + 得意先名(11sp)
  - 削除ボタン(red-50 bg, red-300 border, trash icon)

### カラースキーム（Body部分のみ）
- 背景: neutral-100 (#F5F5F5)
- カード背景: white
- アクセント: amber系 (amber-50 #FFFBEB, amber-200 #FDE68A, amber-300 #FCD34D, amber-600 #D97706, amber-700 #B45309, amber-800 #92400E)
- テキスト: neutral-500 #737373, neutral-600 #525252, neutral-700 #404040
- 削除: red-50 #FEF2F2, red-300 #FCA5A5, red-600 #DC2626

## 使用API

| メソッド | エンドポイント | 用途 |
|----------|---------------|------|
| - | - | API変更なし（UI変更のみ） |

> **注意**: API連携は全て実装・テスト済み。本タスクはUI変更のみ。

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi
- portrait/landscape対応
- テーマ: NoActionBar + Compose TopAppBar
- **P21ヘッダーは現行デザインを維持**（TopAppBar + badges）
- Body部分のみHTMLリファレンスに合わせる
- ViewModel・State・ビジネスロジックの変更は最小限

## 対象ファイル

### 既存変更（UI変更のみ）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt` — P21 Body部分リデザイン

### 参照のみ（変更禁止）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryViewModel.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/PickingHistoryScreen.kt`
- `core/domain/` — ドメイン層変更なし

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: P21 Body リデザイン | 完了 | 2026-02-27 | 左ペイン(商品情報+数量+ボタン)+右ペイン(履歴) |
| P2: ビルド確認 | 完了 | 2026-02-27 | :feature:outbound + :app BUILD SUCCESSFUL |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 現行P21の構造
- OutboundPickingScreen.kt (896行)
- レイアウト: Row { 左Column(ItemInformationCard) + 右Column(QuantityInputCard) }
- BottomBar: OutboundPickingBottomBar (6ボタン: 画像/コース変更/履歴/前へ/登録/次へ)
- ダイアログ: CompletionConfirmationDialog, ImageViewerDialog

### 現行P21のState
- OutboundPickingState: originalTask, pendingItems, currentIndex, pickedQtyInput等
- 履歴表示に必要: originalTaskの全items中、status != PENDING のもの

### カラー定数（ヘッダーで使用、維持）
- TitleRed = 0xFFC0392B
- AccentOrange = 0xFFE67E22
- DividerGold = 0xFFF9A825
- HeaderBg = 0xFFFDFBF2
- BadgeGreen = 0xFF27AE60

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

### P1: P21 Body リデザイン
- 完了日: 2026-02-27
- 実績:
  - OutboundPickingScreen.kt を1166行にリデザイン（旧896行）
  - ヘッダー(TopAppBar + badges)は完全維持
  - Body: 左ペイン(商品情報+ロケ/伝票+受注数/出荷数+ボタン) + 右ペイン(履歴リスト)
  - BottomBar廃止、ボタン(前へ/登録/次へ)は左ペイン内に統合
  - 削除コンポーネント: ItemInformationCard, QuantityInputCard, OutboundPickingBottomBar, InfoRow
  - 新規コンポーネント: OutboundPickingBody, HistoryItemRow
  - 維持: CompletionConfirmationDialog, ImageViewerDialog
  - Body背景: Color.White → Color(0xFFF5F5F5) (neutral-100)
  - カラースキーム: amber/neutral系をHTML準拠で実装
  - :feature:outbound:compileDebugKotlin BUILD SUCCESSFUL

### P2: ビルド確認
- 完了日: 2026-02-27
- 実績:
  - :feature:outbound:compileDebugKotlin BUILD SUCCESSFUL
  - :app:compileDebugKotlin BUILD SUCCESSFUL
  - 警告のみ（deprecated hiltViewModel, SubcomposeAsyncImage state checks）
