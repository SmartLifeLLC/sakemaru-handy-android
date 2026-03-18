# P01ログイン画面をナビゲーション起点に変更 作業計画

## 前提

### 参照仕様
- API仕様: `prompts/api.md`
- デザイン仕様: `prompts/design.md`
- 画面一覧: `prompts/pages.md`

### 完了済みの作業・現在の状況

現状の起動フロー:
1. `MainActivity.onCreate()` で `validateSession()` を呼び出し
2. `tokenManager.isLoggedIn()` が true かつサーバー検証成功 → `startDestination = Routes.Main.route`
3. それ以外 → `startDestination = Routes.Login.route`
4. **セッション有効時はログイン画面がスキップされ、Main が startDestination になる**

目標フロー:
1. 常に `startDestination = Routes.Login.route`
2. `LoginViewModel.init` でセッション検証を実施
3. セッション有効 → `isSuccess = true` → LoginScreen が `onLoginSuccess` コールバックを呼ぶ → Main へ遷移
4. セッション無効 → `isCheckingSession = false` → 通常のログインフォームを表示

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | 現状調査・影響確認 | LoginScreen.kt の isSuccess 処理とコールバック確認 | 変更の影響範囲を把握 |
| P2 | LoginState 修正 | `isCheckingSession` フィールド追加 | ビルドが通る |
| P3 | LoginViewModel 修正 | init でセッション検証、結果を State に反映 | セッション有効時に isSuccess=true になる |
| P4 | MainActivity 修正 | validateSession() 削除、startDestination 固定 | 常に Login が起点になる |
| P5 | 動作確認 | 3パターンの動作テスト | 全パターンが期待通りに動作 |

---

## P1: 現状調査・影響確認

### 目的
LoginScreen.kt が `isSuccess` をどう使っているかを確認し、変更の影響範囲を把握する。

### 調査手順
1. `LoginScreen.kt` を読んで `isSuccess` の処理箇所を確認
2. `isSuccess = true` になったとき `onLoginSuccess` コールバックが呼ばれるか確認
3. `MainActivity.onResume()` のセッション検証ロジック（`tokenManager.clearAuth()` + `recreate()`）を確認し、変更後も維持するか判断

### 確認ポイント
- `LoginScreen.kt` で `LaunchedEffect(state.isSuccess)` or `if (state.isSuccess)` のような処理があるか
- セッション検証中の UI（ローディング）は LoginScreen 側で表示するか？

### 完了条件
- 変更が必要なファイルが boot.md の「対象ファイル」と一致することを確認
- `isCheckingSession` 状態の UI 表示方針を決定

---

## P2: LoginState 修正

### 目的
セッション検証中のローディング状態を表すフィールドを追加する。

### 修正方針
```kotlin
// 変更前
data class LoginState(
    val staffCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

// 変更後
data class LoginState(
    val staffCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isCheckingSession: Boolean = true   // 追加: 起動時セッション検証中フラグ
)
```

### 修正対象ファイル
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginState.kt`

### 完了条件
- `isCheckingSession: Boolean = true` が追加されビルドが通る

---

## P3: LoginViewModel 修正

### 目的
アプリ起動時にセッション検証を行い、有効なら自動ログイン（`isSuccess = true`）、無効なら通常フォームを表示する。

### 修正方針

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val hostPreferences: HostPreferences
) : ViewModel() {

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (!tokenManager.isLoggedIn()) {
            // トークンなし → ローディング終了、フォーム表示
            _state.update { it.copy(isCheckingSession = false) }
            return
        }
        viewModelScope.launch {
            authRepository.validateSession()
                .onSuccess {
                    // セッション有効 → 自動ログイン
                    _state.update { it.copy(isCheckingSession = false, isSuccess = true) }
                }
                .onFailure {
                    // セッション無効 → トークンクリアしてフォーム表示
                    tokenManager.clearAuth()
                    _state.update { it.copy(isCheckingSession = false) }
                }
        }
    }
    // ... 既存のメソッド（login, onStaffCodeChange, etc.）は変更なし
}
```

### 修正対象ファイル
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginViewModel.kt`

### 注意事項
- `authRepository.validateSession()` は `MainActivity.kt` ですでに使用されているメソッド
- `tokenManager.isLoggedIn()` も既存メソッドを流用
- `isCheckingSession = false` を必ず `onSuccess` / `onFailure` 両方で設定すること

### 完了条件
- `init` に `checkExistingSession()` が追加されている
- セッション有効時: `isCheckingSession = false, isSuccess = true`
- セッション無効時: `isCheckingSession = false`（トークンクリア済み）

---

## P4: MainActivity 修正

### 目的
`validateSession()` ロジックを削除し、`startDestination` を常に `Routes.Login.route` に固定する。

### 修正方針

```kotlin
// 変更前
LaunchedEffect(Unit) {
    startDestination = validateSession()
    isValidating = false
    isSessionValidated = true
}

if (isValidating) {
    Box(...) { CircularProgressIndicator() }
} else {
    HandyNavHost(navController, startDestination)
}

// 変更後（isValidating / validateSession() を削除）
HandyNavHost(
    navController = rememberNavController(),
    startDestination = Routes.Login.route
)
```

削除するもの:
- `var isValidating by remember { mutableStateOf(true) }`
- `var startDestination by remember { mutableStateOf(Routes.Login.route) }`
- `LaunchedEffect(Unit) { startDestination = validateSession(); ... }`
- `CircularProgressIndicator()` のローディングブロック
- `validateSession()` サスペンド関数全体
- `private var isSessionValidated = false`
- `@Inject lateinit var authRepository: AuthRepository`（P3でLoginVMが使うため不要になる）
- `authRepository.validateSession()` のimport

維持するもの:
- `onResume()` のセッション検証（トークンが存在している間は定期確認）
  - ただし `isSessionValidated` フラグが不要になるので、`tokenManager.isLoggedIn()` のみで判定
- `@Inject lateinit var tokenManager: TokenManager`（onResume で使用）

### 修正対象ファイル
- `app/src/main/java/biz/smt_life/android/sakemaru_handy_denso/MainActivity.kt`

### 完了条件
- `startDestination` が常に `Routes.Login.route`
- `validateSession()` 関数が削除されている
- `onResume()` が維持されている（`tokenManager.isLoggedIn()` で判定）
- ビルドが通る

---

## P5: 動作確認

### 目的
変更後の3パターンの動作を手動テストで確認する。

### テストパターン

| # | 状況 | 期待動作 |
|---|------|---------|
| 1 | **セッション無効（未ログイン）** | ログイン画面がローディング後に表示される |
| 2 | **セッション有効（ログイン済み）** | ログイン画面が一瞬表示されてから自動でMain画面へ遷移 |
| 3 | **ログアウト操作** | Main → Login へ遷移し、フォームが空で表示される |

### 確認手順
1. アプリをクリーンインストール（未ログイン状態）→ パターン1確認
2. ログイン実施 → アプリを再起動 → パターン2確認
3. Main画面からログアウト → パターン3確認

### 完了条件
- 全3パターンが期待通りに動作すること
- ビルドエラーがないこと
- `onResume()` でセッション切れ時に Login へ戻ること（任意確認）

---

## 制約（厳守）

- `local.properties` の認証情報はコミットしない
- `LoginScreen.kt` は変更しない（isSuccess によるコールバック呼び出しは既存のまま利用）
- `HandyNavHost.kt` / `Routes.kt` は変更しない
- サーバ側エラーは `error.log` に記録する

## 全体完了条件

- P01ログイン画面が常に startDestination になっている
- セッション有効時に自動でMain画面へ遷移する
- ビルド成功・全3動作パターンが確認済み
