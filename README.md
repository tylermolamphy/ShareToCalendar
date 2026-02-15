# ShareToCalendar

Share text to create calendar events using natural language.

ShareToCalendar is an Android app that registers as a share target so you can
highlight text in any app, tap "Share", and instantly create a calendar event.
A built-in natural language parser extracts the date, time, duration, location,
and title from the shared text -- no external services required.

## Screenshots

| Settings | Confirm Event |
|:---:|:---:|
| ![Settings screen](https://github.com/tylermolamphy/ShareToCalendar/raw/ci-screenshots/pr-16/screenshot_settings.png) | ![Confirm screen](https://github.com/tylermolamphy/ShareToCalendar/raw/ci-screenshots/pr-16/screenshot_confirm.png) |

## Features

- **Share intent integration** -- highlight text anywhere and share it directly
  to your calendar
- **Natural language parsing** -- a lightweight, offline regex-based parser
  extracts event details from plain text
- **Calendar integration** -- reads available calendars and inserts events via
  the Android CalendarContract API
- **Default calendar selection** -- pick your preferred calendar once in settings
- **All-day and timed events** -- automatically creates an all-day event when no
  time is specified
- **Material 3 / Material You** -- follows your device theme including dark mode

## Supported Input Formats

The parser handles a wide range of natural language patterns:

**Dates**
- Relative: "today", "tomorrow", "in 3 days", "in 2 weeks"
- Day names: "next Monday", "on Friday"
- Month and day: "Jan 15", "January 15th", "January 15, 2025"
- Numeric: "1/25/2025", "1-25-2025"

**Times**
- Keywords: "at noon", "at midnight"
- 12-hour: "at 2pm", "at 2:30 PM"
- 24-hour: "at 14:00", "at 18:30"

**Duration / End Time**
- Hours and minutes: "for 1 hour", "for 30 minutes", "for 2 hours and 30 minutes"
- Until: "until 4pm"
- Default duration is 1 hour when only a start time is given

**Location**
- "at Conference Room B", "in Building 4"

**Examples**

| Input | Result |
|---|---|
| Lunch with Sarah tomorrow at noon | Tomorrow, 12:00 - 13:00, title "Lunch with Sarah" |
| Team meeting next Tuesday at 3pm for 1 hour in Conference Room B | Next Tuesday, 15:00 - 16:00, location "Conference Room B" |
| Dentist appointment Jan 15 at 2:30pm | Jan 15, 14:30 - 15:30 |
| Team offsite tomorrow | All-day event tomorrow |

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose (BOM 2024.12.01) |
| Design system | Material 3 |
| Navigation | Navigation Compose |
| Persistence | DataStore Preferences |
| Architecture | MVVM (ViewModel + Repository) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## Building

Clone the repository and build with Gradle:

```bash
git clone https://github.com/tylermolamphy/ShareToCalendar.git
cd ShareToCalendar
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

To run tests:

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires a connected device or emulator)
./gradlew connectedAndroidTest
```
