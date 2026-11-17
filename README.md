# FocusGuard

Native Android (Kotlin, Jetpack Compose, Material 3) digital wellbeing and strict distraction-control app.

## Core Features

1. **Short Video & Reels Blocker (Accessibility Service)**
   - `VideoAndUninstallBlockerAccessibilityService` monitors `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`.
   - Detects Instagram Reels, YouTube Shorts, TikTok, Facebook Reels, and Snapchat Spotlight views and triggers `performGlobalAction(GLOBAL_ACTION_BACK)` (with an optional blocking intervention overlay).

2. **App Usage Tracker & Enforcer (`UsageStatsManager` & Battery Exemption)**
   - `UsageStatsHelper` computes accurate daily foreground usage from midnight using `UsageEvents` lifecycle transitions (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`) with `UsageStats` fallback.
   - `BatteryOptimizationHelper` checks `PowerManager.isIgnoringBatteryOptimizations` and requests Doze/battery optimization exemption (`Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, with OEM settings fallbacks) so background usage tracking stays unblocked.
   - `UsageEnforcerForegroundService` enforces configured daily limits in the background and launches `BlockedOverlayActivity` when a target app exceeds its quota until midnight.

3. **Hardened Uninstall Protection & PIN Lock**
   - `PinSecurityManager` stores a salted PBKDF2-HMAC-SHA256 hash of a 4-digit Guard PIN in Jetpack DataStore with failed-attempt lockout.
   - **App-Level Uninstall Flow**: **Vault → Uninstall FocusGuard** opens a PIN verification dialog (`PasswordVisualTransformation`) before authorizing a 35-second window and launching `Intent.ACTION_UNINSTALL_PACKAGE`.
   - **System-Level Anti-Uninstall Guard**: `VideoAndUninstallBlockerAccessibilityService` intercepts FocusGuard App Info, Force Stop, and Package Installer uninstall screens in `com.android.settings` / package installers unless PIN-authorized, kicking back to Home.

4. **Focus Audio Studio (Lyria 3 via Firebase AI Logic)**
   - Composes 30-second reset loops (`lyria-3-clip-preview`) and full-length deep-work soundtracks (`lyria-3-pro-preview`) via Firebase AI Logic (`Firebase.ai`) and App Check, with local playback and looping via `MediaPlayer`.

## Setup Instructions

1. **Accessibility Guard**: Open the **Shield** tab → **Accessibility Guard Service → Grant Access** and enable **FocusGuard Reels & Uninstall Guard**.
2. **Usage Access**: In **Shield** or **Limits**, tap **Grant Access** for **Usage Access Permission** and allow `FocusGuard` in system Usage Access settings.
3. **Unrestricted Battery Access**: In **Shield**, tap **Exempt** on **Unrestricted Battery Access** to exempt FocusGuard from Android Doze throttling.
4. **Start Background Enforcer**: Tap **Start Enforcer Service** in the **Shield** tab (grant notification permission on Android 13+).
5. **Configure Security PIN**: Open **Vault → Set 4-Digit Security PIN** to lock uninstallation and guard toggles.
6. **Firebase AI Logic (Focus Audio)**: Provisioned via `app/google-services.json` with App Check (`configureAppCheck` in `MainActivity.onCreate()`). Note that `lyria-3-clip-preview` / `lyria-3-pro-preview` calls require an active Firebase Blaze billing plan and quota on the linked project.

## Building & Downloading from GitHub

1. Push this project to GitHub using the AI Studio **Settings → Push to GitHub** menu (or export as ZIP and push via `git`).
2. The included GitHub Actions workflow (`.github/workflows/android-build.yml`) automatically runs `:app:testDebugUnitTest` and `:app:assembleDebug` on every push to `main`/`master` (or manually via **Actions → Build FocusGuard APK → Run workflow**).
3. Open the completed workflow run in your GitHub repository's **Actions** tab and download **FocusGuard-debug-apk** under **Artifacts** to install the `.apk` on your Android device. (You can also generate an APK/AAB directly from the AI Studio settings menu.)
