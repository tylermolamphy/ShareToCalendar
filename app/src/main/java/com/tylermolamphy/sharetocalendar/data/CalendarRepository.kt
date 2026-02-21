package com.tylermolamphy.sharetocalendar.data

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.CalendarContract
import com.tylermolamphy.sharetocalendar.model.CalendarEvent
import com.tylermolamphy.sharetocalendar.model.CalendarInfo
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val contentResolver: ContentResolver) {

    suspend fun getCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = it.getLong(0),
                        displayName = it.getString(1) ?: "",
                        accountName = it.getString(2) ?: "",
                        color = it.getInt(3)
                    )
                )
            }
        }
        calendars
    }

    suspend fun insertEvent(calendarId: Long, event: CalendarEvent): Long? = withContext(Dispatchers.IO) {
        val timeZone = TimeZone.getDefault().id
        val zoneId = ZoneId.systemDefault()

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)

            if (event.isAllDay) {
                put(CalendarContract.Events.ALL_DAY, 1)
                val startMillis = event.startDate
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 24 * 60 * 60 * 1000)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                val startDateTime = event.startDate
                    .atTime(event.startTime!!)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
                put(CalendarContract.Events.DTSTART, startDateTime)

                val endTime = event.endTime ?: event.startTime.plusHours(1)
                val endDateTime = event.startDate
                    .atTime(endTime)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
                put(CalendarContract.Events.DTEND, endDateTime)
            }

            if (event.location.isNotBlank()) {
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
            }
            if (event.description.isNotBlank()) {
                put(CalendarContract.Events.DESCRIPTION, event.description)
            }
        }

        val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        uri?.lastPathSegment?.toLongOrNull()
    }
}
