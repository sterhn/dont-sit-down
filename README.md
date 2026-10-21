# Don't Sit Down

An Android app that catches you at the door before you collapse on the couch:
"don't sit down. 17 minutes. go."

Kotlin + Jetpack Compose + Room. Everything stays on the phone: no accounts, no sync, no analytics.

## Build & install

You need JDK 17+ and the Android SDK (platform 35). Android Studio provides both; open this folder in it,
or from the command line:

```sh
./gradlew assembleRelease          # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the debug key so it can be sideloaded as-is. It's for one phone, not the Play Store.

On first launch the app asks for:
- **notifications**, so the reminder can appear at all
- **exact alarms** (Android 12+), so it fires at 19:00 rather than "around then"
- **full-screen** (Android 14+), so it can light up the lock screen like an alarm clock

Settings → *the takeover* shows the state of all three and has a **test the takeover (5 sec)** button.

## What's where

| | |
|---|---|
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

Plain JVM tests cover chain generation and scoring. `ScreensTest` runs the real UI under Robolectric, clicks
through a whole chain and saves screenshots (`-Dscreenshots.dir=/some/dir` picks where they go).
