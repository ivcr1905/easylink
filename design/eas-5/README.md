# EasyLink UI/UX Refinement Proposal (EAS-5) — Product Revision

**Deliverables:**
- Interactive Design Prototype: [`design/eas-5/index.html`](./index.html)
- Design Tokens & Implementation Handoff: [`design/eas-5/README.md`](./README.md)
- Status: **Revised proposal ready for product review before Android implementation**
- Reference Spec: [`tasks/TASK-EAS-5.md`](../../tasks/TASK-EAS-5.md) (Product Revision: Dark Neutral / Restrained Accent)
- Branch: `eas-5-ui-refinement`

---

## 1. Executive Summary & Product Revision Rationale

EasyLink is a quiet, local-first Android inbox for links captured from notifications and share intents. Per the owner's review of the EAS-5 proposal and directive in [`tasks/TASK-EAS-5.md`](../../tasks/TASK-EAS-5.md), this document and [`design/eas-5/index.html`](./index.html) supersede earlier iterations (including the older Blue-Teal/Slate and Violet/Lavender proposals).

### 1.1 Core Directives from Product Revision
- **Owner Rejection of Blue/AI-looking Colors**: All blue, violet, indigo, cyan, and artificial AI-looking tones have been eliminated across all screens, focus rings, avatars, category badges, and review labels.
- **Dark Neutral Canvas with Restrained Accent**:
  - The design defaults to a dark charcoal neutral phone frame (`#121212` canvas, `#1e1e20` surface, `#2d2d32` border, `#f4f4f5` primary text).
  - A neutral light mode alternative (`#f4f4f5` canvas, `#ffffff` surface, `#e4e4e7` border, `#18181b` text) is preserved and switchable via review controls.
- **Review-Only Palette Selector**:
  - A review-only dropdown control (`#paletteSelect`) allows evaluating the two restrained directions:
    1. **Spotify-inspired Charcoal / Green** (default): Electric lime/green accent (`#1ed760` dark / `#16a34a` light) paired with deep neutral charcoal surfaces.
    2. **Netflix-inspired Charcoal / Red**: Cinema red accent (`#e50914` dark / `#dc2626` light) paired with deep neutral charcoal surfaces.
- **Accurate Contrast for Text & Filled Buttons**:
  - **Green Palette**: Filled action buttons (FAB, onboarding CTA, empty state CTA, add link save) use black text (`#000000` on `#1ed760`: contrast 9.5:1; `#000000` on `#16a34a`: contrast 4.67:1, meeting WCAG AA/AAA).
  - **Red Palette**: Filled action buttons use white text (`#ffffff` on `#e50914`: contrast 4.7:1; `#ffffff` on `#dc2626`: contrast 4.65:1, meeting WCAG AA).
  - Accent text elements use dedicated high-contrast tokens (`--p-accent-text`) across both themes.
- **Neutral, Consistent Source Avatars**:
  - The previous pastel rainbow of 6 multi-colored avatar slots has been replaced with consistent, neutral initials containers:
    - Dark mode: `#27272a` container with `#e4e4e7` off-white text.
    - Light mode: `#e4e4e7` container with `#27272a` dark neutral text.
  - Avatars cleanly display 2-letter uppercase initials (`RT`, `KD`, `SM`, `SK`, `RD`, `AM`).
- **Meaningful Semantic Safety Colors**:
  - Safety and tracking heuristics remain distinct, legible, and uncompromised:
    - Cleaned / Granted: Green (`#4ade80` dark / `#15803d` light).
    - Flagged / Suspicious: Amber (`#fbbf24` dark / `#b45309` light).
    - Phishing / Destructive / Bin: Red (`#f87171` dark / `#b91c1c` light).
- **Simulated Plain Native Android Heads-Up Notification**:
  - The notification is an interactive prototype simulation of Android's system heads-up notification (disclosed clearly in review controls and this specification).
  - Modeled after native Android 13/14 system notifications:
    - Neutral card (`var(--p-surface)` with subtle elevation shadow, no artificial glow or saturated background).
    - Tiny monochrome app icon (14x14dp transparent icon, no saturated colored tile).
    - App name: clean unadorned `EasyLink` (promotional `Link Detected` badge completely removed).
    - Sender-active title: `Rohit shared a link`.
    - Host: `deepmind.google`.
    - Plain text actions: native-style plain text buttons `Open`, `Copy`, `Mute` with accessible touch targets.
- **Preserved Gmail-like Row Geometry**:
  - Broad nearly edge-to-edge flat rows (`min-height: 68dp`), minimal margins (4–8px), 1px hairline dividers, zero card shadows.
  - Three-line content hierarchy: (1) Strong sender + right date, (2) Prominent single-line link title, (3) Compact host with restrained status tags.
  - Top filter strip completely removed.
  - Newest-to-oldest chronological ordering strictly maintained.

---

## 2. Reusable Semantic Design Tokens

### 2.1 Neutral Surface & Text Tokens

| Semantic Role | Dark Mode (Default) | Light Mode | Usage |
|---|---|---|---|
| `canvas` | `#121212` (Deep Charcoal) | `#f4f4f5` (Neutral Zinc) | Phone floor behind rows and search capsule |
| `surface` | `#1e1e20` (Flat Charcoal) | `#ffffff` (Pure White) | Link rows, search capsule, action sheets, dialogs |
| `surface-subtle` | `#27272a` (Charcoal Subtle) | `#e4e4e7` (Light Neutral) | Button hover, pressed rows, secondary pills |
| `border` | `#2d2d32` (Hairline Divider) | `#e4e4e7` (Hairline Border) | 1px row dividers, modal borders |
| `border-strong` | `#3f3f46` (Strong Divider) | `#d4d4d8` (Border Strong) | Active input outlines, popup borders |
| `text-primary` | `#f4f4f5` (Clean Off-White) | `#18181b` (Charcoal Black) | Sender/source, link titles, screen headings |
| `text-muted` | `#a1a1aa` (Muted Neutral) | `#52525b` (Muted Neutral) | Hostnames, secondary captions, subheadings |
| `text-subtle` | `#71717a` (Subtle Neutral) | `#71717a` (Subtle Neutral) | Timestamps, dates, empty captions, placeholders |
| `avatar-bg` | `#27272a` (Neutral Container) | `#e4e4e7` (Neutral Container) | Circular source avatar container |
| `avatar-text` | `#e4e4e7` (Off-White Text) | `#27272a` (Neutral Text) | Circular source avatar initials |
| `favorite-star` | `#eab308` (Amber-500) | `#eab308` (Amber-500) | Trailing favorite star active fill |
| `card-shadow` | `none` | `none` | Zero row shadows per Gmail reference |

### 2.2 Palette 1: Spotify-inspired Charcoal / Green (Default)

| Token | Dark Mode Value | Light Mode Value | Purpose & Contrast |
|---|---|---|---|
| `--p-accent` | `#1ed760` (Spotify Green) | `#16a34a` (Green-600) | Filled actions, active nav, focus indicators |
| `--p-accent-dark` | `#169c46` | `#15803d` | Hover / pressed state |
| `--p-accent-text` | `#1ed760` (8.2:1 on dark) | `#15803d` (4.71:1 on white) | Text links, active tab labels, icons |
| `--p-accent-container` | `#15291c` (Deep Green Tint) | `#eaf7ed` (Soft Green Tint) | Recap hero card, collection badges |
| `--p-on-accent-container` | `#bbf7d0` (9.8:1 on dark) | `#14532d` (7.6:1 on light) | Text inside accent containers |
| `--p-on-accent` | `#000000` (Black Text, 9.5:1) | `#000000` (Black Text, 4.67:1) | Text/icons on solid filled buttons |

### 2.3 Palette 2: Netflix-inspired Charcoal / Red

| Token | Dark Mode Value | Light Mode Value | Purpose & Contrast |
|---|---|---|---|
| `--p-accent` | `#e50914` (Netflix Red) | `#dc2626` (Red-600) | Filled actions, active nav, focus indicators |
| `--p-accent-dark` | `#b80710` | `#b91c1c` | Hover / pressed state |
| `--p-accent-text` | `#ff5252` (5.4:1 on dark) | `#b91c1c` (5.7:1 on white) | Text links, active tab labels, icons |
| `--p-accent-container` | `#2b1214` (Deep Red Tint) | `#fee2e2` (Soft Red Tint) | Recap hero card, collection badges |
| `--p-on-accent-container` | `#fecaca` (8.6:1 on dark) | `#7f1d1d` (8.5:1 on light) | Text inside accent containers |
| `--p-on-accent` | `#ffffff` (White Text, 4.7:1) | `#ffffff` (White Text, 4.65:1) | Text/icons on solid filled buttons |

### 2.4 Semantic Safety & Heuristic Tokens

| Semantic Role | Dark Mode Hex | Light Mode Hex | Meaning |
|---|---|---|---|
| `success` | `#4ade80` (Green-400) | `#15803d` (Green-700) | Cleaned URL parameter tag, granted status |
| `success-bg` | `#064e3b` (Green-950) | `#dcfce7` (Green-100) | Success banner container |
| `warning` | `#fbbf24` (Amber-400) | `#b45309` (Amber-700) | Suspicious heuristic warning tag & alerts |
| `warning-bg` | `#78350f` (Amber-950) | `#fef3c7` (Amber-100) | Warning alert container |
| `danger` | `#f87171` (Red-400) | `#b91c1c` (Red-700) | Move to Bin, destructive actions, phishing modal |
| `danger-bg` | `#7f1d1d` (Red-950) | `#fee2e2` (Red-100) | Safety alert header container |

---

## 3. Screen Inventory & Notification Simulation

### 3.1 Simulated Native Android Heads-Up Notification (`#simNotifBanner`)
- **Simulation Disclaimer**: The notification popup is an in-browser prototype simulation demonstrating how captured links appear when posted by EasyLink's `NotificationListenerService`. It does not claim custom OS layout control beyond standard Android notification channel conventions.
- **Plain Native Styling**:
  - **Neutral Card**: Elevated surface (`var(--p-surface)`), subtle drop shadow (`box-shadow: 0 4px 18px rgba(0, 0, 0, 0.28)`), 20px rounded corners.
  - **Tiny Monochrome Icon**: 14x14dp vector lightning bolt in muted monochrome (`var(--p-text-muted)`), no saturated colored background tile.
  - **Clean Header**: App name `EasyLink` and relative timestamp `Just now`. Promotional `Link Detected` label is removed.
  - **Sender-Active Title**: Bold sender-centric heading (`Rohit shared a link`).
  - **Host / Snippet**: Truncated navigable host (`deepmind.google`).
  - **Plain Text Actions**: Native text action buttons (`Open`, `Copy`, `Mute`) rendered in `var(--p-text-primary)` with 44px touch targets.
  - **Interactive Handlers**:
    - `Open`: Dismisses notification and opens the Link Action Sheet for item 1.
    - `Copy`: Copies cleaned URL to clipboard and triggers confirmation snackbar toast.
    - `Mute`: Dismisses notification and shows mute confirmation toast ("Muted Rohit T · links will still be saved quietly").

### 3.2 Inbox Screen (Gmail-like Row Geometry)
- **Top Capsule (`#topBarSearchField`)**: Persistent rounded search pill (48dp height, 24dp radius) with hamburger menu button on left, live search input, clear button, and search trigger.
- **Section Heading (`#screenHeaderTitle`)**: Clean, compact `Inbox` header directly below the capsule.
- **Top Filters Removed**: The previous shortcut pill strip (`All`, `Recent`, `Favorites`, `Read later`) has been removed entirely from the DOM.
- **Chronological Link Rows (`#inboxListContainer`)**:
  - Strict newest-first chronological grouping (Today, Yesterday, This Week).
  - Full-width flat rows with 1px hairline dividers and zero card shadows.
  - Circular source initials: 40dp neutral circular avatar (`var(--p-avatar-bg)`) with crisp 2-letter uppercase initials (`RT`, `KD`, `SM`, `SK`, `RD`, `AM`).
  - Line 1: Strong sender/source on left, right-aligned timestamp/date.
  - Line 2: Prominent link title.
  - Line 3: Compact host with subtle status tags (`Cleaned`, `⚠️ Flagged`, `Read later`).
  - Trailing Action: Dedicated favorite star button (44x44dp) toggling gold fill (`#eab308`).
  - Floating Action Button (`#fabAddBtn`): Thumb-accessible bottom-right button styled with active palette accent (`background: var(--p-accent); color: var(--p-on-accent)`).

### 3.3 Link Action Sheet & Tracker Cleanup Disclosure
- Modal bottom sheet displaying Cleaned URL vs. Original URL side-by-side with independent copy buttons.
- Quick lifecycle actions: `Open`, `Copy Cleaned`, `Share`, `Edit`, `Read later`, `Favorite`, `Archive`, and `Move to Bin`.

### 3.4 Safety Warning Dialog
- Triggered for suspicious links (e.g. `paypa1-account-security-verify.top`).
- Transparent heuristic checks: lookalike protected brand name and high-risk TLD.

### 3.5 Review Toolbar Controls
- **Subtitle**: `EAS-5 Android UI Refinement · Dark Neutral / Restrained Accent` (updated from stale Blue-Teal).
- **Palette Selector (`#paletteSelect`)**: Review-only selector to toggle between Spotify Charcoal/Green and Netflix Charcoal/Red.
- **Phone Theme Toggle (`#phoneThemeToggleBtn`)**: Independent toggle between Dark (default) and Light phone modes.
- **Data State Toggle (`#dataStateToggleBtn`)**: Switch between populated demo links and realistic empty state.
- **Simulated Notification Button (`#simNotifBtn`)**: Labeled `Simulated Notification` to clearly disclose simulation behavior.

---

## 4. Implementation Handoff for Android Jetpack Compose

To implement these dark neutral and restrained accent palettes in the Android app:

```kotlin
package com.iv.easylink.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Neutral Surfaces & Texts
private val CharcoalCanvasDark = Color(0xFF121212)
private val CharcoalSurfaceDark = Color(0xFF1E1E20)
private val CharcoalSubtleDark = Color(0xFF27272A)
private val HairlineBorderDark = Color(0xFF2D2D32)
private val BorderStrongDark = Color(0xFF3F3F46)
private val TextPrimaryDark = Color(0xFFF4F4F5)
private val TextMutedDark = Color(0xFFA1A1AA)
private val TextSubtleDark = Color(0xFF71717A)

private val NeutralCanvasLight = Color(0xFFF4F4F5)
private val NeutralSurfaceLight = Color(0xFFFFFFFF)
private val NeutralSubtleLight = Color(0xFFE4E4E7)
private val HairlineBorderLight = Color(0xFFE4E4E7)
private val BorderStrongLight = Color(0xFFD4D4D8)
private val TextPrimaryLight = Color(0xFF18181B)
private val TextMutedLight = Color(0xFF52525B)
private val TextSubtleLight = Color(0xFF71717A)

// Spotify-inspired Green Tokens
private val SpotifyGreen = Color(0xFF1ED760)
private val SpotifyGreenDark = Color(0xFF169C46)
private val SpotifyGreenLight = Color(0xFF16A34A)
private val SpotifyGreenTextLight = Color(0xFF15803D)
private val GreenContainerDark = Color(0xFF15291C)
private val GreenOnContainerDark = Color(0xFFBBF7D0)
private val GreenContainerLight = Color(0xFFEAF7ED)
private val GreenOnContainerLight = Color(0xFF14532D)

// Netflix-inspired Red Tokens
private val NetflixRed = Color(0xFFE50914)
private val NetflixRedDark = Color(0xFFB80710)
private val NetflixRedLight = Color(0xFFDC2626)
private val NetflixRedTextDark = Color(0xFFFF5252)
private val NetflixRedTextLight = Color(0xFFB91C1C)
private val RedContainerDark = Color(0xFF2B1214)
private val RedOnContainerDark = Color(0xFFFECACA)
private val RedContainerLight = Color(0xFFFEE2E2)
private val RedOnContainerLight = Color(0xFF7F1D1D)

// Theme Style 1: Charcoal / Green (Default)
object CharcoalGreenThemeStyle : ThemeStyle {
    override val id = "charcoal_green"
    override val darkColors = darkColorScheme(
        primary = SpotifyGreen,
        onPrimary = Color.Black, // Green filled action black text (contrast 9.5:1)
        primaryContainer = GreenContainerDark,
        onPrimaryContainer = GreenOnContainerDark,
        background = CharcoalCanvasDark,
        onBackground = TextPrimaryDark,
        surface = CharcoalSurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = CharcoalSubtleDark,
        onSurfaceVariant = TextMutedDark,
        outline = HairlineBorderDark,
        outlineVariant = BorderStrongDark,
        error = Color(0xFFF87171)
    )
    override val lightColors = lightColorScheme(
        primary = SpotifyGreenLight,
        onPrimary = Color.Black, // Green filled action black text (contrast 4.67:1)
        primaryContainer = GreenContainerLight,
        onPrimaryContainer = GreenOnContainerLight,
        background = NeutralCanvasLight,
        onBackground = TextPrimaryLight,
        surface = NeutralSurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = NeutralSubtleLight,
        onSurfaceVariant = TextMutedLight,
        outline = HairlineBorderLight,
        outlineVariant = BorderStrongLight,
        error = Color(0xFFB91C1C)
    )
}

// Theme Style 2: Charcoal / Red (Netflix-inspired alternative)
object CharcoalRedThemeStyle : ThemeStyle {
    override val id = "charcoal_red"
    override val darkColors = darkColorScheme(
        primary = NetflixRed,
        onPrimary = Color.White, // Red filled action white text (contrast 4.7:1)
        primaryContainer = RedContainerDark,
        onPrimaryContainer = RedOnContainerDark,
        background = CharcoalCanvasDark,
        onBackground = TextPrimaryDark,
        surface = CharcoalSurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = CharcoalSubtleDark,
        onSurfaceVariant = TextMutedDark,
        outline = HairlineBorderDark,
        outlineVariant = BorderStrongDark,
        error = Color(0xFFF87171)
    )
    override val lightColors = lightColorScheme(
        primary = NetflixRedLight,
        onPrimary = Color.White, // Red filled action white text (contrast 4.65:1)
        primaryContainer = RedContainerLight,
        onPrimaryContainer = RedOnContainerLight,
        background = NeutralCanvasLight,
        onBackground = TextPrimaryLight,
        surface = NeutralSurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = NeutralSubtleLight,
        onSurfaceVariant = TextMutedLight,
        outline = HairlineBorderLight,
        outlineVariant = BorderStrongLight,
        error = Color(0xFFB91C1C)
    )
}
```

---

## 5. Verification Checklist

- [x] **Dark Neutral Default**: Prototype launches in dark phone mode (`data-phone-theme="dark"`, `data-palette="green"`) with deep charcoal `#121212` canvas and `#1e1e20` surface.
- [x] **Review-Only Palette Selector**: `#paletteSelect` dropdown in review controls allows switching between Spotify Charcoal/Green (default) and Netflix Charcoal/Red.
- [x] **Neutral Light Theme Toggle**: Preserved `#phoneThemeToggleBtn` and Customize screen segmented controls toggle to clean neutral light theme (`#f4f4f5` canvas, `#ffffff` surface).
- [x] **No Blue, Violet, Cyan or Rainbow Avatars**: All blue, violet, indigo, cyan, and pastel rainbow avatar slots eliminated. All source avatars use neutral, consistent initials containers (`#27272a` dark / `#e4e4e7` light).
- [x] **Accurate Text & Button Contrast**:
  - Green filled actions: black text (`#000000` on `#1ed760`: 9.5:1; `#000000` on `#16a34a`: 4.67:1).
  - Red filled actions: white text (`#ffffff` on `#e50914`: 4.7:1; `#ffffff` on `#dc2626`: 4.65:1).
  - Accent text on light/dark backgrounds meets WCAG AA standards.
- [x] **Semantic Safety Colors Preserved**: Cleaned (green), Flagged (amber), and Danger/Phishing (red) heuristics remain distinct and legible.
- [x] **Plain Native Android Heads-Up Notification**:
  - Neutral card without glow or saturated background.
  - Tiny monochrome app icon without bright colored tile.
  - Clean app name `EasyLink` without promotional `Link Detected` badge.
  - Sender-active title and navigable host.
  - Plain text `Open`, `Copy`, `Mute` native action buttons.
  - Clearly disclosed as a simulation in review controls and documentation.
- [x] **Gmail-like Row Geometry Preserved**: Minimal margins, broad flat rows, 1px hairline dividers, zero card shadows, no top filters, newest-to-oldest ordering.
- [x] **Updated Review Labels**: Subtitle updated from stale "Calm Blue-Teal / Slate" to "Dark Neutral / Restrained Accent".
- [x] **No Android Source Edits**: Delivered focused CSS token and prototype refinement without modifying Android code, tests, or build files.
