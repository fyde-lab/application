# 000 · Original design

**Status:** Partly implemented (reference)

The design `design/` held before it was rebuilt from the app (see [`../../README.md`](../../README.md)):
114 hand-drawn reference screens in French (`assets/screens/`), their HTML sources
(`assets/mockups/`), contact sheets and screen maps, and the spec they were drawn to
([`design-system.md`](design-system.md), [`screen-map.md`](screen-map.md),
[`design-system-extraction.md`](design-system-extraction.md)), moved here unchanged.

The app was built towards it screen by screen: the layouts, copy and flows of most screens are
in the app (see the screen IDs in [`../../current/design-system.md`](../../current/design-system.md)
§ Screens), but its visual language (palette, Instrument Sans, radii) never was, and several
screens aren't built at all (listed in § Not in the app there).

Use it for ideas and wording. A new proposal that picks up one of its unbuilt screens draws it
again in its own folder, against `current/`.

Its tooling still works from here (`assets/mockups/render.py`, `sheets.py`,
`assets/maps/screen_map.py`; see the paths in its `design-system.md`, which are relative to this
folder). Nothing is to be updated in it.
