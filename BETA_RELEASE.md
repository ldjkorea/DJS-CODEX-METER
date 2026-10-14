# GPT HUD BETA 9.9.2

Separate phone-only application: dev.bennett.codexmeter.trial, versionCode 990002.
Regular GPT HUD remains on public main v3.0.0. This beta branch contains the actual generated application sources used for the beta APK.

Updates accept only beta-v9.9.N tags and GPT-HUD-BETA-9.9.N.apk assets from this repository. Normal release assets are ignored. The original Korean signing certificate, package identity, checksum and increasing versionCode are required. Installation requires Android user confirmation.

Growth UI: level ring; day/week/month bars; permanent medals with rarity and locked states; title wardrobe; explicit equip; collapsible collection settings. Growth pull-to-refresh is disabled to avoid unhandled spinners. Rendering does not award XP.

Synthetic demo is explicitly labeled and isolated. Existing collection history, equipment and settings stay in the beta sandbox. No conversation or clipboard monitoring or runtime AI API is added.

Build: set JAVA_HOME and ANDROID_HOME, then run android/gradlew --project-dir android :app:assembleRelease :app:lintRelease. Existing signing material is private and excluded from Git. Without signing material the build is unsigned. Do not create replacement keys to update an installed beta.

Verified locally: normal/beta release selection and trust tests; release build/lint; font/provenance; owned-emulator native UI and signed update continuity. Physical phones and real OAuth accounts are not verified.

9.9.2 fixes legacy internal-permission literals that dropped OAuth READY in the beta MainActivity. The account entry button now starts sign-in immediately. A real loopback/receiver test reproduces the old failure and checks the fix, PKCE/state rejection and beta callback resolution without real account authentication.
