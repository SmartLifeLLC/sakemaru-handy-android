package biz.smt_life.android.sakemaru_handy_denso

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.ui.TokenManager
import biz.smt_life.android.sakemaru_handy_denso.navigation.HandyNavHost
import biz.smt_life.android.sakemaru_handy_denso.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity with session validation per Task 4.
 * Validates stored token with server on app start and resume.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide status bar and navigation bar for full-screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Keep screen awake during operations
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            HandyTheme {
                val navController = rememberNavController()
                HandyNavHost(
                    navController = navController,
                    startDestination = Routes.Login.route
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Validate session on resume; recreate if expired so LoginViewModel re-checks
        if (tokenManager.isLoggedIn()) {
            lifecycleScope.launch {
                authRepository.validateSession()
                    .onFailure {
                        tokenManager.clearAuth()
                        recreate()
                    }
            }
        }
    }
}