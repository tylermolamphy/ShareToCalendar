package com.tylermolamphy.sharetocalendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tylermolamphy.sharetocalendar.ui.EventConfirmationScreen
import com.tylermolamphy.sharetocalendar.ui.SettingsScreen
import com.tylermolamphy.sharetocalendar.ui.theme.ShareToCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = extractSharedText(intent)

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
        val sharedText = extractSharedText(intent)
        if (sharedText != null) {
            setContent {
                ShareToCalendarTheme {
                    AppNavigation(
                        sharedText = sharedText,
                        onFinish = { finish() }
                    )
                }
            }
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
    val startDestination = if (sharedText != null) "confirm" else "settings"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("settings") {
            SettingsScreen()
        }
        composable("confirm") {
            EventConfirmationScreen(
                sharedText = sharedText ?: "",
                onDismiss = onFinish
            )
        }
    }
}
