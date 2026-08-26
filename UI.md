# EasyLink UI/UX Specification

Status: **Round 3 implemented — physical-device review pending**
Last updated: 2026-08-26

This document is the durable source of truth for EasyLink's interface. Future contributors and AI agents must read `DESIGN.md` for the visual foundation and this file for product-specific UI behavior before changing Compose UI.

Do not treat the current Android implementation as the design specification. During discovery, screenshots and approved mockups override the current UI. Do not implement a candidate direction until it is recorded as **Approved** below.

## 1. Product experience

EasyLink is a quiet, local-first inbox for links. The interface should help users scan, open, copy, organize, archive, and recover links quickly without presenting every capability at once.

The primary mental model is:

- **Inbox:** incoming and recently saved links.
- **Library:** categories, collections, and automation.
- **Archive:** links intentionally removed from the active Inbox.
- **Bin:** recoverable deletion for 30 days.
- **Secondary tools:** Insights, import/export, settings, privacy, and support.

The app should feel deliberate and calm, not like a dashboard assembled from cards.

## 2. Non-negotiable UX principles

1. Saved links dominate the Inbox viewport.
2. Typography and spacing establish hierarchy before surfaces or decoration.
3. Progressive disclosure: primary actions stay visible; management actions live in contextual menus or sheets.
4. Full URLs are available in link details but do not dominate list rows.
5. Destructive actions require confirmation or offer recovery.
6. Every screen must work in light and dark modes and remain extensible to future themes.
7. Tap targets remain accessible even when the visual design is compact.
8. Empty, loading, error, and large-data states are designed—not left as implementation details.
9. Motion and haptics clarify state changes; they are never required to understand the interface.
10. UI code uses shared tokens and reusable components rather than screen-specific magic values.

## 3. Known dislikes and rejected patterns

- Full-height or near-full-width navigation drawers.
- Multiple navigation systems exposing the same destination.
- A permanent Settings icon when Settings already exists in a menu.
- Large rounded cards for every row and section.
- Oversized search fields, FAB bubbles, navigation indicators, or selected drawer capsules.
- Raw URLs occupying a prominent line in every Inbox row.
- Multiple edit, pin, and delete icons permanently visible on collection rows.
- Decorative secondary text such as “View links” when the tap outcome is already obvious.
- Zero-value dashboard metrics presented as major content.
- Mixing filled, outlined, and unrelated icon families on one surface.
- Decorative or metaphorically vague icons, such as a sparkle for Automation.
- Colors assigned per component without semantic meaning.

## 4. Navigation direction

Status: **Compact menu interaction approved; destination list remains adjustable**

- Primary bottom navigation contains **Inbox** and **Library** only.
- Add is an action, not a navigation destination.
- Secondary destinations open from a compact anchored menu rather than a page-height drawer.
- Match the approved mockup interaction: the menu is content-sized, approximately 246–264 dp wide, uses about a 14 dp corner radius, and contains compact rows of roughly 40 dp visual height with accessible tap targets.
- The menu height wraps its current content and is capped at roughly 60% of available screen height. Longer content scrolls inside the menu instead of making the menu a full page.
- The menu opens below the top-left trigger with screen-edge padding and closes after navigation, outside tap, or Back.
- Suggested menu order: Insights, Archive, Bin, pinned collections, Import & export, Settings, About, Rate EasyLink.
- Avoid duplicate entry points unless user testing proves they save meaningful time.

### Android implementation contract for the compact menu

- Use an anchored Compose popup/dropdown surface, not `ModalNavigationDrawer`, `ModalDrawerSheet`, or a full-screen navigation destination.
- Anchor it to the hamburger icon and place it below the top app bar with 12–16 dp edge clearance.
- The popup must be focusable, dismiss on outside tap and Back, restore focus to the hamburger button, and expose correct accessibility labels/state.
- Use a bounded lazy list when its content exceeds the maximum height. Do not precompose an unbounded list of pinned collections.
- Keep the surrounding Inbox visible. A subtle scrim is optional, but it must not make the popup feel like a separate page.
- Opening and closing uses one short scale-plus-fade transition. Respect Android's reduced-motion setting and never block navigation while the animation completes.
- Pinned collections are capped in the popup; the complete collection list remains in Library.

## 5. Visual foundation

`DESIGN.md` remains the palette foundation. Its warm canvas, coral accent, and editorial personality may remain, but this document controls their product usage.

### Typography

- Serif: EasyLink wordmark and rare editorial moments such as the weekly recap.
- Sans serif: functional screen titles, navigation, controls, list content, settings, and Library headings.
- Link title: strongest text in a saved-link row.
- Host/source/time: secondary sans-serif text.
- Raw URL: details sheet only, unless no meaningful host can be shown.

### Semantic color roles

- Primary/coral: brand, selected state, and primary action.
- Category colors: small dots, icon backgrounds, or restrained accents only.
- Green: success and completed actions only.
- Amber: caution.
- Red: destructive action and error only.
- Large surfaces remain neutral in both themes.

### Geometry candidates

- List rows and main surfaces: 12–16 dp radius where a surface is necessary.
- Search and text inputs: approximately 12–14 dp radius.
- Buttons: 8–12 dp radius.
- Pills: reserved for genuine chips, filters, or compact statuses.
- FAB: compact rounded square, not a large circle.
- Compact menu: 12–16 dp radius with restrained elevation.

### Spacing rhythm

- Screen horizontal padding: 16–20 dp.
- Repeated row gap: 4–8 dp, or separators instead of gaps.
- Row internal padding: 10–14 dp.
- Related text gap: 2–6 dp.
- Major group spacing: 20–24 dp.

Exact values become approved tokens only after visual comparison on real phones.

### Icon system

- Use one consistent outlined icon language throughout Inbox, Library, menus, actions, notifications, empty states, and Settings.
- The v1 implementation uses a curated Material Outlined subset already available to the app, exposed only through app-owned semantic roles. Release shrinking retains only used vectors. A local [Lucide](https://lucide.dev/) vector subset remains a future visual-comparison option, not a required runtime dependency.
- Keep only the icons EasyLink uses, expected to be roughly 25–40 vectors for v1. Vector drawables add negligible runtime memory and storage compared with an icon font or broad library dependency.
- If Lucide vectors are adopted later, preserve its ISC/MIT notices in the repository and About surface.
- UI code must request semantic EasyLink roles such as `EasyLinkIcons.Automation`, `EasyLinkIcons.Video`, or `EasyLinkIcons.Archive`; feature code must not depend directly on vendor icon names. This is the replacement seam if the visual family changes later.
- Use a consistent 24 × 24 coordinate system, approximately 2-unit stroke appearance, standard 20–24 dp rendering, and the same optical weight. State changes may use color or a paired filled variant only when the meaning remains obvious.
- **Automation uses a Route icon.** Automation routes matching links from a domain/source into a destination, so Route communicates the feature more directly than Sparkle, Magic, or Bolt. Workflow is the fallback only if Route fails device review.
- Category mapping remains semantic rather than brand-specific: Videos → video/play rectangle, Articles → file text, Social → messages/users, Shopping → shopping bag, Maps → map pin, Music → headphones/music note, Documents → file, Other → link.
- Do not mix Lucide-style vectors with emoji, arbitrary Unicode glyphs, branded logos, legacy filled Material icons, or one-off hand-drawn assets on the same surface.

## 6. Screen requirements

### Inbox

- Use the **Quiet list** direction as the working Inbox direction.
- Keep the add action as a compact rounded-square floating action at the bottom-right, above bottom navigation and system insets. It must remain thumb-accessible and must not cover the final list row.
- Manual Add accepts a URL or link-containing text and lets the user choose Inbox or an existing collection before saving. Multiple detected links honor the same destination selection.
- The top app bar contains the hamburger action on the left and a search icon on the right. Do not place Add in the top app bar.
- Search is collapsed by default. Tapping the search icon expands a text field horizontally toward the left, stopping after the hamburger/menu area rather than covering it.
- Search expansion and collapse should take about 180–220 ms using a smooth material easing curve. The field receives focus and opens the keyboard after expansion; clear and close remain distinct actions.
- Back closes the keyboard first, then collapses an empty search. A non-empty query remains visible until explicitly cleared or the user leaves the surface.
- Frequently used filters remain discoverable but must not delay access to saved links.
- Each link row should normally show title or meaningful host, hostname, sender/source when available, category, time, and important warning/cleaned state.
- List-leading visuals must be consistent. Use one app-owned vector icon per category; do not mix category icons, favicons, OpenGraph thumbnails, contact photos, and empty placeholders in the same list.
- Recommended category mapping: Videos → play/video, Articles → article/page, Social → people/chat, Shopping → bag/cart, Maps → location, Music → note/headphones, Documents → document, Other → link/globe.
- Keep this as the app-owned vector subset defined by the icon system; do not maintain incomplete brand-logo coverage.
- YouTube, Netflix, and other video destinations use the same Video icon. Brand-specific icons are intentionally avoided so every link has an equally polished fallback.
- Contact/profile photos are not a primary Inbox visual because messaging notifications do not reliably expose a stable photo across apps. If a verified sender image becomes available later, it may appear in sender details without changing the list-row geometry.
- OpenGraph should still be evaluated for titles and site names. OpenGraph images are not shown in the default list because availability is inconsistent; a future details/preview surface may use them consistently.
- Tapping anywhere on a row except the Favorite control opens the compact link-action surface defined below. It must never open the URL immediately.
- Remove the trailing three-dot menu from link rows. Place a single Favorite heart at the trailing edge; tapping it toggles Favorite without opening the action surface. Its selected state, accessible label, and touch target must remain unambiguous.
- Swipe actions must be visually clear, reversible where possible, and disabled in states where they do not apply.
- Multi-selection is entered by long press and does not permanently occupy the default layout.

### Inbox shortcut lists and gestures

- The shortcut strip defaults to **All**, **Recent**, **Favorites**, and **Read later**.
- Recent means the past 24 hours and its definition must be shown in customization UI.
- All is the stable base list. Users may remove Recent, Favorites, and Read later.
- A maximum of five shortcuts is allowed, including All. The four defaults leave one user-selected category or collection slot.
- If All is the only configured shortcut, hide the strip entirely to recover vertical space.
- Long-pressing All opens **Customize Inbox lists**. Long-pressing another shortcut offers remove, reorder, and replace actions.
- Long press is a convenience, not the only discoverability path. The same editor is available from the dedicated **Customize** destination.
- Category and collection overflow menus in Library include **Show in Inbox** / **Remove from Inbox**.
- Shortcut order is user-controlled and persisted locally.
- Two mutually exclusive gesture profiles prevent horizontal-gesture conflicts:
  - **Link actions (recommended default):** row swipe archives or moves to Bin; shortcut changes happen by tapping the strip.
  - **Browse lists:** horizontal screen swipe changes Inbox shortcuts; row swipe actions are disabled and Archive/Bin remain in the link action sheet.
- An optional **No swipe actions** profile may be offered for accessibility. Never run list paging and row dismiss recognizers at the same time.
- In Browse lists mode, the parent Inbox owns the horizontal gesture even when it begins on a link row. Link-row dismiss containers must not remain in the composition in this mode.
- Swiping right from the first shortcut opens the compact menu. Swiping left moves through later shortcuts, and swiping left past the final shortcut opens Library. These edge transitions do not archive or delete a link.

### Compact secondary menu

- Must not resemble a separate full page.
- Show only enough pinned collections to remain compact; provide a Library entry for the rest.
- Use one outlined icon family and consistent icon size.
- Selection is a restrained row highlight, not a full-width capsule.

### Library

- The section order is fixed: **Categories**, then **Collections**, then **Automation**.
- Do not add a Quick lists section. Favorites and Read later are system states exposed in the Inbox shortcut strip, Inbox customization, and link actions; they are not content categories and do not need another Library surface.
- Read later is not placed in the hamburger menu.
- A link action can add/remove Read later without changing its automatic content category or collection memberships.
- Categories show useful counts; category color is a small accent, not the entire tile.
- Library category counts use exactly the same active-row predicate as the opened category: `archivedAt IS NULL AND deletedAt IS NULL`. Archived and binned rows must never inflate an active category count.
- Collection rows prioritize opening the collection.
- Pin, rename, and delete live in an overflow menu. Delete requires confirmation and never deletes the contained links.
- Automation follows Collections and remains visible as a power feature, using compact two-line rows and the Route icon from the shared icon system.
- Empty states teach the next action without adding decorative cards.

### Compact link-action surface

- Use a content-wrapped modal bottom sheet. It should feel compact without crowding its actions: target roughly 40–45% of usable height on a normal phone, with an implementation cap around 380 dp and internal scrolling when display size or font scaling requires it.
- The header shows the category icon, title or hostname, and compact metadata such as source app, exact sender name, and time when available. Do not repeat a large raw URL.
- The first action row contains **Open**, **Copy**, **Share**, and **Edit** as equal compact actions.
- The first secondary row contains **Read later** and **Favorite**. The next row contains **Add to collection** and **Change category**. State labels change when appropriate, for example **Remove from Read later** or **Remove favorite**.
- The final utility row contains **Archive** and **Move to Bin**. Move to Bin uses the destructive color and remains recoverable for 30 days.
- Favorite is repeated in this sheet as requested even though it is also directly available on the Inbox row; both controls mutate the same state.
- When tracker cleanup changed the URL, do not use an ambiguous summary such as “2 parameters removed.” Show two explicit, selectable values labeled **Original URL** and **Cleaned URL**. Provide a compact copy affordance for each so the original remains a practical fallback if EasyLink's conservative cleaner made a mistake.
- Persist the normalized original URL whenever it differs from the cleaned URL. Never overwrite or discard the only original merely because a cleaning rule matched.
- When no cleanup occurred, show the single saved URL without a Cleaned claim.
- Edit opens a compact URL editor. Saving revalidates the HTTP(S) URL, updates host and local safety classification, preserves the newly entered normalized original, and shows the cleaned result before final confirmation when cleaning changes it.

### Insights

- Secondary destination, not a primary tab.
- Prefer interpreted, honest summaries and useful review actions over charts and zero-value metric cards.
- Never claim “read,” “revisited,” or similar behavior until the app actually records that state.
- All insight data remains on-device.

### Customize and Settings

- **Customize** is a distinct compact-menu destination above Settings. It owns appearance, Inbox-list customization, gesture behavior, and future visual preferences.
- **Settings** owns operational behavior: notifications, data, previews, cleanup, source applications, permissions, privacy, and support.
- Use compact grouped rows with dividers or restrained surfaces on both destinations.
- Appearance behaves like a segmented choice inside Customize.
- Permission rows have clear chevrons or button affordance.
- Secondary descriptions explain consequences in plain language.
- Theme selection remains System, Light, and Dark for the current build. Future theme families use the Customize boundary rather than screen-specific switches.

### Link-capture notifications and sender controls

- EasyLink notifications should expose **Open**, **Copy**, and **Mute** when sender identity is reliable and the link is not blocked by a safety warning.
- Notification copy is active but does not claim safety or correctness. With a reliable sender, use **Rohit shared a link** or **Rohit shared 3 links**. Without a sender, use **Link from WhatsApp**. Never say “clean,” “safe,” or another guarantee; suspicious-link warnings remain explicit and separate.
- “Mute” controls EasyLink only; it never claims to block the person in WhatsApp, Telegram, or another source app.
- Settings → Notifications provides **Mute button behavior**, which determines the default behavior for newly muted senders so users do not make the same choice on every notification:
  - **Silence alerts:** continue saving future links from this sender but do not post EasyLink notifications for them.
  - **Ignore future links:** do not notify or add future links from this sender to EasyLink.
- **Silence alerts** is the recommended v1 default because it solves duplicate-notification fatigue without silently losing captured links. Settings may instead select Ignore future links or Ask every time.
- **Also move existing matching links to Bin** is an optional, initially-off setting for Ignore future links. It requires an explicit confirmation showing the affected link count. These links remain recoverable for 30 days. The user-facing name is **Ignore & move existing links to Bin**, not “hard block.”
- For v1, a sender rule key is exactly `(source package, trimmed sender display name)`. Matching is exact and case-sensitive—no substring, fuzzy, or case-folded matching. Therefore `Rohit T` in WhatsApp does not match `Rohit r`, `Rohit TT`, or `Rohit T` in Telegram.
- Sender rules are persisted locally so they survive process death and device restarts. The listener may keep a small immutable hash-map snapshot for constant-time checks, but an in-memory map must not be the source of truth.
- Mute actions must use app-internal/non-exported handling or equivalently protected immutable PendingIntents. Never allow an arbitrary external intent to create, edit, or delete sender rules.
- Exact display-name collisions within one source app are an accepted v1 limitation. A later stable person/conversation identifier may refine identity without changing the settings model.
- If sender text is absent, blank, or cannot be separated from conversation text reliably, omit sender-level Mute. Never silently create an app-wide rule from a missing contact name.
- Add **Manage muted senders** in Settings. Each row shows the exact saved name, source app, effective policy, and an unmute action. Per-sender policy editing and an Undo-capable in-app confirmation may be considered after device testing; the notification action itself stays one tap and follows the global default.
- After Mute, show neutral feedback, for example **Muted Rohit T · links will still be saved** or **Ignoring Rohit T · existing links kept**.
- Notification actions remain bounded and failure-safe. If an action cannot be completed, EasyLink records/logs the failure without crashing the notification listener.

## 7. Performance and implementation contract

- Prefer stable immutable UI models and keyed lazy lists.
- Avoid per-row coroutine scopes, formatters, regex creation, or unbounded image allocations.
- Do not fetch or allocate list thumbnails in v1. Rich previews are limited to bounded title and site-name metadata so every list row has consistent geometry.
- Hoist shared dimensions, shapes, and semantic colors into the theme layer.
- Reuse one link-row component across Inbox, categories, collections, Archive, and Bin.
- Catch failures at external activity, clipboard, file, and network boundaries; UI failures must degrade gracefully.
- Preserve state across navigation without retaining large screen objects unnecessarily.
- Avoid adding a dependency solely for visual polish when Compose/platform APIs are sufficient.

## 8. Screenshot review process

For every reference screenshot, record:

- **Like:** the exact element or interaction worth borrowing.
- **Dislike:** anything that should not be copied.
- **Reason:** density, hierarchy, motion, typography, geometry, navigation, or another concrete quality.
- **EasyLink adaptation:** how the idea serves link management rather than copying another brand.
- **Decision:** Approved, Rejected, or Needs comparison.

Screenshots are references, not templates. Do not reproduce trademarks, branded assets, or an entire competitor interface.

## 9. Decision log

| Date | Decision | Status | Reason |
| --- | --- | --- | --- |
| 2026-08-26 | Establish `UI.md` before further Compose redesign | Approved | Prevents one-off redesigns and gives future contributors a durable specification. |
| 2026-08-26 | Replace the large drawer with a compact anchored menu | Implemented; device review pending | User rejected the full-page menu; the implemented popup is width- and height-bounded. |
| 2026-08-26 | Keep Inbox and Library as primary destinations | Implemented; device review pending | Matches the product mental model while moving secondary tools out of the bottom bar. |
| 2026-08-26 | Use the compact anchored menu behavior from the first mockup | Approved | The open/close interaction and scale are preferred over a full-height drawer. |
| 2026-08-26 | Use Quiet list as the working Inbox direction | Approved direction | It is calmer and denser than the previous card-based UI; details still require screenshot review. |
| 2026-08-26 | Move Add to a bottom-right compact FAB | Approved | Easier thumb access and leaves the top app bar available for Search. |
| 2026-08-26 | Use a right-side search icon that expands left | Approved direction | Preserves vertical space while keeping search immediately available. |
| 2026-08-26 | Put a curated outlined subset behind semantic EasyLink icon roles | Implemented | V1 reuses the already-present Material vectors to avoid a new dependency; the semantic boundary permits a later Lucide comparison without feature rewrites. |
| 2026-08-26 | Use Route for Automation | Approved | The feature routes matching links into a destination; Sparkle is decorative and unclear. |
| 2026-08-26 | Use consistent content-category icons in link rows | Approved direction | Avoids the visual inconsistency of partial previews, contact photos, or brand icon coverage. |
| 2026-08-26 | Default Inbox shortcuts to All, Recent, Favorites, and Read later with a maximum of five | Approved direction | Four universal smart lists are useful and discoverable while leaving one custom category/collection slot. |
| 2026-08-26 | Make horizontal paging and row swipe actions mutually exclusive gesture profiles | Approved direction | Prevents ambiguous gesture recognition while preserving user choice. |
| 2026-08-26 | Keep Library ordered Categories → Collections → Automation with no Quick lists section | Approved | Keeps Library focused on durable organization and avoids duplicating smart states already available in Inbox. |
| 2026-08-26 | Treat Read later as a system state exposed in Inbox and link actions | Approved direction | Read later is user intent, not a content category or separate Library section. |
| 2026-08-26 | Remove the per-link overflow and use a direct Favorite heart | Approved direction | Row tap already opens all other actions; the heart is the only useful one-tap row state. |
| 2026-08-26 | Use a compact content-wrapped link-action sheet with a cleanup disclosure | Approved direction | Keeps actions reachable without covering half the screen and explains tracker removal without storing tracker values. |
| 2026-08-26 | Match muted senders by exact `(source package, trimmed sender name)` in v1 | Approved product direction | This matches the requested predictable behavior and prevents fuzzy cross-contact matches; display-name collision remains a documented limitation. |
| 2026-08-26 | Default new sender mutes to Silence alerts | Implemented | It fixes duplicate notifications while avoiding silent data loss; user can change the global default or per-sender rule. |

## 10. Open decisions

- The current orange-on-black dark palette is rejected and must be redesigned in the dedicated theme discussion; do not make incremental palette tweaks before that comparison.
- Candidate direction: five light and five dark theme families under Customize. Before implementation, compare open-source options for license, bundled size, Compose compatibility, contrast, and consistency; do not ship an entire UI framework merely for palettes.
- Exact Quiet-list typography, separators, row height, and whether any neutral row surface is needed.
- Whether the Inbox top bar shows a small “Inbox” label while search is collapsed or stays title-free.
- Whether All is permanently fixed as the first shortcut or may be hidden after another shortcut is selected as the default.
- Whether No swipe actions should remain visible by default after accessibility and gesture device testing.
- Whether Silence alerts should remain the shipped default after physical-device notification testing.
- Whether v2 should allow optional conversation-level rules in addition to the approved v1 exact sender-and-app key.
- Final type scale, row height, elevation, and motion behavior.
- Final light/dark palette adjustments after screenshots are compared.

## 11. Current feature inventory

This inventory describes the current uncommitted working tree as of 2026-08-26. Features marked “device validation needed” compile and have automated build coverage but still need physical-phone testing before release.

### Capture and cleaning

- Notification-listener onboarding and permission management.
- Link extraction from messaging, social, and email-style notifications.
- Manual URL/text entry and multi-link paste.
- Android share-sheet target for text links.
- Tracker cleanup with conservative general rules and known-site handling, including Instagram and other major social/share parameters.
- EasyLink notifications use sender-active wording and expose Open, Copy, and exact-sender Mute when sender identity is available.
- Recent-event and recent-link duplicate suppression.
- Source app, sender name, and conversation label capture when exposed by the notification.

### Inbox and organization

- Search by URL, host, title, source, or sender.
- Filters for category, source, sender, date, favorites, warnings, and sort order.
- Compact link action sheet with Open, Copy, Share, Edit, Read later, Favorite, collection, category, Archive, and Bin actions.
- Explicit Original URL and Cleaned URL disclosure when tracker removal changes a saved link, with both values retained and copyable.
- Editable saved URLs with local validation, conservative cleanup preview, recategorization, and safety re-scan.
- Configurable, ordered Inbox shortcuts (maximum five) with All, Recent, Favorites, and Read later defaults; long press and Settings open the same editor.
- Mutually exclusive Link actions, Browse lists, and No swipe gesture profiles.
- Multi-selection and bulk collection/archive/delete actions.
- Deterministic local categorization: Videos, Articles, Social, Shopping, Maps, Music, Documents, and Other.
- User collections with pin, rename, delete, and per-link membership.
- Domain-based automation rules with enable, edit, delete, and priority controls.
- Per-source-app exclusion setting.

### Link lifecycle and safety

- Archive and unarchive.
- Recoverable Bin with automatic permanent deletion after 30 days.
- Restore and confirmed permanent deletion.
- Optional retention cleanup that moves older active links to Bin.
- Local suspicious-link heuristics and confirmation before opening flagged links.
- HTTPS-only bounded OpenGraph title/site-name fetching, disabled by default. List-image fetching and its memory cache were removed for visual consistency and lower resource use.

### Sharing and portability — device validation needed

- Full versioned JSON backup export and Merge/Replace import.
- Collection and category export.
- Share/copy an entire collection or category as normal text.
- Share an EasyLink JSON file through Android's share sheet.
- Receive multi-link text and choose Inbox, an existing collection, or a new collection.
- Receive a shared EasyLink file with validation and bounded input size.

### Product surfaces

- Inbox, Library, Archive, Bin, collection, category, Insights, import/export, Settings, About, and onboarding screens.
- Local seven-day category/domain/source insights and review shortcuts.
- System, Light, and Dark appearance modes behind an extensible theme interface.
- Placeholder privacy, terms, website, support, and Play Store rating destinations.
- No ads, analytics SDK, account, AI API, EasyLink server, or cloud sync.

## 12. Remaining product work

### Design validation — current priority

1. Test the implemented round-3 UI on small and large phones in light and dark modes.
2. Review large-font, TalkBack, menu placement, search motion, and both gesture profiles on physical devices.
3. Record only approved device-test refinements here before another visual implementation pass.

### Next local-first feature work

- Validate exact sender extraction and Open/Copy/Mute notification actions across supported messaging apps and OEM notification styles.
- Consider optional per-sender policy editing in Settings after the shipped global default and unmute flow are device-tested.
- Compare the current curated outlined icon set with local Lucide vectors only if device review finds a meaningful visual improvement.
- Improve shortened-link classification only through an explicit, privacy-safe resolution policy; short URLs cannot always be classified correctly from the visible hostname alone.

### Release completion

- Run migration/import/share/notification regression tests on multiple physical Android versions and OEMs.
- Validate accessibility, TalkBack, font scaling, reduced motion, keyboard behavior, and gesture conflicts.
- Replace privacy, terms, website, support, and rating placeholders.
- Configure the real release keystore and produce the Play Store AAB.
- Complete Play Store data-safety declarations, screenshots, listing copy, internal testing, and staged rollout monitoring.

### Later server/account features

- Optional account and encrypted cross-device sync.
- Website and web companion experience.
- AI summaries or classification only after server/API-cost, privacy, consent, and abuse controls are designed.
- More advanced cross-app contact unification only if stable identifiers and user-controlled merging can be provided safely.
