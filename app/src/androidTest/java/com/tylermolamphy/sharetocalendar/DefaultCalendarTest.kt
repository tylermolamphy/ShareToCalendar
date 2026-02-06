package com.tylermolamphy.sharetocalendar

import android.Manifest
import android.content.ContentValues
import android.provider.CalendarContract
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultCalendarTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    private var testCalendarId: Long = -1

    @Before
    fun seedCalendar() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.NAME, "CI Test Calendar")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "CI Test Calendar")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF1A73E8.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "test@local")
            put(CalendarContract.Calendars.ACCOUNT_NAME, "test@local")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }
        val uri = context.contentResolver.insert(
            CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "test@local")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build(),
            values
        )
        testCalendarId = uri?.lastPathSegment?.toLong() ?: -1
    }

    @After
    fun removeCalendar() {
        if (testCalendarId > 0) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.contentResolver.delete(
                CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendPath(testCalendarId.toString())
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "test@local")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    .build(),
                null, null
            )
        }
    }

    @Test
    fun settingsScreen_showsCalendarList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // Top bar should say "Share to Calendar"
            composeTestRule.onNodeWithText("Share to Calendar").assertIsDisplayed()

            // The "Select default calendar" heading should appear
            composeTestRule.onNodeWithText("Select default calendar").assertIsDisplayed()

            // Our seeded calendar should be visible
            composeTestRule.onNodeWithText("CI Test Calendar").assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_selectDefaultCalendar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // Tap on the test calendar to select it
            composeTestRule.onNodeWithText("CI Test Calendar").performClick()
            composeTestRule.waitForIdle()

            // The confirmation card should now show the selected calendar
            composeTestRule.onNodeWithText(
                "Events will be saved to \"CI Test Calendar\"",
                substring = false
            ).assertIsDisplayed()
        }
    }
}
