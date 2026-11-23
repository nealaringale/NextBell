# NextBell Android upload signing

Google Play requires every uploaded Android App Bundle to be signed. NextBell's release workflow uses a persistent **upload key**. Google Play App Signing then manages the app-signing key for distribution.

## Important before creating a key

- If NextBell has already been uploaded to Play Console, reuse the same upload keystore currently registered there. Do not generate a replacement casually; an upload-key reset must be requested through Play Console.
- If this is the first Play release, create one upload keystore and keep two secure offline backups.
- Never commit the `.jks` file, passwords, or Base64 contents to GitHub. The repository already ignores `android/app/nextbell-upload.jks`.

## Create a new upload key (first Play release only)

Run this in a private local terminal with JDK 17 installed:

```powershell
keytool -genkeypair -v -keystore nextbell-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nextbell-upload
```

Choose a strong keystore password and key password and store them securely. Keep the alias exactly as `nextbell-upload`.

Export the public upload certificate for Play Console if requested:

```powershell
keytool -export -rfc -keystore nextbell-upload.jks -alias nextbell-upload -file nextbell-upload-certificate.pem
```

The private `.jks` file is the signing key. The `.pem` file is only the public certificate.

## Add GitHub Actions secrets

Open **GitHub → nealaringale/NextBell → Settings → Secrets and variables → Actions** and create:

| Secret | Value |
|---|---|
| `NEXTBELL_KEYSTORE_BASE64` | Base64 contents of the `.jks` file |
| `NEXTBELL_KEYSTORE_PASSWORD` | Keystore password |
| `NEXTBELL_KEY_ALIAS` | `nextbell-upload` (or your existing key alias) |
| `NEXTBELL_KEY_PASSWORD` | Key password |

On Windows PowerShell, copy the Base64 value without adding line breaks:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\nextbell-upload.jks")) | Set-Clipboard
```

Paste the clipboard contents into the GitHub secret named `NEXTBELL_KEYSTORE_BASE64`. On macOS/Linux, an equivalent command is:

```sh
base64 -w 0 nextbell-upload.jks
```

Do not place any password or key material in source files, workflow YAML, issues, logs, or chat messages.

## Optional Firebase configuration

To include NextBell's existing Firebase email sign-in and Firestore sync in the release AAB, also configure:

- `NEXTBELL_GOOGLE_SERVICES_JSON_BASE64`: Base64 contents of the Android Firebase `google-services.json` for package `com.nealaringale.nextbell`.

The workflow validates and decodes this file into the ignored `android/app/google-services.json` path. If this secret is omitted, the app still builds, but that AAB does not have Firebase Android client configuration and cloud sign-in/sync will not work.

## Build a Play Store AAB

After the four signing secrets are present:

1. Open **Actions → Build NextBell Android Apps → Run workflow**.
2. Enter the release version name (for example, `0.6.1`).
3. Leave version code blank to use the workflow's increasing automatic value, or enter a positive value greater than the last version uploaded to Play Console.
4. Run the workflow and wait for the `release-aab` job to finish successfully.
5. Download the `NextBell-play-release-aab` artifact. The bundle is at `android/app/build/outputs/bundle/release/app-release.aab` inside the build.

Alternatively, push a tag such as `nextbell-v0.6.1`. The tag suffix becomes the version name; the workflow generates the version code.

The Gradle build intentionally fails for `:app:bundleRelease` or `:app:assembleRelease` when the persistent signing credentials are missing. This prevents accidentally treating an unsigned bundle as a Play upload.

## Key preservation

Keep the upload keystore and passwords in a password manager and at least one secure offline backup. Loss of the upload key can delay updates. Google Play App Signing does not mean the upload key can be discarded.
