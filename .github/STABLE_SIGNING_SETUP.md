# NextBell stable Android signing

NextBell must use the same signing key for APK updates. The public repository must not contain the private keystore, so GitHub Actions reads it from repository Actions secrets.

## One-time setup

1. Download the generated signing keystore attached to this chat: `nextbell-upload.jks`.
2. In PowerShell, run:

```powershell
$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Downloads\nextbell-upload.jks"))
Set-Clipboard $base64
```

If the file is elsewhere, replace the path.

3. Open the repository's **Settings → Secrets and variables → Actions** and add these repository secrets:

- `NEXTBELL_KEYSTORE_BASE64` — paste the clipboard value.
- `NEXTBELL_KEYSTORE_PASSWORD` — `NextBell2026!`
- `NEXTBELL_KEY_ALIAS` — `nextbell`
- `NEXTBELL_KEY_PASSWORD` — `NextBell2026!`

4. Re-run the latest Android workflow.

The current installed debug APKs were signed with an older/different debug key. Because Android requires the same signing key for an in-place update, the first APK built with this stable key requires one uninstall/reinstall. Every later APK from this pipeline will have the same signing identity and an automatically increasing versionCode, so Android can install it as an update.

Do not commit the keystore or any secret value to the repository.
