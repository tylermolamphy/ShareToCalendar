package com.tylermolamphy.sharetocalendar.validation

import java.time.LocalTime

/**
 * Pure, stateless validation and sanitization helpers for user-supplied event fields.
 *
 * All functions are free of Android context so they can be exercised by plain JUnit4 unit tests.
 */
object InputValidation {

    // ── Character limits ────────────────────────────────────────────────────
    const val MAX_TITLE_LENGTH = 150
    const val MAX_LOCATION_LENGTH = 200
    const val MAX_DESCRIPTION_LENGTH = 1000

    // Soft limit applied to the NLP-extracted title before showing it in the UI
    const val PARSED_TITLE_MAX_LENGTH = 50

    // ── Sanitization ────────────────────────────────────────────────────────

    /**
     * Trim leading/trailing whitespace from [raw].
     * Returns `null` when the result is blank (empty or whitespace-only).
     */
    fun sanitizeTitle(raw: String): String? {
        val trimmed = raw.trim()
        return if (trimmed.isBlank()) null else trimmed
    }

    /**
     * Trim leading/trailing whitespace from [raw].
     * Empty/whitespace-only locations are returned as an empty string (location is optional).
     */
    fun sanitizeLocation(raw: String): String = raw.trim()

    // ── NLP title shortening ────────────────────────────────────────────────

    /**
     * Shorten a parser-extracted title so it fits in [PARSED_TITLE_MAX_LENGTH] chars using
     * the following cascade:
     *
     * 1. Already short enough → return as-is.
     * 2. Has multiple sentences → return the first sentence (if it fits).
     * 3. Still too long → truncate at the last word boundary before char 47, add "…".
     * 4. No word boundary found (one very long word) → hard-truncate at [PARSED_TITLE_MAX_LENGTH].
     */
    fun shortenParsedTitle(title: String): String {
        if (title.length <= PARSED_TITLE_MAX_LENGTH) return title

        val firstSentence = title.split(Regex("[.!?]"), limit = 2).first().trim()
        // Only use the first sentence if it's non-blank (a leading punctuation char like "!" or "?"
        // would produce an empty first segment which we must skip).
        if (firstSentence.isNotBlank() && firstSentence.length <= PARSED_TITLE_MAX_LENGTH) return firstSentence

        // Source for truncation: prefer the non-blank first sentence; fall back to the full title.
        val source = if (firstSentence.isNotBlank()) firstSentence else title
        val truncated = source.take(PARSED_TITLE_MAX_LENGTH - 3).substringBeforeLast(' ')
        return if (truncated.isBlank()) title.take(PARSED_TITLE_MAX_LENGTH) else "$truncated…"
    }

    // ── Time range validation ───────────────────────────────────────────────

    /**
     * Returns `true` when the time range is valid:
     * - Either time is `null` (not set) → considered valid (nothing to check).
     * - Both set → [end] must be **strictly after** [start].
     *
     * Equal start/end times are considered invalid (zero-duration events are not useful).
     */
    fun isValidTimeRange(start: LocalTime?, end: LocalTime?): Boolean {
        if (start == null || end == null) return true
        return end.isAfter(start)
    }

    // ── Field-length guards (used in UI onValueChange) ──────────────────────

    /** Returns `true` if [text] is within the allowed title length. */
    fun isTitleLengthValid(text: String): Boolean = text.length <= MAX_TITLE_LENGTH

    /** Returns `true` if [text] is within the allowed location length. */
    fun isLocationLengthValid(text: String): Boolean = text.length <= MAX_LOCATION_LENGTH

    /** Returns `true` if [text] is within the allowed description length. */
    fun isDescriptionLengthValid(text: String): Boolean = text.length <= MAX_DESCRIPTION_LENGTH
}
