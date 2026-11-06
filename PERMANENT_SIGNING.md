# Fire AI permanent Android signing key

The release APK is signed with the permanent Fire AI release key. The private keystore is intentionally stored outside this repository and is required for future updates.

- Alias: `fire-ai-release`
- Package/application ID: `Com.fire_AI.in`
- SHA-1: `26:2E:AF:8A:92:CD:DA:36:93:47:0F:DA:6B:16:55:FE:0C:C7:FC:0F`
- SHA-256: `5D:0E:7F:30:BF:B8:23:99:49:41:61:9F:DA:8B:7D:E9:A6:9F:A6:22:81:BC:7B:93:12:4E:6E:97:8B:D5:AE:C0`
- Certificate validity: 2026-11-06 through 2300-08-22

Register both fingerprints for the Android app in Firebase Console under **Project settings → Your apps → Fire AI → SHA certificate fingerprints**. Google Sign-In requires the SHA-1 to match the signing certificate used by the installed APK.
