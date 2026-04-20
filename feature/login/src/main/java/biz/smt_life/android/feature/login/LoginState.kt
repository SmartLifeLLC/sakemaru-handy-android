package biz.smt_life.android.feature.login

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
