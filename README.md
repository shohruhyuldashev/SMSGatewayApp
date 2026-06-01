# CyberBro SMS Gateway

This repository contains a scaffold Android app implementing a local SMS gateway using Kotlin, Ktor, Room, WorkManager and SmsManager.

Quick start (in VS Code):

1. Install Android SDK and Android Platform Tools. Ensure the SDK path is available (example: `/home/youruser/Android/Sdk`).
2. Open this folder in VS Code.
3. Make sure `local.properties` contains `sdk.dir=/path/to/Android/Sdk` (this repo contains a placeholder).
4. Install recommended VS Code extensions (see `.vscode/extensions.json`).
5. Build the debug APK:

```bash
./gradlew :app:assembleDebug
```

Or use the VS Code task: `Run Build` (Tasks -> Run Task -> `Gradle: assembleDebug`).

Running on device/emulator:

```bash
./gradlew :app:installDebug
```

Notes:
- The app runs an embedded Ktor server on port 8080 (inside the device). Use `adb forward tcp:8080 tcp:8080` to access it from your host.
- Default API key is `default-demo-key` stored in `ApiKeyStore` (change in app settings).
- Permissions declared in `AndroidManifest.xml` are required: `SEND_SMS`, `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `READ_PHONE_STATE`.

Files added/important:
- `app/` Android application module
- `app/src/main/java/...` Kotlin sources
- `.vscode/tasks.json` and `.vscode/launch.json` for VS Code integration
- `local.properties` (please update to your SDK path)

If you want, I can try building an APK here — provide the Android SDK path or allow me to install SDK components (large download).