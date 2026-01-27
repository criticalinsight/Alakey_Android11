# Alakey Development Workflows

This document outlines the standard workflows for maintaining and developing the Alakey Podcast App.

## 🏗️ Building
- **Command**: `/build-apk`
- **Purpose**: Compiles the project and generates `app-debug.apk` in `app/build/outputs/apk/debug/`.
- **Requirements**: Java 17, Gradle.

## 🚀 Releasing
- **Command**: `/release`
- **Purpose**: Automates Git commit, push, and GitHub Release creation.
- **Notes**: Ensure you've built the APK first.

## 📱 Verification
- **Command**: `/test-on-device`
- **Purpose**: Installs the APK to a connected Android device and launches it.
- **Requirements**: ADB in `~/android-sdk/platform-tools/`.

## 🧪 REPL Interaction
Use ADB broadcasts to interact with the app in real-time:
```bash
adb shell am broadcast -a com.example.alakey.REPL --es cmd "play-id <podcast_id>"
```
Available commands: `play-id`, `toggle`, `next`, `prev`, `stop`, `sql <query>`, `assert-fact <json>`.
