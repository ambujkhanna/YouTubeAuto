# YouTubeAuto v0.1.1

Minimal Android WebView proof of concept for testing a YouTube experience on compatible Android Auto setups.

## Build

This project uses Android Gradle Plugin 9.3 and AGP 9 built-in Kotlin support. Do not add `org.jetbrains.kotlin.android`; AGP provides Kotlin support for this project.

The included GitHub Actions workflow installs Gradle 9.5 and builds `app-debug.apk`.

## Security posture

The app requests only `INTERNET`. It has no analytics SDK and no location, contacts, microphone, camera, SMS, notification, or storage permissions.
