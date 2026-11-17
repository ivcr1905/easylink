# EAS-5 — Refine the EasyLink UI and UX

Issue: https://linear.app/taishan/issue/EAS-5/redesign-the-easylink-ui

## Scope
Create editable design screens for product review before Android implementation.
Owner confirmed top-to-bottom, newest-to-oldest Inbox ordering. Do not reverse it.
Refine the entire existing UI/UX including color scheme, hierarchy, spacing,
navigation, empty and populated states, safety, and link actions.

## Target files
- design/eas-5/index.html: self-contained editable interactive design prototype.
- design/eas-5/README.md: palette tokens, screen inventory and implementation handoff.
Read existing preview.html and app UI screens for feature fidelity; preserve them.

## Constraints
Use Antigravity --model gemini-3.8-flash-high --sandbox for implementation.
Do not modify Android source, tests, build files, or existing preview.html.
No new backend, dependencies, remote fonts, external scripts, or images.
Preserve capture permissions, normalization, link actions and existing data behavior.
Design is a proposal, not a completed Android refinement.

## Acceptance
- Editable HTML/CSS/JS source, reusable semantic light/dark color tokens.
- Refined Inbox, Library, Insights, Settings, notification-access/onboarding state,
  search/no-results, empty/populated states, link actions and safety warning sheet.
- Newest-to-oldest list; useful metadata, clear primary actions, accessible navigation.
- Working navigation, light/dark and empty/populated review controls outside phone.
- Prototype controls work; disclose simulated actions; no dead decorative buttons.
- Keyboard focus, labeled controls, sufficient contrast, 44px+ touch targets.
- Responsive at 360px without horizontal overflow; no text below 11px.
- Readable system typography, restrained blue-teal/slate palette with safety colors.
- No gratuitous gradients, decorative metrics or invented product capabilities.
- Product review before Android implementation.

## Verification
Review source diff; preview light, dark, 360px and 1280px; inspect screenshots.
Check controls and console. No Android builds for design-only deliverable.

## Product revision — Gmail reference
Owner supplied a Gmail screenshot and requests:
- Remove the top Inbox shortcut/filter strip entirely (All/Recent/Favorites/Read later).
- Use more horizontal space per link: minimal outer margins, broad flat rows,
  restrained separators/gaps instead of floating padded cards.
- Follow the screenshot layout: pale neutral/lavender canvas, persistent rounded
  search field and hamburger, small Inbox heading, circular source initials,
  sender/source as strong first line with date on the right, prominent link title
  second line, compact host/context third line, trailing favorite star.
- Maintain newest-first order and working search, actions, safety and theme controls.
- No mail/account/chat/video features; retain EasyLink Inbox/Library navigation.
- Update README to supersede conflicting older row/filter/search/palette guidance.
Reference image (read-only):
/home/boxes/.devbox/desktop-attachments/easylink-d88dad64648b/ph7fjhxr1t1jwxnw7vv12vnn498jbtz2/1000072122.jpg

## Product revision — dark neutral / restrained accent
Owner rejects blue/AI-looking colors, especially notification styling. Explore
Netflix-inspired charcoal/red OR Spotify-inspired charcoal/green, without brand
logos or copying unrelated media-app navigation. Add a review-only palette toggle
for the two directions; default charcoal/green until owner chooses. Dark phone
mode should open initially; keep a neutral light alternative.
- Replace blue/lavender/violet tones across all screens, focus rings, review copy,
  source avatars, category tints and banners. Neutral charcoal surfaces dominate;
  green/red used sparingly. Keep safety semantics legible and distinct.
- Notification: platform-like compact neutral Android heads-up card, monochrome
  small icon, plain sender-active title, host/snippet, native-looking text actions
  Open / Copy / Mute. No promotional badges, glow, gradients, colored cards or
  saturated icon tile. This is a simulation, not a custom OS notification promise.
- Preserve Gmail-like full-width rows, no top filters, newest-first ordering.
- Update README and review labels; no Android code or build changes.
