# Fire AI for TV v0.3.0 — Uploaded source migration

## Migrated features

- Migrated the newer uploaded Fire AI source into the standalone `fire-ai-for-tv` repository.
- Preserved the requested Android package/application ID: `Com.fire_AI.in`.
- Added the Fire Keyboard with D-pad, touch, air-mouse, shift, punctuation, backspace, space, enter, and close controls.
- Added actual input-device capability detection and focus restoration.
- Added voice typing with editable transcription and optional auto-send.
- Added the Models screen and free-tier model filtering.
- Preserved streaming Gemini chat, context, stop, regenerate, copy, clear, Markdown, and history rename/delete.
- Kept Firebase Authentication with Credential Manager on phones and Google Play Services account-picker fallback on Android TV.
- Kept offline continuation when authentication is unavailable.
- Used the supplied Firebase project configuration and existing permanent release certificate.

## Verification

- JDK 17
- Gradle 9.3.1
- Android Gradle Plugin 9.1.1
- Android API 37 compile SDK
- `:app:assembleDebug :app:assembleRelease` passed
- Release APK package: `Com.fire_AI.in`
- Release APK SHA-1: `26:2E:AF:8A:92:CD:DA:36:93:47:0F:DA:6B:16:55:FE:0C:C7:FC:0F`
- Release APK SHA-256 certificate: `5D:0E:7F:30:BF:B8:23:99:49:41:61:9F:DA:8B:7D:E9:A6:9F:A6:22:81:BC:7B:93:12:4E:6E:97:8B:D5:AE:C0`
- Release APK file SHA-256: `9c16d5e073906ebdcbe6ab981b386245ca54523525faa1057c0683013e35344f`

The original uploaded ZIP and all web repositories remain untouched.
