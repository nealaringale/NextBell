# Stable NextBell Android signing

The CI workflows can sign the student APK with a stable keystore so one CI build can update an earlier one without removing the app.

In the repository's **Settings → Secrets and variables → Actions**, configure all four secrets:

- `NEXTBELL_KEYSTORE_BASE64`: base64-encoded contents of the existing keystore.
- `NEXTBELL_KEYSTORE_PASSWORD`: keystore password.
- `NEXTBELL_KEY_ALIAS`: key alias.
- `NEXTBELL_KEY_PASSWORD`: key password.

The workflow decodes the keystore into an ignored file inside the build checkout and removes it when the runner ends. The key and passwords are never written to the repository. Do not paste them into source files, issues, or logs.

If the keystore secret is absent, CI builds with GitHub's temporary debug key. Those APKs can be installed for testing, but later builds may not update them because Android checks the signing certificate. Preserve app data before uninstalling a differently signed build.

This repository does not create, rotate, or replace signing credentials. Keep a protected backup of the existing keystore before changing its storage.
