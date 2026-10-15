# Device Information App

An Android app that displays device and Android system details, lists installed applications, and shows each app's package name, version, system-app status, and requested permissions.

## Features
- Device model, manufacturer, Android version, SDK level, security patch, build fingerprint and ABI details.
- Installed-app list with search.
- App details and requested permissions.
- Refresh controls and clear package-visibility messaging.
- Native Android UI written in Kotlin, with no external runtime dependencies.

## Requirements
- Recent stable Android Studio
- JDK 17
- Android SDK platform 35

## Build
1. Open this repository in Android Studio.
2. Allow Gradle sync to finish.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated APK on your device.

A GitHub Actions workflow also builds a debug APK on pushes, pull requests, or manual runs. Download it from the workflow run's Artifacts section.

## Privacy
The app reads local device information and installed-app metadata. It does not send data to a server and does not request internet access. The manifest declares `QUERY_ALL_PACKAGES` to list installed packages; distribution through Google Play is subject to Google's package-visibility policy.

Android restricts access to some identifiers and app details. This app does not attempt to retrieve protected identifiers or bypass Android security.
