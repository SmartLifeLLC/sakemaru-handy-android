# P21 ヘッダーデザイン改修 作業計画

## Phase 1: ヘッダー用共通コンポーネントの設計と実装

### 1-1: タイマー状態の管理
- [ ] `OutboundPickingScreen` に `LaunchedEffect` を使用した秒単位のタイマーを実装。
- [ ] `MM:SS` 形式のフォーマット関数を作成。

### 1-2: 3段構成ヘッダーコンポーネントの作成
- [ ] `OutboundPickingHeader` コンポーネントの新規作成。
- [ ] 縦画面（Portrait）用レイアウトの実装。
- [ ] 横画面（Landscape）用レイアウトの実装。
- [ ] デザイン仕様（`upgrade-design-2.md`）の忠実な再現。

### 1-3: テキスト縁取り（Outline）の実装
- [ ] 進捗バー上のテキストに `Shadow` を適用し、オレンジ背景でも視認性を確保。

### ハネス適合チェック (Phase 1)
- [ ] `harness.md` のカラー定義（TitleRed, AccentOrange等）を使用しているか。
- [ ] 画面解像度 1080x2400 (420dpi) を考慮したサイズ設定か。

---

## Phase 2: OutboundPickingScreen への組み込み

### 2-1: 既存ヘッダーの置き換え
- [ ] `Scaffold` の `topBar` を `OutboundPickingHeader` に変更。
- [ ] ボディ部分のパディング（`PaddingValues`）の調整。

### 2-2: 画面回転ボタンの動作確認
- [ ] 「画面回転」ボタン押下時に正しく Landscape/Portrait が切り替わるか。

### ハネス適合チェック (Phase 2)
- [ ] ViewModel の既存ロジック（`moveToPrevGroup`, `moveToNextGroup`等）との連携が正しいか。

---

## Phase 3: プレビューと最終確認

### 3-1: プレビューの実装
- [ ] PortraitPreview, LandscapePreview の作成。

### 3-2: 実機/シミュレータでの動作確認
- [ ] デザイン崩れがないか、操作性が確保されているか。

### ハネス適合チェック (Phase 3)
- [ ] `pages.md` の P21 定義が最新のUIと一致しているか。
