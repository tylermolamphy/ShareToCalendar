package com.tylermolamphy.sharetocalendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tylermolamphy.sharetocalendar.data.CalendarRepository
import com.tylermolamphy.sharetocalendar.data.PreferencesRepository
import com.tylermolamphy.sharetocalendar.model.CalendarEvent
import com.tylermolamphy.sharetocalendar.parser.NaturalLanguageParser
import com.tylermolamphy.sharetocalendar.validation.InputValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventConfirmationViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarRepository = CalendarRepository(application.contentResolver)
    private val preferencesRepository = PreferencesRepository(application)

    private val _event = MutableStateFlow(CalendarEvent())
    val event: StateFlow<CalendarEvent> = _event.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    val selectedCalendarId: StateFlow<Long?> = preferencesRepository.selectedCalendarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun parseSharedText(text: String) {
        val parsed = withContext(Dispatchers.Default) {
            NaturalLanguageParser.parse(text)
        }
        _event.value = parsed.copy(
            title = InputValidation.shortenParsedTitle(parsed.title),
            description = text
        )
    }

    fun updateEvent(event: CalendarEvent) {
        _event.value = event
    }

    fun saveEvent() {
        viewModelScope.launch {
            // Use the cached StateFlow value if ready; fall back to a direct DataStore
            // read so save never fails due to the StateFlow not having emitted yet.
            val calendarId = selectedCalendarId.value
                ?: preferencesRepository.selectedCalendarId.first()
            if (calendarId == null) {
                _saveResult.value = SaveResult.Error("No calendar selected. Please select a calendar in Settings.")
                return@launch
            }
            val sanitizedTitle = InputValidation.sanitizeTitle(_event.value.title)
            if (sanitizedTitle == null) {
                _saveResult.value = SaveResult.Error("Event title cannot be empty.")
                return@launch
            }
            val currentEvent = _event.value.copy(
                title = sanitizedTitle,
                location = InputValidation.sanitizeLocation(_event.value.location)
            )
            if (!currentEvent.isAllDay &&
                !InputValidation.isValidTimeRange(currentEvent.startTime, currentEvent.endTime)
            ) {
                _saveResult.value = SaveResult.Error("End time must be after start time.")
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
                android.util.Log.e("EventConfirmationVM", "Failed to save event", e)
                _saveResult.value = SaveResult.Error("Failed to save event. Please try again.")
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
