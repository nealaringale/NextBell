# NextBell

NextBell is a native Android day planner for FE Division B students. A student's roll number selects batch and tutorial entries from the published timetable. The app also includes a manual spending log, monthly budgets, recurring expenses, optional class reminders, six color themes, and reduced-motion controls.

## Product and data model

- **Android app:** Java, Android SDK 35, minimum Android 8.0 (API 26), target SDK 35. Screens are native views hosted in `MainActivity`; timetable, expense, notification, and backup logic live in separate Java classes.
- **Timetable:** bundled official FE Division B timetable, W.E.F. 16 September 2026, Revision 00. Schedule data is available offline; batch filtering is based on the student's roll number.
- **Profile and spending:** stored locally in the app's private `SharedPreferences`; there is no server, account system, or network permission. System cloud backup and device-to-device transfer are excluded. The user can export a readable JSON backup to a file they choose. Anyone who receives that file can read it.
- **Money entry:** manual. The app requests no SMS permissions and does not read messages.
- **Admin companion:** a separate local draft tool. It does not publish timetable changes to student devices.
- **Browser prototype:** `src/` is an older Vite demo with fixture data; it is not the Android app or the download website and should not be treated as the live timetable source.

## Build and verify Android

Install JDK 17, Android SDK Platform 35, Android Build Tools 35.0.0, and Gradle 8.9 or newer. From `android/`, run:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug assembleDebug
```

The student APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

`feature/announcements` runs the same Android unit tests, lint, and debug APK build in GitHub Actions. It also uploads the student and admin APKs as workflow artifacts. The public Pages publishing workflow is manual; pushing a feature branch does not publish the site.

### Stable signing for Android updates

The student APK can use a persistent keystore when these four repository Actions secrets are configured: `NEXTBELL_KEYSTORE_BASE64`, `NEXTBELL_KEYSTORE_PASSWORD`, `NEXTBELL_KEY_ALIAS`, and `NEXTBELL_KEY_PASSWORD`. See [.github/STABLE_SIGNING_SETUP.md](.github/STABLE_SIGNING_SETUP.md). If they are absent, CI falls back to its temporary debug key, and a later APK may not be installable as an in-place update. Export a NextBell backup before removing an incompatible installation.

## Build the browser prototype

With Node.js installed, run:

```sh
npm ci
npm run build
```

## Install on a connected Android device

After enabling USB debugging and accepting the device's computer prompt, check the serial with `adb devices -l`. Install an update with:

```sh
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Android only accepts an in-place update when the app ID and signing certificate match. If installation reports a signature conflict, first export the profile and spending from **Settings → Backup & Restore**. Reinstalling by removing the old app can erase its local data.
