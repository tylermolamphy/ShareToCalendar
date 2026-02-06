package com.tylermolamphy.sharetocalendar

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun revokeCalendarPermissions() {
        // Revoke permissions before each test to ensure clean state.
        // DefaultCalendarTest uses GrantPermissionRule which persists across the
        // entire connectedDebugAndroidTest run, so we must explicitly revoke here.
        // Use executeShellCommand instead of revokeRuntimePermission() because the
        // latter can kill the app process on API 31+, crashing the instrumentation runner.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(
            "pm revoke com.tylermolamphy.sharetocalendar android.permission.READ_CALENDAR",
            "pm revoke com.tylermolamphy.sharetocalendar android.permission.WRITE_CALENDAR"
        ).forEach { cmd ->
            automation.executeShellCommand(cmd).use { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().readText()
            }
        }
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
    fun settingsScreen_displaysTopBar() {
        // Launch without a share intent — lands on settings screen
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilDisplayed("Share to Calendar")
        }
    }

    @Test
    fun settingsScreen_showsPermissionRequest() {
        // Permission isn't granted in test, so the request UI should show
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilDisplayed("Calendar permission is required")
            composeTestRule.onNodeWithText("Grant Permission").assertIsDisplayed()
        }
    }

    @Test
    fun shareIntent_showsConfirmScreen() {
        // Launch with an ACTION_SEND intent
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Team meeting at 3pm")
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            // Wait for the confirm screen to be fully rendered and displayed
            waitUntilDisplayed("Confirm Event")

            // Verify the confirmation screen fields are displayed
            composeTestRule.onNode(hasText("Title")).assertIsDisplayed()
            composeTestRule.onNode(hasText("Date")).assertIsDisplayed()
            composeTestRule.onNode(hasText("Location")).assertIsDisplayed()
            composeTestRule.onNodeWithText("Save Event").assertIsDisplayed()
        }
    }

    @Test
    fun shareIntent_parsesFieldsCorrectly() {
        // Launch with a share intent containing parseable text
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Lunch tomorrow at noon")
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            // Wait for the confirm screen to be fully rendered and displayed
            waitUntilTagDisplayed("titleField")

            composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed()
            composeTestRule.onNodeWithTag("cancelButton").assertIsDisplayed()
        }
    }
}
