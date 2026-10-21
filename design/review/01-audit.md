# 01 — Audit of the current app (Prompt 1)

Source: `design/design-system.md`, `design/assets/design-reference.png`, every screen under
`shared/src/commonMain/kotlin/com/peltoche/fyde/`, `composeResources/values/strings.xml`, the
bindings (`sdkBridge/rust/src/lib.rs`) and the SDK's `CLAUDE.md` / models, at
`application@e8d7bbf`. The app could not be built in this environment (its build needs
`buf.build` and a running server), so visual findings come from the code and the reference
image; screenshots of the running app would confirm them.

## What already works

- **A real design system, enforced.** Tokens, a component catalogue (`DesignSystemScreen`),
  "found via review" rationale next to each rule, and rules for agents. Few apps at this stage
  have it; any redesign should keep this machinery and change values, not throw it away.
- **Honest UI.** Inert controls were removed (sort, grid toggle, dead filters, placeholder nav
  items). Filters come from SDK facets only. Keep this principle.
- **Serious copy around keys.** The register warning ("Your password is the only key…"), the
  logout and disconnect confirmations, the insecure-storage opt-in are clear and correct.
- **Density by input method** (row heights per size class) and a fixed-width list pane.
- **Background-safe scraper runs** (`ScraperRuns` owned by `App`, a question dialog that can
  appear from any screen).

## Findings

Severity: **B** blocker · **M** major · **m** minor · **p** polish. Promise hurt:
① documents arrive by themselves · ② only you can read them · ③ on all your devices · 📱 mobile quality.

| # | Screen | Observation | Why it matters | Sev | Promise |
|---|---|---|---|---|---|
| 1 | Navigation | The automatic-fetching feature lives in a tab called **"Scripts"** ("Installed (n)", "Available (n)", "v4", "Parameters", "Run now"). | It's the product's main differentiator, named and shaped like a developer tool. Users think in providers ("EDF", "Free", "impots.gouv"), not scripts. Parsers (classifiers) are listed next to scrapers, so the list mixes "things that fetch" with internal plumbing. | B | ① |
| 2 | Scripts | No freshness or health per source: no last run, no "x new documents", no next run, no "password changed". Only a live run shows state; a failure is a banner that disappears when dismissed. | The user can't tell whether the automation works, which is the whole promise. Needs SDK data (last run time/outcome per installed scraper; the debug `Report` is not a status). | B | ① |
| 3 | Scripts | Runs are manual ("Run now"); nothing is scheduled. | "Automatic" is currently "one tap per provider". Needs a scheduler (per-platform background work + an SDK "due" notion). | M | ① |
| 4 | First run | After sign-up the user lands on "No documents yet — Documents you import will show up here" with Upload as the only action. | The first minute should connect a first source. Today the onboarding sells manual upload, the least differentiating feature. | M | ① |
| 5 | Verification code | `ScraperQuestionDialog` is a generic modal with a free-text "Your answer" field and a "“X” needs your input" title. | 2FA codes are the most time-sensitive moment of the loop. On a phone it needs a one-time-code field (`textContentType=oneTimeCode` / SMS autofill), the provider's name, which step is waiting, and a notification when the app is in the background. | M | ① 📱 |
| 6 | Trust | Encryption is shown as a static lock + "End-to-end encryption — Your documents are secured", only on desktop margins and in Settings. | A claim without evidence. Nothing explains what the server can and can't see, where keys live, or that this device's copy is local. On mobile the badge doesn't appear at all outside Settings. | M | ② 📱 |
| 7 | Trust | The password can't be reset and there is no recovery path (no recovery key / kit, no "verify you still know your password"). | One forgotten password = total loss. The warning is honest but leaves the user alone with the risk. Needs an SDK recovery mechanism; design can at least make the risk visible over time. | M | ② |
| 8 | Sync | Devices exist only as a Settings list ("Connected since …"). No sync state anywhere: not "synced", not "3 changes waiting", not "last synced 2 min ago". Offline banner talks about uploads only. | Multi-device is a promise the UI never shows being kept. `OpenSession` has no last-seen; the changelog cursor isn't surfaced. | M | ③ |
| 9 | Documents (phone) | The phone gets the desktop reference squeezed: search + Type/Period/Category/Subcategory chips + a flat newest-first list; category browsing only through a filter chip. | On a phone, people browse ("my payslips", "last energy bill") more than they search. 11 categories × subcategories are a ready-made browsing structure that's hidden behind a menu. | M | 📱 |
| 10 | Documents | Rows show "Invoice · Telecom" + date; amounts, issuer, due dates (attributes) aren't on rows yet, though the reference shows them. | The amount and the issuer are what people scan for. The SDK has `attributes` (amount in minor units) — surface the few that matter. | m | 📱 |
| 11 | Capture | Upload = native file picker. No camera scan, no share-sheet / "Open in Fyde" target. | On mobile, paper documents and email attachments are the two main manual sources. | M | 📱 |
| 12 | Detail (phone) | `CompactDocumentDetail` stacks header, 2-col info grid, keywords, then the preview. Only action: Download. | On a phone the preview is the document; it should come first and open full-screen. Share/export (the OS share sheet) is the expected mobile action, not "Download". | m | 📱 |
| 13 | Visual | Radii 2/4/6/10px, 13px captions, 11px micro labels, warm gray `#F5F4F1` background. | Right for the dense desktop reference, a bit severe and small on a phone held at arm's length; 11px Micro in `text-muted` sits near the legibility floor. Dynamic type isn't specified (the doc says so). | m | 📱 |
| 14 | Visual | Brand presence is minimal (a 28dp mark on desktop, a logo on auth). Encryption and automation, the two things that make Fyde different, have no visual signature. | A wallet holding your payslips and tax notices should feel like a vault, not like a file browser. | p | ② |
| 15 | Settings | Plan usage, devices, theme, language and log out are one page; "Active scripts 2 of 3" is plan jargon. | Devices and security deserve their own place (they're promise ②/③); plan limits read as a paywall. | m | ②③ |
| 16 | Errors | `server_unreachable_description`: "Check that the Fyde server is running and reachable". | Developer copy reaching end users; for a local-first app, being offline should be a normal state, not an error screen. | m | ③ |
| 17 | Platform | One Compose UI for all platforms with a custom bottom bar; no mention of iOS large titles, swipe-back, haptics, Android predictive back or system share. | Mobile-first means platform-native interaction on top of a shared visual language (the doc already says so in *Visual consistency is not interaction uniformity*, but no screen applies it yet). | m | 📱 |
| 18 | Accessibility | Contrast verified only for `border-strong`; other pairs are "proposed starting points". 44/48pt targets are specified. Text scaling not covered. | Must be closed before shipping on iOS/Android stores. | m | 📱 |

## The five problems that limit the product most

1. **The automation is invisible and named like a dev tool** (#1, #2, #3). Users can't see that
   documents arrive by themselves, which is the reason to install Fyde.
2. **The first run doesn't get the user to value** (#4): it asks for an upload instead of a
   connection.
3. **The security promise is asserted, not demonstrated, and its main risk is unmanaged**
   (#6, #7).
4. **Sync is invisible** (#8): nothing shows the documents are on every device, or when they
   aren't.
5. **The phone experience is the desktop one, shrunk** (#9, #11, #12, #13): browsing, capture
   and preview patterns of a mobile wallet are missing.
