# Fonts used by the reference screens

- **Instrument Sans** (400, 500, 600, 700) — SIL Open Font License 1.1, from the
  `@fontsource/instrument-sans` package (upstream: github.com/Instrument/instrument-sans).
- **IBM Plex Mono** (400, 500) — SIL Open Font License 1.1, from the app's bundled
  `plex_mono_regular.ttf` and the `@fontsource/ibm-plex-mono` package.

They're here only so `render.py` draws the screens with the real faces. The app bundles its own
copies under `shared/src/commonMain/composeResources/font/` (licenses in
`shared/THIRD_PARTY_LICENSES.md`).
