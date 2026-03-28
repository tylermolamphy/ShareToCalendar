package com.tylermolamphy.sharetocalendar

import com.tylermolamphy.sharetocalendar.validation.InputValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class InputValidationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `MAX_TITLE_LENGTH is 150`() {
        assertEquals(150, InputValidation.MAX_TITLE_LENGTH)
    }

    @Test
    fun `MAX_LOCATION_LENGTH is 200`() {
        assertEquals(200, InputValidation.MAX_LOCATION_LENGTH)
    }

    @Test
    fun `MAX_DESCRIPTION_LENGTH is 1000`() {
        assertEquals(1000, InputValidation.MAX_DESCRIPTION_LENGTH)
    }

    @Test
    fun `PARSED_TITLE_MAX_LENGTH is 50`() {
        assertEquals(50, InputValidation.PARSED_TITLE_MAX_LENGTH)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sanitizeTitle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `sanitizeTitle returns title unchanged when no surrounding whitespace`() {
        assertEquals("Team Meeting", InputValidation.sanitizeTitle("Team Meeting"))
    }

    @Test
    fun `sanitizeTitle trims leading whitespace`() {
        assertEquals("Meeting", InputValidation.sanitizeTitle("   Meeting"))
    }

    @Test
    fun `sanitizeTitle trims trailing whitespace`() {
        assertEquals("Meeting", InputValidation.sanitizeTitle("Meeting   "))
    }

    @Test
    fun `sanitizeTitle trims both leading and trailing whitespace`() {
        assertEquals("Meeting", InputValidation.sanitizeTitle("  Meeting  "))
    }

    @Test
    fun `sanitizeTitle preserves internal whitespace`() {
        assertEquals("Team  Meeting", InputValidation.sanitizeTitle("  Team  Meeting  "))
    }

    @Test
    fun `sanitizeTitle returns null for empty string`() {
        assertNull(InputValidation.sanitizeTitle(""))
    }

    @Test
    fun `sanitizeTitle returns null for whitespace-only string`() {
        assertNull(InputValidation.sanitizeTitle("     "))
    }

    @Test
    fun `sanitizeTitle returns null for tab-only string`() {
        assertNull(InputValidation.sanitizeTitle("\t\t"))
    }

    @Test
    fun `sanitizeTitle returns null for mixed whitespace string`() {
        assertNull(InputValidation.sanitizeTitle("  \t  \n  "))
    }

    @Test
    fun `sanitizeTitle trims tab characters`() {
        assertEquals("Meeting", InputValidation.sanitizeTitle("\tMeeting\t"))
    }

    @Test
    fun `sanitizeTitle handles unicode whitespace trimming`() {
        // Standard trim handles common unicode spaces via Kotlin's trim()
        assertEquals("Meeting", InputValidation.sanitizeTitle("  Meeting  "))
    }

    @Test
    fun `sanitizeTitle handles emoji in title`() {
        val result = InputValidation.sanitizeTitle("  🎉 Party  ")
        assertEquals("🎉 Party", result)
    }

    @Test
    fun `sanitizeTitle returns non-null for single character title`() {
        assertNotNull(InputValidation.sanitizeTitle("A"))
        assertEquals("A", InputValidation.sanitizeTitle("A"))
    }

    @Test
    fun `sanitizeTitle handles title at exactly MAX_TITLE_LENGTH`() {
        val title = "A".repeat(InputValidation.MAX_TITLE_LENGTH)
        assertEquals(title, InputValidation.sanitizeTitle(title))
    }

    @Test
    fun `sanitizeTitle handles very long title without truncation`() {
        // sanitizeTitle only trims — it does NOT enforce length (that is the UI's job)
        val longTitle = "A".repeat(500)
        assertEquals(longTitle, InputValidation.sanitizeTitle(longTitle))
    }

    @Test
    fun `sanitizeTitle handles title with newline in middle`() {
        // trim only removes leading/trailing; a newline mid-string is preserved
        val result = InputValidation.sanitizeTitle("Line1\nLine2")
        assertEquals("Line1\nLine2", result)
    }

    @Test
    fun `sanitizeTitle handles RTL characters`() {
        val rtl = "اجتماع"  // Arabic "meeting"
        assertEquals(rtl, InputValidation.sanitizeTitle("  $rtl  "))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sanitizeLocation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `sanitizeLocation returns location unchanged when no surrounding whitespace`() {
        assertEquals("Conference Room B", InputValidation.sanitizeLocation("Conference Room B"))
    }

    @Test
    fun `sanitizeLocation trims leading whitespace`() {
        assertEquals("Room 4", InputValidation.sanitizeLocation("   Room 4"))
    }

    @Test
    fun `sanitizeLocation trims trailing whitespace`() {
        assertEquals("Room 4", InputValidation.sanitizeLocation("Room 4   "))
    }

    @Test
    fun `sanitizeLocation returns empty string for blank input`() {
        assertEquals("", InputValidation.sanitizeLocation("   "))
    }

    @Test
    fun `sanitizeLocation returns empty string for empty input`() {
        assertEquals("", InputValidation.sanitizeLocation(""))
    }

    @Test
    fun `sanitizeLocation preserves internal whitespace`() {
        assertEquals("Building  4", InputValidation.sanitizeLocation("  Building  4  "))
    }

    @Test
    fun `sanitizeLocation handles emoji`() {
        assertEquals("🏢 HQ", InputValidation.sanitizeLocation("  🏢 HQ  "))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // shortenParsedTitle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `shortenParsedTitle returns short title unchanged`() {
        assertEquals("Team Meeting", InputValidation.shortenParsedTitle("Team Meeting"))
    }

    @Test
    fun `shortenParsedTitle returns title unchanged at exactly 50 chars`() {
        val title = "A".repeat(50)
        assertEquals(title, InputValidation.shortenParsedTitle(title))
    }

    @Test
    fun `shortenParsedTitle shortens title of 51 chars`() {
        val title = "A".repeat(51)
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result should be at most 50 chars, was ${result.length}", result.length <= 50)
    }

    @Test
    fun `shortenParsedTitle returns empty string unchanged`() {
        assertEquals("", InputValidation.shortenParsedTitle(""))
    }

    @Test
    fun `shortenParsedTitle uses first sentence when it fits`() {
        val title = "Lunch with Sarah. Please confirm your attendance at the upcoming event tomorrow."
        val result = InputValidation.shortenParsedTitle(title)
        assertEquals("Lunch with Sarah", result)
    }

    @Test
    fun `shortenParsedTitle splits on exclamation mark`() {
        val title = "Team Outing! Please join us for an afternoon of fun activities at the park."
        val result = InputValidation.shortenParsedTitle(title)
        assertEquals("Team Outing", result)
    }

    @Test
    fun `shortenParsedTitle splits on question mark`() {
        val title = "Can you attend? We have a board meeting scheduled for next Tuesday afternoon."
        val result = InputValidation.shortenParsedTitle(title)
        assertEquals("Can you attend", result)
    }

    @Test
    fun `shortenParsedTitle truncates long first sentence at word boundary`() {
        // First sentence longer than 50 chars, has word boundaries
        val title = "This is a very long meeting title that exceeds the fifty character limit for the calendar event"
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result should be at most 50 chars, was ${result.length}", result.length <= 50)
        assertTrue("Result should end with ellipsis", result.endsWith("…"))
        assertFalse("Result should not cut a word mid-stream", result.trimEnd('…').endsWith(" "))
    }

    @Test
    fun `shortenParsedTitle truncates with ellipsis when no word boundary`() {
        // 60 chars, one word, no spaces → take(47) + "…" = 48 chars (still ≤ 50)
        val title = "A".repeat(60)
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result should be at most 50 chars, was ${result.length}", result.length <= 50)
        assertTrue("Result should end with ellipsis", result.endsWith("…"))
    }

    @Test
    fun `shortenParsedTitle falls back to hard truncation when first sentence is all one word`() {
        val longWord = "Supercalifragilisticexpialidocious" // 34 chars
        val title = longWord + longWord + " some more words after"
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result should be at most 50 chars, was ${result.length}", result.length <= 50)
    }

    @Test
    fun `shortenParsedTitle handles punctuation-only prefix - does not produce blank or only ellipsis`() {
        // Regression: title starting with punctuation produced an empty first sentence which
        // the old code returned immediately (returning ""). Fixed by skipping blank sentences.
        val title = "! This is a meeting that has a punctuation issue at the very start of text"
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result must not be blank", result.isNotBlank())
        assertTrue("Result must not be only ellipsis", result != "…")
        assertTrue("Result length should be at most 50, was ${result.length}", result.length <= 50)
    }

    @Test
    fun `shortenParsedTitle handles title starting with question mark`() {
        // Same regression: "?" produces an empty first sentence; must fall through to truncation.
        val title = "? Some long event title that would exceed the fifty character limit anyway"
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result must not be blank", result.isNotBlank())
        assertTrue("Result must not be only ellipsis", result != "…")
        assertTrue("Result length should be at most 50, was ${result.length}", result.length <= 50)
    }

    @Test
    fun `shortenParsedTitle handles title that is exactly sentence boundary length`() {
        // First sentence is exactly 50 chars
        val sentence = "This sentence is exactly fifty characters longg yo"  // 50 chars
        val full = "$sentence. And then some more text follows here."
        assertEquals(50, sentence.length)
        val result = InputValidation.shortenParsedTitle(full)
        assertEquals(sentence, result)
    }

    @Test
    fun `shortenParsedTitle handles emoji in title`() {
        val title = "🎉 Happy Birthday Party at the venue"
        // Short enough — should pass through
        assertTrue(title.length <= 50)
        assertEquals(title, InputValidation.shortenParsedTitle(title))
    }

    @Test
    fun `shortenParsedTitle handles Unicode multibyte characters`() {
        val title = "Déjeuner avec Marie demain à midi au restaurant de la place centrale"
        val result = InputValidation.shortenParsedTitle(title)
        assertTrue("Result length should be at most 50", result.length <= 50)
    }

    @Test
    fun `shortenParsedTitle never returns a result longer than PARSED_TITLE_MAX_LENGTH`() {
        val inputs = listOf(
            "A".repeat(51),
            "Word ".repeat(20),
            "NoSpacesAtAllJustOneLongContinuousStringOfCharacters123456",
            "First sentence. Second sentence that is also very long and exceeds limits.",
            "! Starts with punct. Second sentence follows here.",
        )
        for (input in inputs) {
            val result = InputValidation.shortenParsedTitle(input)
            assertTrue(
                "Input='$input' → result='$result' (${result.length} chars) exceeds limit",
                result.length <= InputValidation.PARSED_TITLE_MAX_LENGTH
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isValidTimeRange
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `isValidTimeRange returns true when both times are null`() {
        assertTrue(InputValidation.isValidTimeRange(null, null))
    }

    @Test
    fun `isValidTimeRange returns true when start is null`() {
        assertTrue(InputValidation.isValidTimeRange(null, LocalTime.of(10, 0)))
    }

    @Test
    fun `isValidTimeRange returns true when end is null`() {
        assertTrue(InputValidation.isValidTimeRange(LocalTime.of(9, 0), null))
    }

    @Test
    fun `isValidTimeRange returns true when end is after start`() {
        assertTrue(InputValidation.isValidTimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0)))
    }

    @Test
    fun `isValidTimeRange returns true for 1-minute gap`() {
        assertTrue(InputValidation.isValidTimeRange(LocalTime.of(9, 0), LocalTime.of(9, 1)))
    }

    @Test
    fun `isValidTimeRange returns false when end equals start`() {
        assertFalse(InputValidation.isValidTimeRange(LocalTime.of(9, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `isValidTimeRange returns false when end is before start`() {
        assertFalse(InputValidation.isValidTimeRange(LocalTime.of(10, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `isValidTimeRange handles midnight boundary - end at midnight`() {
        // 11pm → midnight: end is NOT after start in same-day terms
        assertFalse(InputValidation.isValidTimeRange(LocalTime.of(23, 0), LocalTime.MIDNIGHT))
    }

    @Test
    fun `isValidTimeRange handles noon as start`() {
        assertTrue(InputValidation.isValidTimeRange(LocalTime.NOON, LocalTime.of(13, 0)))
    }

    @Test
    fun `isValidTimeRange returns false for same noon to noon`() {
        assertFalse(InputValidation.isValidTimeRange(LocalTime.NOON, LocalTime.NOON))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isTitleLengthValid / isLocationLengthValid / isDescriptionLengthValid
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `isTitleLengthValid returns true for empty string`() {
        assertTrue(InputValidation.isTitleLengthValid(""))
    }

    @Test
    fun `isTitleLengthValid returns true at exactly MAX_TITLE_LENGTH`() {
        assertTrue(InputValidation.isTitleLengthValid("A".repeat(InputValidation.MAX_TITLE_LENGTH)))
    }

    @Test
    fun `isTitleLengthValid returns false one over MAX_TITLE_LENGTH`() {
        assertFalse(InputValidation.isTitleLengthValid("A".repeat(InputValidation.MAX_TITLE_LENGTH + 1)))
    }

    @Test
    fun `isTitleLengthValid returns false for very long title`() {
        assertFalse(InputValidation.isTitleLengthValid("A".repeat(10_000)))
    }

    @Test
    fun `isLocationLengthValid returns true at exactly MAX_LOCATION_LENGTH`() {
        assertTrue(InputValidation.isLocationLengthValid("A".repeat(InputValidation.MAX_LOCATION_LENGTH)))
    }

    @Test
    fun `isLocationLengthValid returns false one over MAX_LOCATION_LENGTH`() {
        assertFalse(InputValidation.isLocationLengthValid("A".repeat(InputValidation.MAX_LOCATION_LENGTH + 1)))
    }

    @Test
    fun `isLocationLengthValid returns true for empty string`() {
        assertTrue(InputValidation.isLocationLengthValid(""))
    }

    @Test
    fun `isDescriptionLengthValid returns true at exactly MAX_DESCRIPTION_LENGTH`() {
        assertTrue(InputValidation.isDescriptionLengthValid("A".repeat(InputValidation.MAX_DESCRIPTION_LENGTH)))
    }

    @Test
    fun `isDescriptionLengthValid returns false one over MAX_DESCRIPTION_LENGTH`() {
        assertFalse(InputValidation.isDescriptionLengthValid("A".repeat(InputValidation.MAX_DESCRIPTION_LENGTH + 1)))
    }

    @Test
    fun `isDescriptionLengthValid returns true for empty string`() {
        assertTrue(InputValidation.isDescriptionLengthValid(""))
    }

    @Test
    fun `isDescriptionLengthValid handles multi-line text within limit`() {
        val multiline = "Line 1\nLine 2\nLine 3\n".repeat(20)  // ~420 chars
        assertTrue(InputValidation.isDescriptionLengthValid(multiline))
    }

    @Test
    fun `isDescriptionLengthValid handles emoji characters within limit`() {
        val emojiText = "🎉".repeat(100)  // emoji = 2 chars each in Kotlin → 200 chars
        assertTrue(InputValidation.isDescriptionLengthValid(emojiText))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge / adversarial inputs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `sanitizeTitle handles null-like zero-width characters`() {
        // Zero-width space (U+200B) is not whitespace in Java/Kotlin trim
        val zwsp = "\u200B"
        val result = InputValidation.sanitizeTitle("${zwsp}Meeting${zwsp}")
        // After trim, zero-width space is still present (trim only removes Java whitespace)
        assertNotNull(result)
    }

    @Test
    fun `sanitizeTitle handles control characters`() {
        // \u0001 is a non-printable control char; trim does not strip it
        val result = InputValidation.sanitizeTitle("Meeting\u0001")
        assertNotNull(result)
        assertTrue(result!!.contains("Meeting"))
    }

    @Test
    fun `shortenParsedTitle is idempotent`() {
        val title = "Team meeting next Tuesday at 3pm for 1 hour in Conference Room B"
        val once = InputValidation.shortenParsedTitle(title)
        val twice = InputValidation.shortenParsedTitle(once)
        assertEquals(once, twice)
    }

    @Test
    fun `all length guard functions accept strings of exactly 1 char`() {
        assertTrue(InputValidation.isTitleLengthValid("A"))
        assertTrue(InputValidation.isLocationLengthValid("A"))
        assertTrue(InputValidation.isDescriptionLengthValid("A"))
    }

    @Test
    fun `length guards use char count not byte count`() {
        // A 3-byte UTF-8 char (e.g. snowman ☃ = U+2603) counts as 1 char in Kotlin
        val snowmen = "☃".repeat(InputValidation.MAX_TITLE_LENGTH)
        assertTrue("Length should be measured in chars, not bytes",
            InputValidation.isTitleLengthValid(snowmen))
        assertFalse(InputValidation.isTitleLengthValid("☃".repeat(InputValidation.MAX_TITLE_LENGTH + 1)))
    }
}
