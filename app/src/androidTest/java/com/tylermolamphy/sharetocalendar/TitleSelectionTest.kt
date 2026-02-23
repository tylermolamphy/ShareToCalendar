package com.tylermolamphy.sharetocalendar

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests covering the three title-field UX behaviours introduced
 * alongside the "Next" button feature:
 *
 *  1. When NLP detects a title the field is pre-filled with that title.
 *  2. When NLP detects a title the entire text is selected so a single keystroke
 *     replaces it without a manual clear.
 *  3. When NLP finds no title (input is entirely date/time/location metadata)
 *     the field is blank so the user can type immediately.
 *  4. Tapping "Next" opens the date-picker dialog (equivalent to a Tab key press
 *     moving focus from the title field into the date/time editing flow).
 */
@RunWith(AndroidJUnit4::class)
class TitleSelectionTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun launchWithText(text: String): ActivityScenario<MainActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        ).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return ActivityScenario.launch(intent)
    }

    /**
     * Polls until [titleField] contains non-empty editable text, which means NLP
     * parsing has completed and a title was detected.
     */
    private fun waitForTitlePopulated() {
        composeTestRule.waitUntil(5_000) {
            try {
                composeTestRule.onNodeWithTag("titleField")
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.EditableText]
                    .text
                    .isNotEmpty()
            } catch (_: Throwable) {
                false
            }
        }
    }

    /**
     * Polls until [descriptionField] shows [expected] text.
     *
     * After [EventConfirmationViewModel.parseSharedText] completes,
     * `event.description` is set to the original shared text.  Waiting on that
     * field is a reliable "parse complete" signal even when the title is blank
     * (because a blank title doesn't produce an observable state change).
     */
    private fun waitForDescriptionPopulated(expected: String) {
        composeTestRule.waitUntil(5_000) {
            try {
                composeTestRule.onNodeWithTag("descriptionField")
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.EditableText]
                    .text == expected
            } catch (_: Throwable) {
                false
            }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * When NLP extracts a title the title field must show that text so the user
     * can review (or replace) it before saving.
     */
    @Test
    fun titleField_populatedWithParsedTitle_whenNlpDetectsTitle() {
        launchWithText("Gym session tomorrow at 3pm").use {
            waitForTitlePopulated()

            val title = composeTestRule.onNodeWithTag("titleField")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text

            assertEquals("Gym session", title)
        }
    }

    /**
     * The pre-filled title must be fully selected (TextRange 0..length) so that
     * typing a single character immediately replaces the entire suggestion.
     */
    @Test
    fun titleField_isFullySelected_whenNlpDetectsTitle() {
        launchWithText("Team standup tomorrow at 10am").use {
            waitForTitlePopulated()

            val node = composeTestRule.onNodeWithTag("titleField").fetchSemanticsNode()
            val titleText = node.config[SemanticsProperties.EditableText].text
            val selection = node.config[SemanticsProperties.TextSelectionRange]

            assertEquals(
                "Selection should start at 0 (full selection)",
                0,
                selection.start
            )
            assertEquals(
                "Selection end should equal title length (full selection)",
                titleText.length,
                selection.end
            )
        }
    }

    /**
     * When the entire input is consumed by date/time/location extraction NLP
     * returns a blank title.  The title field must be empty so the user can
     * start typing without clearing a stale placeholder first.
     */
    @Test
    fun titleField_isBlank_whenNlpFindsNoTitle() {
        val sharedText = "tomorrow at 3pm"
        launchWithText(sharedText).use {
            // The description field is always set to the raw shared text after
            // parsing — use it as a reliable "parse finished" signal when the
            // title itself would remain blank (no observable change to watch).
            waitForDescriptionPopulated(sharedText)

            val title = composeTestRule.onNodeWithTag("titleField")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text

            assertEquals("Title field should be blank when NLP finds no title", "", title)
        }
    }

    /**
     * Tapping "Next" must dismiss the keyboard and open the date-picker dialog,
     * mirroring a Tab key press that moves the user from the title field into
     * the date/time editing flow.
     */
    @Test
    fun nextButton_opensDatePickerDialog_whenClicked() {
        launchWithText("Meeting tomorrow at 2pm").use {
            composeTestRule.waitUntil(5_000) {
                try {
                    composeTestRule.onNodeWithTag("nextButton").assertExists()
                    true
                } catch (_: AssertionError) {
                    false
                }
            }

            composeTestRule.onNodeWithTag("nextButton").performClick()

            // The M3 DatePickerDialog always shows an "OK" confirm button.
            composeTestRule.waitUntil(3_000) {
                try {
                    composeTestRule.onNodeWithText("OK").assertExists()
                    true
                } catch (_: AssertionError) {
                    false
                }
            }
        }
    }
}
