package com.tylermolamphy.sharetocalendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val SELECTED_CALENDAR_ID = longPreferencesKey("selected_calendar_id")
    }

    val selectedCalendarId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_CALENDAR_ID]
    }

    suspend fun setSelectedCalendarId(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_CALENDAR_ID] = id
        }
    }
}
