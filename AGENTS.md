# Agent Guide — EasyLink (for automated coding agents)

Purpose: give an AI coding agent the minimal, actionable knowledge to be immediately productive in this repository.

- Project type: Android app (Kotlin + Jetpack Compose). Key module: `app/`.
- Build system: Gradle Kotlin DSL using Version Catalog (`gradle/libs.versions.toml`). AGP: 9.0.1, Kotlin: 2.0.21, Java compatibility set to 11.

Quick checklist to get started
- Build: `./gradlew assembleDebug` or `./gradlew assembleRelease`
- Install on device/emulator: `./gradlew installDebug` (or install APK/AAB from `app/build/outputs/`)
- Unit tests: `./gradlew test`  Android instrumented tests: `./gradlew connectedAndroidTest` or run in Android Studio.
- View logs: `adb logcat -s EasyLink` (service logs use tag "EasyLink")

Big picture architecture (what to read first)
- `app/` module contains all code. Start with:
  - `app/src/main/AndroidManifest.xml` — shows exported components and permissions (notification listener service, a broadcast receiver, a share activity).
  - `app/src/main/java/com/iv/easylink/service/LinkNotificationListener.kt` — core runtime component: a NotificationListenerService that extracts links and posts actionable notifications.
  - `app/src/main/java/com/iv/easylink/utils/LinkExtractor.kt` and `SecurityScanner.kt` — link detection and heuristics used by the service.
  - `app/src/main/java/com/iv/easylink/actions/LinkActionReceiver.kt` and `share/ShareReceiverActivity.kt` — action handlers (copy/open) and share-intent handling.
  - `app/src/main/java/com/iv/easylink/ui/MainActivity.kt` — small Compose onboarding UI that directs the user to grant the notification listener and POST_NOTIFICATIONS permission.

Key patterns & project-specific conventions
- Version catalog: dependencies and plugin aliases are defined in `gradle/libs.versions.toml` and used as `libs.` aliases in `build.gradle.kts`.
- Compose: Material 3, BOM usage (`platform(libs.androidx.compose.bom)`) — prefer resolving compose versions via BOM rather than hard-coding.
- Notification dedup: `LinkNotificationListener` keeps `lastProcessedKey` and `lastProcessedUrl` to avoid duplicate notifications. Any change that mutates notification IDs must respect this dedup logic.
- Intent action names: receiver expects actions `ACTION_COPY_LINK` and `ACTION_OPEN_LINK` (see `AndroidManifest.xml` & `LinkActionReceiver.kt`). Use these exact strings when creating PendingIntents.
- URL extraction: `LinkExtractor.extractLinks` trims punctuation, enforces a small curated TLD list for naked domains, adds https:// if missing, and strips tracking query parameters for certain hosts (instagram/twitter/x).
- Suspicion heuristics: `SecurityScanner.isSuspiciousUrl` uses simple host checks (IP address, suspicious TLDs, keywords in subdomains, known brands in subdomains). Code relies on this deterministic function — don't change its return shape.

Integration points & external requirements
- Android notification listener permission: users must enable the app in Settings > Notification access. The app also requests POST_NOTIFICATIONS on Android 13+ (declared in manifest).
- Uses Android platform APIs only — no remote servers or 3rd-party SDKs present in repo.
- App icons and resources referenced in `LinkNotificationListener` (e.g. `R.drawable.ic_notification_bolt`, `ic_action_copy`) live under `app/src/main/res/` (check if adding notification assets; missing icons will fail resource linking).

Developer workflows & debugging tips
- To reproduce link-detection flows locally: run on a device/emulator, grant notification-listener permission, then send an SMS or use adb to post a notification from another package.
- Helpful log tag: "EasyLink" (see catch blocks in `LinkNotificationListener`). Use `adb logcat -s EasyLink`.
- When adding new dependencies, update `gradle/libs.versions.toml` and use `libs.` aliases in build files to match the existing pattern.
- The onboarding UI uses a lifecycle observer to refresh permission state on resume — when testing permissions, press home and re-open the app to trigger the check.

Files to reference when changing behavior
- `AndroidManifest.xml` — permissions, exported attributes, services/receivers.
- `LinkNotificationListener.kt` — main processing, showLinkNotification, notification channel creation.
- `LinkExtractor.kt` & `SecurityScanner.kt` — link parsing and safety heuristics.
- `ShareReceiverActivity.kt` & `LinkActionReceiver.kt` — user-facing copy/open flows.
- `app/build.gradle.kts` & `gradle/libs.versions.toml` — build configuration and dependency management.

Notes for automated changes
- Preserve exact intent action string literals and PendingIntent flag combinations (`FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`) to maintain compatibility with existing behavior.
- Avoid modifying the `lastProcessedKey` / `lastProcessedUrl` dedup strategy unless the full notification lifecycle (posted/removed/updated) is considered.
- Keep URL normalization behaviour (adding https and removing tracking queries for chosen hosts) intact unless explicitly needing a change — many components assume produced URLs are navigable.

If you need more context
- Read `app/src/main/java/com/iv/easylink/service/LinkNotificationListener.kt` and `utils/` first; they encode most of the runtime logic.
- When in doubt about resource names, inspect `app/src/main/res/`.

-- End of agent brief

