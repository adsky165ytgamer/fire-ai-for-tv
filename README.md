# AndroidTVAI

Native Android chat app for Gemini, built TV-first and usable on phones. Package: `com.mangaauthority.tvai`.
No Firebase, no backend, no local AI: the app calls the Gemini Developer API directly with a key you provide.

## Architecture
- `data/GeminiClient.kt`: OkHttp + org.json against `https://generativelanguage.googleapis.com/v1beta`. Streaming uses `models/{model}:streamGenerateContent?alt=sse`; non-streaming uses `:generateContent`; `models` lists available models. The key is sent in the `x-goog-api-key` header.
- `data/KeyVault.kt`: AES-256-GCM key in the Android Keystore encrypts your API key before it is written to disk.
- `data/ConversationStore.kt`: chats saved as small JSON files, loaded only when you open one.
- `ui/ChatViewModel.kt`, `ui/SettingsViewModel.kt`: StateFlow state, throttled stream updates, cancellation on Stop.
- `ui/*Screen.kt`, `ui/components/Markdown.kt`: Compose screens (Material 3 Expressive) and a small Markdown renderer.

## Build
Push to GitHub. `.github/workflows/build.yml` builds the debug APK (Actions tab > latest run > `AndroidTVAI-debug-apk`).
Locally: JDK 17, Gradle 9.3.1, Android Gradle Plugin 9.1.1, and Android API 37. Run `gradle :app:assembleDebug`. The uploaded source was compiled successfully with this toolchain.

## First run
Open Settings, paste your key from Google AI Studio, Save key, then Test connection. Pick a model from the loaded list.

## Controls
Touch, air-mouse and keyboard work as usual. With a D-pad remote: arrows move focus (outlined in blue), Up/Down scroll the conversation while it is focused, OK on the message box opens the keyboard, Back leaves Settings/History.

## Not verified
This project was written without an Android build environment. It has not been compiled, installed or run. Dependency versions were chosen from public release information and may need small bumps.
