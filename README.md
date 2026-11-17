# FocusGuard

Native Android (Kotlin, Jetpack Compose, Material 3) digital wellbeing and strict distraction-control app.

## Core Features

1. **Short Video & Reels Blocker (Accessibility Service)**
   - `VideoAndUninstallBlockerAccessibilityService` monitors `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`.
   - Detects Instagram Reels, YouTube Shorts, TikTok, Facebook Reels, and Snapchat Spotlight views and triggers `performGlobalAction(GLOBAL_ACTION_BACK)` (with an optional blocking intervention overlay).

2. **App Usage Tracker & Enforcer (`UsageStatsManager`)**
   - `UsageStatsHelper` computes accurate daily foreground usage from midnight using `UsageEvents` lifecycle transitions (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`) with `UsageStats` fallback.
   - `UsageEnforcerForegroundService` enforces configured daily limits in the background and launches `BlockedOverlayActivity` when a target app exceeds its quota until midnight.

3. **Hardened Uninstall Protection & PIN Lock**
   - `PinSecurityManager` stores a salted PBKDF2-HMAC-SHA256 hash of a 4-digit Guard PIN in Jetpack DataStore with failed-attempt lockout.
   - **App-Level Uninstall Flow**: **Vault → Uninstall FocusGuard** opens a PIN verification dialog (`PasswordVisualTransformation`) before authorizing a 35-second window and launching `Intent.ACTION_UNINSTALL_PACKAGE`.
   - **System-Level Anti-Uninstall Guard**: `VideoAndUninstallBlockerAccessibilityService` intercepts FocusGuard App Info, Force Stop, and Package Installer uninstall screens in `com.android.settings` / package installers unless PIN-authorized, kicking back to Home.

4. **Focus Audio Studio (Lyria 3 via Firebase AI Logic)**
   - Composes 30-second reset loops (`lyria-3-clip-preview`) and full-length deep-work soundtracks (`lyria-3-pro-preview`) via Firebase AI Logic (`Firebase.ai`) and App Check, with local playback and looping via `MediaPlayer`.

## Setup Instructions

1. **Enable Accessibility Guard**: Open the **Shield** tab → **Accessibility Guard Service → Grant Access** and enable **FocusGuard Reels & Uninstall Guard**.
2. **Enable Usage Access**: In **Shield** or **Limits**, tap **Grant Access** for **Usage Access Permission** and allow `FocusGuard` in system Usage Access settings.
3. **Start Background Enforcer**: Tap **Start Enforcer Service** in the **Shield** tab (grant notification permission on Android 13+).
4. **Configure Security PIN**: Open **Vault → Set 4-Digit Security PIN** to lock uninstallation and guard toggles.
5. **Firebase AI Logic (Focus Audio)**: Provisioned via `app/google-services.json` with App Check (`configureAppCheck` in `MainActivity.onCreate()`). Note that `lyria-3-clip-preview` / `lyria-3-pro-preview` calls require an active Firebase Blaze billing plan and quota on the linked project.
