anroid アプリにおいて顧客より次の改善依頼があったので　改善を実施したい。
/Users/jungsinyu/Projects/sakemaru-handy-android/prompts/pages.md を事前に確認する。



| **P21** | 出庫 - データ入力画面 | 
上部のデザインを変更

レイアウト仕様
画面の向き（縦・横）によって、3段構成のヘッダーレイアウトが以下のように変化します。
1. 縦画面（Portrait）時のレイアウト
参考スクショ　/Users/jungsinyu/Projects/sakemaru-handy-android/prompts/20260322/20260322-update-features/Portrait.png　
1段目 (ヘッダー): Rowで以下を配置
左端: 「もどる」テキストボタン
中央: 時計アイコン ＋ タイマーテキスト（MM:SS） ※グレーの丸角背景で囲む
右端: 「画面回転」テキストボタン
2段目 (進捗バー):
背景グレー、進捗部分オレンジ（#dd833a）のLinearProgressIndicator（角丸）。
バーの**中央（上にオーバーレイ）**に 1/47 のような「現在のページ/全ページ数」のテキストを配置。
※背景がオレンジになっても文字が読めるように、テキストには白い縁取り（ShadowやStroke）を実装すること。
3段目 (コントローラー): Rowで以下を配置
左端: 左矢印アイコンボタン（currentPageが1の場合は無効化）
中央: 「作業番号 {currentPage}/{totalPages}」のテキスト
右端: 右矢印アイコンボタン（currentPageが47の場合は無効化）

2. 横画面（Landscape）時のレイアウト
参考スクショ　/Users/jungsinyu/Projects/sakemaru-handy-android/prompts/20260322/20260322-update-features/Landscape.png
1段目 (ヘッダー): Rowで以下を配置
左端: 「もどる | 画面回転」のテキスト（2つのボタンを並べる）
中央: 時計アイコン ＋ タイマーテキスト（MM:SS）
右端: 「完了: {currentPage}/{totalPages}」のテキスト

2段目 (進捗バー):
背景グレー、進捗部分オレンジのLinearProgressIndicator（角丸）。
バーの**中央（上にオーバーレイ）**に 50% のような「パーセント表記」のテキストを配置。
※縦画面と同様に、文字が読めるよう白い縁取りを実装すること。

3段目 (コントローラー):
縦画面の3段目と全く同じレイアウトと機能。

