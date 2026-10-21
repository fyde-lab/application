# Fyde — design review prompt set

Five prompts, meant to be run in order by an AI agent (Claude Code in this repo works best,
because it can read the code and render the desktop app with the `run-fyde-application`
skill). Each prompt's output is the next one's input. Attach current screenshots of the app
(phone, tablet, desktop, light and dark) to Prompt 1 whenever you have them.

Shared context to paste above any prompt run on its own:

> **Fyde** is a personal document wallet. It fetches documents automatically from third-party
> sites (Lua *scraper* scripts that log into a provider and download invoices, payslips, tax
> notices…; *parser* scripts classify them), stores them **end-to-end encrypted**, keeps them
> **local-first** (SQLite on each device, full-text search runs locally) and **syncs across
> devices** through a server that only relays encrypted events. Accounts use OPAQUE: the
> password never leaves the device and cannot be reset — losing it loses the documents.
> Client: Kotlin Multiplatform + Compose Multiplatform (`fyde-lab/application`), on the Rust
> `fyde-lab/sdk`. Targets: **mobile first (iOS, Android)**, then desktop and web.
> The visual source of truth is `design/design-system.md` (warm neutrals, deep teal accent,
> Public Sans + IBM Plex Mono, small radii, density by input method). The owner is open to
> big changes, including to the design system.

---

## Prompt 1 — Audit

> You are a senior product designer reviewing Fyde before a redesign. Read `CLAUDE.md`,
> `design/design-system.md`, every screen in `shared/src/commonMain/kotlin/com/peltoche/fyde/`,
> the English strings (`composeResources/values/strings.xml`) and the SDK's `CLAUDE.md` (what
> the product can actually do: documents, categories/subcategories/types, attributes, scripts,
> scraper progress steps and questions, sessions, usage). Look at the attached screenshots and,
> if you can, render the desktop app yourself.
>
> Produce an audit of the **current** app against the product's three promises —
> (1) documents arrive by themselves, (2) only you can read them, (3) they are on all your
> devices — and against mobile-first quality. For each finding give: the screen, what you
> observed (quote the string or the code), why it matters for the user, severity
> (blocker / major / minor / polish) and the promise it hurts. Cover: information architecture
> and naming, onboarding and first run, the automatic-fetching loop (connect, run, 2FA, failures,
> freshness), trust and encryption communication, sync and multi-device state, offline,
> search and browsing on a phone, document detail, capture (camera, share sheet), settings,
> accessibility (contrast, touch targets, dynamic type), platform conventions (iOS vs Android
> vs desktop), and the design system itself (what works, what fights mobile).
> End with the 5 problems that most limit the product, ranked. No solutions yet.

## Prompt 2 — Directions

> From the audit, propose **three distinct design directions**, not three skins of the same
> layout. Each must differ in at least one of: the app's central object (documents vs sources
> vs time), the navigation model, and the visual language. For each direction give: a name, a
> one-line thesis, who it serves best, the information architecture (destinations, depth),
> the home screen concept, how it expresses each of the three promises, how it adapts from
> phone to desktop, what it changes in `design/design-system.md` (tokens, components), its
> risks and its cost to build in Compose Multiplatform with the existing components.
> Keep every direction feasible with what the SDK exposes today; flag anything that needs a
> new SDK capability (e.g. scheduled runs, "last fetched" timestamps, recovery kit) as such.
> Finish with a comparison table and a recommendation.

## Prompt 3 — Screens and screenshots

> For each direction, design the key screens as high-fidelity mockups and export screenshots:
> phone (390×844, iOS) for **Home**, **Document detail**, **Sources / automatic fetching**
> (including a run waiting for a verification code) and **Trust & devices** (encryption,
> sync, devices); plus one **desktop** screen (1440×900) of the main view. Use realistic French
> and English content from the product's own categories (Energy, Telecom, Payslips, Taxes…),
> realistic providers and amounts, light theme first; show one screen in dark mode per direction.
> Respect each direction's tokens exactly; every interactive target ≥ 44pt; text contrast
> ≥ 4.5:1. Render the mockups as HTML/CSS and capture them with a headless browser, so the
> screenshots and the specs can't drift apart. Name files `<direction>-<screen>-<device>.png`.

## Prompt 4 — Detailed specs

> Write the implementation spec for each direction, precise enough for an engineer to build it
> in Compose Multiplatform without guessing: navigation map; per screen, the layout at Compact /
> Medium / Expanded, every component (mapped to an existing `App*` component, a variant to add,
> or a new component to create, with its props and states), copy (EN + FR), empty / loading /
> error / offline states, and interactions (gestures, keyboard shortcuts on desktop);
> design-token changes as a diff against `design/design-system.md` (name, light, dark, contrast
> ratio); SDK requirements (existing calls used, new capabilities needed); accessibility notes;
> and a phased delivery plan (what ships first, what can wait).

## Prompt 5 — Critique and verification

> Act as a skeptical reviewer who did not make these designs. For each direction: check every
> screenshot against its spec and its tokens (list mismatches), recompute the contrast of every
> text/background pair used, check touch targets, check that nothing shown depends on data the
> SDK cannot provide (and that anything that does is flagged), and check the three promises are
> each visible on the phone home screen without scrolling. Then stress-test each direction with
> five scenarios: first launch with zero documents; a provider asks for a 2FA code while the
> app is in the background; the user is offline on a plane; the user logs in on a second device;
> 2,000 documents over 8 years. Report what breaks and the fix, then update specs and screenshots.
