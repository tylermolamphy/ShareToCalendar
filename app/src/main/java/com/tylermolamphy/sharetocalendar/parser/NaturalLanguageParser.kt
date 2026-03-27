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
 * Parsing order:
 *   1. Dates  – explicit (month/day, ISO, M/D/YYYY) then relative (tomorrow, next Monday …)
 *   2. Time range – "2pm-4pm", "from 9am to 12pm" (start + end in one expression)
 *   3. Single start time – noon, midnight, o'clock, "at X", standalone "Xpm"
 *   4. End time / duration – "for N hours", "for half an hour", "until X", "till X", "by X"
 *   5. Location  – "at/in <Place>"
 *   6. Title     – everything remaining after all extractions
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

        // Pre-consume "by [time]" before start-time extraction so that "by 5pm"
        // is never mistakenly captured as a start time by REGEX_STANDALONE_TIME.
        var pendingEndFromBy: LocalTime? = null
        val byPreResult = extractByTime(text)
        if (byPreResult != null) {
            pendingEndFromBy = byPreResult.first
            text             = byPreResult.second
        }

        // --- 2. Try time range first (captures start + end simultaneously) ---
        val rangeResult = extractTimeRange(text)
        if (rangeResult != null) {
            startTime = rangeResult.first
            endTime   = rangeResult.second
            text      = rangeResult.third
        } else {
            // --- 3. Single start time ---
            val timeResult = extractStartTime(text)
            if (timeResult != null) {
                startTime = timeResult.first
                text = timeResult.second
            }

            // --- 4. End time / duration ---
            val endTimeResult = extractEndTime(text, startTime)
            if (endTimeResult != null) {
                endTime = endTimeResult.first
                text    = endTimeResult.second
            }

            // Apply pre-extracted "by X" as end time only when a start time was found
            if (startTime != null && endTime == null && pendingEndFromBy != null) {
                endTime = pendingEndFromBy
            }
        }

        // --- 5. Location ---
        val locationResult = extractLocation(text)
        if (locationResult != null) {
            location = locationResult.first
            text     = locationResult.second
        }

        // --- 6. Title = everything remaining ---
        val title = cleanTitle(text)

        val isAllDay  = startTime == null
        val finalDate = date ?: referenceDate

        // Default 1-hour duration when start time is known but end time is not
        val finalEndTime = when {
            startTime != null && endTime == null -> startTime.plusHours(1)
            else -> endTime
        }

        return CalendarEvent(
            title     = title,
            startDate = finalDate,
            startTime = startTime,
            endTime   = finalEndTime,
            location  = location,
            isAllDay  = isAllDay
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lookup tables
    // ─────────────────────────────────────────────────────────────────────────

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
        "monday"    to DayOfWeek.MONDAY,
        "tuesday"   to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday"  to DayOfWeek.THURSDAY,
        "friday"    to DayOfWeek.FRIDAY,
        "saturday"  to DayOfWeek.SATURDAY,
        "sunday"    to DayOfWeek.SUNDAY,
        // Abbreviated forms — all four day-name regexes pick these up automatically
        // via DAY_NAMES.keys.joinToString("|"). Note: \bsun\b and \bsat\b carry a small
        // false-positive risk in free text, but calendar-share snippets are typically short.
        "mon"       to DayOfWeek.MONDAY,
        "tue"       to DayOfWeek.TUESDAY,  "tues"  to DayOfWeek.TUESDAY,
        "wed"       to DayOfWeek.WEDNESDAY,
        "thu"       to DayOfWeek.THURSDAY, "thur"  to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
        "fri"       to DayOfWeek.FRIDAY,
        "sat"       to DayOfWeek.SATURDAY,
        "sun"       to DayOfWeek.SUNDAY
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Date regexes (compiled once)
    // ─────────────────────────────────────────────────────────────────────────

    private val REGEX_TODAY    = Regex("\\btoday\\b",   RegexOption.IGNORE_CASE)
    private val REGEX_TONIGHT  = Regex("\\btonight\\b", RegexOption.IGNORE_CASE)
    private val REGEX_TOMORROW = Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE)

    private val REGEX_RELATIVE = Regex(
        "\\bin\\s+(\\d+)\\s+(day|days|week|weeks)\\b", RegexOption.IGNORE_CASE
    )

    // Explicit calendar dates — checked before relative day names so that
    // "on Monday Jan 14" resolves to Jan 14, not just "Monday".
    private val REGEX_MONTH_DAY = Regex(
        "\\b(${MONTH_NAMES.keys.joinToString("|")})\\.?\\s+(\\d{1,2})" +
                "(?:(?:st|nd|rd|th))?(?:[,\\s]+(\\d{4}))?\\b",
        RegexOption.IGNORE_CASE
    )
    // "15th of January", "3rd of March 2025" — ordinal-first (British / formal invite style)
    private val REGEX_DAY_OF_MONTH = Regex(
        "\\b(\\d{1,2})(?:st|nd|rd|th)\\s+(?:of\\s+)?(${MONTH_NAMES.keys.joinToString("|")})\\.?" +
                "(?:[,\\s]+(\\d{4}))?\\b",
        RegexOption.IGNORE_CASE
    )

    // ISO 8601: 2025-01-15  (strict month/day ranges to avoid false positives)
    private val REGEX_ISO_DATE = Regex(
        "\\b(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])\\b"
    )
    // M/D/YYYY or M-D-YYYY  (year required — takes priority over short M/D)
    private val REGEX_NUMERIC_DATE = Regex("\\b(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})\\b")
    // M/D without year — must NOT be followed by another / or digit (avoids re-matching M/D/YYYY)
    private val REGEX_SHORT_NUMERIC_DATE = Regex("\\b(\\d{1,2})/(\\d{1,2})(?![/\\-\\d])")

    // Relative / named days
    private val REGEX_NEXT_DAY = Regex(
        "\\bnext\\s+(${DAY_NAMES.keys.joinToString("|")})\\b", RegexOption.IGNORE_CASE
    )
    private val REGEX_THIS_DAY = Regex(
        "\\bthis\\s+(${DAY_NAMES.keys.joinToString("|")})\\b", RegexOption.IGNORE_CASE
    )
    private val REGEX_ON_DAY = Regex(
        "\\bon\\s+(${DAY_NAMES.keys.joinToString("|")})\\b", RegexOption.IGNORE_CASE
    )
    // Bare day name — lowest priority, catches "Monday at 3pm", "Friday 2pm", etc.
    private val REGEX_BARE_DAY = Regex(
        "\\b(${DAY_NAMES.keys.joinToString("|")})\\b", RegexOption.IGNORE_CASE
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Time regexes
    // ─────────────────────────────────────────────────────────────────────────

    private val REGEX_NOON     = Regex("\\b(?:at\\s+)?noon\\b",     RegexOption.IGNORE_CASE)
    private val REGEX_MIDNIGHT = Regex("\\b(?:at\\s+)?midnight\\b", RegexOption.IGNORE_CASE)
    // "3 o'clock" or "at 3 o'clock [pm]"
    private val REGEX_OCLOCK   = Regex(
        "\\b(?:at\\s+)?(\\d{1,2})\\s+o'?clock(?:\\s*([aApP][mM]))?\\b", RegexOption.IGNORE_CASE
    )
    // "half past 3", "half past 2pm"
    private val REGEX_HALF_PAST = Regex(
        "\\b(?:at\\s+)?half\\s+past\\s+(\\d{1,2})(?:\\s*([aApP][mM]))?\\b", RegexOption.IGNORE_CASE
    )
    // "quarter past 9am", "quarter past 2"
    private val REGEX_QUARTER_PAST = Regex(
        "\\b(?:at\\s+)?quarter\\s+past\\s+(\\d{1,2})(?:\\s*([aApP][mM]))?\\b", RegexOption.IGNORE_CASE
    )
    // "quarter to 5pm", "quarter to 4"
    private val REGEX_QUARTER_TO = Regex(
        "\\b(?:at\\s+)?quarter\\s+to\\s+(\\d{1,2})(?:\\s*([aApP][mM]))?\\b", RegexOption.IGNORE_CASE
    )
    private val REGEX_TIME_WITH_AT    = Regex("\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])?\\b")
    private val REGEX_STANDALONE_TIME = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])\\b")

    /**
     * Time range: "2pm-4pm", "2-4pm", "from 2pm to 4pm", "9am–5pm", "9:00–17:00pm".
     *
     * The end am/pm is required so the parser knows the meridiem.  The start
     * inherits the end meridiem when it has none (e.g. "2-4pm" → both PM).
     * Separators: hyphen(s), en/em dash, "to", "through", "thru".
     */
    private val REGEX_TIME_RANGE = Regex(
        "\\b(?:from\\s+)?" +
                "(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])?" +
                "\\s*(?:--?|–|—|to|through|thru)\\s*" +
                "(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])\\b",
        RegexOption.IGNORE_CASE
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Duration / end-time regexes
    // ─────────────────────────────────────────────────────────────────────────

    // "for half an hour" / "for half hour" / "for a half hour"
    private val REGEX_DURATION_HALF_HOUR = Regex(
        "\\bfor\\s+(?:a\\s+)?half(?:\\s+an?)?\\s*(?:hour|hr)\\b", RegexOption.IGNORE_CASE
    )
    // "for an hour" / "for a hour"
    private val REGEX_DURATION_AN_HOUR = Regex(
        "\\bfor\\s+an?\\s+(?:hour|hr)\\b", RegexOption.IGNORE_CASE
    )
    // "for 1.5 hours" / "for 2.5 hrs" — decimal .5 only (= 30 extra minutes)
    private val REGEX_DURATION_DECIMAL_HOURS = Regex(
        "\\bfor\\s+(\\d+)\\.5\\s*(?:hour|hours|hr|hrs)\\b", RegexOption.IGNORE_CASE
    )
    private val REGEX_DURATION_HOURS = Regex(
        "\\bfor\\s+(\\d+)\\s*(?:hour|hours|hr|hrs)" +
                "(?:\\s*(?:and\\s*)?(\\d+)\\s*(?:minute|minutes|min|mins))?\\b",
        RegexOption.IGNORE_CASE
    )
    private val REGEX_DURATION_MINUTES = Regex(
        "\\bfor\\s+(\\d+)\\s*(?:minute|minutes|min|mins)\\b", RegexOption.IGNORE_CASE
    )
    // "until / till / thru / through <time>"
    private val REGEX_UNTIL = Regex(
        "\\b(?:until|till|til|through|thru)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])?\\b",
        RegexOption.IGNORE_CASE
    )
    // "by 5pm" — end-time deadline; only applied when a start time exists
    private val REGEX_BY_TIME = Regex(
        "\\bby\\s+(\\d{1,2})(?::(\\d{2}))?\\s*([aApP][mM])\\b", RegexOption.IGNORE_CASE
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Location regex
    // ─────────────────────────────────────────────────────────────────────────

    private val REGEX_AT_LOCATION   = Regex(
        "\\b(?:at|in)\\s+([A-Z][A-Za-z0-9' ]+?)(?:\\s*$)", RegexOption.MULTILINE
    )
    private val REGEX_LOCATION_DIGIT = Regex("\\d+.*")

    // ─────────────────────────────────────────────────────────────────────────
    // Title cleanup regexes
    // ─────────────────────────────────────────────────────────────────────────

    private val REGEX_COLLAPSE_WHITESPACE = Regex("\\s{2,}")
    private val REGEX_LEADING_PUNCT       = Regex("^[\\s,;\\-]+")
    private val REGEX_TRAILING_PUNCT      = Regex("[\\s,;\\-]+$")

    // ─────────────────────────────────────────────────────────────────────────
    // Date extraction
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractDate(text: String, ref: LocalDate): Pair<LocalDate, String>? {
        // ── Relative anchors ────────────────────────────────────────────────
        REGEX_TODAY.find(text)?.let { return ref to text.removeRange(it.range).trim() }
        // "tonight" is an alias for today (time is captured separately)
        REGEX_TONIGHT.find(text)?.let { return ref to text.removeRange(it.range).trim() }
        REGEX_TOMORROW.find(text)?.let { return ref.plusDays(1) to text.removeRange(it.range).trim() }

        REGEX_RELATIVE.find(text)?.let { m ->
            val n    = m.groupValues[1].toLong()
            val unit = m.groupValues[2].lowercase()
            val d    = if (unit.startsWith("week")) ref.plusWeeks(n) else ref.plusDays(n)
            return d to text.removeRange(m.range).trim()
        }

        // ── Explicit calendar dates (checked before day-name patterns so that
        //    "on Monday Jan 14" resolves to Jan 14, not the generic "Monday") ─

        // "15th of January", "3rd March 2025" — ordinal-first (British / formal invite style)
        REGEX_DAY_OF_MONTH.find(text)?.let { m ->
            val day   = m.groupValues[1].toInt()
            val month = MONTH_NAMES[m.groupValues[2].lowercase()]!!
            val year  = if (m.groupValues[3].isNotEmpty()) {
                m.groupValues[3].toInt()
            } else {
                val candidate = LocalDate.of(ref.year, month, day)
                if (candidate.isBefore(ref)) ref.year + 1 else ref.year
            }
            return safeDate(year, month, day)?.let { it to text.removeRange(m.range).trim() }
        }

        REGEX_MONTH_DAY.find(text)?.let { m ->
            val month = MONTH_NAMES[m.groupValues[1].lowercase()]!!
            val day   = m.groupValues[2].toInt()
            val year  = if (m.groupValues[3].isNotEmpty()) {
                m.groupValues[3].toInt()
            } else {
                val candidate = LocalDate.of(ref.year, month, day)
                if (candidate.isBefore(ref)) ref.year + 1 else ref.year
            }
            return safeDate(year, month, day)?.let { it to text.removeRange(m.range).trim() }
        }

        REGEX_ISO_DATE.find(text)?.let { m ->
            val year  = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val day   = m.groupValues[3].toInt()
            return safeDate(year, month, day)?.let { it to text.removeRange(m.range).trim() }
        }

        REGEX_NUMERIC_DATE.find(text)?.let { m ->
            val month = m.groupValues[1].toInt()
            val day   = m.groupValues[2].toInt()
            val year  = m.groupValues[3].toInt()
            return safeDate(year, month, day)?.let { it to text.removeRange(m.range).trim() }
        }

        // ── Named-day patterns ────────────────────────────────────────────────
        REGEX_NEXT_DAY.find(text)?.let { m ->
            val dow = DAY_NAMES[m.groupValues[1].lowercase()]!!
            return ref.with(TemporalAdjusters.next(dow)) to text.removeRange(m.range).trim()
        }
        REGEX_THIS_DAY.find(text)?.let { m ->
            val dow = DAY_NAMES[m.groupValues[1].lowercase()]!!
            return ref.with(TemporalAdjusters.nextOrSame(dow)) to text.removeRange(m.range).trim()
        }
        REGEX_ON_DAY.find(text)?.let { m ->
            val dow = DAY_NAMES[m.groupValues[1].lowercase()]!!
            return ref.with(TemporalAdjusters.nextOrSame(dow)) to text.removeRange(m.range).trim()
        }

        // ── Short numeric date M/D (no year) ─────────────────────────────────
        REGEX_SHORT_NUMERIC_DATE.find(text)?.let { m ->
            val month = m.groupValues[1].toInt()
            val day   = m.groupValues[2].toInt()
            if (month in 1..12 && day in 1..31) {
                val year = run {
                    val candidate = LocalDate.of(ref.year, month, day)
                    if (candidate.isBefore(ref)) ref.year + 1 else ref.year
                }
                return safeDate(year, month, day)?.let { it to text.removeRange(m.range).trim() }
            }
        }

        // ── Bare day name — lowest priority ───────────────────────────────────
        // Catches "Monday at 3pm", "Friday 2pm", day names without a preposition.
        REGEX_BARE_DAY.find(text)?.let { m ->
            val dow = DAY_NAMES[m.groupValues[1].lowercase()]!!
            return ref.with(TemporalAdjusters.nextOrSame(dow)) to text.removeRange(m.range).trim()
        }

        return null
    }

    /** Returns null instead of throwing for out-of-range date components. */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Time range extraction
    // ─────────────────────────────────────────────────────────────────────────

    /** Extracts a "by [time]" end-time deadline, removing it from the text. */
    private fun extractByTime(text: String): Pair<LocalTime, String>? {
        val m = REGEX_BY_TIME.find(text) ?: return null
        return parseTimeComponents(m.groupValues[1], m.groupValues[2], m.groupValues[3])
            ?.let { it to text.removeRange(m.range).trim() }
    }

    /**
     * Extracts a start+end time from a single range expression.
     * When the start time has no explicit am/pm it inherits from the end
     * (e.g. "2-4pm" → both PM; "10am-12pm" → start AM, end PM).
     */
    private fun extractTimeRange(text: String): Triple<LocalTime, LocalTime, String>? {
        val m = REGEX_TIME_RANGE.find(text) ?: return null
        val endAmPm            = m.groupValues[6]
        val effectiveStartAmPm = m.groupValues[3].ifEmpty { endAmPm }
        val startTime = parseTimeComponents(m.groupValues[1], m.groupValues[2], effectiveStartAmPm)
            ?: return null
        val endTime   = parseTimeComponents(m.groupValues[4], m.groupValues[5], endAmPm)
            ?: return null
        return Triple(startTime, endTime, text.removeRange(m.range).trim())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Single start-time extraction
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractStartTime(text: String): Pair<LocalTime, String>? {
        // "half past 3pm" → 15:30,  "half past 9" → 9:30
        REGEX_HALF_PAST.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], "", m.groupValues[2])?.let { base ->
                return base.withMinute(30) to text.removeRange(m.range).trim()
            }
        }
        // "quarter past 9am" → 9:15,  "quarter past 2" → 2:15
        REGEX_QUARTER_PAST.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], "", m.groupValues[2])?.let { base ->
                return base.withMinute(15) to text.removeRange(m.range).trim()
            }
        }
        // "quarter to 5pm" → 16:45,  "quarter to 12" → 11:45
        REGEX_QUARTER_TO.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], "", m.groupValues[2])?.let { base ->
                val adjusted = base.minusHours(1).withMinute(45)
                return adjusted to text.removeRange(m.range).trim()
            }
        }
        REGEX_NOON.find(text)?.let {
            return LocalTime.of(12, 0) to text.removeRange(it.range).trim()
        }
        REGEX_MIDNIGHT.find(text)?.let {
            return LocalTime.of(0, 0) to text.removeRange(it.range).trim()
        }
        // "3 o'clock" / "at 3 o'clock [pm]"
        REGEX_OCLOCK.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], "", m.groupValues[2])?.let { t ->
                return t to text.removeRange(m.range).trim()
            }
        }
        // "at 2pm", "at 2:30 PM", "at 14:00"
        REGEX_TIME_WITH_AT.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { t ->
                return t to text.removeRange(m.range).trim()
            }
        }
        // Standalone "2pm", "3:30PM" (am/pm required to avoid matching bare numbers)
        REGEX_STANDALONE_TIME.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { t ->
                return t to text.removeRange(m.range).trim()
            }
        }
        return null
    }

    private fun parseTimeComponents(hourStr: String, minuteStr: String, ampmStr: String): LocalTime? {
        var hour   = hourStr.toIntOrNull() ?: return null
        val minute = if (minuteStr.isNotEmpty()) minuteStr.toIntOrNull() ?: 0 else 0

        if (ampmStr.isNotEmpty()) {
            val isPm = ampmStr.lowercase() == "pm"
            if (isPm && hour != 12) hour += 12
            if (!isPm && hour == 12) hour = 0
        }

        if (hour > 23 || minute > 59) return null
        return LocalTime.of(hour, minute)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // End time / duration extraction
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractEndTime(text: String, startTime: LocalTime?): Pair<LocalTime, String>? {
        // "for half an hour"
        REGEX_DURATION_HALF_HOUR.find(text)?.let { m ->
            if (startTime != null)
                return startTime.plusMinutes(30) to text.removeRange(m.range).trim()
        }
        // "for an hour" / "for a hour"
        REGEX_DURATION_AN_HOUR.find(text)?.let { m ->
            if (startTime != null)
                return startTime.plusHours(1) to text.removeRange(m.range).trim()
        }
        // "for 1.5 hours" / "for 2.5 hrs" — integer part + 30 minutes
        REGEX_DURATION_DECIMAL_HOURS.find(text)?.let { m ->
            if (startTime != null) {
                val wholeHours = m.groupValues[1].toLong()
                return startTime.plusHours(wholeHours).plusMinutes(30) to
                        text.removeRange(m.range).trim()
            }
        }
        // "for N hour(s) [and N minutes]"
        REGEX_DURATION_HOURS.find(text)?.let { m ->
            if (startTime != null) {
                val hours   = m.groupValues[1].toLong()
                val minutes = m.groupValues[2].toLongOrNull() ?: 0L
                return startTime.plusHours(hours).plusMinutes(minutes) to
                        text.removeRange(m.range).trim()
            }
        }
        // "for N minutes"
        REGEX_DURATION_MINUTES.find(text)?.let { m ->
            if (startTime != null) {
                return startTime.plusMinutes(m.groupValues[1].toLong()) to
                        text.removeRange(m.range).trim()
            }
        }
        // "until / till / through / thru <time>"
        REGEX_UNTIL.find(text)?.let { m ->
            parseTimeComponents(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { t ->
                return t to text.removeRange(m.range).trim()
            }
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Location extraction
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractLocation(text: String): Pair<String, String>? {
        val m = REGEX_AT_LOCATION.find(text) ?: return null
        val loc = m.groupValues[1].trim()
        if (loc.length >= 2 && !loc.matches(REGEX_LOCATION_DIGIT)) {
            return loc to text.removeRange(m.range).trim()
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Title cleanup
    // ─────────────────────────────────────────────────────────────────────────

    private fun cleanTitle(text: String): String = text
        .replace(REGEX_COLLAPSE_WHITESPACE, " ")
        .replace(REGEX_LEADING_PUNCT, "")
        .replace(REGEX_TRAILING_PUNCT, "")
        .trim()
}
