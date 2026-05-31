# EasyLink

**EasyLink** is a lightweight Android app that watches your incoming notifications for URLs and surfaces them as instant, one-tap actions — copy to clipboard or open in browser — without you ever unlocking the full notification.

Built with Kotlin and Jetpack Compose. No backend, no analytics, no accounts. Everything runs locally on your device.

---

## Features

- **Instant link detection** — monitors notifications from any app (SMS, WhatsApp, Telegram, email, etc.) and extracts URLs automatically
- **One-tap actions** — actionable notification with Copy and Open buttons, no app switch required
- **Security scanning** — flags suspicious URLs (IP-based links, lookalike domains, known-bad TLDs) before you tap
- **Tracking strip** — removes tracking query parameters from Instagram and Twitter/X links automatically
- **Privacy first** — zero network calls, zero data collection, 100% on-device processing
- **Share sheet support** — share any link from any app to EasyLink to copy/open it

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Single-module (`app/`), service + broadcast pattern |
| Build | Gradle Kotlin DSL + Version Catalog |
| Min SDK | 28 (Android 9) |
| Target SDK | 36 |

---

## Project Structure

```
app/src/main/java/com/iv/easylink/
├── ui/
│   └── MainActivity.kt          # Onboarding screen + EasyLink theme
├── service/
│   └── LinkNotificationListener.kt  # Core: NotificationListenerService
├── actions/
│   └── LinkActionReceiver.kt    # Handles Copy / Open broadcast actions
├── share/
│   └── ShareReceiverActivity.kt # Share-intent entry point
└── utils/
    ├── LinkExtractor.kt         # URL detection & normalization
    └── SecurityScanner.kt       # Suspicious URL heuristics
```

---

## How It Works

```
Incoming notification (any app)
        │
        ▼
LinkNotificationListener
        │  extracts URLs via LinkExtractor
        │  scans via SecurityScanner
        ▼
Actionable notification posted
        │
   ┌────┴────┐
   ▼         ▼
 Copy      Open
   │         │
clipboard  browser
```

---

## Getting Started

### Prerequisites
- Android Studio (latest stable, via JetBrains Toolbox recommended)
- Android SDK 36
- A device or emulator running Android 9+

### Build & Run

```bash
# Debug APK
./gradlew assembleDebug

# Install directly to connected device
./gradlew installDebug

# Run unit tests
./gradlew test
```

### Permissions Required

| Permission | Why |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read incoming notifications to detect links |
| `POST_NOTIFICATIONS` | Show the copy/open action notification (Android 13+) |

The app walks the user through granting both on first launch.

---

## Design

EasyLink uses an editorial design language — a **blush pearl canvas** paired with a **mulberry accent** (`#8B1E41`). The visual identity is intentionally warm and reading-focused, avoiding the harsh blues and neons common in utility apps.

Key design tokens:

| Token | Value | Use |
|---|---|---|
| Canvas | `#FCFAFB` | Page background |
| Mulberry | `#8B1E41` | Primary CTAs, active states |
| Ink | `#1A1517` | Headlines and primary text |
| Body | `#4A4045` | Running text |
| Surface Card | `#F5EFF2` | Card backgrounds |

Full design spec in [`DESIGN.md`](./DESIGN.md).

---

## Roadmap

- [ ] Phase 2 — Link history / saved links list
- [ ] Phase 2 — Bundled Lora + Inter custom fonts
- [ ] Phase 2 — Dark mode (aubergine surface `#1A1316`)
- [ ] Phase 3 — AI-powered link summaries
- [ ] Phase 3 — Collections and tagging

---

## Logs & Debugging

```bash
adb logcat -s EasyLink
```

---

## License

MIT — see [LICENSE](./LICENSE) for details.
