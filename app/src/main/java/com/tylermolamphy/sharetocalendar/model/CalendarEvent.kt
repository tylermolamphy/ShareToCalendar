package com.tylermolamphy.sharetocalendar.model

import java.time.LocalDate
import java.time.LocalTime

data class CalendarEvent(
    val title: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val location: String = "",
    val description: String = "",
    val isAllDay: Boolean = false
)
