# 開発ハネス（設計・実装の制約）

本プロジェクトにおける「不変のルール」を定義する。AIは作業の各Phase完了時に、このハネスへの適合性を自己チェックしなければならない。

## 1. 物理制約・環境

- **画面解像度**: 1080 x 2400 (420dpi / Android 13準拠)
  - 広い画面を活かし、情報密度を高めること。
  - 基本は Portrait だが、P21などは Landscape にも対応する。
- **ターゲットSDK**: 36 (Android 15) / Min SDK 26
- **UIフレームワーク**: Jetpack Compose (Material 3)

## 2. デザイン・UIパターン (Design Harness)

- **ヘッダー構成 (3段パターン)**
  - 1段目: タイトル、時計、電池残量、Wi-Fi（システムバー代替）
  - 2段目: 補助情報（コース名、作業者名など）
  - 3段目: 進捗バー（オレンジ背景に白文字）
- **配色定義**
  - `TitleRed`: #C0392B (ヘッダー文字)
  - `AccentOrange`: #E67E22 (進捗バー、ボタン)
  - `DividerGold`: #F9A825 (区切り線)
  - `HeaderBg`: #FDFBF2 (ヘッダー背景)
  - `BodyBg`: #F5F5F5 (メイン背景)
- **ファンクションキー**
  - `FunctionKeyBar` を画面下部に配置（F1〜F4）。
  - 各画面で一貫したキー割り当て（F2: 戻る、F3: 登録/次へ など）を行う。

## 3. 実装・コーディング規約 (Code Harness)

- **状態管理**: `ViewModel` + `StateFlow` (CollectAsStateWithLifecycle)
- **非同期処理**: `Kotlin Coroutines` (ViewModelScope)
- **依存性注入**: `Hilt`
- **通信**: `Retrofit` + `Kotlinx Serialization`
  - エラー時は `NetworkException` にラップして扱う。
  - `is_success: false` の場合は `errorMessage` をUIに表示する。
- **プレビュー**: 各Compose Screenには、Portrait/Landscape両方のPreviewを実装すること。

## 4. プロセス制約 (Process Harness)

- **APIテスト**: 実装前に必ず `curl` または単体テストでエンドポイントを確認する。
- **セッション永続化**: 作業状況は常に `prompts/{PLAN_ID}/boot.md` に記録する。
- **仕様更新**: 画面の変更・追加時は `prompts/pages.md` を必ず最新に保つ。
- **認証情報**: `local.properties` を参照し、絶対にコミットに含めない。
- **エラーログ**: サーバーエラー発生時は `prompts/{PLAN_ID}/error.log` に詳細を記録する。
