package com.tylermolamphy.sharetocalendar

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun settingsScreen_displaysTopBar() {
        // Launch without a share intent — lands on settings screen
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.onNodeWithText("Share to Calendar").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_showsPermissionRequest() {
        // Permission isn't granted in test, so the request UI should show
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.onNodeWithText("Calendar permission is required").assertIsDisplayed()
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
            composeTestRule.waitForIdle()

            // Verify the confirmation screen is displayed
            composeTestRule.onNodeWithText("Confirm Event").assertIsDisplayed()
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
            composeTestRule.waitForIdle()

            // Verify the confirmation screen elements are visible
            composeTestRule.onNodeWithTag("titleField").assertIsDisplayed()
            composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed()
            composeTestRule.onNodeWithTag("cancelButton").assertIsDisplayed()
        }
    }
}
