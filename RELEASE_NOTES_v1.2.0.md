# Fire AI for TV v1.2.0

## Google Sign-In fix

- Android TV now uses the Google Play Services account picker, which works on TV devices that have no Credential Manager credential provider and previously showed “no credentials available”.
- Phones continue to use Credential Manager first and automatically fall back to the same account picker when a provider is unavailable.
- Firebase Authentication still receives the Google ID token using the supplied web OAuth client ID.
- Clear cancellation and Firebase/OAuth error messages are shown instead of a dead-end sign-in screen.
- No fake QR or passkey flow was added: QR/passkey authentication requires a compatible WebAuthn/Firebase backend and cannot be safely simulated by the APK alone.

## Signing and package

- Package/application ID remains exactly `Com.fire_AI.in`.
- Added a long-lived release key through 2300 with documented SHA-1 and SHA-256 fingerprints.
- Release signing configuration reads the keystore path/password from environment variables and never stores the private key in Git.

## Build verification

- JDK 17
- Gradle 9.3.1
- Android Gradle Plugin 9.1.1
- Android API 37 compile SDK
- `:app:assembleDebug :app:assembleRelease` passed
- Release APK certificate SHA-1: `26:2E:AF:8A:92:CD:DA:36:93:47:0F:DA:6B:16:55:FE:0C:C7:FC:0F`
- Release APK certificate SHA-256: `5D:0E:7F:30:BF:B8:23:99:49:41:61:9F:DA:8B:7D:E9:A6:9F:A6:22:81:BC:7B:93:12:4E:6E:97:8B:D5:AE:C0`
