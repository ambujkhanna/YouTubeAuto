# YouTube Auto

A minimal, security-focused Android WebView app intended to test YouTube playback on Android Auto head units while parked.

## Security design

- Only `INTERNET` permission.
- No analytics or telemetry.
- No microphone, location, contacts, SMS, notification, camera, or storage permissions.
- File/content access disabled in WebView.
- HTTPS-only navigation.
- Navigation is restricted to YouTube/Google media domains used by YouTube.
- No code intended to bypass Android Auto's driving restrictions.

## Important

This is an experimental sideloaded app. Android Auto may restrict or block it depending on the Android Auto/Google Play Services version and head unit. The project follows the same general manifest approach used by the open-source AABrowser project, but it is intentionally much smaller.

Use only while safely parked or as appropriate for your local laws and Android Auto safety restrictions.

## Build in GitHub Codespaces / GitHub Actions

1. Create a new GitHub repository.
2. Upload this project.
3. GitHub Actions will build the debug APK.
4. Download the `YouTubeAuto-debug-apk` artifact.
5. Install it on your Android phone.
6. In Android Auto settings, enable Developer mode by tapping Version 10 times, then enable **Unknown sources**.
7. Connect the phone to the car and check whether **YouTube Auto** appears.

## Local build

Requires JDK 21 and Android SDK 37.

```bash
gradle assembleDebug
```

The APK is under `app/build/outputs/apk/debug/`.
