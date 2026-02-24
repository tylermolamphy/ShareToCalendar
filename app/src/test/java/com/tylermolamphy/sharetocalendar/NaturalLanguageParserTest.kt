package com.tylermolamphy.sharetocalendar

import com.tylermolamphy.sharetocalendar.parser.NaturalLanguageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NaturalLanguageParserTest {

    // Fixed reference date for deterministic tests — Friday, Jan 10, 2025
    private val refDate = LocalDate.of(2025, 1, 10)

    // ─────────────────────────────────────────────────────────────────────────
    // Existing tests (must continue to pass)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse lunch with Sarah tomorrow at noon`() {
        val event = NaturalLanguageParser.parse("Lunch with Sarah tomorrow at noon", refDate)
        assertEquals("Lunch with Sarah", event.title)
        assertEquals(refDate.plusDays(1), event.startDate)
        assertEquals(LocalTime.of(12, 0), event.startTime)
        assertFalse(event.isAllDay)
    }

    @Test
    fun `parse team meeting with all fields`() {
        val event = NaturalLanguageParser.parse(
            "Team meeting next Tuesday at 3pm for 1 hour in Conference Room B",
            refDate
        )
        assertEquals("Team meeting", event.title)
        assertEquals(LocalDate.of(2025, 1, 14), event.startDate) // next Tuesday
        assertEquals(LocalTime.of(15, 0), event.startTime)
        assertEquals(LocalTime.of(16, 0), event.endTime)
        assertEquals("Conference Room B", event.location)
        assertFalse(event.isAllDay)
    }

    @Test
    fun `parse dentist appointment with month date`() {
        val event = NaturalLanguageParser.parse(
            "Dentist appointment Jan 15 at 2:30pm",
            refDate
        )
        assertEquals("Dentist appointment", event.title)
        assertEquals(LocalDate.of(2025, 1, 15), event.startDate)
        assertEquals(LocalTime.of(14, 30), event.startTime)
    }

    @Test
    fun `parse no date defaults to reference date`() {
        val event = NaturalLanguageParser.parse("Call Mom at 5pm", refDate)
        assertEquals("Call Mom", event.title)
        assertEquals(refDate, event.startDate)
        assertEquals(LocalTime.of(17, 0), event.startTime)
    }

    @Test
    fun `parse no time results in all-day event`() {
        val event = NaturalLanguageParser.parse("Team offsite tomorrow", refDate)
        assertEquals("Team offsite", event.title)
        assertEquals(refDate.plusDays(1), event.startDate)
        assertTrue(event.isAllDay)
        assertNull(event.startTime)
    }

    @Test
    fun `parse today keyword`() {
        val event = NaturalLanguageParser.parse("Standup today at 9am", refDate)
        assertEquals("Standup", event.title)
        assertEquals(refDate, event.startDate)
        assertEquals(LocalTime.of(9, 0), event.startTime)
    }

    @Test
    fun `parse numeric date format`() {
        val event = NaturalLanguageParser.parse("Birthday party 1/25/2025 at 7pm", refDate)
        assertEquals("Birthday party", event.title)
        assertEquals(LocalDate.of(2025, 1, 25), event.startDate)
        assertEquals(LocalTime.of(19, 0), event.startTime)
    }

    @Test
    fun `parse relative days`() {
        val event = NaturalLanguageParser.parse("Follow up in 3 days at 10am", refDate)
        assertEquals("Follow up", event.title)
        assertEquals(refDate.plusDays(3), event.startDate)
        assertEquals(LocalTime.of(10, 0), event.startTime)
    }

    @Test
    fun `parse relative weeks`() {
        val event = NaturalLanguageParser.parse("Review in 2 weeks at 2pm", refDate)
        assertEquals("Review", event.title)
        assertEquals(refDate.plusWeeks(2), event.startDate)
        assertEquals(LocalTime.of(14, 0), event.startTime)
    }

    @Test
    fun `parse duration in minutes`() {
        val event = NaturalLanguageParser.parse(
            "Quick sync tomorrow at 3pm for 30 minutes",
            refDate
        )
        assertEquals("Quick sync", event.title)
        assertEquals(LocalTime.of(15, 0), event.startTime)
        assertEquals(LocalTime.of(15, 30), event.endTime)
    }

    @Test
    fun `parse duration hours and minutes`() {
        val event = NaturalLanguageParser.parse(
            "Workshop tomorrow at 1pm for 2 hours and 30 minutes",
            refDate
        )
        assertEquals(LocalTime.of(13, 0), event.startTime)
        assertEquals(LocalTime.of(15, 30), event.endTime)
    }

    @Test
    fun `parse until end time`() {
        val event = NaturalLanguageParser.parse(
            "Meeting tomorrow at 2pm until 4pm",
            refDate
        )
        assertEquals(LocalTime.of(14, 0), event.startTime)
        assertEquals(LocalTime.of(16, 0), event.endTime)
    }

    @Test
    fun `parse midnight time`() {
        val event = NaturalLanguageParser.parse("New Year countdown tomorrow at midnight", refDate)
        assertEquals(LocalTime.of(0, 0), event.startTime)
    }

    @Test
    fun `parse 24-hour time format`() {
        val event = NaturalLanguageParser.parse("Dinner tomorrow at 18:30", refDate)
        assertEquals(LocalTime.of(18, 30), event.startTime)
    }

    @Test
    fun `default end time is start plus one hour`() {
        val event = NaturalLanguageParser.parse("Call tomorrow at 3pm", refDate)
        assertEquals(LocalTime.of(15, 0), event.startTime)
        assertEquals(LocalTime.of(16, 0), event.endTime)
    }

    @Test
    fun `parse next day names`() {
        // refDate is Friday Jan 10, 2025
        val event = NaturalLanguageParser.parse("Gym next Monday at 6am", refDate)
        assertEquals(LocalDate.of(2025, 1, 13), event.startDate) // next Monday
        assertEquals(LocalTime.of(6, 0), event.startTime)
    }

    @Test
    fun `parse full month name`() {
        val event = NaturalLanguageParser.parse("Conference February 20 at 9am", refDate)
        assertEquals(LocalDate.of(2025, 2, 20), event.startDate)
    }

    @Test
    fun `parse month date with year`() {
        val event = NaturalLanguageParser.parse("Wedding Jun 15, 2026", refDate)
        assertEquals("Wedding", event.title)
        assertEquals(LocalDate.of(2026, 6, 15), event.startDate)
    }

    @Test
    fun `empty input returns blank event`() {
        val event = NaturalLanguageParser.parse("", refDate)
        assertEquals("", event.title)
        assertEquals(refDate, event.startDate)
        assertTrue(event.isAllDay)
    }

    @Test
    fun `input with only date and time yields blank title`() {
        val event = NaturalLanguageParser.parse("tomorrow at 3pm", refDate)
        assertEquals("", event.title)
        assertEquals(refDate.plusDays(1), event.startDate)
        assertEquals(LocalTime.of(15, 0), event.startTime)
        assertFalse(event.isAllDay)
    }

    @Test
    fun `title only input`() {
        val event = NaturalLanguageParser.parse("Something important", refDate)
        assertEquals("Something important", event.title)
        assertEquals(refDate, event.startDate)
        assertTrue(event.isAllDay)
    }

    @Test
    fun `parse 12pm correctly`() {
        val event = NaturalLanguageParser.parse("Lunch at 12pm", refDate)
        assertEquals(LocalTime.of(12, 0), event.startTime)
    }

    @Test
    fun `parse 12am correctly`() {
        val event = NaturalLanguageParser.parse("Late night at 12am", refDate)
        assertEquals(LocalTime.of(0, 0), event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: "tonight" → today's date
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `tonight maps to today date`() {
        val event = NaturalLanguageParser.parse("Dinner tonight at 8pm", refDate)
        assertEquals("Dinner", event.title)
        assertEquals(refDate, event.startDate)
        assertEquals(LocalTime.of(20, 0), event.startTime)
        assertFalse(event.isAllDay)
    }

    @Test
    fun `tonight without explicit time is all-day on today`() {
        val event = NaturalLanguageParser.parse("Party tonight", refDate)
        assertEquals("Party", event.title)
        assertEquals(refDate, event.startDate)
        assertTrue(event.isAllDay)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: "this [day]" → upcoming occurrence (nextOrSame)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `this day keyword resolves to upcoming weekday`() {
        // refDate = Friday Jan 10; "this Thursday" → next Thu = Jan 16
        val event = NaturalLanguageParser.parse("Yoga this Thursday at 7am", refDate)
        assertEquals("Yoga", event.title)
        assertEquals(LocalDate.of(2025, 1, 16), event.startDate)
        assertEquals(LocalTime.of(7, 0), event.startTime)
    }

    @Test
    fun `this day keyword resolves to same day when today matches`() {
        // refDate = Friday Jan 10; "this Friday" → today = Jan 10
        val event = NaturalLanguageParser.parse("Standup this Friday at 9am", refDate)
        assertEquals(refDate, event.startDate)
        assertEquals(LocalTime.of(9, 0), event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: bare day name without "on"/"next"/"this"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `bare day name resolves to upcoming occurrence`() {
        // refDate = Friday Jan 10; "Monday" → nextOrSame Monday = Jan 13
        val event = NaturalLanguageParser.parse("Coffee Monday at 3pm", refDate)
        assertEquals("Coffee", event.title)
        assertEquals(LocalDate.of(2025, 1, 13), event.startDate)
        assertEquals(LocalTime.of(15, 0), event.startTime)
    }

    @Test
    fun `bare day name resolves to same day when today matches`() {
        // refDate = Friday Jan 10; "Friday" → today Jan 10
        val event = NaturalLanguageParser.parse("Lunch Friday at noon", refDate)
        assertEquals(refDate, event.startDate)
        assertEquals(LocalTime.of(12, 0), event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: ISO date YYYY-MM-DD
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse ISO date format`() {
        val event = NaturalLanguageParser.parse("Launch event 2025-03-15 at 7pm", refDate)
        assertEquals("Launch event", event.title)
        assertEquals(LocalDate.of(2025, 3, 15), event.startDate)
        assertEquals(LocalTime.of(19, 0), event.startTime)
    }

    @Test
    fun `ISO date takes priority over bare day name in same string`() {
        // "2025-01-13" (a Monday) alongside the word "Monday"
        val event = NaturalLanguageParser.parse("Meeting Monday 2025-01-13 at 10am", refDate)
        assertEquals(LocalDate.of(2025, 1, 13), event.startDate)
        assertEquals(LocalTime.of(10, 0), event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: short numeric date M/D without year
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse short numeric date without year`() {
        // refDate = Jan 10; 1/15 is after refDate → same year
        val event = NaturalLanguageParser.parse("Dentist 1/15 at 9am", refDate)
        assertEquals("Dentist", event.title)
        assertEquals(LocalDate.of(2025, 1, 15), event.startDate)
        assertEquals(LocalTime.of(9, 0), event.startTime)
    }

    @Test
    fun `short numeric date uses next year when date has already passed`() {
        // refDate = Jan 10, 2025; 1/5 (Jan 5) is BEFORE refDate → rolls to 2026
        val event = NaturalLanguageParser.parse("Reunion 1/5 at 7pm", refDate)
        assertEquals(LocalDate.of(2026, 1, 5), event.startDate)
        assertEquals(LocalTime.of(19, 0), event.startTime)
    }

    @Test
    fun `short numeric date not matched when followed by year`() {
        // Full "1/25/2025" should be parsed by NUMERIC_DATE, not SHORT_NUMERIC_DATE
        val event = NaturalLanguageParser.parse("Birthday 1/25/2025 at noon", refDate)
        assertEquals(LocalDate.of(2025, 1, 25), event.startDate)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: time ranges
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse hyphenated time range same meridiem`() {
        // "9am-10am" — both meridiem explicit
        val event = NaturalLanguageParser.parse("Standup 9am-10am tomorrow", refDate)
        assertEquals("Standup", event.title)
        assertEquals(LocalTime.of(9, 0),  event.startTime)
        assertEquals(LocalTime.of(10, 0), event.endTime)
    }

    @Test
    fun `parse time range with inherited am-pm`() {
        // "2-4pm" — start has no am/pm, inherits from end
        val event = NaturalLanguageParser.parse("Meeting 2-4pm tomorrow", refDate)
        assertEquals(LocalTime.of(14, 0), event.startTime)
        assertEquals(LocalTime.of(16, 0), event.endTime)
        assertFalse(event.isAllDay)
    }

    @Test
    fun `parse from X to Y time range`() {
        val event = NaturalLanguageParser.parse("Workshop from 9am to 12pm tomorrow", refDate)
        assertEquals("Workshop", event.title)
        assertEquals(LocalTime.of(9, 0),  event.startTime)
        assertEquals(LocalTime.of(12, 0), event.endTime)
    }

    @Test
    fun `parse time range with en dash`() {
        // Eventbrite-style: "7:30 PM – 9:30 PM"
        val event = NaturalLanguageParser.parse(
            "Tech Meetup Friday, February 14 · 7:30 PM – 9:30 PM",
            refDate
        )
        assertEquals(LocalTime.of(19, 30), event.startTime)
        assertEquals(LocalTime.of(21, 30), event.endTime)
    }

    @Test
    fun `parse time range cross meridiem`() {
        // "10am-2pm"
        val event = NaturalLanguageParser.parse("Conference 10am-2pm", refDate)
        assertEquals(LocalTime.of(10, 0), event.startTime)
        assertEquals(LocalTime.of(14, 0), event.endTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: o'clock
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse o'clock time`() {
        val event = NaturalLanguageParser.parse("Staff meeting at 9 o'clock", refDate)
        assertEquals("Staff meeting", event.title)
        assertEquals(LocalTime.of(9, 0), event.startTime)
        assertFalse(event.isAllDay)
    }

    @Test
    fun `parse oclock without apostrophe`() {
        val event = NaturalLanguageParser.parse("Call at 11 oclock tomorrow", refDate)
        assertEquals(LocalTime.of(11, 0), event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: "till" as synonym for "until"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse till as until synonym`() {
        val event = NaturalLanguageParser.parse("Meeting at 3pm till 5pm tomorrow", refDate)
        assertEquals(LocalTime.of(15, 0), event.startTime)
        assertEquals(LocalTime.of(17, 0), event.endTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: "for half an hour" duration
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse for half an hour duration`() {
        val event = NaturalLanguageParser.parse(
            "Quick sync tomorrow at 10am for half an hour",
            refDate
        )
        assertEquals("Quick sync", event.title)
        assertEquals(LocalTime.of(10, 0), event.startTime)
        assertEquals(LocalTime.of(10, 30), event.endTime)
    }

    @Test
    fun `parse for half hour without an`() {
        val event = NaturalLanguageParser.parse("Call at 2pm for half hour", refDate)
        assertEquals(LocalTime.of(14, 0), event.startTime)
        assertEquals(LocalTime.of(14, 30), event.endTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: "by X" as end-time deadline
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse by as end time when start exists`() {
        // "at 2pm" gives start, "by 4pm" gives end
        val event = NaturalLanguageParser.parse(
            "Presentation at 2pm, wrap up by 4pm tomorrow",
            refDate
        )
        assertEquals(LocalTime.of(14, 0), event.startTime)
        assertEquals(LocalTime.of(16, 0), event.endTime)
    }

    @Test
    fun `by without start time does not produce timed event`() {
        // No start time → can't infer an end-only event; remains all-day
        val event = NaturalLanguageParser.parse("Submit report by 5pm tomorrow", refDate)
        assertTrue(event.isAllDay)
        assertNull(event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: invalid date components don't crash
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `invalid numeric date is silently ignored`() {
        // Month 13 is invalid — should fall through without throwing
        val event = NaturalLanguageParser.parse("Event 13/32/2025 at 3pm", refDate)
        // Date couldn't be parsed; falls back to reference date
        assertEquals(refDate, event.startDate)
        assertEquals(LocalTime.of(15, 0), event.startTime)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New: real-world message formats
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse eventbrite style text`() {
        // "Friday, February 14 · 7:30 PM – 9:30 PM" without explicit year
        val event = NaturalLanguageParser.parse(
            "Comedy Night\nFriday, February 14 · 7:30 PM – 9:30 PM",
            refDate
        )
        assertEquals(LocalDate.of(2025, 2, 14), event.startDate)
        assertEquals(LocalTime.of(19, 30), event.startTime)
        assertEquals(LocalTime.of(21, 30), event.endTime)
    }

    @Test
    fun `parse email calendar invite style`() {
        // "When: Tuesday January 14 2025 at 3:00 PM"
        val event = NaturalLanguageParser.parse(
            "Team Sync\nWhen: Tuesday January 14, 2025 at 3:00 PM",
            refDate
        )
        assertEquals(LocalDate.of(2025, 1, 14), event.startDate)
        assertEquals(LocalTime.of(15, 0), event.startTime)
    }

    @Test
    fun `parse text message style`() {
        // No "on", no "next" — bare day name + bare time
        val event = NaturalLanguageParser.parse("Hey, can you do Thursday 2pm?", refDate)
        // refDate is Friday Jan 10; nextOrSame(Thursday) → Jan 16
        assertEquals(LocalDate.of(2025, 1, 16), event.startDate)
        assertEquals(LocalTime.of(14, 0), event.startTime)
    }
}
