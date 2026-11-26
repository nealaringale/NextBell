# NextBell

NextBell is a local-first Android student planner for NMIET First Year Engineering. On the development branch, a student's division and roll number select the matching timetable and practical/tutorial groups. It includes attendance records and target-based bunk estimates, assignment/exam/task reminders, a next-class home-screen widget, daily and weekly money controls, recurring-expense suggestions, and optional on-device bank SMS parsing.

## NMIET First Year timetable source imports

The official FE Semester I source-sheet metadata received so far is catalogued in
[`docs/timetables/fe-2026-27-sem1-divisions-g-m.json`](docs/timetables/fe-2026-27-sem1-divisions-g-m.json).
This first import covers Divisions G, H, I, J, K, L and M (W.E.F. 16 September 2026,
Revision 00), including each sheet's class teacher, subject/faculty abbreviations,
and practical-batch roll ranges. Division B remains the existing app timetable.

The accompanying [human-readable sheet transcription](docs/timetables/fe-2026-27-sem1-divisions-g-m-transcription.md)
preserves the printed weekly cell labels for review.

The catalog is a source-data staging file, not yet the live timetable engine.
Before a new division is enabled for home schedules, attendance, widgets or alarms,
its weekly cells must be normalized into time-bounded events and checked against
the source sheet for batch overlap, tutorial/library roll splits, room and faculty.
Do not infer missing timetable details or enable reminders from an unverified import.

## NMIET First Year timetable source imports — second batch

A second set of official FE Semester I source sheets has been catalogued at
[`docs/timetables/fe-2026-27-sem1-divisions-a-b-n-o-p.json`](docs/timetables/fe-2026-27-sem1-divisions-a-b-n-o-p.json)
with a [human-readable transcription](docs/timetables/fe-2026-27-sem1-divisions-a-b-n-o-p-transcription.md).
This batch covers Divisions A, B, N, O and P (W.E.F. 16 September 2026, Revision 00).
Division B already exists in the Android implementation; its newly supplied official
sheet is for source comparison, not an automatic schedule replacement.

Together with the first batch, official-sheet metadata has now been staged for
Divisions A, B, G, H, I, J, K, L, M, N, O and P. These files remain source catalogues
and transcriptions, not the live timetable engine. New divisions must not be enabled
for student reminders, attendance, widgets or live schedules until every merged cell,
time slot, room, faculty abbreviation and batch audience is normalized and reviewed.

## NMIET First Year timetable source imports — third batch

The third batch of official FE Semester I sheets is catalogued in
[`docs/timetables/fe-2026-27-sem1-divisions-c-d-e.json`](docs/timetables/fe-2026-27-sem1-divisions-c-d-e.json)
with a [human-readable transcription](docs/timetables/fe-2026-27-sem1-divisions-c-d-e-transcription.md).
This batch covers Divisions C, D and E (W.E.F. 16 September 2026, Revision 00).

Official timetable source sheets have now been staged for Divisions A, B, C, D, E,
G, H, I, J, K, L, M, N, O and P. Division F is the only A–P sheet still outstanding.
Division B's newly supplied official sheet is a source-validation copy and must be
compared against its existing Android timetable before any replacement.

These files remain source catalogues/transcriptions, not the live timetable engine.
Every merged cell must be normalized to its exact time interval and audience before
new divisions are enabled for schedules, attendance, widgets or notifications.

## NMIET First Year timetable source imports — final A–P division

The final missing official FE Semester I sheet, Division F, has been catalogued in
[`docs/timetables/fe-2026-27-sem1-division-f.json`](docs/timetables/fe-2026-27-sem1-division-f.json)
with its [human-readable transcription](docs/timetables/fe-2026-27-sem1-division-f-transcription.md).
This sheet is W.E.F. 16 September 2026, Revision 00, and records F-1 (rolls 1–25),
F-2 (26–50), and F-3 (51 onwards).

Official source sheets are now staged for all 16 First Year divisions A–P. This is
completion of source collection, not activation in the Android timetable engine.
Division B remains the existing bundled Android schedule pending source reconciliation;
the other divisions still require exact slot-boundary normalization, review and tests
before their schedules can drive Home, Week, Attendance, notifications or widgets.

## Product and data model

- **Android app:** Java, Android SDK 36 (Android 16), minimum Android 8.0 (API 26), target SDK 36. Screens are native views hosted in `MainActivity`; timetable, attendance, planner, finance, notification, SMS parsing, and backup logic live in Java classes.
- **Timetable:** Division B retains its original bundled timetable and identifiers. The development branch adds official Semester I schedules for Divisions A and C–P in `FirstYearTimetableData`, with roll-range and batch-aware filtering. New users select a division during setup; existing installs without a saved division migrate to B. All timetable data remains available offline.
- **Study planning:** attendance marks, assignment/exam/task items, and due dates are stored locally. Bunk estimates use only sessions the student has recorded. Deadline reminders are approximate Android alarms and require notifications to be enabled.
- **Profile, study data, and money:** stored locally in the app's private `SharedPreferences` by default. Optional Cloud Sync uses Firebase Authentication and Cloud Firestore when the user configures it and signs in. Android system cloud backup and device-to-device transfer are excluded. The user can export a readable JSON backup to a file they choose. Anyone who receives that file can read it.
- **Money entry:** direct SMS ingestion is the primary bank-alert path after the user explicitly enables RECEIVE_SMS. Notification access remains an additional fallback for UPI/wallet alerts. The private debug APK must be installed via ADB so Android's shell installer can allowlist hard-restricted SMS permissions; tapping an APK in a file manager may not allow RECEIVE_SMS/READ_SMS. READ_SMS is requested separately only when the user chooses a 30-day inbox import. Raw message text is not saved or uploaded; only parsed transaction fields and duplicate fingerprints are retained. Parsed expense records can sync to the user's own Firebase account only when Cloud Sync is separately enabled. Google Play lists SMS-based money management (including budgeting apps) as an eligible SMS-permission use case, subject to the Play Console declaration and review/approval. Keep the prominent disclosure, consent, privacy policy and Data safety answers aligned with the actual on-device parsing and optional cloud-sync behaviour.
- **Private debug install (Windows):** enable USB debugging, connect the phone, extract the student APK, then run `adb install -r NextBell-student-debug.apk` from the folder containing the APK. ADB's shell package installer allowlists restricted permissions by default unless told to restrict them; this does not itself grant runtime permissions. Open NextBell → Money → Manage → Enable SMS tracking and grant the Android prompt.
- **realme/ColorOS background settings:** allow NextBell background activity/auto-launch and remove battery restrictions when available. OEM power controls can stop background listener services even while Notification access still appears granted.
- **Distribution note:** notification access is highly sensitive and must be transparently disclosed and used only for the user's requested transaction tracking. The app retains its opt-in SMS ingestion and inbox-import features; Google Play lists SMS-based money management as a possible exception, but it requires the feature to be core, there must be no alternative, and approval is not automatic; see the release checklist in [.github/PLAY_STORE_RELEASE.md](.github/PLAY_STORE_RELEASE.md). See the release checklist in [.github/PLAY_STORE_RELEASE.md](.github/PLAY_STORE_RELEASE.md).
- **Play Store resources:** [privacy policy](https://nealaringale.github.io/NextBell/privacy-policy.html) and [account/data deletion request](https://nealaringale.github.io/NextBell/delete-account.html) are published from `public/` by the GitHub Pages deployment. Verify both live URLs after merging to `main` before entering them in Play Console. See [Google Play's SMS permission policy](https://support.google.com/googleplay/android-developer/answer/10208820) and [Android's hard-restricted permission documentation](https://source.android.com/docs/core/permissions/runtime_perms).
- **Admin companion:** a separate local draft tool. It does not publish timetable changes to student devices.
- **Browser prototype:** `src/` is an older Vite demo with fixture data; it is not the Android app or the download website and should not be treated as the live timetable source.

## Multi-division Android implementation (dev)

The Android setup screen asks new users to select a First Year division (A–P) and
enter their roll number. The profile stores both values. The selected division is
used by Home, Week, attendance course selection, class reminders, live status and
the home-screen widget. Practical batches are derived from each division's printed
roll ranges; divisions J, K, L, M and P reject rolls above 74, as their source sheets
specify. Other supplied sheets state 51 onwards.

Existing installations without a division preference are migrated to Division B,
and Division B timetable queries continue to delegate to the original `TimetableData` implementation. Division changes reschedule alarms and refresh the widget/live status.
The Android backup includes all preferences, and the web backup now preserves the
division field.

**Verification:** `FirstYearTimetableDataTest` checks batch boundaries, weekday coverage,
invalid capped roll numbers, representative official cells and merged-session durations,
duplicate IDs, immutable results, non-overlapping/sorted schedules, and Division B
regression equivalence. GitHub Actions runs on pushes to `dev` as well as release branches.
New divisions are not being merged into `main` until review and verification are complete.

## Build and verify Android

Install JDK 17, Android SDK Platforms 35 and 36, Android Build Tools 36.0.0, and Gradle 8.13. The student app targets API 36; the private Admin companion still compiles against API 35. From `android/`, run:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug assembleDebug
```

The student APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs Android unit tests, lint, and both debug APK builds on pushes to `main` / `feature/announcements` and on pull requests to `main`. It uploads separate student and Admin debug APK artifacts. Run the same workflow manually (or push a `nextbell-v*` tag) to build a signed student Play Store AAB. See [.github/PLAY_STORE_RELEASE.md](.github/PLAY_STORE_RELEASE.md). The website and privacy-policy page are deployed separately by the Pages workflow when changes reach `main`.

### Stable signing for Android updates

The student app's Play Store AAB must be signed with a persistent upload keystore. Configure these four repository Actions secrets: `NEXTBELL_KEYSTORE_BASE64`, `NEXTBELL_KEYSTORE_PASSWORD`, `NEXTBELL_KEY_ALIAS`, and `NEXTBELL_KEY_PASSWORD`. The release job fails rather than producing an unsigned Play bundle when they are missing. Debug CI may still use its temporary key. See [.github/STABLE_SIGNING_SETUP.md](.github/STABLE_SIGNING_SETUP.md).

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
