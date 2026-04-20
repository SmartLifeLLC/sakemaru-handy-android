# Android Plan: login-screen-to-top

- **ID**: login-screen-to-top
- **作成日**: 2026-02-19
- **最終更新**: 2026-02-19
- **ステータス**: 進行中
- **ディレクトリ**: prompts/login-screen-to-top/

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

アプリ起動時に `MainActivity.validateSession()` がセッション有効なら直接 Main へ飛ぶ実装になっている。
P01ログイン画面を常にナビゲーションの起点（startDestination）にし、セッション検証ロジックを `LoginViewModel` へ移動する。

## 使用API

| メソッド | エンドポイント | 用途 |
|----------|---------------|------|
| GET | `/api/me` | セッション検証（LoginViewModel内で利用） |

## 重要な設計制約

- 画面解像度: 1080 x 2400, 420dpi
- portrait/landscape対応、API Level 33
- テーマ: NoActionBar + Compose TopAppBar
- 画面構成: Scaffold + TopAppBar + FunctionKeyBar
- **ログイン画面は常に NavHost の startDestination = `Routes.Login.route` とする**
- **セッション有効時は LoginScreen が自動で Main へ遷移する**
- **MainActivity の validateSession() ロジックは LoginViewModel へ移行する**

## 対象ファイル

### 既存変更
- `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/MainActivity.kt`
  - `validateSession()` を削除し、startDestination を常に `Routes.Login.route` に固定
  - `onResume` のセッション検証は維持（再起動で Login へ戻す）
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginViewModel.kt`
  - `init` ブロックで `checkExistingSession()` を呼び出し
  - セッション有効なら `isSuccess = true` をセット（自動遷移をトリガー）
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginState.kt`
  - `isCheckingSession: Boolean = true` フィールドを追加

### 参照のみ（変更禁止）
- `app/src/main/java/.../navigation/HandyNavHost.kt`（ルーティング定義は変更不要）
- `app/src/main/java/.../navigation/Routes.kt`（ルート定義は変更不要）
- `feature/login/src/main/java/.../LoginScreen.kt`（既に `isSuccess` で遷移コールバック呼び出し済み）

## テストデータ

テストユーザー: `local.properties` の `API_TEST_USER` / `API_TEST_PW` を参照

---

## 進捗

| Phase | 状態 | 更新日 | 備考 |
|-------|------|--------|------|
| P1: 現状調査・影響確認 | 完了 | 2026-02-19 | LaunchedEffect(isSuccess) 確認済み |
| P2: LoginState 修正 | 完了 | 2026-02-19 | isLoading=true 初期値で代替、変更不要 |
| P3: LoginViewModel 修正 | 完了 | 2026-02-19 | init + checkExistingSession() 追加 |
| P4: MainActivity 修正 | 完了 | 2026-02-19 | validateSession() 削除・startDestination 固定 |
| P5: 動作確認 | 未着手 | - | セッション有効・無効・ログアウトの3パターン確認 |

---

## 作業中コンテキスト

> Phase作業中に蓄積される中間データ。セッション再開時に必ず確認。

### LoginScreen.kt の isSuccess 処理（P1確認後に記入）
- `LaunchedEffect(state.isSuccess)` (line 74) で isSuccess=true → onLoginSuccess() 自動呼び出し
- `isLoading=true` でフォームフィールド無効化・ボタンにスピナー表示（LoginScreen変更不要）
- `LoginState.isCheckingSession` は不要と判断。`isLoading=true` を初期値で代替

### Git ブランチ
- 作業ブランチ: feature/design-upgrade
- ベースブランチ: master

---

## Phase完了記録

> 各Phase完了時にここに実績を追記する。

### P1: 現状調査・影響確認
- 完了日: 2026-02-19
- 実績:
  - LoginScreen.kt: `LaunchedEffect(state.isSuccess)` で自動遷移済み確認
  - `isLoading=true` を初期値にすることで LoginScreen 変更不要と判断

### P2: LoginState 修正
- 完了日: 2026-02-19
- 実績:
  - `isCheckingSession` フィールド追加は不要と判断
  - `LoginState.isLoading = true` を初期値にする方針で対応（LoginViewModel で設定）

### P3: LoginViewModel 修正
- 完了日: 2026-02-19
- 実績:
  - `_state = MutableStateFlow(LoginState(isLoading = true))` に変更
  - `init { checkExistingSession() }` を追加
  - `checkExistingSession()`: トークンなし→isLoading=false、セッション有効→isSuccess=true、無効→tokenクリア+isLoading=false

### P4: MainActivity 修正
- 完了日: 2026-02-19
- 実績:
  - `private var isSessionValidated` フィールド削除
  - `validateSession()` サスペンド関数削除
  - `LaunchedEffect` + ローディングUI削除
  - `startDestination = Routes.Login.route` に固定
  - `onResume()`: `isSessionValidated` フラグ削除、`tokenManager.isLoggedIn()` のみで判定

### P5: 動作確認
- 完了日: -
- 実績:
  - (完了後に記入)
