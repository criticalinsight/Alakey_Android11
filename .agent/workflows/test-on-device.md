---
description: Install and test the app on a connected device
---

# Workflow: Test on Device

This workflow checks for connected devices, installs the latest debug APK, and launches the main activity.

// turbo
1. Check for connected devices:
```bash
/Users/brixelectronics/android-sdk/platform-tools/adb devices -l
```

2. Install the APK:
```bash
/Users/brixelectronics/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

3. Launch the app:
```bash
/Users/brixelectronics/android-sdk/platform-tools/adb shell am start -n com.example.alakey/.MainActivity
```
