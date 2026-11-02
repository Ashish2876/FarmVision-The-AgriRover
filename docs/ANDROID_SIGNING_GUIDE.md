# FarmVision — Android APK Release Signing Guide

This guide details the procedure for generating a production release keystore, configuring signing credentials, and verifying APK signatures for the FarmVision AgriRover platform.

---

## 1. Security Rules

1. **NEVER** commit `.jks`, `.keystore`, or password files to Git.
2. **NEVER** share release signing passwords in plain text.
3. Keep backups of your release keystore in secure, encrypted offline storage. Losing the release key prevents publishing updates to Google Play.

---

## 2. Generating a Production Release Keystore

Generate a 2048-bit RSA key valid for at least 25 years (10,000 days):

```bash
keytool -genkey -v -keystore farmvision-release.jks -alias farmvision_release -keyalg RSA -keysize 2048 -validity 10000
```
Follow the interactive prompts to define your keystore password, owner organization details, and key password.

---

## 3. Configuring Local Signing Credentials

The project build system supports two secure methods:

### Method A: Local Properties File (Recommended for Local Dev)
1. Copy `local.signing.properties.example` to `local.signing.properties` in the repository root.
2. Set your credentials:
   ```properties
   signing.keystore.path=C:/secure/keys/farmvision-release.jks
   signing.keystore.password=YourKeystorePassword
   signing.key.alias=farmvision_release
   signing.key.password=YourKeyPassword
   ```
*(Note: `local.signing.properties` is strictly ignored by `.gitignore`).*

### Method B: Environment Variables (Recommended for CI/CD)
Define the following environment variables:
- `FARMVISION_RELEASE_KEYSTORE_PATH`
- `FARMVISION_RELEASE_KEYSTORE_PASSWORD`
- `FARMVISION_RELEASE_KEY_ALIAS`
- `FARMVISION_RELEASE_KEY_PASSWORD`

---

## 4. Building the Signed Release APK

Once configured, run:

```bash
gradlew.bat --offline assembleRelease
```
The output `app/build/outputs/apk/release/app-release.apk` will be automatically signed with your production credentials.

---

## 5. Verifying the Signature

Use Android SDK's `apksigner` tool to verify the signature:

```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Expected output:
- `Verifies: true`
- `Verified using v2 scheme: true`
- `Verified using v3 scheme: true`
- `Signer #1 certificate DN: ...`

