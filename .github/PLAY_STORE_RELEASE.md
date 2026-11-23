# NextBell Google Play release checklist

This guide publishes the **student Android app**. The separate Admin companion is intentionally not part of the Play Store bundle.

## What this repository now builds

- Application ID: `com.nealaringale.nextbell` (keep it unchanged after publication).
- Student app: Java, min SDK 26, compile/target SDK 36 (Android 16).
- Android Gradle Plugin: 8.13.2; Gradle: 8.13; JDK: 17.
- Play upload artifact: signed Android App Bundle at `android/app/build/outputs/bundle/release/app-release.aab`.
- Existing student features and permissions are retained. This release preparation does not remove timetable, attendance, planner, finance, SMS parsing, notification access, widget, backup, or Firebase code.

## 1. Configure signing and optional Firebase

Follow [STABLE_SIGNING_SETUP.md](STABLE_SIGNING_SETUP.md) and add the four upload-keystore secrets:

- `NEXTBELL_KEYSTORE_BASE64`
- `NEXTBELL_KEYSTORE_PASSWORD`
- `NEXTBELL_KEY_ALIAS`
- `NEXTBELL_KEY_PASSWORD`

Add `NEXTBELL_GOOGLE_SERVICES_JSON_BASE64` as a required release secret, containing the Base64-encoded Firebase Android `google-services.json` for package `com.nealaringale.nextbell`. The release workflow validates the package ID and fails rather than generating a feature-incomplete Play bundle when this secret is missing.

## 2. Build the bundle from GitHub

1. Open the repository's **Actions** tab.
2. Select **Build NextBell Android Apps**.
3. Choose **Run workflow** after this release-readiness branch has been merged into `main` (workflow_dispatch must exist on the default branch).
4. Enter a version name (for example, `0.6.1`). Leave version code blank to use the workflow-generated value, or provide a positive integer greater than the most recent Play Store version code.
5. Start the workflow and inspect the `release-aab` job.
6. Download the `NextBell-play-release-aab` artifact.

The release job validates the keystore and fails if the signing secrets are incomplete or the keystore/alias cannot be opened. It runs unit tests, release lint, and `:app:bundleRelease`. A signed AAB is uploaded only if these steps succeed.

A tagged release also works: push `nextbell-v0.6.1`. The tag suffix is used as the version name. Every uploaded version code must be higher than the last version code in Play Console.

## 3. Google Play Console declarations

Before production rollout, complete all relevant Play Console forms and listing information:

- App access instructions, content rating, target audience, ads declaration, and store listing.
- Data safety form that accurately describes local processing and the optional Firebase email-auth / Firestore sync.
- Public privacy policy URL: `https://nealaringale.github.io/NextBell/privacy-policy.html` after the Pages deployment completes. The repository includes `public/privacy-policy.html`; verify the live page before submitting.
- Public account-deletion URL: `https://nealaringale.github.io/NextBell/delete-account.html`. Enter it in the Play Console account-deletion URL field and verify the live page. It accepts an email request from the Firebase account address and describes the cloud data that will be deleted.
- The app now links to its privacy policy and account-deletion request from Settings → About & Feedback. The signed-in Cloud Sync dialog also includes the account/cloud-data deletion link. The SMS and notification-access screens show disclosures before sending the user to the relevant Android permissions/settings.
- Notification-listener access is highly sensitive and is limited to the optional transaction-alert feature the user enables. Parsed expenses sync only when the user separately enables Cloud Sync.

### SMS permission declaration — do not skip

The student app intentionally keeps `READ_SMS` and `RECEIVE_SMS` so the existing opt-in bank-alert parsing and optional inbox-import features are not removed.

Google Play's general rule is that SMS permissions are for apps that are actively registered as the default SMS or Assistant handler. Its separate exceptions table lists **SMS-based money management** (for example, apps that track and manage a budget) as a possible exception for `READ_SMS`, `RECEIVE_SMS`, and related SMS permissions. That exception is not automatic: the use must be a critical core app function, there must be no alternative method, and Google Play reviews and approves each declaration. NextBell is primarily a student planner, so Play may decide that SMS-based money management is not core enough; do not assume the exception will be granted. In Play Console, submit the SMS permissions declaration and select the closest matching SMS-based money management use case only if the app genuinely meets those conditions. Explain that the app:

- Parses transaction alerts on-device only after the student explicitly enables tracking.
- Offers a separate user-initiated 30-day inbox import.
- Does not save or upload raw SMS message text.
- Saves parsed financial transaction fields; those transaction records can be synced to the user's own Firebase account only when the user configures and enables Cloud Sync.
- Does not use SMS data for advertising or unrelated profiling.

Ensure the Play listing makes the money-management functionality prominent enough to justify the permission request. Complete the privacy policy, Data safety and prominent-disclosure/consent requirements accurately. Google Play makes the final eligibility decision. See the official [SMS and Call Log permission policy](https://support.google.com/googleplay/android-developer/answer/10208820).

If Play rejects the SMS declaration, do not silently submit a build with these permissions while claiming the feature is approved. Review the rejection and choose a compliant distribution design with the developer; the current repository branch preserves the SMS feature and permission declarations.

## 4. Test before production

- Upload the AAB to **Internal testing** first.
- Install from the Play testing link rather than using only an ADB debug install.
- Test on Android 16 and at least one older supported Android version.
- Verify notifications, next-class widget, alarms/reminders, attendance, finance screens, export/import backup, notification-access parsing, SMS opt-in and inbox import (on eligible test devices), and Firebase sync if configured.
- Check portrait/tablet and system-bar/keyboard layouts after the API 36 target change.
- Complete the applicable closed-testing requirement for your developer account before requesting production access.

## 5. Store assets and release details still supplied in Play Console

This repository change does not invent store listing assets or developer identity details. Prepare the final app icon, phone screenshots, feature graphic, short/full descriptions, support contact, and any required account-access instructions in Play Console. Keep the upload key backed up; never commit it to GitHub.
