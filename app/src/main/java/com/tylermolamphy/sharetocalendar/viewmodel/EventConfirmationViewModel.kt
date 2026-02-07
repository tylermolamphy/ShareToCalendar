package com.tylermolamphy.sharetocalendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tylermolamphy.sharetocalendar.data.CalendarRepository
import com.tylermolamphy.sharetocalendar.data.PreferencesRepository
import com.tylermolamphy.sharetocalendar.model.CalendarEvent
import com.tylermolamphy.sharetocalendar.parser.NaturalLanguageParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventConfirmationViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarRepository = CalendarRepository(application.contentResolver)
    private val preferencesRepository = PreferencesRepository(application)

    private val _event = MutableStateFlow(CalendarEvent())
    val event: StateFlow<CalendarEvent> = _event.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    val selectedCalendarId: StateFlow<Long?> = preferencesRepository.selectedCalendarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun parseSharedText(text: String) {
        val parsed = NaturalLanguageParser.parse(text)
        _event.value = parsed.copy(
            title = shortenTitle(parsed.title),
            description = text
        )
    }

    private fun shortenTitle(title: String): String {
        if (title.length <= 50) return title
        // Take first sentence (split on . ! ?)
        val firstSentence = title.split(Regex("[.!?]"), limit = 2).first().trim()
        if (firstSentence.length <= 50) return firstSentence
        // Still too long — truncate at word boundary + ellipsis
        return firstSentence.take(47).substringBeforeLast(' ') + "…"
    }

    fun updateEvent(event: CalendarEvent) {
        _event.value = event
    }

    fun saveEvent() {
        viewModelScope.launch {
            val calendarId = preferencesRepository.selectedCalendarId.first()
            if (calendarId == null) {
                _saveResult.value = SaveResult.Error("No calendar selected. Please select a calendar in Settings.")
                return@launch
            }
            val currentEvent = _event.value
            if (currentEvent.title.isBlank()) {
                _saveResult.value = SaveResult.Error("Event title cannot be empty.")
                return@launch
            }
            try {
                val eventId = calendarRepository.insertEvent(calendarId, currentEvent)
                if (eventId != null) {
                    _saveResult.value = SaveResult.Success
                } else {
                    _saveResult.value = SaveResult.Error("Failed to save event.")
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(e.message ?: "Unknown error occurred.")
            }
        }
    }

    fun resetSaveResult() {
        _saveResult.value = SaveResult.Idle
    }

    sealed class SaveResult {
        data object Idle : SaveResult()
        data object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}
