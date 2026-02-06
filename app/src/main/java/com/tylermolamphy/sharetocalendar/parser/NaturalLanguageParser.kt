package com.tylermolamphy.sharetocalendar.parser

import com.tylermolamphy.sharetocalendar.model.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Stateless regex-based parser that converts natural language text into a [CalendarEvent].
 *
 * Parsing order: dates -> times -> duration/end time -> location -> title (remainder).
 */
object NaturalLanguageParser {

    fun parse(input: String, referenceDate: LocalDate = LocalDate.now()): CalendarEvent {
        var text = input.trim()
        var date: LocalDate? = null
        var startTime: LocalTime? = null
        var endTime: LocalTime? = null
        var location = ""

        // --- 1. Parse dates ---
        val dateResult = extractDate(text, referenceDate)
        if (dateResult != null) {
            date = dateResult.first
            text = dateResult.second
        }

        // --- 2. Parse times ---
        val timeResult = extractStartTime(text)
        if (timeResult != null) {
            startTime = timeResult.first
            text = timeResult.second
        }

        // --- 3. Parse duration / end time ---
        val endTimeResult = extractEndTime(text, startTime)
        if (endTimeResult != null) {
            endTime = endTimeResult.first
            text = endTimeResult.second
        }

        // --- 4. Parse location ---
        val locationResult = extractLocation(text)
        if (locationResult != null) {
            location = locationResult.first
            text = locationResult.second
        }

        // --- 5. Title = everything remaining ---
        val title = cleanTitle(text)

        val isAllDay = startTime == null
        val finalDate = date ?: referenceDate

        // Default 1-hour duration if start time present but no end time
        val finalEndTime = when {
            startTime != null && endTime == null -> startTime.plusHours(1)
            else -> endTime
        }

        return CalendarEvent(
            title = title,
            startDate = finalDate,
            startTime = startTime,
            endTime = finalEndTime,
            location = location,
            isAllDay = isAllDay
        )
    }

    // ---- Date extraction ----

    private val MONTH_NAMES = mapOf(
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sep" to 9, "sept" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12
    )

    private val DAY_NAMES = mapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

    private fun extractDate(text: String, ref: LocalDate): Pair<LocalDate, String>? {
        val lower = text.lowercase()

        // "today"
        val todayMatch = Regex("\\btoday\\b", RegexOption.IGNORE_CASE).find(text)
        if (todayMatch != null) {
            return ref to text.removeRange(todayMatch.range).trim()
        }

        // "tomorrow"
        val tomorrowMatch = Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE).find(text)
        if (tomorrowMatch != null) {
            return ref.plusDays(1) to text.removeRange(tomorrowMatch.range).trim()
        }

        // "in N days/weeks"
        val relativePattern = Regex("\\bin\\s+(\\d+)\\s+(day|days|week|weeks)\\b", RegexOption.IGNORE_CASE)
        val relativeMatch = relativePattern.find(text)
        if (relativeMatch != null) {
            val n = relativeMatch.groupValues[1].toLong()
            val unit = relativeMatch.groupValues[2].lowercase()
            val date = if (unit.startsWith("week")) ref.plusWeeks(n) else ref.plusDays(n)
            return date to text.removeRange(relativeMatch.range).trim()
        }

        // "next Monday", "next Tuesday", etc.
        val nextDayPattern = Regex("\\bnext\\s+(${DAY_NAMES.keys.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
        val nextDayMatch = nextDayPattern.find(text)
        if (nextDayMatch != null) {
            val dayOfWeek = DAY_NAMES[nextDayMatch.groupValues[1].lowercase()]!!
            val date = ref.with(TemporalAdjusters.next(dayOfWeek))
            return date to text.removeRange(nextDayMatch.range).trim()
        }

        // Day name without "next" — means the upcoming occurrence
        val dayPattern = Regex("\\bon\\s+(${DAY_NAMES.keys.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
        val dayMatch = dayPattern.find(text)
        if (dayMatch != null) {
            val dayOfWeek = DAY_NAMES[dayMatch.groupValues[1].lowercase()]!!
            val date = ref.with(TemporalAdjusters.nextOrSame(dayOfWeek))
            return date to text.removeRange(dayMatch.range).trim()
        }

        // "Month Day" or "Month Day, Year" — e.g. "Jan 15", "January 15, 2025"
        val monthNames = MONTH_NAMES.keys.joinToString("|")
        val monthDayPattern = Regex(
            "\\b($monthNames)\\.?\\s+(\\d{1,2})(?:(?:st|nd|rd|th))?(?:[,\\s]+(\\d{4}))?\\b",
            RegexOption.IGNORE_CASE
        )
        val monthDayMatch = monthDayPattern.find(text)
        if (monthDayMatch != null) {
            val month = MONTH_NAMES[monthDayMatch.groupValues[1].lowercase()]!!
            val day = monthDayMatch.groupValues[2].toInt()
            val year = if (monthDayMatch.groupValues[3].isNotEmpty()) {
                monthDayMatch.groupValues[3].toInt()
            } else {
                // Use current year, but if the date already passed, use next year
                val candidate = LocalDate.of(ref.year, month, day)
                if (candidate.isBefore(ref)) ref.year + 1 else ref.year
            }
            return LocalDate.of(year, month, day) to text.removeRange(monthDayMatch.range).trim()
        }

        // "M/D/YYYY" or "M-D-YYYY"
        val numericDatePattern = Regex("\\b(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})\\b")
        val numericDateMatch = numericDatePattern.find(text)
        if (numericDateMatch != null) {
            val month = numericDateMatch.groupValues[1].toInt()
            val day = numericDateMatch.groupValues[2].toInt()
            val year = numericDateMatch.groupValues[3].toInt()
            return LocalDate.of(year, month, day) to text.removeRange(numericDateMatch.range).trim()
        }

        return null
    }

    // ---- Time extraction ----

    private fun extractStartTime(text: String): Pair<LocalTime, String>? {
        // "noon"
        val noonMatch = Regex("\\b(?:at\\s+)?noon\\b", RegexOption.IGNORE_CASE).find(text)
        if (noonMatch != null) {
            return LocalTime.of(12, 0) to text.removeRange(noonMatch.range).trim()
        }

        // "midnight"
        val midnightMatch = Regex("\\b(?:at\\s+)?midnight\\b", RegexOption.IGNORE_CASE).find(text)
        if (midnightMatch != null) {
            return LocalTime.of(0, 0) to text.removeRange(midnightMatch.range).trim()
        }

        // "at 2pm", "at 2:30pm", "at 14:00", "at 2:30 PM"
        val timePattern = Regex(
            "\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])?\\b"
        )
        val timeMatch = timePattern.find(text)
        if (timeMatch != null) {
            val time = parseTimeComponents(
                timeMatch.groupValues[1],
                timeMatch.groupValues[2],
                timeMatch.groupValues[3]
            )
            if (time != null) {
                return time to text.removeRange(timeMatch.range).trim()
            }
        }

        // Standalone time without "at": "2pm", "3:30PM" — but only if at a word boundary
        val standaloneTimePattern = Regex(
            "\\b(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])\\b"
        )
        val standaloneMatch = standaloneTimePattern.find(text)
        if (standaloneMatch != null) {
            val time = parseTimeComponents(
                standaloneMatch.groupValues[1],
                standaloneMatch.groupValues[2],
                standaloneMatch.groupValues[3]
            )
            if (time != null) {
                return time to text.removeRange(standaloneMatch.range).trim()
            }
        }

        return null
    }

    private fun parseTimeComponents(hourStr: String, minuteStr: String, ampmStr: String): LocalTime? {
        var hour = hourStr.toIntOrNull() ?: return null
        val minute = if (minuteStr.isNotEmpty()) minuteStr.toIntOrNull() ?: 0 else 0

        if (ampmStr.isNotEmpty()) {
            val isPm = ampmStr.lowercase() == "pm"
            if (isPm && hour != 12) hour += 12
            if (!isPm && hour == 12) hour = 0
        }

        if (hour > 23 || minute > 59) return null
        return LocalTime.of(hour, minute)
    }

    // ---- End time / Duration ----

    private fun extractEndTime(text: String, startTime: LocalTime?): Pair<LocalTime, String>? {
        // "for N hour(s)" / "for N minute(s)" / "for N hour(s) and N minute(s)"
        val durationPattern = Regex(
            "\\bfor\\s+(\\d+)\\s*(?:hour|hours|hr|hrs)(?:\\s*(?:and\\s*)?(\\d+)\\s*(?:minute|minutes|min|mins))?\\b",
            RegexOption.IGNORE_CASE
        )
        val durationMatch = durationPattern.find(text)
        if (durationMatch != null && startTime != null) {
            val hours = durationMatch.groupValues[1].toLong()
            val minutes = if (durationMatch.groupValues[2].isNotEmpty()) {
                durationMatch.groupValues[2].toLong()
            } else 0L
            val end = startTime.plusHours(hours).plusMinutes(minutes)
            return end to text.removeRange(durationMatch.range).trim()
        }

        // "for N minutes" (minutes only)
        val minutesDurationPattern = Regex(
            "\\bfor\\s+(\\d+)\\s*(?:minute|minutes|min|mins)\\b",
            RegexOption.IGNORE_CASE
        )
        val minutesDurationMatch = minutesDurationPattern.find(text)
        if (minutesDurationMatch != null && startTime != null) {
            val minutes = minutesDurationMatch.groupValues[1].toLong()
            val end = startTime.plusMinutes(minutes)
            return end to text.removeRange(minutesDurationMatch.range).trim()
        }

        // "until 4pm" / "until 16:00"
        val untilPattern = Regex(
            "\\buntil\\s+(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])?\\b",
            RegexOption.IGNORE_CASE
        )
        val untilMatch = untilPattern.find(text)
        if (untilMatch != null) {
            val time = parseTimeComponents(
                untilMatch.groupValues[1],
                untilMatch.groupValues[2],
                untilMatch.groupValues[3]
            )
            if (time != null) {
                return time to text.removeRange(untilMatch.range).trim()
            }
        }

        return null
    }

    // ---- Location extraction ----

    /**
     * Extracts location from "at <Place>" or "in <Place>" patterns.
     * Distinguishes from time "at" by checking if followed by a digit (time token).
     */
    private fun extractLocation(text: String): Pair<String, String>? {
        // "at <Place>" — only if NOT followed by a digit (which would be a time)
        // Location is assumed to be the rest of the phrase until end-of-string or a known delimiter
        val atLocationPattern = Regex(
            "\\b(?:at|in)\\s+([A-Z][A-Za-z0-9' ]+?)(?:\\s*$)",
            RegexOption.MULTILINE
        )
        val atMatch = atLocationPattern.find(text)
        if (atMatch != null) {
            val loc = atMatch.groupValues[1].trim()
            // Don't treat very short matches or time-like strings as locations
            if (loc.length >= 2 && !loc.matches(Regex("\\d+.*"))) {
                return loc to text.removeRange(atMatch.range).trim()
            }
        }

        return null
    }

    // ---- Title cleanup ----

    private fun cleanTitle(text: String): String {
        return text
            .replace(Regex("\\s{2,}"), " ")  // collapse whitespace
            .replace(Regex("^[\\s,;\\-]+"), "")  // trim leading punctuation
            .replace(Regex("[\\s,;\\-]+$"), "")  // trim trailing punctuation
            .trim()
    }
}
