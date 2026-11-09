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

NextBell uses one Firebase project for Firebase Authentication (Email/Password) and Cloud Firestore. Both clients write per-record documents under `users/{uid}/records`. The website and Android app must use the **same Firebase project** and the **same account email/password**.

### 1. Configure the Firebase project

1. Open the Firebase Console and select the project you already created for NextBell.
2. Under **Authentication → Sign-in method**, enable **Email/Password**.
3. Create **Cloud Firestore** in production mode.
4. In **Project settings → General → Your apps**, register a Web app and copy its config values.
5. Register an Android app with package name `com.nealaringale.nextbell`, download its `google-services.json`, and keep that file private to your build machine.
6. In **Firestore → Rules**, publish the repository's `firestore.rules`. These rules restrict reads and writes to the authenticated user's own `users/{uid}` path.

Firebase Web config values (apiKey, authDomain, projectId, appId, messagingSenderId and storageBucket) are public client identifiers. Never use a service-account JSON, service-account private key, or Admin SDK credential in either client.

### 2. Local website configuration

Copy `.env.example` to `.env.local` in the repository root and fill in the six `VITE_FIREBASE_*` values from the Web app config. Then run:

```powershell
npm install
npm run dev
```

The website uses Firebase Auth's local persistence and Firestore multi-tab offline persistence on a trusted personal browser. The browser's localStorage copy is retained as a fallback and as the first-sync migration source.

### 3. Local Android configuration

Place the downloaded Android client file at:

```
android/app/google-services.json
```

It is deliberately ignored by Git. Gradle applies the Google Services plugin only when this file exists. After placing it, rebuild the student app and open **Settings → Cloud Sync**.

### 4. Configure GitHub Actions for the live website and APK builds

In GitHub, open **NextBell → Settings → Secrets and variables → Actions → New repository secret**.

Add these six secrets from the Web app config:

- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_STORAGE_BUCKET`
- `VITE_FIREBASE_MESSAGING_SENDER_ID`
- `VITE_FIREBASE_APP_ID`

For Android APKs built by GitHub Actions, create a base64 copy of `google-services.json` on Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\\google-services.json")) | Set-Clipboard
```

Create one more repository secret named `NEXTBELL_GOOGLE_SERVICES_JSON_BASE64` and paste the clipboard value. Do not commit the JSON file or its base64 contents. When this secret is absent, CI still builds an APK, but Firebase cloud sign-in is not configured in that APK.

The website deployment workflow injects the six `VITE_FIREBASE_*` secrets at build time. The Android workflow decodes the Google Services file into the ignored app module path before Gradle runs.

### 5. Connect both clients

1. Deploy the website after adding the six web secrets.
2. Install the Android APK built after adding `NEXTBELL_GOOGLE_SERVICES_JSON_BASE64`.
3. Open **Settings → Cloud Sync** on either client and create one Email/Password account.
4. On the other client, sign in using that exact same email and password.

Do not create separate accounts for the website and app.

### 6. What syncs

- Expenses and transaction metadata (merchant, payment mode, amount, source and debit/credit direction)
- Attendance sessions and attendance target
- Planner tasks and completion status
- Profile and settings, including theme and daily limit
- Recurring-expense rules, retained in Firestore even though the website does not yet expose their editor

Each expense, attendance session, task and recurring rule is a separate Firestore record. Deletions use tombstones so an offline device does not resurrect removed rows. Initial sync merges local-only records with cloud records; cloud copies win when both sides already have the same record ID. The Android client also merges device-only finance settings (weekly allowance, monthly budgets, daily alerts and learned merchant categories) into the cloud settings document rather than discarding them when the website signs in first.

**Attendance keys are shared across platforms:** `date|lowercase subject|scheduled start time`. That lets the same class meeting update one attendance record rather than create a second row.

Offline edits are queued by Firestore and delivered after reconnecting. Different records merge independently. If two devices edit the exact same record while offline, Firestore uses last-write-wins for that record; the app does not attempt field-level merging of simultaneous edits.

### 7. First connection / migration safety

- Export the Android backup and website JSON backup before linking accounts.
- The first successful online snapshot is used to merge existing local records; demo fixture data that has never been saved by the user is not bulk-uploaded.
- When linking a different Firebase account on a device, NextBell clears the previous account's synced local data before loading the new account, to avoid mixing students' records.
- Never set Firestore rules to public read/write. Keep the owner-only rules in `firestore.rules`.
- Web Firestore persistence stores cached documents in the browser profile. Use it only on a trusted personal browser and sign out on shared computers.
