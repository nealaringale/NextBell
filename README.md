# NextBell 🔔

**NextBell is a native Android timetable app for college students.**

The current MVP is built as an Android application and uses the student's division, batch, and roll number to show the most relevant class schedule.

## Android MVP

- Native Android app
- Today view with current/next class
- Live minutes remaining
- Weekly day switching
- Batch-aware practical/tutorial labels
- Class room and teacher details
- Day statistics and free-period count
- Dark UI designed for quick glances

## Build the APK with GitHub Actions

Every push to `main` triggers:

```text
GitHub Push
    ↓
GitHub Actions
    ↓
Android SDK + Java 17
    ↓
Gradle assembleDebug
    ↓
NextBell-debug-apk
```

Open the **Actions** tab, select **Build NextBell Android**, and download the `NextBell-debug-apk` artifact from a successful run.

## Local Android build

You need Android Studio/SDK and Gradle 8.9+.

```bash
cd android
gradle assembleDebug
```

The APK is generated at:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Timetable data

The demo timetable is currently stored in:

`android/app/src/main/java/com/nealaringale/nextbell/TimetableData.java`

Replace the sample classes with the real NMIET schedule as you collect it.

## Roadmap

1. Replace demo timetable with the real NMIET timetable.
2. Add a first-run profile setup screen.
3. Store profile + attendance locally.
4. Add notifications for upcoming classes.
5. Add Supabase for shared class updates and admin edits.
6. Produce a signed release APK/AAB for wider distribution.
