# ローカルサーバ接続設定と設定画面エラー修正
when complete:
- http://10.0.2.2 サーバへのアクセスとAPIの利用が可能
- /Users/jungsinyu/Projects/sakemaru-handy-android/prompts/pages.md　のP03ページの設定ボタンをクリックしてエラーが発生しない（設定画面が表示される）


## 概要
sakemaru-handy-android プロジェクトでローカル開発サーバ（10.0.2.2）に接続するための設定と、設定ボタンクリック時のエラー修正方法

---

## 1. ローカルサーバ接続設定（10.0.2.2）

### 1.1 HostPreferences.kt のデフォルトURL変更

**ファイル**: `core/ui/src/main/java/biz/smt_life/android/core/ui/HostPreferences.kt`

**現在の設定（本番用）**:
```kotlin
companion object {
    private val BASE_URL_KEY = stringPreferencesKey("base_url")
    const val DEFAULT_BASE_URL = "https://wms.lw-hana.net/"
}
```

**ローカル開発用に変更**:
```kotlin
companion object {
    private val BASE_URL_KEY = stringPreferencesKey("base_url")
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
}
```

> **注意**: `10.0.2.2` はAndroidエミュレータからホストマシンのlocalhostにアクセスするための特別なIPアドレスです

### 1.2 network_security_config.xml の作成

HTTP（非HTTPS）通信を許可するための設定ファイルを作成します。

**ファイル作成**: `app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

### 1.3 AndroidManifest.xml への参照追加

**ファイル**: `app/src/main/AndroidManifest.xml`

`<application>` タグに `android:networkSecurityConfig` 属性を追加:

```xml
<application
    android:name=".HandyApplication"
    android:allowBackup="true"
    android:networkSecurityConfig="@xml/network_security_config"
    ... 他の属性 ...
>
```

**変更後の完全なapplicationタグ**:
```xml
<application
    android:name=".HandyApplication"
    android:allowBackup="true"
    android:networkSecurityConfig="@xml/network_security_config"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.Sakemaruhandydenso">
```

---

## 2. 設定画面エラーの修正

### 2.1 問題の原因

`LoginScreen.kt` で `HostPreferences` を手動でインスタンス化しているため、Hiltのシングルトン管理と競合している可能性があります。

**問題のあるコード** (`feature/login/src/main/java/.../LoginScreen.kt` 61-63行目):
```kotlin
val context = androidx.compose.ui.platform.LocalContext.current
val hostPreferences = remember { HostPreferences(context) }
val hostUrl by hostPreferences.baseUrl.collectAsState(initial = HostPreferences.DEFAULT_BASE_URL)
```

### 2.2 修正方法

#### オプション A: LoginViewModel経由で取得（推奨）

**LoginViewModel.kt** に追加:
```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val hostPreferences: HostPreferences  // 追加
) : ViewModel() {

    val hostUrl: StateFlow<String> = hostPreferences.baseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HostPreferences.DEFAULT_BASE_URL)

    // ... 既存のコード
}
```

**LoginScreen.kt** を修正:
```kotlin
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    appVersion: String = "1.0",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val hostUrl by viewModel.hostUrl.collectAsState()  // ViewModelから取得
    val focusManager = LocalFocusManager.current

    // 以下のコードを削除:
    // val context = androidx.compose.ui.platform.LocalContext.current
    // val hostPreferences = remember { HostPreferences(context) }
    // val hostUrl by hostPreferences.baseUrl.collectAsState(initial = HostPreferences.DEFAULT_BASE_URL)

    // ... 残りのコード
}
```

#### オプション B: ApplicationContextを使用

最小限の変更で修正する場合:

```kotlin
val context = LocalContext.current.applicationContext  // applicationContextを使用
val hostPreferences = remember { HostPreferences(context) }
```

---

## 3. 重要な注意事項

### 3.1 URL変更後のアプリ再起動

`NetworkModule.kt` でRetrofitインスタンスは起動時に一度だけ作成されます:

```kotlin
@Provides
@Singleton
fun provideRetrofit(...): Retrofit {
    val baseUrl = runBlocking { hostPreferences.getBaseUrlOnce() }
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        ...
}
```

**設定変更後はアプリの再起動が必要です**。設定画面で保存後、アプリを完全に終了して再起動してください。

### 3.2 本番リリース時の注意

本番リリース前に `HostPreferences.kt` のデフォルトURLを本番URLに戻すことを忘れないでください:

```kotlin
const val DEFAULT_BASE_URL = "https://wms.lw-hana.net/"
```

---

## 4. 変更ファイル一覧

| ファイル | 変更内容 |
|---------|---------|
| `core/ui/.../HostPreferences.kt` | DEFAULT_BASE_URL を `http://10.0.2.2:8000/` に変更 |
| `app/src/main/res/xml/network_security_config.xml` | 新規作成（HTTP通信許可設定） |
| `app/src/main/AndroidManifest.xml` | networkSecurityConfig 属性追加 |
| `feature/login/.../LoginViewModel.kt` | hostUrl StateFlow追加 |
| `feature/login/.../LoginScreen.kt` | HostPreferences参照をViewModel経由に変更 |

---

## 5. テスト手順

1. 上記の変更を適用
2. ローカルサーバ（localhost:8000）を起動
3. エミュレータでアプリをビルド・実行
4. ログイン画面下部に `http://10.0.2.2:8000/` が表示されることを確認
5. 設定ボタン（歯車アイコン）をクリックしてエラーが発生しないことを確認
6. ログインが正常に機能することを確認
