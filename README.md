# NextBell

NextBell is a local-first Android student planner for FE Division B. A student's roll number selects batch and tutorial entries from the published timetable. It now includes attendance records and target-based bunk estimates, assignment/exam/task reminders, a next-class home-screen widget, daily and weekly money controls, recurring-expense suggestions, and optional on-device bank SMS parsing.

## Product and data model

- **Android app:** Java, Android SDK 35, minimum Android 8.0 (API 26), target SDK 35. Screens are native views hosted in `MainActivity`; timetable, attendance, planner, finance, notification, SMS parsing, and backup logic live in Java classes.
- **Timetable:** bundled official FE Division B timetable, W.E.F. 16 September 2026, Revision 00. Schedule data is available offline; batch filtering is based on the student's roll number.
- **Study planning:** attendance marks, assignment/exam/task items, and due dates are stored locally. Bunk estimates use only sessions the student has recorded. Deadline reminders are approximate Android alarms and require notifications to be enabled.
- **Profile, study data, and money:** stored in the app's private `SharedPreferences`; there is no server, account system, or network permission. System cloud backup and device-to-device transfer are excluded. The user can export a readable JSON backup to a file they choose. Anyone who receives that file can read it.
- **Money entry:** direct SMS ingestion is the primary bank-alert path after the user explicitly enables RECEIVE_SMS. Notification access remains an additional fallback for UPI/wallet alerts. The private debug APK must be installed via ADB so Android's shell installer can allowlist hard-restricted SMS permissions; tapping an APK in a file manager may not allow RECEIVE_SMS/READ_SMS. READ_SMS is requested separately only when the user chooses a 30-day inbox import. Raw message text is not saved or uploaded; only parsed transaction fields and duplicate fingerprints are retained. Google Play distribution requires a separate SMS-permission policy assessment.
- **Private debug install (Windows):** enable USB debugging, connect the phone, extract the student APK, then run `adb install -r NextBell-student-debug.apk` from the folder containing the APK. ADB's shell package installer allowlists restricted permissions by default unless told to restrict them; this does not itself grant runtime permissions. Open NextBell → Money → Manage → Enable SMS tracking and grant the Android prompt.
- **realme/ColorOS background settings:** allow NextBell background activity/auto-launch and remove battery restrictions when available. OEM power controls can stop background listener services even while Notification access still appears granted.
- **Distribution note:** notification access is highly sensitive and must be transparently disclosed and used only for the user's requested on-device transaction tracking. Google Play separately restricts SMS and Call Log permissions; this private-build approach is not a guarantee of Play policy approval. See [Google Play's SMS permission policy](https://support.google.com/googleplay/android-developer/answer/10208820) and [Android's hard-restricted permission documentation](https://source.android.com/docs/core/permissions/runtime_perms).
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
npm install
npm run build
```

## Install on a connected Android device

After enabling USB debugging and accepting the device's computer prompt, check the serial with `adb devices -l`. Install an update with:

```sh
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Android only accepts an in-place update when the app ID and signing certificate match. If installation reports a signature conflict, first export the profile and spending from **Settings → Backup & Restore**. Reinstalling by removing the old app can erase its local data.


## Firebase cross-device sync (website + Android)

NextBell uses Firebase Authentication (Email/Password) and Cloud Firestore. The website and Android app must use the **same Firebase project** and the **same account email/password**.

### 1. Configure Firebase

1. In Firebase Console, open the project you created for NextBell.
2. In **Authentication → Sign-in method**, enable **Email/Password**.
3. Create a **Cloud Firestore** database.
4. In Project settings → General, add a **Web app** and copy its Firebase config into `src/firebaseConfig.js` (apiKey, authDomain, projectId, appId, messagingSenderId).
5. In Project settings → General, add an **Android app** with package name `com.nealaringale.nextbell`.
6. Download that Android app's `google-services.json` and place it at `android/app/google-services.json`. It is ignored by Git, so it stays local to your build machine.
7. In Firestore → Rules, publish the contents of `firestore.rules`.

Firebase client config values are public identifiers, not service-account credentials. Never put a service-account JSON/private key in this repository, website, or APK. Firestore rules must remain owner-only.

### 2. Build and install

- Website: run `npm run build` and deploy the Vite output as usual. The Firebase Web SDK is loaded from Google's CDN; the values in `src/firebaseConfig.js` must belong to the same project as Android.
- Android: sync Gradle and build the student app after placing `google-services.json` in `android/app/`. The Google Services plugin is only applied when that file exists, so CI can still run without your private project config.
- Open **Settings → Cloud Sync** on the website and **Settings → Cloud Sync** in Android. Create an account on one side, then sign in with the same email/password on the other.

### 3. What syncs

- Expenses (including merchant, payment mode, amount, source and direction fields supported by Android)
- Attendance sessions and attendance target
- Planner tasks
- Profile and theme/daily-limit settings
- Android recurring expense rules are retained in the shared Firestore records, even though the current website does not yet have a recurring-rules editor.

Each row is a separate Firestore record with an update time and a deletion tombstone. This avoids replacing the entire user's data document when one row changes. Firestore offline persistence queues writes and replays them after reconnecting. Initial migration merges local-only rows with the cloud; an existing cloud version wins when both sides already have the same record ID.

### 4. First connection / migration notes

- Back up the Android app and export the website JSON before linking accounts.
- If the cloud is empty, each client uploads its existing local-only records at first sign-in.
- If both devices have an item with the same ID, the cloud copy wins during first migration. Subsequent edits use record-level update timestamps.
- Do not enable public read/write Firestore rules. The rules in `firestore.rules` require `request.auth.uid` to match the user path.

