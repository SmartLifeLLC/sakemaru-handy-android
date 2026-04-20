# P01 ログイン画面 - サーバ接続確認ボタン追加

- **作成日**: 2026-03-17
- **ステータス**: ドラフト
- **対象画面**: P01 ログイン画面 (LoginScreen)
- **ディレクトリ**: `feature/login/`

## 背景・目的

ログイン画面でサーバへの接続状況が不明なため、ログイン操作前にAPI通信の疎通確認ができるボタンを追加する。設定画面でホストURLを変更した後や、ネットワーク環境が不安定な現場での運用時に、サーバ到達性を事前に確認できるようにする。

## 現状の実装

### LoginScreen.kt
- スタッフコード・パスワード入力フィールド
- ログインボタン
- フッター: 日付、バージョン、ホストURL表示
- トップバー: 設定アイコンボタン

### LoginViewModel.kt
- `AuthRepository` を使用したログイン処理
- `HostPreferences` からベースURL取得
- `NetworkException` によるエラーハンドリング

### AuthService.kt
- `GET /api/me` — セッション検証（Bearer token必要）
- `POST /api/auth/login` — ログイン（API Key必要、token不要）

## 変更内容

### 概要

ログイン画面のフッター付近（ホストURL表示の隣）に「接続確認」ボタンを追加。タップすると現在設定されているホストURLに対してHTTPリクエストを送信し、サーバの到達性を確認して結果を表示する。

### 詳細設計

#### 接続確認のAPI仕様

認証不要のエンドポイントで疎通確認を行う。以下の優先順で検討：

1. **`POST /api/auth/login` に空リクエスト** → API Keyヘッダー付きでリクエストし、レスポンスが返ること（バリデーションエラーでもOK）で接続確認
2. または **ベースURL (`/`) にGETリクエスト** → HTTP 302等のレスポンスが返れば接続OK

**推奨**: 方式1（`/api/auth/login` への空POST）。API Keyインターセプターが既に設定済みのため、既存のRetrofitクライアントをそのまま使える。バリデーションエラー（422等）が返れば「サーバに到達できている」と判断可能。

#### LoginState 変更

```kotlin
data class LoginState(
    val staffCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    // 追加
    val isCheckingConnection: Boolean = false,
    val connectionResult: ConnectionResult? = null
)

enum class ConnectionResult {
    SUCCESS,  // サーバ接続OK
    FAILURE   // サーバ接続NG
}
```

#### LoginViewModel 変更

```kotlin
fun checkConnection() {
    viewModelScope.launch {
        _state.update { it.copy(isCheckingConnection = true, connectionResult = null) }
        try {
            // AuthRepository に接続確認メソッドを追加
            authRepository.checkConnection()
            _state.update { it.copy(isCheckingConnection = false, connectionResult = ConnectionResult.SUCCESS) }
        } catch (e: Exception) {
            _state.update { it.copy(isCheckingConnection = false, connectionResult = ConnectionResult.FAILURE) }
        }
    }
}

fun clearConnectionResult() {
    _state.update { it.copy(connectionResult = null) }
}
```

#### AuthRepository / AuthService 変更

`AuthRepository` インターフェースに追加:
```kotlin
suspend fun checkConnection(): Result<Unit>
```

`AuthRepositoryImpl` に実装:
```kotlin
override suspend fun checkConnection(): Result<Unit> {
    return try {
        // login に空値を送ってバリデーションエラーでもレスポンスが返ればOK
        authService.login(code = "", password = "", deviceId = "")
        Result.success(Unit)
    } catch (e: HttpException) {
        // 4xx系（422 Validation等）もサーバ到達 = 成功
        if (e.code() in 400..499) {
            Result.success(Unit)
        } else {
            Result.failure(e)
        }
    } catch (e: IOException) {
        // ネットワーク到達不可
        Result.failure(e)
    }
}
```

#### UI変更 (LoginScreen.kt)

フッターのホストURL表示行の横に「接続確認」テキストボタンを追加:

```
[日付]
Ver.1.0 (https://wms.example.com)  [接続確認]
```

- **通常時**: 「接続確認」テキストボタン（TextButton）
- **確認中**: 小さなCircularProgressIndicator
- **成功時**: 「接続OK」を緑色テキストで表示（3秒後に自動消去）
- **失敗時**: 「接続NG」を赤色テキストで表示（3秒後に自動消去）

### 影響範囲

| ファイル | 影響 |
|---|---|
| `LoginState.kt` | フィールド追加 |
| `LoginViewModel.kt` | `checkConnection()` メソッド追加 |
| `LoginScreen.kt` | UI追加（フッター部分） |
| `AuthRepository.kt` | `checkConnection()` メソッド追加 |
| `AuthRepositoryImpl.kt` | 実装追加 |

## 制約

- ログインボタンの動作・位置に影響を与えないこと
- 接続確認は認証不要（トークンなし）で行うこと
- 接続確認中もUI操作（スタッフコード入力等）をブロックしないこと
- 既存のRetrofit/OkHttpクライアント（API Keyインターセプター付き）を使用すること

## 対象ファイル

### 既存変更
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginState.kt`
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginViewModel.kt`
- `feature/login/src/main/java/biz/smt_life/android/feature/login/LoginScreen.kt`
- `core/domain/src/main/java/biz/smt_life/android/core/domain/repository/AuthRepository.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/repository/AuthRepositoryImpl.kt`

### 参照のみ
- `core/network/src/main/java/biz/smt_life/android/core/network/api/AuthService.kt`
- `core/network/src/main/java/biz/smt_life/android/core/network/NetworkResult.kt`
- `core/ui/src/main/java/biz/smt_life/android/core/ui/HostPreferences.kt`
- `prompts/pages.md`

## 確認事項

1. **接続確認のエンドポイント**: `/api/auth/login` への空POSTで良いか？サーバ側に専用の `/api/health` や `/api/ping` エンドポイントがあれば、そちらを使う方が望ましい
/api/auth/login` への空POSTで良い
2. **結果表示のデザイン**: テキストボタン + 結果テキスト表示で良いか？ダイアログやSnackbarの方が良い場合は要検討
   テキストボタン + 結果テキスト表示で良い
3. **自動消去の時間**: 成功/失敗表示を3秒後に消す仕様で良いか？
5秒