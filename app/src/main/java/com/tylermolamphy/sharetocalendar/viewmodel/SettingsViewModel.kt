package com.tylermolamphy.sharetocalendar.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tylermolamphy.sharetocalendar.data.CalendarRepository
import com.tylermolamphy.sharetocalendar.data.PreferencesRepository
import com.tylermolamphy.sharetocalendar.model.CalendarInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarRepository = CalendarRepository(application.contentResolver)
    private val preferencesRepository = PreferencesRepository(application)

    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()

    val selectedCalendarId: StateFlow<Long?> = preferencesRepository.selectedCalendarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    init {
        val ctx = getApplication<Application>()
        val readGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        if (readGranted && writeGranted) {
            _permissionGranted.value = true
            loadCalendars()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) {
            loadCalendars()
        }
    }

    fun loadCalendars() {
        viewModelScope.launch {
            _calendars.value = calendarRepository.getCalendars()
        }
    }

    fun selectCalendar(calendarId: Long) {
        viewModelScope.launch {
            preferencesRepository.setSelectedCalendarId(calendarId)
        }
    }
}
