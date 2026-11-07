# Fire AI for TV

Native TV-first Gemini chat app for Android TV and phones. The same landscape interface adapts to touch, D-pad, mouse, gamepad, and physical keyboard input.

Package/application ID: `Com.fire_AI.in`

## Included in v0.3 migration

- Uploaded Fire AI v0.2 source migrated to the requested package.
- Fire Keyboard with D-pad, air-mouse, touch, shift, punctuation, backspace, space, enter, and close controls.
- Actual input-device capability detection and focus restoration.
- Voice typing with editable transcription and optional auto-send.
- Free-tier model filtering and Models screen.
- Streaming Gemini chat, conversation context, stop, regenerate, copy, clear, Markdown, history rename/delete.
- Firebase Authentication with Credential Manager on phones and Google Play Services account picker fallback on Android TV.
- Offline continuation when authentication is unavailable.
- Supplied Firebase project configuration and permanent release signing.

## Build

Use JDK 17, Gradle 9.3.1, Android Gradle Plugin 9.1.1, and Android API 37.

```bash
export FIRE_AI_KEYSTORE=/path/to/fire-ai-release.jks
export FIRE_AI_KEYSTORE_PASSWORD='your-keystore-password'
./gradlew :app:assembleDebug :app:assembleRelease
```

The private release keystore is intentionally not stored in Git. See `PERMANENT_SIGNING.md` for the Firebase SHA fingerprints.

## First run

Sign in with Google or continue offline. In Settings, enter a Gemini API key, save it, test the connection, and choose an eligible free-tier model.
