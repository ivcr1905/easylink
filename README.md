# EasyLink

EasyLink is a local-first Android link inbox. It detects links in notifications, accepts links from Android's share sheet or manual entry, cleans common tracking parameters, categorizes them, and keeps them organized without accounts or an EasyLink backend.

## MVP features

- Multi-link extraction across common notification styles
- Quick Open and Copy notifications with bounded duplicate suppression
- Searchable inbox with category, source, date, favorite, warning, and sort filters
- Swipe delete with Undo, bulk delete, bulk collection assignment, scoped history clearing, and retention controls
- Offline deterministic categorization and suspicious-link heuristics
- User collections and editable domain automation rules
- Conservative URL tracking cleanup
- Optional direct-to-website rich previews with strict time, redirect, response-size, and memory limits
- Local seven-day insights by category, domain, and source app
- Extensible Claude-inspired System, Light, and Dark themes
- Privacy disclosure, source-app exclusions, and local-only database/settings backup rules

EasyLink contains no ads, analytics, accounts, AI API, or first-party server integration.

## Architecture

The project intentionally stays in one Android module. A small manual `AppContainer` owns replaceable repository, settings, ingestion, and metadata boundaries. Room provides transactional storage; ViewModels expose bounded Flow queries; Compose implements the feature UI.

```text
Notification / share / manual entry
              │
              ▼
         LinkIngestor
extract → normalize → clean → scan → categorize → rules
              │
              ▼
        Room repository
              │
              ▼
      ViewModels → Compose UI
```

There is deliberately no DI framework, networking framework, WebView, analytics SDK, or background-work dependency.

## Build

Prerequisites: Java 11+, Android SDK 36, and an Android 9+ device or emulator.

```bash
./gradlew testDebugUnitTest assembleDebug
./gradlew lintRelease assembleRelease bundleRelease
./gradlew connectedDebugAndroidTest
```

The debug APK is generated under `app/build/outputs/apk/debug/`. The shrunk release APK and Play Store AAB are under `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.

## Privacy and release preparation

- The in-app website, privacy, terms, and support values in `AppLinks.kt` are placeholders.
- Publish and finalize [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md) before store submission.
- Follow [`docs/PLAY_STORE_RELEASE_CHECKLIST.md`](docs/PLAY_STORE_RELEASE_CHECKLIST.md).
- Rich previews make direct requests to destination websites and can be disabled in Settings.
- Notification message bodies and sender/contact names are not persisted.

## Debugging

```bash
adb logcat -s EasyLink
```

See [`DESIGN.md`](DESIGN.md) and [`ROADMAP.md`](ROADMAP.md) for the product and visual source material.
