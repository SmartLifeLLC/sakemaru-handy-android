# P01 ログイン画面 サーバ接続確認ボタン 作業計画

## 前提

- P01ログイン画面は実装済み（LoginScreen / LoginViewModel / LoginState）
- AuthRepository / AuthRepositoryImpl で `POST /api/auth/login` を使用済み
- API Keyインターセプターは既存OkHttpClientに設定済み
- 確認事項の回答: エンドポイント=`/api/auth/login`空POST、UI=テキスト表示、自動消去=5秒

---

## Phase 一覧

| # | Phase | 概要 | 完了条件 |
|---|-------|------|---------|
| P1 | AuthRepository に checkConnection 追加 | Domain層にインターフェース追加、Network層に実装追加 | コンパイルエラーなし |
| P2 | LoginState・LoginViewModel 変更 | 接続確認の状態管理とロジック追加 | コンパイルエラーなし |
| P3 | LoginScreen UI 追加 | フッターに接続確認ボタンと結果表示を追加 | コンパイルエラーなし、Preview表示確認 |
| P4 | ビルド確認・動作テスト | gradleビルド成功 | `./gradlew assembleDebug` 成功 |

---

## P1: AuthRepository に checkConnection 追加

### 目的

サーバ到達性を確認するための `checkConnection()` メソッドをDomain層・Network層に追加する。

### 修正対象ファイル

1. **`core/domain/.../AuthRepository.kt`** — インターフェースにメソッド追加
2. **`core/network/.../AuthRepositoryImpl.kt`** — 実装追加

### 修正内容

#### AuthRepository.kt

```kotlin
interface AuthRepository {
    suspend fun login(staffCode: String, password: String): Result<AuthResult>
    suspend fun logout(): Result<Unit>
    suspend fun validateSession(): Result<AuthResult>
    suspend fun checkConnection(): Result<Unit>  // 追加
}
```

#### AuthRepositoryImpl.kt

```kotlin
override suspend fun checkConnection(): Result<Unit> {
    return try {
        authService.login(code = "", password = "", deviceId = "")
        // 正常レスポンスが返った場合も接続OK
        Result.success(Unit)
    } catch (e: HttpException) {
        // 4xx系（422 Validation, 401 Unauthorized等）= サーバに到達できている
        if (e.code() in 400..499) {
            Result.success(Unit)
        } else {
            Result.failure(e)
        }
    } catch (e: IOException) {
        // ネットワーク到達不可
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 完了条件

- 2ファイルの変更が完了し、コンパイルエラーがないこと

---

## P2: LoginState・LoginViewModel 変更

### 目的

接続確認の状態管理（確認中・結果）と、ViewModelに確認実行メソッドを追加する。

### 修正対象ファイル

1. **`feature/login/.../LoginState.kt`** — 状態フィールド追加
2. **`feature/login/.../LoginViewModel.kt`** — `checkConnection()` メソッド追加

### 修正内容

#### LoginState.kt

```kotlin
data class LoginState(
    val staffCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isCheckingConnection: Boolean = false,
    val connectionResult: ConnectionResult? = null
)

enum class ConnectionResult {
    SUCCESS,
    FAILURE
}
```

#### LoginViewModel.kt

```kotlin
fun checkConnection() {
    viewModelScope.launch {
        _state.update { it.copy(isCheckingConnection = true, connectionResult = null) }
        authRepository.checkConnection()
            .onSuccess {
                _state.update { it.copy(isCheckingConnection = false, connectionResult = ConnectionResult.SUCCESS) }
            }
            .onFailure {
                _state.update { it.copy(isCheckingConnection = false, connectionResult = ConnectionResult.FAILURE) }
            }
    }
}

fun clearConnectionResult() {
    _state.update { it.copy(connectionResult = null) }
}
```

### 完了条件

- 2ファイルの変更が完了し、コンパイルエラーがないこと

---

## P3: LoginScreen UI 追加

### 目的

ログイン画面のフッター部分に接続確認ボタンと結果表示を追加する。

### 修正対象ファイル

1. **`feature/login/.../LoginScreen.kt`** — UI変更

### 修正内容

#### LoginScreen composable

`LoginScreen` に `onCheckConnection` と `onClearConnectionResult` コールバックを追加:

```kotlin
LoginContent(
    ...
    onCheckConnection = viewModel::checkConnection,
    onClearConnectionResult = viewModel::clearConnectionResult,
    ...
)
```

#### LoginContent composable

パラメータ追加:
```kotlin
onCheckConnection: () -> Unit,
onClearConnectionResult: () -> Unit,
```

#### フッター部分の変更

現在のフッター:
```
[日付]
Ver.1.0 (hostUrl)
```

変更後:
```
[日付]
Ver.1.0 (hostUrl)
[接続確認ボタン] [結果テキスト]
```

具体的なUI:
- `TextButton` で「接続確認」テキスト
- 確認中: `CircularProgressIndicator`（size=16.dp）を表示
- 成功時: 「接続OK」を `Color(0xFF4CAF50)`（緑）で表示
- 失敗時: 「接続NG」を `MaterialTheme.colorScheme.error`（赤）で表示
- `LaunchedEffect(state.connectionResult)` で5秒後に `onClearConnectionResult()` を呼び出し自動消去

```kotlin
// フッター内に追加
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
) {
    TextButton(
        onClick = onCheckConnection,
        enabled = !state.isCheckingConnection
    ) {
        if (state.isCheckingConnection) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        } else {
            Text("接続確認", style = MaterialTheme.typography.bodySmall)
        }
    }
    state.connectionResult?.let { result ->
        Text(
            text = when (result) {
                ConnectionResult.SUCCESS -> "接続OK"
                ConnectionResult.FAILURE -> "接続NG"
            },
            color = when (result) {
                ConnectionResult.SUCCESS -> Color(0xFF4CAF50)
                ConnectionResult.FAILURE -> MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// 自動消去
LaunchedEffect(state.connectionResult) {
    if (state.connectionResult != null) {
        delay(5000)
        onClearConnectionResult()
    }
}
```

#### Preview の更新

既存Previewの `LoginContent` 呼び出しに `onCheckConnection = {}` と `onClearConnectionResult = {}` を追加。

### 完了条件

- LoginScreen.kt の変更が完了し、コンパイルエラーがないこと
- 全Previewが壊れていないこと

---

## P4: ビルド確認・動作テスト

### 目的

全変更がコンパイル通過し、アプリが正常にビルドできることを確認する。

### 手順

1. `./gradlew assembleDebug` を実行
2. エラーがあれば修正

### 完了条件

- `./gradlew assembleDebug` が成功すること

---

## 制約（厳守）

- ログインボタンの動作・位置を変更しないこと
- 接続確認は認証トークンなし（API Keyのみ）で行うこと
- 接続確認中もログイン入力操作をブロックしないこと
- 既存のRetrofitクライアント・インターセプターを再利用すること
- `AuthService.kt` に新規エンドポイントを追加しないこと（既存の `login` を流用）

## 全体完了条件

- `./gradlew assembleDebug` が成功
- ログイン画面フッターに「接続確認」ボタンが表示される
- タップするとサーバ接続を確認し、結果（OK/NG）が5秒間表示される
