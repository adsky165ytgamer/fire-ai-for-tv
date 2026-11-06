# Fire AI for TV v1.0.0

## Included

- Complete native Android TV/phone source for the Fire AI for TV Gemini chat app.
- TV-first Compose interface with D-pad, keyboard, touch, and air-mouse support.
- Gemini REST client with streaming responses, model discovery, stop/cancel handling, and connection testing.
- AES-256-GCM API-key storage backed by Android Keystore.
- Local conversation history and settings persistence.
- Markdown rendering, TV focus rings, responsive theme, and history/settings screens.
- Debug APK compiled successfully from the current source.

## Build verification

- JDK 17
- Gradle 9.3.1
- Android Gradle Plugin 9.1.1
- Android API 37 compile SDK
- `:app:assembleDebug` passed
- APK SHA-256: `8a04d1a4ac03c020c6be55a6e9362a0f3cab6614aa0cdf22b3ef6a4061d0abf2`

## Security

The repository contains no Gemini API key or backend credential. Configure the key inside the app’s Settings screen at runtime.
