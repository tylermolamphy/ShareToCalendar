package com.tylermolamphy.sharetocalendar

import android.content.Intent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that run WITHOUT calendar permissions.
 * Must be run in a separate instrumentation invocation from [DefaultCalendarTest]
 * because revoking dangerous permissions on API 31+ kills the app process.
 * The CI workflow runs this class first, before any permissions are granted.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

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

            // Verify the confirmation screen fields exist and can be scrolled to.
            // On small emulator screens, fields below the fold aren't "displayed"
            // until scrolled into view.
            composeTestRule.onNode(hasText("Title")).assertIsDisplayed()
            composeTestRule.onNode(hasText("Date")).performScrollTo().assertIsDisplayed()
            composeTestRule.onNode(hasText("Location")).performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        }
    }

    @Test
    fun shareIntent_titleIsFocusedOnOpen() {
        // When a share intent launches the confirm screen, the title field should be
        // automatically focused and the soft keyboard should be raised — verified by
        // asserting focus state on the title node after the screen renders.
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

            // Poll until the title field reports focused (the LaunchedEffect has a 100 ms delay)
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                try {
                    composeTestRule.onNodeWithTag("titleField").assertIsFocused()
                    true
                } catch (_: AssertionError) {
                    false
                }
            }
            composeTestRule.onNodeWithTag("titleField").assertIsFocused()
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
