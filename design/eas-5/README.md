# EasyLink UI/UX Refinement Proposal (EAS-5) — Product Revision

**Deliverables:**
- Interactive Design Prototype: [`design/eas-5/index.html`](./index.html)
- Design Tokens & Implementation Handoff: [`design/eas-5/README.md`](./README.md)
- Status: **Revised proposal ready for product review before Android implementation**
- Reference Spec: [`tasks/TASK-EAS-5.md`](../../tasks/TASK-EAS-5.md) (Product Revision: Gmail Reference)
- Branch: `eas-5-ui-refinement`

---

## 1. Executive Summary & Product Revision Rationale

EasyLink is a quiet, local-first Android inbox for links captured from notifications and share intents. Per the owner's review of the EAS-5 proposal and reference Gmail screenshot, this document and [`design/eas-5/index.html`](./index.html) supersede earlier iterations (including the older Blue-Teal/Slate palette, top shortcut/filter strip, and floating card lists).

### 1.1 Core Directives from Product Revision
- **Persistent Search Links Capsule with Hamburger**: In place of a traditional app bar with expanding search, a persistent rounded capsule (`48dp` height, `24dp` radius) houses the top-left hamburger menu trigger (`#menuTriggerBtn`), full-width live search input (`#searchInput`, placeholder "Search links"), clear button (`#searchClearBtn`), and search trigger (`#searchTriggerBtn`).
- **Small Inbox Section Heading**: Directly below the search capsule, a small clean heading (`Inbox`) introduces the feed, matching the compact section header convention in modern Gmail on Android.
- **Top Filter Strip Completely Removed**: The top shortcut filter strip (`All`, `Recent`, `Favorites`, `Read later`) has been removed entirely from the DOM (not merely hidden via CSS). Filtering listeners and count rendering references have been safely updated with existence guards.
- **Broad Nearly Edge-to-Edge Gmail-like Rows**: Links occupy maximum horizontal space with minimal outer margins (~4–8px), broad flat rows (`min-height: 68dp`), restrained 1px hairline dividers (`border-bottom: 1px solid var(--p-border)`), and zero card shadows (`box-shadow: none`). Floating padded cards have been eliminated.
- **Three-Line Row Content Hierarchy**:
  1. **Circular Source Initials**: A 40dp circular avatar on the left displaying uppercase contact or source initials (e.g. `RT` for Rohit T, `KD` for Kotlin Devs, `SM` for SMS, `SK` for Sarah K, `RD` for Reddit, `AM` for Alex M) in soft container tones, replacing bulky rounded-square category icons.
  2. **Line 1 (Strong Sender/Source + Right Date)**: Strong sender/source text on the left (`font-weight: 700`) paired with a right-aligned timestamp/date (`10:42 AM`, `Nov 16`).
  3. **Line 2 (Prominent Link Title)**: Prominent single-line truncated title (`font-weight: 600`, line-height 1.35).
  4. **Line 3 (Compact Host/Context)**: Hostname (`deepmind.google`) alongside restrained text tags (`Cleaned`, `⚠️ Flagged`, `Read later`), avoiding noisy badge clutter.
  5. **Trailing Favorite Star**: A dedicated 5-point star icon button (`44x44dp` touch target) that turns golden amber (`#EAB308`) when active.
- **Refined Color Palette**:
  - **Light Mode**: Pale lavender neutral light canvas (`#F2F0F9`), crisp white/soft flat rows (`#FFFFFF`), and muted violet/indigo accent (`#58509C`).
  - **Dark Mode**: Complementary deep neutral dark canvas (`#13111C`), soft flat dark rows (`#1C1A27`), and luminous muted violet/indigo accent (`#A399F3`).
- **Fidelity to Real Product Behavior**:
  - No invented unread semantics (no bold unread dots or unread count badges).
  - No invented user accounts (no profile avatar inside the search capsule).
  - Preserved bottom navigation: `Inbox` and `Library` tabs only.
  - Fully functional link actions sheet (with side-by-side tracker cleanup disclosure), safety warning dialog for deceptive links, and prototype controls outside the phone frame.

---

## 2. Reusable Semantic Design Tokens

### 2.1 Color Tokens

| Semantic Role | Light Mode Hex | Dark Mode Hex | Usage |
|---|---|---|---|
| `canvas` | `#F2F0F9` (Pale Lavender Neutral) | `#13111C` (Deep Violet-Slate) | Floor / screen background behind search capsule & rows |
| `surface` | `#FFFFFF` (Flat Card White) | `#1C1A27` (Deep Surface) | Flat link rows, search capsule, action sheets, menus |
| `surface-subtle` | `#EAE7F6` (Soft Lavender Tint) | `#292639` (Slate-Violet Tint) | Secondary button hover, row active press, search pills |
| `accent` | `#58509C` (Muted Violet/Indigo) | `#A399F3` (Luminous Violet) | Primary actions, FAB, active bottom nav, focus indicators |
| `accent-dark` | `#443C82` | `#877CE3` | Button hover and pressed states |
| `accent-container` | `#EDE9FE` (Soft Violet-100) | `#332D59` (Violet-900) | Recap hero card, active indicators, avatar containers |
| `on-accent-container`| `#2B2269` | `#EDEAFC` | Text/icons inside accent containers |
| `on-accent` | `#FFFFFF` | `#15112F` | Text on solid accent buttons |
| `border` | `#E3E0F0` (Lavender Hairline) | `#2C293D` (Dark Hairline) | 1px hairline row dividers, card borders, menu dividers |
| `border-strong` | `#CECADF` | `#413D59` | Focused inputs, popup outlines, selected borders |
| `text-primary` | `#191627` (Deep Violet-Black) | `#F2F1F8` (Off-White) | Sender/source, link titles, screen headings |
| `text-muted` | `#545068` (Muted Neutral) | `#A6A2BE` (Muted Slate) | Hostnames, secondary descriptions, screen subheadings |
| `text-subtle` | `#797490` (Subtle Neutral) | `#7A7693` (Subtle Slate) | Timestamps/dates, empty state captions, placeholder text |
| `favorite-star` | `#EAB308` (Amber-500) | `#FBBF24` (Amber-400) | Trailing favorite star active fill and hover state |
| `card-shadow` | `none` | `none` | Zero box shadows on list rows per Gmail reference |
| `success` | `#15803D` (Green-700) | `#4ADE80` (Green-400) | "Cleaned" tracker status, granted permissions |
| `success-bg` | `#DCFCE7` (Green-100) | `#064E3B` (Green-950) | Success banner container |
| `warning` | `#B45309` (Amber-700) | `#FBBF24` (Amber-400) | Suspicious heuristic warning tag & alerts |
| `warning-bg` | `#FEF3C7` (Amber-100) | `#78350F` (Amber-950) | Warning alert container |
| `danger` | `#B91C1C` (Red-700) | `#F87171` (Red-400) | "Move to Bin", destructive actions, phishing alerts |
| `danger-bg` | `#FEE2E2` (Red-100) | `#7F1D1D` (Red-950) | Destructive button hover, safety warning banner |

### 2.2 Circular Avatar Container Tokens

| Avatar Palette Slot | Light Background / Text | Dark Background / Text | Typical Assignment |
|---|---|---|---|
| Slot 1 (Violet) | `#EDE9FE` / `#5B21B6` | `#2E1065` / `#C4B5FD` | Telegram contacts, Rohit T (`RT`) |
| Slot 2 (Indigo) | `#E0E7FF` / `#3730A3` | `#1E1B4B` / `#A5B4FC` | Telegram channels, Kotlin Devs (`KD`) |
| Slot 3 (Sky) | `#E0F2FE` / `#0369A1` | `#082F49` / `#7DD3FC` | SMS captures (`SM`) |
| Slot 4 (Green) | `#DCFCE7` / `#15803D` | `#052E16` / `#86EFAC` | WhatsApp contacts, Sarah K (`SK`) |
| Slot 5 (Amber) | `#FEF3C7` / `#B45309` | `#451A03` / `#FDE68A` | Reddit captures (`RD`) |
| Slot 6 (Rose) | `#FCE7F3` / `#BE185D` | `#4C0519` / `#FBCFE8` | Telegram contacts, Alex M (`AM`) |

---

## 3. Screen Inventory & Feature Fidelity

### 3.1 Inbox Screen (Revised Gmail Layout)
- **Persistent Search Links Capsule (`#topBarSearchField`)**:
  - Embedded hamburger menu button (`#menuTriggerBtn`) on the left (40x40dp, accessible touch region).
  - Live filtering text input (`#searchInput`, placeholder "Search links") filtering across link titles, hostnames, sender display names, and source app names.
  - Auto-appearing Clear button (`#searchClearBtn`, `X`) when text is present.
  - Search trigger icon (`#searchTriggerBtn`, `#searchIconSvg`) on the right.
- **Small Inbox Section Heading (`#screenHeaderTitle`)**:
  - Positioned in `.screen-subbar` directly below the search capsule.
  - Small, clean, muted typography (`font-size: 13px`, `font-weight: 700`, `letter-spacing: 0.2px`, `color: var(--p-text-muted)`).
- **Top Filter Strip Completely Removed**:
  - The older horizontal shortcut pills (`All`, `Recent`, `Favorites`, `Read later`) have been removed completely from DOM.
  - All count references (`#countAll`, `#countRecent`, `#countFavs`, `#countReadLater`) and filter pill listeners are guarded safely against null references.
- **Chronological Link List (`#inboxListContainer`)**:
  - **Strictly newest-to-oldest ordering**: Today, Yesterday, This Week.
  - Minimal outer margins (~4–8px), flat rows with 1px hairline dividers (`border-bottom: 1px solid var(--p-border)`), zero card shadows.
  - Leading visual: 40dp circular source initials avatar (`.source-avatar`).
  - Line 1: Strong sender/source on left (`.link-source-strong`), right-aligned date/timestamp (`.link-date-text`).
  - Line 2: Prominent link title (`.link-title-line`).
  - Line 3: Compact host (`.context-host`) with subtle contextual text tags (`Cleaned`, `⚠️ Flagged`, `Read later`).
  - Trailing Action: Dedicated favorite star button (`.link-fav-btn`, 44x44dp) toggling gold fill (`#EAB308`).
  - Row tap: Opens the compact Link Action Sheet.
- **Floating Action Button (`#fabAddBtn`)**:
  - Rounded square (52dp, 16dp radius), thumb-accessible on Inbox, styled with muted violet accent (`#58509C`).

### 3.2 Compact Anchored Secondary Menu (`#anchoredDropdown`)
- Anchored directly below the top-left hamburger button (`top: 62px`, `left: 12px`, `width: 252px`).
- Destinations: `Insights`, `Archive`, `Bin` (30 days badge), Pinned collections (`Research & AI`, `Weekend Reads`), `Customize`, `Settings`, `About EasyLink`.

### 3.3 Compact Link Action Sheet (`#actionSheetBackdrop`)
- Content-wrapped modal bottom sheet capped at 410px.
- **Header**: Title, Hostname, Source app, Sender, Time.
- **Row 1 (Primary Actions)**: `Open` (triggers Safety Sheet if suspicious), `Copy` (copies Cleaned URL), `Share`, `Edit`.
- **Tracker Cleanup Disclosure Box (`#trackerDisclosureBox`)**:
  - Side-by-side disclosure: **Cleaned URL** vs **Original URL** with dedicated independent Copy buttons.
- **Row 2 (Status Toggles)**: `Read later` toggle & `Favorite` toggle.
- **Row 3 (Lifecycle Actions)**: `Archive` (with undo toast) & `Move to Bin` (recoverable for 30 days).

### 3.4 Safety Warning Sheet / Dialog (`#safetyModalBackdrop`)
- Triggered whenever a suspicious or deceptive link is clicked (e.g. `paypa1-account-security-verify.top`).
- On-device heuristic checks: IP hostnames, suspicious TLDs (`.top`, `.cf`, `.ml`), lookalike protected brands.
- Transparent disclaimer: local heuristics check, not a guarantee.
- Actions: Recommended "Keep Safe" (`#safetyCancelBtn`) and "Open Anyway" (`#safetyProceedBtn`).

### 3.5 Library Screen (`#screen-library`)
- **Categories Grid (`#categoryGrid`)**: 2x4 grid with active counts (`archivedAt IS NULL AND deletedAt IS NULL`). Tapping filters Inbox.
- **Collections (`#collectionsList`)**: User collections with item counts.
- **Automation (`#rulesList`)**: Domain-based routing rules featuring the app-owned **Route** vector icon with interactive toggle switches.

### 3.6 Insights Screen (`#screen-insights`)
- **Hero Card**: "7-Day Local Recap" with total links captured and primary sources.
- **Top Sources & Domains Breakdown**: Ranked breakdowns.
- **Local Privacy Notice**: 100% on-device guarantee.

### 3.7 Customize & Settings Screens
- **Customize (`#screen-customize`)**: Theme selector (`System`, `Light`, `Dark`) and gesture profiles.
- **Settings (`#screen-settings`)**: Exact sender mute manager, tracker cleanup toggle, local safety heuristics toggle.

---

## 4. Implementation Handoff for Android Jetpack Compose

To apply this refined Violet / Pale Lavender palette to the Android app in a future task:

### 4.1 Update `app/src/main/java/com/iv/easylink/ui/theme/EasyLinkTheme.kt`

```kotlin
// Muted Violet / Indigo Accent Palette
private val VioletPrimary = Color(0xFF58509C)       // Muted Violet-700
private val VioletPrimaryDark = Color(0xFFA399F3)   // Luminous Violet-400
private val VioletContainer = Color(0xFFEDE9FE)     // Violet-100
private val VioletContainerDark = Color(0xFF332D59) // Violet-900

// Pale Lavender Neutral Palette
private val LavenderCanvas = Color(0xFFF2F0F9)       // Pale Lavender Neutral
private val LavenderCanvasDark = Color(0xFF13111C)   // Deep Violet-Slate
private val FlatSurface = Color(0xFFFFFFFF)          // Crisp White Row
private val FlatSurfaceDark = Color(0xFF1C1A27)      // Flat Dark Surface
private val SurfaceSubtle = Color(0xFFEAE7F6)        // Soft Lavender Tint
private val SurfaceSubtleDark = Color(0xFF292639)    // Dark Tint
private val HairlineBorder = Color(0xFFE3E0F0)       // 1px Hairline Divider
private val HairlineBorderDark = Color(0xFF2C293D)   // Dark Hairline
private val TextPrimary = Color(0xFF191627)          // Deep Violet-Black
private val TextPrimaryDark = Color(0xFFF2F1F8)      // Off-White
private val TextMuted = Color(0xFF545068)            // Muted Neutral
private val TextMutedDark = Color(0xFFA6A2BE)        // Muted Slate

object VioletNeutralThemeStyle : ThemeStyle {
    override val id = "violet_neutral"
    override val lightColors = lightColorScheme(
        primary = VioletPrimary,
        onPrimary = Color.White,
        primaryContainer = VioletContainer,
        onPrimaryContainer = Color(0xFF2B2269),
        background = LavenderCanvas,
        onBackground = TextPrimary,
        surface = FlatSurface,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceSubtle,
        onSurfaceVariant = TextMuted,
        outline = HairlineBorder,
        outlineVariant = Color(0xFFCECADF),
        error = Color(0xFFB91C1C),
    )
    override val darkColors = darkColorScheme(
        primary = VioletPrimaryDark,
        onPrimary = Color(0xFF15112F),
        primaryContainer = VioletContainerDark,
        onPrimaryContainer = Color(0xFFEDEAFC),
        background = LavenderCanvasDark,
        onBackground = TextPrimaryDark,
        surface = FlatSurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = SurfaceSubtleDark,
        onSurfaceVariant = TextMutedDark,
        outline = HairlineBorderDark,
        outlineVariant = Color(0xFF413D59),
        error = Color(0xFFF87171),
    )
    override val extras = EasyLinkExtraColors(
        card = FlatSurface,
        softSurface = SurfaceSubtle,
        mutedText = TextMuted,
        success = Color(0xFF15803D),
        warning = Color(0xFFB45309),
        favoriteStar = Color(0xFFEAB308),
        categoryColors = listOf(
            Color(0xFF58509C), // Primary Violet
            Color(0xFF0D9488), // Videos
            Color(0xFF0284C7), // Articles
            Color(0xFF4F46E5), // Social
            Color(0xFFD97706), // Shopping
            Color(0xFF059669), // Maps
            Color(0xFF7C3AED), // Music
            Color(0xFF545068), // Documents
        ),
    )
    override val darkExtras = EasyLinkExtraColors(
        card = FlatSurfaceDark,
        softSurface = SurfaceSubtleDark,
        mutedText = TextMutedDark,
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFBBF24),
        favoriteStar = Color(0xFFFBBF24),
        categoryColors = listOf(
            Color(0xFFA399F3),
            Color(0xFF2DD4BF),
            Color(0xFF38BDF8),
            Color(0xFF818CF8),
            Color(0xFFFBBF24),
            Color(0xFF34D399),
            Color(0xFFB88E9E),
            Color(0xFFA6A2BE),
        ),
    )
}
```

---

## 5. Verification Checklist

- [x] **Top Filter Strip Completely Removed**: Verified that `.shortcut-strip` is eliminated entirely from `#screen-inbox` in HTML and CSS.
- [x] **Safe Filtering & Count References**: `countAll`, `countRecent`, `countFavs`, and `countReadLater` references in JS are guarded with existence checks (`if (el) ...`), ensuring zero console errors.
- [x] **Persistent Search Links Capsule**: Verified persistent capsule `#topBarSearchField` containing `#menuTriggerBtn`, `#searchInput`, `#searchClearBtn`, and `#searchTriggerBtn`.
- [x] **Small Inbox Section Heading**: Verified `#screenHeaderTitle` rendered in `.screen-subbar` directly beneath the search capsule.
- [x] **Broad Flat Gmail-like Rows**: Verified minimal outer margins (~4–8px), flat rows with 1px hairline dividers, and zero card shadows (`box-shadow: none`).
- [x] **Circular Source Initials**: Verified 40dp circular avatar on the left displaying contact/source initials with soft container colors.
- [x] **Three-Line Row Content**:
  - Line 1: Strong sender/source on left (`.link-source-strong`) + right-aligned date (`.link-date-text`).
  - Line 2: Prominent link title (`.link-title-line`).
  - Line 3: Compact host (`.context-host`) and restrained context status (`Cleaned`, `Flagged`, `Read later`).
  - Trailing Action: Dedicated favorite star (`.link-fav-btn`) with 5-point polygon turning gold (`#EAB308`).
- [x] **Palette Updated**: Pale lavender neutral light canvas (`#F2F0F9`), white/soft flat rows (`#FFFFFF`), muted violet/indigo accent (`#58509C`), and complementary dark palette (`#13111C` canvas, `#1C1A27` surface, `#A399F3` accent).
- [x] **Real Product Behavior Maintained**: No unread semantics, no user account avatar, bottom nav with Inbox/Library retained, action sheet and safety warning functional.
- [x] **Zero `vh` Units**: Confirmed zero occurrences of `[0-9]vh` in CSS/JS.
- [x] **Typography Floor**: Confirmed zero font sizes below 11px.
- [x] **44px+ Touch Targets**: All buttons, nav items, search triggers, star buttons, and rows adhere to accessibility standards.
- [x] **Android Source & Build Files Untouched**: No changes to `app/`, tests, build files, or existing `preview.html`.
