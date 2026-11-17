# EasyLink UI/UX Refinement Proposal (EAS-5)

**Deliverables:**
- Interactive Design Prototype: [`design/eas-5/index.html`](./index.html)
- Design Tokens & Implementation Handoff: [`design/eas-5/README.md`](./README.md)
- Status: **Proposal ready for product review before Android implementation**
- Branch: `eas-5-ui-refinement`

---

## 1. Executive Summary & Design Rationale

EasyLink is a quiet, local-first Android inbox for links captured from notifications and share intents. In accordance with [`UI.md`](../../UI.md) Section 10, the legacy high-saturation orange-on-black dark palette was rejected for lack of calm focus and eye fatigue.

This proposal refines the entire EasyLink visual and interaction architecture using:
- **Calm Blue-Teal Accent** (`#0E7490` light / `#2DD4BF` dark) paired with **Slate Neutrals** (`#F8FAFC` light canvas / `#0F172A` dark canvas).
- **Strict top-to-bottom, newest-to-oldest chronological ordering** in the Inbox, grouped cleanly under uppercase date headers (`TODAY`, `YESTERDAY`, `THIS WEEK`).
- **Semantic Safety System**: Cleaned tracking status (green), caution heuristics (amber), and destructive/phishing danger (red).
- **Quiet List Philosophy**: Text hierarchy and spacing structure the viewport; raw URLs are tucked away into the compact link-action sheet unless explicitly inspecting tracker cleanup.
- **Strict Compliance with Boxes Visual Constraints**:
  - Page background transparent.
  - Host `data-theme` supported on `<html>`.
  - Zero `vh` units in CSS/JS.
  - Phone frame capped near 440px (412px standard Android flagship width).
  - Review controls outside phone wrap responsively down to 360px without horizontal overflow.
  - All controls labeled with accessible text and visible keyboard focus ring.
  - Phone light/dark review toggle operates independently from the host theme.

---

## 2. Reusable Semantic Design Tokens

### 2.1 Color Tokens

| Semantic Role | Light Mode Hex | Dark Mode Hex | Usage |
|---|---|---|---|
| `accent` | `#0E7490` (Deep Calm Cyan-Teal) | `#2DD4BF` (Luminous Teal) | Primary actions, FAB, active tabs, selected states |
| `accent-dark` | `#155E75` | `#14B8A6` | Button hover and active press |
| `accent-container` | `#E0F2FE` (Sky-100 wash) | `#134E4A` (Teal-900) | Selected shortcut pills, recap hero card, icon backgrounds |
| `on-accent-container`| `#0C4A6E` | `#CCFBF1` | Text/icons inside container surfaces |
| `on-accent` | `#FFFFFF` | `#042F2E` | Text on solid accent buttons |
| `canvas` | `#F8FAFC` (Slate-50) | `#0F172A` (Slate-900) | App floor / screen background |
| `surface` | `#FFFFFF` (Card white) | `#1E293B` (Slate-800) | List rows, cards, bottom nav, popup menus |
| `surface-subtle` | `#F1F5F9` (Slate-100) | `#334155` (Slate-700) | Secondary buttons, search bar, empty state circles |
| `border` | `#E2E8F0` (Slate-200) | `#334155` (Slate-700) | 1px hairlines between rows and card borders |
| `border-strong` | `#CBD5E1` (Slate-300) | `#475569` (Slate-600) | Selected borders, search outline, focused inputs |
| `text-primary` | `#0F172A` (Slate-900) | `#F8FAFC` (Slate-50) | Link titles, screen headings, primary actions |
| `text-muted` | `#475569` (Slate-600) | `#94A3B8` (Slate-400) | Hostnames, senders, secondary descriptions |
| `text-subtle` | `#64748B` (Slate-500) | `#64748B` (Slate-500) | Timestamps, empty state captions, pill counts |
| `success` | `#15803D` (Green-700) | `#4ADE80` (Green-400) | "Cleaned" badge, granted permissions |
| `success-bg` | `#DCFCE7` (Green-100) | `#064E3B` (Green-950) | "Cleaned" pill container |
| `warning` | `#B45309` (Amber-700) | `#FBBF24` (Amber-400) | Suspicious heuristic warning tag & alerts |
| `warning-bg` | `#FEF3C7` (Amber-100) | `#78350F` (Amber-950) | Warning pill container |
| `danger` | `#B91C1C` (Red-700) | `#F87171` (Red-400) | "Move to Bin", destructive actions, phishing danger |
| `danger-bg` | `#FEE2E2` (Red-100) | `#7F1D1D` (Red-950) | Destructive button hover, safety warning banner |

### 2.2 Category Color Accents (Outlined Vector Icons & Sub-dots)

| Category | Light Hex | Dark Hex | Semantic Meaning & Outlined Vector |
|---|---|---|---|
| **Videos** | `#0D9488` | `#2DD4BF` | Video player / play-rectangle (YouTube, Netflix, Vimeo) |
| **Articles** | `#0284C7` | `#38BDF8` | Document / file-text (Blogs, publications, Medium) |
| **Documents** | `#475569` | `#94A3B8` | File folder / code doc (PDF, markdown, dev specs) |
| **Social** | `#4F46E5` | `#818CF8` | Message bubble / user group (Mastodon, Bluesky, X) |
| **Shopping** | `#D97706` | `#FBBF24` | Shopping bag (Products, stores, wishlist) |
| **Maps** | `#059669` | `#34D399` | Map pin (Locations, navigation links) |
| **Music** | `#7C3AED` | `#A78BFA` | Musical note / headphones (Spotify, Bandcamp) |
| **Other** | `#0891B2` | `#22D3EE` | Universal link / globe (General links) |

---

## 3. Screen Inventory & Feature Fidelity

### 3.1 Inbox Screen
- **Top App Bar**:
  - Left: Hamburger icon trigger for the compact anchored secondary menu.
  - Center: Clean screen title `Inbox`.
  - Right: Search icon that expands smoothly toward the left without obscuring the menu button.
- **Expanding Search Bar**:
  - Auto-focuses keyboard controller.
  - Live filtering against link titles, hostnames, sender display names, and source app names.
  - Dedicated Clear button (`X`) and graceful collapse.
  - If no matches: displays friendly "No matches found" empty state with one-tap "Reset Filters" action.
- **Configurable Shortcut Strip**:
  - Four default smart shortcuts: `All`, `Recent` (24-hour window), `Favorites`, and `Read later`.
  - Accessible pill bounding boxes (>44px touch region).
  - Live count indicator on each pill.
- **Chronological Link List**:
  - **Strictly newest-to-oldest ordering**.
  - Categorized under sticky date headers (`TODAY`, `YESTERDAY`, `THIS WEEK`).
  - Leading visual: One unified outlined vector icon per category (no mixed favicon/thumbnail inconsistency).
  - Title: Strongest visual element in the row.
  - Subtitle: `hostname · Sender Name · Source App`.
  - Status Pills: `Cleaned` (green), `Read later` (teal container), `Flagged` (amber warning).
  - Trailing Action: Dedicated Favorite heart button (one-tap toggle without opening sheet).
  - Row tap: Opens the compact Link Action Sheet.
- **Floating Action Button (FAB)**:
  - Bottom-right compact rounded square (52dp, 16dp radius), thumb-accessible above bottom navigation.
  - Tapping opens the "Add Link to Inbox" modal.

### 3.2 Compact Anchored Secondary Menu
- Anchored directly below the top-left hamburger button.
- Dimensions: Width 252dp, max height 360dp, 14dp rounded corners.
- Preserves surrounding viewport context through a subtle scrim.
- Ordered destinations:
  1. `Insights` (local recap)
  2. `Archive`
  3. `Bin` (with "30 days" badge)
  4. Pinned collections: `Research & AI`, `Weekend Reads`
  5. Divider
  6. `Customize` (theme & gestures)
  7. `Settings` (operational settings)
  8. `About EasyLink` (privacy guarantee)

### 3.3 Compact Link Action Sheet
- Content-wrapped modal bottom sheet capped at 410px.
- **Header**: Category icon, Title, Hostname, Source app, Sender, Time.
- **Row 1 (Primary Actions)**:
  - `Open`: Directly navigates or triggers the Safety Sheet if suspicious.
  - `Copy`: Copies the Cleaned URL to clipboard with confirmation toast.
  - `Share`: Invokes Android share intent.
  - `Edit`: Opens link editor modal.
- **Tracker Cleanup Disclosure Box**:
  - Explicit side-by-side disclosure:
    - **Cleaned URL**: Stripped parameters with dedicated Copy button.
    - **Original URL**: Preserved original link with dedicated Copy button.
    - Informative note: "Tracking parameters stripped (Cleaned)".
- **Row 2 (Status Toggles)**: `Read later` toggle & `Favorite` toggle.
- **Row 3 (Destructive / Lifecycle)**:
  - `Archive`: Moves to archive, provides undo snackbar.
  - `Move to Bin`: Red destructive styling, recoverable for 30 days.

### 3.4 Safety Warning Sheet / Dialog
- Triggered whenever an unsafe or suspicious link is clicked (e.g. `paypa1-account-security-verify.top`).
- Heuristic checks: IP hostnames, suspicious TLDs (`.top`, `.cf`, `.ml`), lookalike protected brands in subdomains.
- Explicit, honest copy: "EasyLink local heuristics flagged characteristics commonly used by deceptive links. This is an on-device check, not a guarantee."
- Actions: Recommended "Keep Safe" (primary neutral) and "Open Anyway" (destructive outlined).

### 3.5 Library Screen
- **Categories (2x4 Grid)**: Shows active link counts calculated with `archivedAt IS NULL AND deletedAt IS NULL`. Tapping a category immediately filters the Inbox.
- **Collections**: Lists user collections with item counts and pinned status. Includes "+ New" collection trigger.
- **Automation**: Domain-based routing rules featuring the app-owned **Route** vector icon (no decorative sparkles). Includes interactive toggle switches for each rule.

### 3.6 Insights Screen
- **Hero Card**: "7-Day Local Recap" summarizing total links captured and primary sources.
- **Top Sources**: Ranked list (Telegram, Slack, WhatsApp).
- **Top Domains**: Clean breakdown (`github.com`, `deepmind.google`, `youtube.com`).
- **Local Privacy Notice**: "Calculated 100% on this device. EasyLink never transmits reading habits or insight summaries."

### 3.7 Customize & Settings Screens
- **Customize**: Appearance selector (`System`, `Light`, `Dark`) and mutually exclusive gesture profiles:
  - *Link actions (Recommended default)*
  - *Browse lists*
  - *No swipe actions*
- **Settings**:
  - Mute button behavior dropdown: `Silence alerts` (recommended default) vs `Ignore future links`.
  - Muted senders manager (showing exact `(sourceApp, senderName)` rules with one-tap Unmute).
  - Tracker cleanup toggle.
  - Local safety heuristics toggle.
  - Automatic retention cleanup (90 days).

### 3.8 Notification Capture & Exact Sender Mute
- Simulated incoming Android notification: `"Rohit shared a link"`.
- Notification action buttons: `Open`, `Copy`, `Mute Rohit`.
- Tapping `Mute` executes exact-sender silence rule without fuzzy contact matching.

---

## 4. Implementation Handoff for Android Jetpack Compose

To apply this refinement to the Android app source code in a future task:

### 4.1 Update `app/src/main/java/com/iv/easylink/ui/theme/EasyLinkTheme.kt`

Replace the legacy terracotta palette with the approved Blue-Teal / Slate system:

```kotlin
// Blue-Teal Accent Palette
private val TealPrimary = Color(0xFF0E7490)       // Cyan-700
private val TealPrimaryDark = Color(0xFF2DD4BF)   // Teal-400
private val TealContainer = Color(0xFFE0F2FE)     // Sky-100
private val TealContainerDark = Color(0xFF134E4A) // Teal-900

// Slate Neutral Palette
private val SlateCanvas = Color(0xFFF8FAFC)       // Slate-50
private val SlateCanvasDark = Color(0xFF0F172A)   // Slate-900
private val SlateSurface = Color(0xFFFFFFFF)
private val SlateSurfaceDark = Color(0xFF1E293B)   // Slate-800
private val SlateSubtle = Color(0xFFF1F5F9)       // Slate-100
private val SlateSubtleDark = Color(0xFF334155)   // Slate-700
private val SlateBorder = Color(0xFFE2E8F0)       // Slate-200
private val SlateBorderDark = Color(0xFF334155)   // Slate-700
private val SlateText = Color(0xFF0F172A)         // Slate-900
private val SlateTextDark = Color(0xFFF8FAFC)     // Slate-50
private val SlateMuted = Color(0xFF475569)        // Slate-600
private val SlateMutedDark = Color(0xFF94A3B8)    // Slate-400

object TealSlateThemeStyle : ThemeStyle {
    override val id = "teal_slate"
    override val lightColors = lightColorScheme(
        primary = TealPrimary,
        onPrimary = Color.White,
        primaryContainer = TealContainer,
        onPrimaryContainer = Color(0xFF0C4A6E),
        background = SlateCanvas,
        onBackground = SlateText,
        surface = SlateSurface,
        onSurface = SlateText,
        surfaceVariant = SlateSubtle,
        onSurfaceVariant = SlateMuted,
        outline = SlateBorder,
        outlineVariant = Color(0xFFCBD5E1),
        error = Color(0xFFB91C1C),
    )
    override val darkColors = darkColorScheme(
        primary = TealPrimaryDark,
        onPrimary = Color(0xFF042F2E),
        primaryContainer = TealContainerDark,
        onPrimaryContainer = Color(0xFFCCFBF1),
        background = SlateCanvasDark,
        onBackground = SlateTextDark,
        surface = SlateSurfaceDark,
        onSurface = SlateTextDark,
        surfaceVariant = SlateSubtleDark,
        onSurfaceVariant = SlateMutedDark,
        outline = SlateBorderDark,
        outlineVariant = Color(0xFF475569),
        error = Color(0xFFF87171),
    )
    override val extras = EasyLinkExtraColors(
        card = SlateSurface,
        softSurface = SlateSubtle,
        mutedText = SlateMuted,
        success = Color(0xFF15803D),
        warning = Color(0xFFB45309),
        categoryColors = listOf(
            Color(0xFF0891B2), // Other
            Color(0xFF0D9488), // Videos
            Color(0xFF0284C7), // Articles
            Color(0xFF4F46E5), // Social
            Color(0xFFD97706), // Shopping
            Color(0xFF059669), // Maps
            Color(0xFF7C3AED), // Music
            Color(0xFF475569), // Documents
        ),
    )
    override val darkExtras = EasyLinkExtraColors(
        card = SlateSurfaceDark,
        softSurface = SlateSubtleDark,
        mutedText = SlateMutedDark,
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFBBF24),
        categoryColors = listOf(
            Color(0xFF22D3EE),
            Color(0xFF2DD4BF),
            Color(0xFF38BDF8),
            Color(0xFF818CF8),
            Color(0xFFFBBF24),
            Color(0xFF34D399),
            Color(0xFFB88E9E),
            Color(0xFF94A3B8),
        ),
    )
    // Typography and Shapes inherit existing standards with 11px floor
}
```

---

## 5. Verification Checklist

- [x] **File Structure**: `design/eas-5/index.html` and `design/eas-5/README.md` self-contained and editable.
- [x] **Android Source Untouched**: Verified via `git status` that `app/`, `preview.html`, tests, and build scripts have zero modifications.
- [x] **Zero `vh` Units**: Grep check confirmed zero occurrences of `[0-9]vh` in CSS/JS.
- [x] **Page Background**: Transparent background configured on `html` and `body`.
- [x] **Host Data-Theme**: Full support for `html[data-theme="dark"]` and `html[data-theme="light"]`.
- [x] **Phone Theme Independence**: Phone light/dark toggle controls `.phone-frame[data-phone-theme]` without altering host theme.
- [x] **Phone Capping**: Phone width capped at 412px (below 440px constraint).
- [x] **360px Responsiveness**: Review toolbar outside phone wraps cleanly without horizontal overflow.
- [x] **Accessibility & Keyboard Focus**: Every interactive control features `:focus-visible` styling (`2px solid #0284c7`, offset `2px`) and appropriate ARIA attributes.
- [x] **44px+ Tap Targets**: All buttons, nav items, menu rows, and action items provide minimum 44px touch regions.
- [x] **Typography Floor**: Zero font sizes below 11px.
- [x] **Newest-to-Oldest Ordering**: Verified chronological top-to-bottom Inbox list.
- [x] **Tracker Disclosure**: Side-by-side disclosure of Cleaned URL vs Original URL with independent copy actions.
- [x] **Safety Warning Flow**: Working prompt with simulated suspicious link interception.
- [x] **No External Dependencies**: Zero remote fonts, zero CDN links, zero external scripts. All SVG graphics rendered inline.
