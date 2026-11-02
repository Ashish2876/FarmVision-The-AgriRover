# FarmVision — Release Build Guide

**Project:** FarmVision – The AgriRover  
**Target:** Android Release Packaging (APK)  
**Tools Required:** JDK 17, Android SDK Build-Tools 35.0.0+, Gradle 8.7.2  

---

## 1. Prerequisites

1. **Java Development Kit:** JDK 17 installed and configured:
   ```bash
   set JAVA_HOME=C:\path\to\jdk-17
   ```
2. **Android SDK:** Installed with platform-tools and build-tools 35.0.0.
3. **Repository Cleanliness:** Ensure `git status` has no unintended modifications.

---

## 2. Building the Release APK

To execute an offline, optimized release build with R8 minification and resource shrinking:

```bash
gradlew.bat --offline assembleRelease
```

### Build Outputs
- **Release APK:** `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if keystore configured)
- **R8 Mapping File:** `app/build/outputs/mapping/release/mapping.txt`
- **Seeds & Configuration:** `app/build/outputs/mapping/release/configuration.txt`

---

## 3. Building the Debug APK

To compile the debug development APK:

```bash
gradlew.bat --offline assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Running Unit Tests Before Release

Always verify tests before packaging:

```bash
gradlew.bat --offline testDebugUnitTest
```
All 340 tests must pass with 0 failures and 0 skipped.

