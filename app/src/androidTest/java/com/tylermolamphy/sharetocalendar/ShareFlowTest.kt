package com.tylermolamphy.sharetocalendar

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.tylermolamphy.sharetocalendar.data.PreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end share flow tests.
 *
 * For each example text: assert the event is NOT on the calendar,
 * share it via ACTION_SEND, tap Save, then assert it IS on the calendar.
 *
 * Must run in a separate instrumentation invocation with calendar permissions
 * (same constraints as [DefaultCalendarTest]).
 */
@RunWith(AndroidJUnit4::class)
class ShareFlowTest {

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
            put(CalendarContract.Calendars.NAME, "ShareFlow Test Calendar")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "ShareFlow Test Calendar")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF1A73E8.toInt())
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
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
                .appendQueryParameter(
                    CalendarContract.Calendars.ACCOUNT_TYPE,
                    CalendarContract.ACCOUNT_TYPE_LOCAL
                )
                .build(),
            values
        )
        testCalendarId = uri?.lastPathSegment?.toLong() ?: -1

        // Set the test calendar as the app's default so events are saved there
        val prefsRepo = PreferencesRepository(context)
        runBlocking { prefsRepo.setSelectedCalendarId(testCalendarId) }
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
                    .appendQueryParameter(
                        CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.ACCOUNT_TYPE_LOCAL
                    )
                    .build(),
                null, null
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Query whether an event with the given [description] exists in the test calendar.
     * The app stores the original shared text as DESCRIPTION (CalendarRepository line 82).
     */
    private fun queryEventExists(description: String): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} = ?",
            arrayOf(testCalendarId.toString(), description),
            null
        )
        val exists = (cursor?.count ?: 0) > 0
        cursor?.close()
        return exists
    }

    /**
     * Launch [MainActivity] with an ACTION_SEND intent carrying [text],
     * wait for the save button, click it, and wait for the activity to finish.
     */
    private fun shareTextAndSave(text: String) {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            // Wait for the save button to appear (NLP parsing + UI render)
            composeTestRule.waitUntil(15_000) {
                try {
                    composeTestRule.onNodeWithTag("saveButton").assertExists()
                    true
                } catch (_: AssertionError) {
                    false
                }
            }

            composeTestRule.onNodeWithTag("saveButton").performClick()

            // Wait for the activity to finish (save success triggers finish())
            composeTestRule.waitUntil(15_000) {
                scenario.state == Lifecycle.State.DESTROYED
            }
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun shareFlow_fullParse() {
        val text = "Team meeting tomorrow at 3pm for 1 hour in Conference Room B"
        assertFalse("Event should not exist before share", queryEventExists(text))
        shareTextAndSave(text)
        assertTrue("Event should exist after save", queryEventExists(text))
    }

    @Test
    fun shareFlow_timeOnly() {
        val text = "Dentist appointment at 2:30pm"
        assertFalse("Event should not exist before share", queryEventExists(text))
        shareTextAndSave(text)
        assertTrue("Event should exist after save", queryEventExists(text))
    }

    @Test
    fun shareFlow_specificDateKeywordTime() {
        val text = "Birthday party on January 15 at noon"
        assertFalse("Event should not exist before share", queryEventExists(text))
        shareTextAndSave(text)
        assertTrue("Event should exist after save", queryEventExists(text))
    }

    @Test
    fun shareFlow_minimalInput() {
        val text = "Gym"
        assertFalse("Event should not exist before share", queryEventExists(text))
        shareTextAndSave(text)
        assertTrue("Event should exist after save", queryEventExists(text))
    }

    @Test
    fun shareFlow_relativeDateWithTime() {
        val text = "Coffee with Sarah next Monday at 10am"
        assertFalse("Event should not exist before share", queryEventExists(text))
        shareTextAndSave(text)
        assertTrue("Event should exist after save", queryEventExists(text))
    }
}
