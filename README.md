# still going

One Android app with two jobs:

- **don't sit down**: catches you at the door before you collapse on the couch.
  "don't sit down. 17 minutes. go."
- **the daily check-in**: sleep, body and mood in the morning, an intention for the day, and an honest
  close of the day in the evening, with your hoodie-wearing character watching from a taped-in polaroid.
  Small weekly and monthly goals, a journal, a week story. No streaks, on purpose: the days you wrote
  something are quietly marked, and stats opens on how you've been feeling, not how consistent you've been.

They look like two different apps, on purpose. The check-in is the web app's graph-paper notebook (navy ink,
pastel cards stuck on crooked, Gaegu and Nunito, the character everywhere) and follows the phone's light/dark
setting. Don't sit down is stark: ink, amber, monospace, no paper, no character. The one place they touch is the
black-and-amber chores ticket taped into the today page. They still talk to each other: the evening check-in
shows what chores you got done, and the week story includes both.

Kotlin + Jetpack Compose + Room. Everything stays on the phone: no accounts, no sync, no analytics.

## The tabs

| | |
|---|---|
| | **the check-in** (notebook) |
| **today** | the check-in. morning / evening, yesterday / today (before 5am it opens on last night). autosaves every tap. a strip at the bottom points at tonight's chain and the next goal |
| **goals** | small adventures, weekly or monthly, with a doodle each |
| **progress** | how you've been lately, last 7 days, averages, chart, boundary hold rates, mood after sleep, journal, week story |
| | **don't sit down** (stark) |
| **chain** | don't sit down: the chain, the timer, "just one", "not now", and **not home today**. `week →` at the top is the couch-vs-you summary, `tasks →` edits the task list |
| **settings** | your tasks, days away, the takeover, check-in reminders, check-in import / export, full backup, theme |

## Not home today

On the chain screen, the takeover, or the takeover notification: **not home today**. Pick just today,
today + tomorrow, 3 days or a week. While you're away there's no takeover, no snooze, no scroll-shame nudge,
and the day doesn't count for the couch (it shows as "away" in the weekly summary). Doing a chore anyway
still counts for you. Settings → *not home* shows it and has **i'm home →** to undo.

## Moving your check-ins over from the web app

Your history, goals and boundaries come over in one go. In the web app, open **progress**, then either:

- **copy data**, then in this app tap **paste**, or
- **export data (JSON)**, then in this app tap **open file** and pick it.

The buttons are on the today page while the check-in is still empty, and always under
**settings → check-in**. You see what's in the file before anything is replaced, and your chores and tasks
aren't touched. **export file** / **copy data** here write the same format back, so the web app can read it too.

## Backup

**settings → backup → save backup** writes everything (tasks, the chore log, chain history, settings, days
away, and the check-in) to one JSON file. **restore** puts it all back, on this phone or a new one.

## Build & install

You need JDK 17+ and the Android SDK (platform 35). Android Studio provides both; open this folder in it,
or from the command line:

```sh
./gradlew assembleRelease          # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

The app id is still `com.dontsitdown`, so it installs straight over Don't Sit Down and keeps your tasks,
history and settings. The release build is signed with the debug key so it can be sideloaded as-is. It's for one phone, not the Play Store.

On first launch the app asks for:
- **notifications**, so the reminder can appear at all
- **exact alarms** (Android 12+), so it fires at 19:00 rather than "around then"
- **full-screen** (Android 14+), so it can light up the lock screen like an alarm clock

Settings → *the takeover* shows the state of all three and has a **test the takeover (5 sec)** button.

## What's where

| | |
|---|---|
| `checkin/Model.kt` | the check-in data, same JSON shape as the web app; a lenient importer ported from it |
| `checkin/Checkin.kt` | days written, goal periods, faces for moods, the story and markdown |
| `checkin/CheckinStore.kt` | one JSON file, written a moment after each change |
| `ui/checkin/` | today, goals, stats, journal, week story |
| `ui/components/Paper.kt`, `Doodles.kt`, `Moments.kt` | polaroid, tape, paper cards, the 1–10 scale, goal doodles, toasts, confetti |
| `service/CheckinReminders.kt` | optional quiet morning / evening check-in reminders |
| `chain/ChainGenerator.kt` | picks 2–4 tasks in ≤ N minutes: overdue first, then stalest, some randomness, never two long tasks, learned times over estimates |
| `chain/ChainSession.kt` | the ready → dread → active ⇄ between → complete state machine, combo counting, saving results |
| `chain/MessageGenerator.kt` | every line the app says, normal and spite voice, negotiator and ghost copy, no repeats in a row |
| `chain/WeekSummary.kt` | the weekly summary, couch-vs-you score, the factual grid |
| `service/ReminderScheduler.kt`, `Reminders.kt`, `ReminderReceiver.kt` | `setAlarmClock` takeover, snooze (with limit), dismissal logging, the optional 2-hour scroll-shame nudge |
| `service/FullScreenReminderActivity.kt` | the lock-screen takeover, then ghost → negotiator → "alright. noted." |
| `service/TimerService.kt` | foreground service: ongoing chronometer notification with DONE / KEEP MOVING |
| `service/DailyWorker.kt`, `WeeklySummaryWorker.kt` | WorkManager: daily due-date refresh + alarm safety net, sunday-evening summary |

## Tests

```sh
./gradlew testDebugUnitTest
```

Plain JVM tests cover chain generation, scoring (including away days) and the check-in logic and web-app
import. `ScreensTest` and `CheckinScreensTest` run the real UI under Robolectric, click through a whole chain,
a check-in and a day away, and save screenshots (`-Dscreenshots.dir=/some/dir` picks where they go).

`data/Backup.kt` writes and restores the full backup. The character stickers (`res/drawable-nodpi/char_*.webp`) come from the daily-checkin web app. The handwriting
font is [Gaegu](https://fonts.google.com/specimen/Gaegu), subset to latin, under the SIL Open Font License
(`licenses/Gaegu-OFL.txt`).
