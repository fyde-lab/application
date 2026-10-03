# Fyde Design System

> **Superseded by [`design/design-system.md`](design/design-system.md)** —
> kept here for history only. That document is dark-**and**-light, built
> around a teal accent and bordered fields, and is implemented (mostly) under
> `shared/src/commonMain/kotlin/com/peltoche/fyde/theme/`; this one is
> dark-only, lime-accented, and describes underline-style fields that no
> longer match the app. See `design/design-system.md`'s own *Relationship to
> `DESIGN.md`* section for exactly what changed and why. Do not implement
> against this file.

This document defines the visual language for `fyde-application` (Kotlin
Multiplatform + Compose Multiplatform, targeting Android, iOS, Desktop, Web).
It is a **spec, not code** — nothing here is implemented yet. When it is, it
should live under `shared/src/commonMain/kotlin/com/peltoche/fyde/theme/`
(e.g. `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`), following the existing
`expect`/`actual` seam already used for platform code.

## Principles

- **Material 3 underneath, Fyde on top.** We use Compose Multiplatform's M3
  `ColorScheme`/`Typography`/`Shapes` APIs and component set (`Button`,
  `TextField`, `Card`, `Scaffold`, …) rather than hand-rolling widgets. Every
  token below maps onto an M3 role. Identity comes from *which* values we
  pick and a few deliberate deviations (flat surfaces instead of shadows,
  underline text fields, non-pill buttons), not from replacing the system.
- **Dark by default, and only, for now.** There is one shipped theme — dark.
  No light/system-theme toggle exists at v1; every screen is designed and
  reviewed against the dark palette only. (A light palette is sketched below
  purely so color roles aren't designed into a corner, not as a v1 deliverable.)
- **Minimal.** No illustrations, no gradients, no decorative chrome. Content
  and one accent color carry the UI. Elevation is shown with flat tonal
  surface steps, not drop shadows. Every element earns its presence. When in
  doubt, cut the element rather than add a variant for it.
- **Mobile-first, desktop-capable.** Layouts are designed at phone width
  first (single column, full-bleed lists, thumb-reachable actions) and then
  constrained/reflowed for wider viewports (centered content column, more
  breathing room) rather than the other way around.

## Color palette

The existing `FydeDarkColorScheme` (`App.kt`) already establishes the core
identity — near-black background, off-white text, a lime accent. This
formalizes and extends it into a full token set.

### Dark theme (default)

| Role | Value | Usage |
|---|---|---|
| `background` / `surface` | `#141414` | App background, default surface |
| `surfaceContainerLow` | `#191919` | Slightly raised content on background |
| `surfaceContainer` | `#1F1F1F` | Cards, sheets, dialogs |
| `surfaceContainerHigh` | `#242424` | Top app bar, nav bar, popovers (was `surfaceVariant`) |
| `surfaceContainerHighest` | `#2A2A2A` | Highest flat "elevation" step (menus) |
| `onBackground` / `onSurface` | `#F2F2F2` | Primary text/icons |
| `onSurfaceVariant` | `#8A8A8E` | Secondary text, captions, placeholders |
| `onSurfaceDisabled` | `#5A5A5C` | Disabled text/icon content |
| `outline` | `#3A3A3C` | Interactive borders (outlined button, focus rest state) |
| `outlineVariant` | `#2A2A2C` | Dividers, hairlines, unfocused field indicators |
| `primary` | `#D7EE5B` ("Fyde Lime") | Primary actions, selection, focus, links |
| `onPrimary` | `#141414` | Text/icons on filled-primary surfaces |
| `primaryContainer` | `#333B12` | Muted lime fill (chips, selected row bg) |
| `onPrimaryContainer` | `#E9F79B` | Content on `primaryContainer` |
| `error` | `#FF8A80` | Error text, invalid field indicator |
| `onError` | `#141414` | Text/icons on filled-error surfaces |
| `errorContainer` | `#3A1616` | Error banners, error field background |
| `onErrorContainer` | `#FFB4A9` | Content on `errorContainer` |
| `success` | `#7BD88F` | Confirmation states (e.g. upload complete) — non-M3-core, additive role |
| `scrim` | `#000000` @ 40% | Modal/sheet backdrop |

`tertiary` is not assigned a distinct hue at v1 — it defaults to `primary`'s
values. Add one only when a second accent is actually needed (e.g. a badge
category color), not speculatively.

> Not building a light theme now. If one is ever needed, `primary` will have
> to be re-derived (`#D7EE5B` fails contrast on a white background) rather
> than reused as-is — that's the only reason it's flagged here at all.

### Accessibility rule

`primary` (`#D7EE5B`) is a light, high-luminance color — it fails text
contrast against `background`. **Never use `primary` as body text color on
`background`/`surface`.** It is for: filled-button fills (with `onPrimary`
text), icons, focus rings, selection indicators, and short labels on
`primaryContainer`. All text/background pairs must meet WCAG AA (4.5:1 body,
3:1 for text ≥18sp/14sp-bold and icons).

## Typography

No custom typeface is bundled (`composeResources` has no font files) — v1
uses the system default (Roboto on Android, San Francisco on iOS/Desktop
depending on platform fallback). Add a bundled font later only if brand
requires it; don't speculatively wire one in now.

Trimmed to the roles the app actually needs, each mapped to its M3 slot:

| Role (M3 slot) | Size / Line height | Weight | Tracking | Usage |
|---|---|---|---|---|
| Headline (`headlineMedium`) | 28 / 34sp | Bold | 0 | Auth screen titles ("Log in") |
| Title (`titleLarge`) | 22 / 28sp | Bold | 0 | Screen headers ("Latest imported documents") |
| Title small (`titleMedium`) | 16 / 22sp | SemiBold | 0 | Section headers, dialog titles, empty-state titles |
| Body large (`bodyLarge`) | 16 / 22sp | SemiBold | 0 | Primary list-item content (document title) |
| Body (`bodyMedium`) | 14 / 20sp | Regular | 0 | Standard body copy, empty/error-state descriptions |
| Body small (`bodySmall`) | 12 / 16sp | Regular | 0.1sp | Captions, metadata lines ("2.1 MB · 2026-08-01") |
| Label (`labelLarge`) | 14 / 20sp | SemiBold | 0.1sp | Button text |
| Label small (`labelSmall`) | 11 / 16sp | Medium | 0.2sp | Status strings, field errors, badges |

Unused M3 slots (`displayLarge/Medium/Small`, remaining `title`/`label`
variants) inherit the M3 defaults scaled off `bodyMedium` rather than being
hand-tuned — they're not on any current screen.

## Spacing scale

4dp base unit, matching what's already in use across `App.kt`/`DocumentRow.kt`:

| Token | Value |
|---|---|
| `space-xs` | 4dp |
| `space-sm` | 8dp |
| `space-md` | 12dp |
| `space-lg` | 16dp |
| `space-xl` | 20dp |
| `space-2xl` | 24dp |
| `space-3xl` | 32dp |
| `space-4xl` | 48dp |

Screen edge margin: `space-lg` (16dp) on mobile; grows to `space-3xl` (32dp)
at desktop widths (see Navigation § layout below). Default gap between
stacked form fields: `space-md`. Default list-row vertical padding:
`space-md`.

## Corner radii

Deliberately **not** the M3 default full-pill buttons — flatter, more
geometric radii are part of Fyde's identity, echoing the existing 10dp radius
on the PDF file-type icon.

| Token | Value | Usage |
|---|---|---|
| `radius-none` | 0dp | Dividers, full-bleed images |
| `radius-xs` | 4dp | Chips, badges, text-field top corners |
| `radius-sm` | 8dp | Small buttons, input fields |
| `radius-md` | 12dp | Buttons, cards, list-item icons (default) |
| `radius-lg` | 16dp | Sheets, dialogs, large cards |
| `radius-full` | 999dp | Avatars, circular icon containers only |

## Elevation

Dark + minimal ⇒ elevation is communicated with **flat tonal surface steps**,
not shadows, for anything resting on the page. Shadows are reserved for
content that floats *above* the page (menus, dropdowns, snackbars) and are
kept subtle (low-opacity, small blur) since they read poorly on dark
backgrounds.

| Level | Surface color | Shadow | Usage |
|---|---|---|---|
| 0 | `background` (`#141414`) | none | Page background |
| 1 | `surfaceContainerLow` (`#191919`) | none | Grouped content on the page |
| 2 | `surfaceContainer` (`#1F1F1F`) | none | Cards |
| 3 | `surfaceContainerHigh` (`#242424`) | none | Top app bar, bottom nav bar |
| 4 | `surfaceContainerHighest` (`#2A2A2A`) | 0dp Y / 8dp blur, `scrim` @ 24% | Menus, dropdowns, tooltips |
| 5 (overlay) | `surfaceContainerHighest` | 4dp Y / 16dp blur, `scrim` @ 32% | Modal sheets, dialogs, snackbars |

## Buttons

All variants: min height 48dp (mobile touch target), `radius-md` (12dp,
**not** stadium/pill), `labelLarge` text, `space-lg` horizontal internal
padding. Icon-only buttons: 44dp minimum touch target regardless of the
visual icon size (24dp).

| Variant | Fill | Content | Border | Usage |
|---|---|---|---|---|
| Filled (primary) | `primary` | `onPrimary` | none | One per screen: the primary CTA ("Log in", "Sign up", "Upload") |
| Tonal | `primaryContainer` | `onPrimaryContainer` | none | Secondary but still emphasized actions |
| Outlined | transparent | `onSurface` | 1dp `outline` | Secondary actions alongside a filled button |
| Text/ghost | transparent | `primary` | none | Low-emphasis actions, inline links ("Sign up", "Log in" cross-links) |
| Icon | transparent | `onSurfaceVariant` (→ `primary` when active/selected) | none | App bar actions, list-row actions |

States (all variants): 
- **Hover** (desktop/pointer only): +8% `onX` color as a state-layer overlay.
- **Pressed**: +12% state-layer overlay, 150ms fade.
- **Focus** (keyboard nav, desktop/web): 2dp `primary` ring, 2dp offset outside the button bounds.
- **Disabled**: content + fill at 38% opacity, no state layers, not focusable.

## Text fields

Follows the existing pattern in `App.kt` (transparent fill, bottom indicator
only) rather than M3's boxed `OutlinedTextField` — this is the other
deliberate deviation that gives the app its identity: fields feel like part
of the page, not boxed-in controls.

- Container: transparent, `radius-xs` top corners only (underline style).
- Indicator: 1dp `outlineVariant` unfocused → 2dp `primary` focused, 150ms transition.
- Label: floats above input when filled/focused; `bodySmall`, `onSurfaceVariant` unfocused → `primary` focused.
- Placeholder: `bodyLarge`, `onSurfaceVariant`.
- Input text: `bodyLarge`, `onBackground`.
- Height: 56dp default (single line), grows with content for multiline.
- Error state: indicator → `error`, label → `error`, `bodySmall` helper text in `error` below the field at `space-xs` top margin.
- Disabled: indicator `outlineVariant` at 38%, content `onSurfaceDisabled`.

## Cards

Not yet used on any screen, specified for when list items graduate beyond
plain rows (e.g. a document detail card, a settings section):

- Surface: `surfaceContainer` (elevation level 2).
- Radius: `radius-lg` (16dp).
- Border: none by default; 1dp `outlineVariant` when the card needs
  definition without implying it's floating (e.g. adjacent cards of equal
  weight) — border and shadow are never combined.
- Padding: `space-lg` (16dp) internal.
- Pressed/clickable card: +4% `onSurface` state-layer overlay, no shadow change (shadows don't move on press in a flat system).

## Navigation

IA is currently shallow (auth → document list), so v1 favors a simple screen
stack over a persistent nav bar; revisit if the section count grows.

**Mobile (compact width, <600dp):**
- Full-screen stack navigation, one screen at a time.
- Top app bar (56dp, `surfaceContainerHigh`, no shadow): back chevron (icon
  button) + `titleLarge` title, centered content below.
- Screen transitions: horizontal slide, 250ms, standard M3 easing (`FastOutSlowIn`).
- No bottom navigation bar at v1 — add one only once there are 3+ peer
  top-level destinations to switch between.

**Desktop/wide (expanded width, ≥840dp):**
- Content column caps at 480–560dp and centers horizontally (mirrors the
  `AuthFormMaxWidth`-style constraint already prototyped for auth forms);
  side margins absorb the rest rather than stretching text fields/buttons
  edge-to-edge.
- Top app bar height grows to 64dp; back navigation may also be exposed as a
  keyboard shortcut (Esc / Cmd+[) since desktop users have a keyboard.
- Screen transitions: crossfade, 200ms, instead of slide (slide reads as a
  mobile-specific gesture metaphor).

## Empty states

Centered in the available content area (not top-anchored):

1. Icon, 48dp, `onSurfaceVariant`, inside a 96dp `surfaceContainerLow` circle (`radius-full`).
2. `space-lg` gap.
3. Title, `titleMedium`, `onSurface`.
4. `space-xs` gap.
5. Description, `bodyMedium`, `onSurfaceVariant`, center-aligned, max 280dp width.
6. `space-xl` gap, then an optional Filled button for the primary recovery action ("Import a document").

No illustrations — an outline icon is the ceiling of visual weight here, per the minimal principle.

## Loading states

Three patterns, chosen by how much of the layout is already known:

1. **Inline status text** — for background operations that don't block
   content (e.g. the existing `sdkStatus` line): `labelSmall`,
   `onSurfaceVariant`, no spinner.
2. **Skeleton rows** — preferred for lists with a known shape (e.g. the
   document list while `listDocuments` resolves): placeholder blocks shaped
   like `DocumentRow`, filled `surfaceContainerHigh`, pulsing opacity 40%↔100%
   over 1000ms, ease-in-out, looping. Chosen over a spinner because it
   preserves layout and communicates *what's* loading.
3. **Full-screen spinner** — only when there's no content shape to skeleton
   (e.g. initial server connection before anything is known): centered
   `CircularProgressIndicator`, `primary`, 36dp, plus a `bodySmall`
   `onSurfaceVariant` caption below it if the wait can be non-trivial.

## Error states

Three patterns, by severity/scope:

1. **Field-level** — inline under the offending `TextField` (see Text
   fields § error state above). Never a dialog for a single-field
   validation error.
2. **Status banner** — thin bar, `errorContainer` background, `error` icon
   + `onErrorContainer`-toned `bodySmall` text, optional dismiss icon
   button. Sits at the top of the affected content region (not a global
   toast) — this generalizes the existing `sdkStatus` failure text into a
   dismissible component instead of a silent label change.
3. **Full-screen error** — same layout as Empty states, but the icon circle
   uses `errorContainer`/`error` instead of neutral tones, and the primary
   button is "Retry" rather than a creation action. Used when the screen has
   nothing to show at all (e.g. server unreachable on first launch).
