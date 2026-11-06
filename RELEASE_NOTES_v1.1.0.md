# Fire AI for TV v1.1.0

## Updated

- Changed the Android package/application ID to `Com.fire_AI.in`.
- Added the supplied Firebase project configuration for `fireai-2b2e4`.
- Added Firebase Authentication with Credential Manager-based Google Sign-In.
- Added a clean Fire AI account gate with Google sign-in, clear error states, loading state, and offline continuation.
- Preserved the existing TV-first chat, history, settings, Markdown, D-pad, touch, and air-mouse experience.
- Added immediate UI state transitions when sign-in or offline mode is selected.

## Build verification

- JDK 17
- Gradle 9.3.1
- Android Gradle Plugin 9.1.1
- Android API 37 compile SDK
- `:app:assembleDebug` passed
- APK SHA-256: `b0126958b4097ccab7825241ff0c1b48508aab2965b7bfdb6d1db53c7e62e101`

## Security

The Firebase client configuration is included for Android initialization. No Gemini API key or Firebase Admin credential is included. Gemini keys remain runtime-configured and protected by Android Keystore.
