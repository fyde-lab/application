# Design

Two kinds of design live here, and they never mix:

| Folder | What it is | Changes when |
|---|---|---|
| [`current/`](current/) | **The app as built.** Screenshots of the real screens and the design system as it is in the code. The base every new design starts from. | Only when the code changes, regenerated from it. |
| [`proposals/`](proposals/) | **Propositions for new or changed UI**, one folder each. They show what *would* change, against `current/`. | Freely, while a proposal is being designed. |
| [`kit/`](kit/) | Tooling to draw proposals in today's look: `tokens.css` mirroring `shared/.../theme/`, the icon sprite (the app's fonts load from `composeResources/`), a starter page and `render.py`. | With the theme (`tokens.css`), or when the tooling improves. |

## `current/`: what the app looks like today

- [`current/design-system.md`](current/design-system.md): colour, type, shape, spacing, layout,
  every component, the UI patterns, the catalogue of screens and how they lead to one another,
  all read from the code. When this document and the code disagree, the document is wrong.
- `current/screens/<screen>-<compact|medium|expanded>.png`: every screen at 390×844, 834×1194
  and 1440×900 (rendered at 2×), **in English**, drawn by the app's own composables from the fake
  data in `shared/src/jvmTest/.../ScreenshotFixtures.kt`. `current/sheets/` has a contact sheet
  per area and size.
- **Never edit them by hand.** `design/capture.sh` re-renders everything through
  `ScreenshotsJvmTest`, rebuilds the sheets and runs `current/check.py` (every PNG has a row in
  the catalogue, and the reverse). A new screen or state is a new `shot(...)` in
  `ScreenshotFixtures.kt` plus its catalogue row.

Code comments citing "design-system.md § Section" mean `current/design-system.md`.

## `proposals/`: designing something new

Each proposition is a folder `proposals/NNN-short-slug/` (next free number), started from
[`proposals/TEMPLATE.md`](proposals/TEMPLATE.md):

```
proposals/012-document-tags/
  proposal.md      # what and why, the screens it changes or adds, SDK needs, status
  mockups/*.html   # optional: drawn with ../../../kit/ (tokens.css, base.css, starter.html)
  screens/*.png    # the proposed screens; a changed screen keeps its current/ name
```

Rules:

1. **A proposal only adds files in its own folder.** It never edits `current/`, `kit/` or
   another proposal; it points at `../../current/screens/…` for the "before".
2. Name a changed screen like the current one it replaces (`vault-home-compact.png`), and a new
   screen with a new name in the same pattern. Cover every size the change affects.
3. Draw in the current visual language (`kit/`) unless the proposal is *about* changing it, and
   then say so in `proposal.md`.
4. Its `Status` line moves **Draft → Accepted → Implemented** (or **Dropped**).

## The lifecycle

1. **Draft**: design in the proposal's folder; iterate freely.
2. **Accepted**: the proposal is what gets built. Implementation PRs follow it.
3. **Implemented**: the PR that ships the UI also, in the same change:
   - adds or updates the `shot(...)`s in `ScreenshotFixtures.kt` for the new screens and states;
   - runs `design/capture.sh`, which regenerates `current/screens/` and `current/sheets/`;
   - updates `current/design-system.md` (components, patterns, catalogue, navigation) and,
     if the theme changed, `kit/tokens.css`;
   - sets the proposal's status to Implemented, noting the PR and anything built differently.

   From then on `current/` shows the new design, and the next proposal starts from it. A
   proposal's folder stays as the record of what was proposed; it's never updated after
   it's implemented.

[`proposals/000-original-design/`](proposals/000-original-design/) is the earlier target design
this app was being built towards (in French, on a palette and type the code doesn't use yet).
It's kept for reference and still holds ideas that aren't built; its screen IDs (O1, V1, D6…)
appear in `current/design-system.md`'s catalogue.
