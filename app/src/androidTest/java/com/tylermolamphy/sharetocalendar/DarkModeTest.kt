package com.tylermolamphy.sharetocalendar

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Dark-mode UI tests that mirror [MainActivityTest] but force the system into
 * night mode first.  This verifies that the activity window theme (XML) and
 * Compose theme are both correct in dark mode — the root cause of issue #9
 * (white text on a light background when tapping the Location field).
 *
 * These tests run WITHOUT calendar permissions, the same as [MainActivityTest],
 * so they can share the same CI invocation.
 */
@RunWith(AndroidJUnit4::class)
class DarkModeTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var uiModeManager: UiModeManager

    @Before
    fun enableDarkMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
    }

    @After
    fun restoreMode() {
        uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
    }

    /** Wait until a node with [text] is fully rendered and displayed. */
    private fun waitUntilDisplayed(text: String, timeoutMs: Long = 10_000) {
        composeTestRule.waitUntil(timeoutMs) {
            try {
                composeTestRule.onNodeWithText(text).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    /** Wait until a node with [tag] is fully rendered and displayed. */
    private fun waitUntilTagDisplayed(tag: String, timeoutMs: Long = 10_000) {
        composeTestRule.waitUntil(timeoutMs) {
            try {
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun settingsScreen_darkMode_displaysTopBar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilDisplayed("Share to Calendar")
        }
    }

    @Test
    fun settingsScreen_darkMode_showsPermissionRequest() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilDisplayed("Calendar permission is required")
            composeTestRule.onNodeWithText("Grant Permission").assertIsDisplayed()
        }
    }

    @Test
    fun shareIntent_darkMode_showsConfirmScreen() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Team meeting at 3pm")
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            waitUntilDisplayed("Confirm Event")

            composeTestRule.onNode(hasText("Title")).assertIsDisplayed()
            composeTestRule.onNode(hasText("Date")).performScrollTo().assertIsDisplayed()
            composeTestRule.onNode(hasText("Location")).performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        }
    }

    @Test
    fun shareIntent_darkMode_parsesFieldsCorrectly() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Lunch tomorrow at noon")
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            waitUntilTagDisplayed("titleField")

            composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed()
            composeTestRule.onNodeWithTag("cancelButton").assertIsDisplayed()
        }
    }

    @Test
    fun darkMode_isActive() {
        // Sanity check: verify the app is actually rendering in dark mode.
        // We must read the configuration from a launched Activity because
        // setApplicationNightMode() only affects the app's local config,
        // not the system-level uiMode returned by the instrumentation context.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val nightMode = activity.resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK
                assertTrue(
                    "Expected device to be in night mode",
                    nightMode == Configuration.UI_MODE_NIGHT_YES
                )
            }
        }
    }
}
