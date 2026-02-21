package com.tylermolamphy.sharetocalendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tylermolamphy.sharetocalendar.ui.EventConfirmationScreen
import com.tylermolamphy.sharetocalendar.ui.SettingsScreen
import com.tylermolamphy.sharetocalendar.ui.theme.ShareToCalendarTheme

private object NavRoutes {
    const val CONFIRM = "confirm"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sharedText = extractSharedText(intent)

        setContent {
            ShareToCalendarTheme {
                AppNavigation(
                    sharedText = sharedText,
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newText = extractSharedText(intent)
        if (newText != null) {
            sharedText = newText
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }
}

@Composable
fun AppNavigation(
    sharedText: String?,
    onFinish: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (sharedText != null) NavRoutes.CONFIRM else NavRoutes.SETTINGS

    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavRoutes.SETTINGS) {
            SettingsScreen()
        }
        composable(NavRoutes.CONFIRM) {
            EventConfirmationScreen(
                sharedText = sharedText ?: "",
                onDismiss = onFinish
            )
        }
    }
}
