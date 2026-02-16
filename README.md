
ShareToCalendar is an Android app that registers as a share target so you can
highlight text in any app, tap "Share", and instantly create a calendar event.
A built-in natural language parser extracts the date, time, duration, location,
and title from the shared text -- no external services required.

⚠️ This is entirely AI generated code. It works well on my daily device, but be warned that it has not been tested beyond this, and is always subject to breaking changes. Consider yourself warned.


## Download

https://github.com/tylermolamphy/ShareToCalendar/releases


## Screenshots

| Settings | Adding to Event |
|:---:|:---:|
| ![Settings screen](https://github.com/tylermolamphy/ShareToCalendar/raw/ci-screenshots/latest/screenshot_settings.png) | ![Confirm screen](https://github.com/tylermolamphy/ShareToCalendar/raw/ci-screenshots/latest/screenshot_confirm.png) |


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

