---
description: Build the Alakey debug APK
---

# Workflow: Build APK

This workflow builds the debug APK for the Alakey Podcast App using the established Java 17 and Gradle environment.

// turbo
1. Set Java 17 and build the APK:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 && export PATH=$JAVA_HOME/bin:$PATH && ./gradlew assembleDebug
```

2. Verify the APK exists:
```bash
ls -l app/build/outputs/apk/debug/app-debug.apk
```
