## Overview

Easy Link brings a refined, editorial interface to the link-management category. The base atmosphere is a **blush-tinted pearl canvas** (`{colors.canvas}` — #fcfafb) — deliberately warm and inviting, avoiding the sterile cool-grays of standard utility apps. Headlines utilize a **high-contrast serif display** (Lora / Playfair Display) at weight 400, paired with a clean, humanist **Inter / Cabin** body sans. The combination feels like a curated digital library or a high-end publication.

Brand voltage comes from the **pearl + mulberry pairing** — mulberry (`{colors.primary}` — #8b1e41) is the signature accent, used on primary CTAs, active states, and AI-insight highlights. The mulberry is rich, sophisticated, and grounded—a deliberate move away from hyper-saturated tech blues or neons, ensuring the app remains easy on the eyes during long reading sessions.

The system relies on three primary surface modes to create rhythm:
1. **Pearl canvas** (`{colors.canvas}`) — default light background floor
2. **Soft mauve cards** (`{colors.surface-card}`) — subtle containers for grouping links
3. **Deep aubergine surfaces** (`{colors.surface-dark}`) — dark mode canvases, footer, and AI-generation mockups

**Key Characteristics:**
- Blush-tinted pearl canvas (`{colors.canvas}` — #fcfafb) paired with warm plum-ink text (`{colors.ink}` — #1a1517).
- Mulberry primary CTA (`{colors.primary}` — #8b1e41). Used with strict restraint to draw the eye only to what matters.
- Serif display headlines (Lora) at weight 400. Pairs with a readable sans body for a literary voice.
- Deep aubergine dark surfaces (`{colors.surface-dark}` — #1a1316) — an almost-black purple that keeps the dark mode cohesive with the brand rather than defaulting to flat gray.
- Border radius is hierarchical: `{rounded.md}` (8px) for buttons, `{rounded.lg}` (12px) for link cards, `{rounded.pill}` for tags and category filters.
- Generous whitespace to let AI summaries and dense link collections breathe without feeling cluttered.

## Colors

### Brand & Accent
- **Mulberry / Primary** (`{colors.primary}` — #8b1e41): The signature rich purplish-red. Used for all primary actions, active tab underlines, and key AI highlights.
- **Mulberry Active** (`{colors.primary-active}` — #691430): The pressed / hover-darker variant.
- **Mulberry Muted** (`{colors.primary-muted}` — #e8d5dc): A heavily desaturated pink/purple used for subtle active backgrounds or selected states.
- **Accent Rose** (`{colors.accent-rose}` — #d96c8a): A slightly more vibrant pink used sparingly for "New" badges or AI spark icons.

### Surface
- **Canvas** (`{colors.canvas}` — #fcfafb): The default page floor. A warm, pearl-like off-white with a fractional hint of pink.
- **Surface Soft** (`{colors.surface-soft}` — #f7f3f5): Section dividers, very soft band backgrounds.
- **Surface Card** (`{colors.surface-card}` — #f5eff2): Link cards, AI summary containers. One step darker than canvas.
- **Surface Dark** (`{colors.surface-dark}` — #1a1316): The dominant dark surface. A very deep, almost-black aubergine.
- **Surface Dark Elevated** (`{colors.surface-dark-elevated}` — #261d22): Elevated cards inside dark mode (e.g., a floating search bar).
- **Hairline** (`{colors.hairline}` — #e6dce0): The 1px border tone on light surfaces.

### Text
- **Ink** (`{colors.ink}` — #1a1517): All headlines and primary text. A warm, dark plum-black.
- **Body Strong** (`{colors.body-strong}` — #2e262a): Emphasized paragraphs, link titles.
- **Body** (`{colors.body}` — #4a4045): Default running-text color for AI summaries.
- **Muted** (`{colors.muted}` — #786b72): URLs, metadata, dates, breadcrumbs.
- **On Primary** (`{colors.on-primary}` — #ffffff): Text on mulberry buttons.
- **On Dark** (`{colors.on-dark}` — #fcfafb): Pearl-tinted white used on dark surfaces.
- **On Dark Muted** (`{colors.on-dark-muted}` — #a69b9f): Secondary text in dark mode.

## Typography

### Font Family
The system runs **Lora** (or Playfair Display) as the serif face for headlines, and **Inter** (or Cabin) as the clean sans-serif for UI labels, link descriptions, and metadata. **JetBrains Mono** is used exclusively for raw URLs and code snippets.

### Hierarchy

| Token | Size | Weight | Line Height | Use |
|---|---|---|---|---|
| `{typography.display-lg}` | 48px | 400 | 1.1 | Page headers, Hero sections — Lora |
| `{typography.display-md}` | 32px | 400 | 1.2 | Section heads, Category titles — Lora |
| `{typography.title-lg}` | 20px | 500 | 1.3 | Link card titles — Inter |
| `{typography.title-md}` | 16px | 600 | 1.4 | Small headers, AI summary intro — Inter |
| `{typography.body-md}` | 16px | 400 | 1.6 | Default running-text, AI summaries — Inter |
| `{typography.body-sm}` | 14px | 400 | 1.5 | Metadata, dates — Inter |
| `{typography.caption}` | 12px | 500 | 1.4 | Tags, badges — Inter |
| `{typography.url}` | 13px | 400 | 1.4 | Raw web links — JetBrains Mono |
| `{typography.button}` | 14px | 500 | 1.0 | Standard button labels — Inter |

### Principles
Display sizes use weight 400 (regular). The serif character gives the app its premium, reading-focused voice. Body text uses weight 400 for paragraphs and weight 500/600 for titles to ensure scannability when browsing lists of links.

## Layout & Whitespace

- **Base unit:** 4px.
- **Grid:** Content is centered with a max-width of `840px` for optimal reading length (especially for AI summaries).
- **Spacing Scale:** `{spacing.xs}` 8px · `{spacing.sm}` 12px · `{spacing.md}` 16px · `{spacing.lg}` 24px · `{spacing.xl}` 32px.
- **Whitespace Philosophy:** Because the app deals with dense information (links, tags, AI text), the UI must provide generous breathing room. Link cards should have `{spacing.md}` between them, with `{spacing.lg}` internal padding.

## Elevation & Depth

| Level | Treatment | Use |
|---|---|---|
| Flat | No shadow, no border | Canvas floor, basic text |
| Soft hairline | 1px `{colors.hairline}` border | Link cards, sidebars, inputs |
| Hover state | Border shifts to `{colors.primary}` | Interactive cards on hover |
| Floating | `0 10px 25px -5px rgba(26, 19, 22, 0.1)` | Dropdowns, floating action buttons |

The elevation philosophy is **color-block and border first, shadow rare**. Depth is created through the contrast of the canvas and the cards.

## Components

### Buttons
**`button-primary`** — The signature Mulberry CTA. Background `{colors.primary}`, text `{colors.on-primary}`, rounded `{rounded.md}` (8px).
**`button-secondary`** — Canvas background, 1px hairline border, text `{colors.ink}`. Used for secondary actions like "Edit Link" or "Share".

### Cards
**`link-card`** — The core component. Background `{colors.surface-card}`, 1px hairline border, rounded `{rounded.lg}` (12px). Carries the link title in `{typography.title-lg}`, the URL in `{typography.url}` (`{colors.muted}`), and the AI summary below it.
**`ai-summary-box`** — An internal container within the link card. Uses a left-border of 2px `{colors.primary}` to denote AI-generated content, with `{typography.body-md}` text.

### Tags & Inputs
**`category-tag`** — Background `{colors.surface-card}`, text `{colors.ink}`, rounded `{rounded.pill}`. For active tags, background switches to `{colors.primary-muted}` with `{colors.primary}` text.
**`search-input`** — Background `{colors.canvas}`, 1px hairline border. On focus, the border transitions to `{colors.primary}`.

## Do's and Don'ts

### Do
- Use the blush-tinted pearl canvas (`{colors.canvas}`) as the foundation. Pure white feels too clinical for this aesthetic.
- Reserve the `{colors.primary}` mulberry exclusively for interactive elements, buttons, and AI-generation markers.
- Allow for plenty of whitespace between saved links so the user can easily scan their collection.
- Keep dark mode in the aubergine/plum family (`{colors.surface-dark}`) to maintain brand consistency.

### Don't
- Don't flood backgrounds or large cards with red, pink, or maroon. It causes visual fatigue and looks cheap.
- Don't use bold weights (700+) on the serif display fonts; it ruins the editorial elegance.
- Don't use harsh, pure-black drop shadows. If shadows are needed, tint them slightly with the aubergine color (`rgba(26, 19, 22, X)`).