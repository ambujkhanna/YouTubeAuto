# YouTubeAuto v0.1

Minimal security-focused YouTube WebView proof of concept for testing with Android Auto.

## Scope

- Kotlin + Android WebView
- HTTPS-only
- YouTube/Google media host allowlist
- `INTERNET` permission only
- No analytics or tracking SDK
- No location, microphone, contacts, storage, or notification permissions
- Fullscreen WebView video support
- GitHub Actions debug APK build

## Android Auto note

This POC uses the same broad sideloaded Android Auto discovery pattern seen in AABrowser (`CAR_LAUNCHER` plus automotive metadata). Android Auto controls whether and when the app is visible or usable. The project does not attempt to bypass driving restrictions.

## Build

GitHub Actions installs Gradle 9.5 and JDK 17, then runs `assembleDebug`. The resulting APK is uploaded as a workflow artifact.
