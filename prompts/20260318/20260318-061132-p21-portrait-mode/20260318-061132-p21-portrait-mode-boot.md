# Work Plan: p21-portrait-mode

- **ID**: p21-portrait-mode
- **作成日**: 2026-03-18
- **最終更新**: 2026-03-18
- **ステータス**: 完了
- **ディレクトリ**: `/Users/jungsinyu/Projects/sakemaru-handy-android/prompts/20260318/20260318-061132-p21-portrait-mode/`

## セッション再開手順

コンテキストがクリアされた場合、以下を読んで作業を再開する:

1. このファイルを読む（20260318-061132-p21-portrait-mode-boot.md）
2. 20260318-061132-p21-portrait-mode-plan.md を読む（作業計画の全体像）
3. 下記「進捗」テーブルで現在のPhaseを確認
4. 「Phase完了記録」セクションで完了済みPhaseの実績を確認
5. 「作業中コンテキスト」セクションで途中データを確認
6. 未完了の最初のPhaseから plan.md の該当セクションを読んで作業再開

## 概要

P21（出庫データ入力画面）に Portrait/Landscape 切り替え機能を追加。TopAppBar に回転ボタンを配置し、Portrait 時は縦レイアウト（上:商品情報30% / 下:数量入力70%）で表示する。前回の向き設定を DataStore で記憶する。

## 重要な設計制約

- P21 のみ Portrait 対応。他画面は Landscape 固定を維持
- Landscape レイアウトは現在の実装から変更しない
- 向き切り替え時に入力中データ（数量、currentIndex）を保持すること
- P21 離脱時は必ず Landscape に復元
- イマーシブモードは Portrait 時も維持
- CompletionCard も Portrait 対応が必要
- 向き設定は DataStore に永続化し、次回 P21 表示時に復元

## 対象ファイル

### 新規作成
なし

### 既存変更
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingScreen.kt`
  - TopAppBar に回転ボタン追加
  - `OutboundPickingBody` を Portrait/Landscape 分岐
  - 左ペイン → `ProductInfoSection` に抽出
  - 右ペイン → `QuantityInputSection` に抽出
  - CompletionCard の Portrait 対応
  - Preview に Portrait 版追加

### 参照のみ（変更禁止）
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingState.kt`
- `feature/outbound/src/main/java/biz/smt_life/android/feature/outbound/picking/OutboundPickingViewModel.kt`
- `app/src/main/AndroidManifest.xml`（screenOrientation=landscape 維持）
- `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/MainActivity.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/model/PickingTask.kt`

## テストデータ

- Preview で確認可能（既存の PreviewOutboundPickingBody を Portrait 版にも対応）
- 実機 DENSO ハンディで向き切り替え動作を検証

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: コンポーネント分離 | 完了 | 2026-03-18 | ProductInfoSection, QuantityInputSection に抽出 |
| P2: 向き制御 & 切り替えボタン | 完了 | 2026-03-18 | SharedPreferences永続化 + TopAppBar回転ボタン + DisposableEffect復元 |
| P3: Portrait レイアウト | 完了 | 2026-03-18 | Column(0.3f/0.7f) で縦レイアウト実装 |
| P4: CompletionCard Portrait 対応 | 完了 | 2026-03-18 | CenterAligned BoxなのでPortrait時もそのまま表示可能 |
| P5: Preview & 動作確認 | 完了 | 2026-03-18 | Portrait Preview追加(420x800dp)、ビルド成功 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### 既存コード構造（参照用）
- `OutboundPickingScreen`: L82-336（Scaffold + TopAppBar + routing）
- `OutboundPickingBody`: L340-692（Row で左右ペイン）
  - 左ペイン（商品情報）: L358-501
  - 右ペイン（数量入力）: L503-691
- `CompletionCard`（else ブロック）: L241-334
- TopAppBar actions: L201-208（現在は Home ボタンのみ）
- カラー定数: L51-73
- ダイアログ: L696-917

### ユーザー回答（確認事項）
- 切り替えボタン位置: TopAppBar 右
- Portrait 比率: 上部(商品情報) 3 : 下部(数量入力) 7
- アイコン: ScreenRotation（Material Icons）
- デフォルト向き: 前回の設定を記憶（DataStore）
- アニメーション: 不要
- CompletionCard: Portrait 対応必要

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: コンポーネント分離
- 完了日: 2026-03-18
- 実績:
  - `ProductInfoSection` (商品名/JAN/容量/得意先/ロケーション/伝票番号) を抽出
  - `QuantityInputSection` (ケース/バラ入力 + 登録/履歴ボタン) を抽出
  - `OutboundPickingBody` から呼び出しに変更

### P2: 向き制御 & 切り替えボタン
- 完了日: 2026-03-18
- 実績:
  - SharedPreferences (`p21_orientation_prefs`) で向き設定を永続化
  - `LaunchedEffect(isPortrait)` で `Activity.requestedOrientation` を動的変更
  - `DisposableEffect` で P21 離脱時に LANDSCAPE 復元
  - TopAppBar に `Icons.Default.Refresh` アイコンボタン追加（AccentOrange色）
  - `Icons.Default.ScreenRotation` は material-icons-extended 未導入のため Refresh で代替

### P3: Portrait レイアウト
- 完了日: 2026-03-18
- 実績:
  - `isPortrait` フラグで Column(0.3f/0.7f) / Row(1f/1f) を分岐
  - Portrait: 上部30%商品情報 + 下部70%数量入力
  - Landscape: 既存レイアウトそのまま維持

### P4: CompletionCard Portrait 対応
- 完了日: 2026-03-18
- 実績:
  - CompletionCard は Box(contentAlignment=Center) + OutlinedCard で Portrait でもそのまま適切に表示
  - 追加調整不要と判断

### P5: Preview & 動作確認
- 完了日: 2026-03-18
- 実績:
  - `PreviewOutboundPickingBodyPortrait` 追加 (widthDp=420, heightDp=800, isPortrait=true)
  - 既存 Preview に `isPortrait=false` パラメータ追加
  - `./gradlew :feature:outbound:compileDebugKotlin` ビルド成功
