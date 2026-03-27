
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
- Relative: "today", "tonight", "tomorrow", "in 3 days", "in 2 weeks"
- Day names (full): "next Monday", "this Thursday", "on Friday", bare "Monday"
- Day names (abbreviated): "Mon", "Tue" / "Tues", "Wed", "Thu" / "Thur" / "Thurs", "Fri", "Sat", "Sun"
- Month and day: "Jan 15", "January 15th", "January 15, 2025", "Feb 20"
- Ordinal-first: "15th of January", "3rd March", "21st June 2026"
- Numeric with year: "1/25/2025", "1-25-2025"
- Numeric without year: "1/15" (rolls to next year if the date has already passed)
- ISO 8601: "2025-03-15"

**Times**
- Keywords: "noon", "at noon", "midnight", "at midnight"
- 12-hour: "at 2pm", "at 2:30 PM", "3pm" (standalone, no "at" required)
- 24-hour: "at 14:00", "at 18:30"
- O'clock: "3 o'clock", "at 9 o'clock pm"
- Half / quarter: "half past 2pm", "quarter past 9am", "quarter to 5pm"

**Time Ranges**
- Hyphen / dash: "9am-10am", "2-4pm" (start inherits end meridiem), "9:00–17:00"
- From … to: "from 9am to 12pm"
- With separators: "7:30 PM – 9:30 PM" (en dash, em dash)

**Duration / End Time**
- Hours: "for 1 hour", "for an hour", "for 2 hours"
- Hours and minutes: "for 2 hours and 30 minutes"
- Decimal hours: "for 1.5 hours", "for 2.5 hours"
- Minutes: "for 30 minutes", "for 45 mins"
- Half hour: "for half an hour", "for half hour"
- Until: "until 4pm", "till 3:30pm", "through 5pm", "thru 6pm"
- Deadline: "by 5pm" (used as end time when a start time is present)
- Default duration is 1 hour when only a start time is given

**Location**
- "at Conference Room B", "in Building 4", "at Starbucks"

**Examples**

| Input | Result |
|---|---|
| Lunch with Sarah tomorrow at noon | Tomorrow · 12:00–13:00 · "Lunch with Sarah" |
| Team meeting next Tuesday at 3pm for 1 hour in Conference Room B | Next Tue · 15:00–16:00 · "Conference Room B" |
| Dentist appointment Jan 15 at 2:30pm | Jan 15 · 14:30–15:30 |
| Team offsite tomorrow | All-day tomorrow |
| Mon at 3pm | Next Monday · 15:00–16:00 |
| Workshop from 9am to 12pm | Today · 09:00–12:00 |
| Doctor at quarter past 9am | Today · 09:15–10:15 |
| 15th of January at 3pm | Jan 15 · 15:00–16:00 |
| Friday, February 14 · 7:30 PM – 9:30 PM | Feb 14 · 19:30–21:30 |
| When: Tuesday January 14, 2025 at 3:00 PM | Jan 14 2025 · 15:00–16:00 |
| Hey, can you do Thursday 2pm? | Next Thu · 14:00–15:00 |

