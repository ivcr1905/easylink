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
