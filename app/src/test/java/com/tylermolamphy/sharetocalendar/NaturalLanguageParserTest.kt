package com.tylermolamphy.sharetocalendar

import com.tylermolamphy.sharetocalendar.parser.NaturalLanguageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NaturalLanguageParserTest {

    // Fixed reference date for deterministic tests
    private val refDate = LocalDate.of(2025, 1, 10) // Friday, Jan 10, 2025

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
}
