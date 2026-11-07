# Design-system extraction audit

What the screens build for themselves that belongs in the design system
(`shared/.../theme/`, `components/`, `layout/`), found by reading every screen file under
`shared/src/commonMain/kotlin/com/peltoche/fyde/`. Scope: **moving code, not changing it.** Every
extraction keeps today's pixels; the token migration described in
[`design-system.md`](design-system.md) § Status is a separate, later piece of work, and none of
the items below anticipate it.

Parity is checked with `ScreenshotsJvmTest` (`shared/src/jvmTest`): it renders every screen in
fixed fake states at Compact (390×844), Medium (834×1194) and Expanded (1440×900) to
`$FYDE_SCREENSHOTS_DIR`, and each extraction must leave those PNGs identical
(`compare -metric AE`).

A candidate qualifies when it has two call sites or more, or is plainly a design-system
primitive (an icon, a token). Ranked by value against risk.

## Findings

### 1. The design system depends on the screens' package (glyphs) — high value, low risk

**Done:** the three files are in `icons/` (`com.peltoche.fyde.icons`); screenshots unchanged.

`components/` imports 9 glyphs from `com.peltoche.fyde` (`AlertGlyph`, `CheckGlyph`,
`ChevronRightGlyph`, `StopGlyph`, `ChevronLeftGlyph`, `ClockGlyph`, `CloseGlyph`, `EyeGlyph`,
`LockGlyph`), so the design system can't be read or reused without the app. All 46 glyphs live
beside the screens:

| File | Glyphs |
|---|---|
| `NavGlyphs.kt` | 32 (search, chevrons, alert, check, stop, devices, key, shield, …) |
| `CategoryGlyphs.kt` | 13 (home, wallet, briefcase, …) + the private `PathGlyph` |
| `DocumentGlyph.kt` | 1 |

**Proposal:** a new `icons/` package (`com.peltoche.fyde.icons`) holding these three files
unchanged except for the package line; every importer updated. `FydeLogo.kt` (the brand mark) and
`VaultCategories.kt`'s `CategoryGlyph(category)` (maps an SDK `FydeCategory` to a glyph: domain
logic) stay where they are. `components/navigation/AppTopBar.kt`'s private `BackChevron` and
`DesignSystemScreen.kt`'s `DotIcon` stay private (one caller each).

### 2. Surface groups and their rows, defined in `SettingsScreen.kt` and used everywhere — high value, low risk

**Done:** `AppGroup`, `AppGroupCard` and `AppGroupRow`. `ClickableRow` is gone; `SettingsLinkRow`,
`SettingRow`, `FactRow` and `SessionRow` lay out their content inside `AppGroupRow`.
`SessionChoice` keeps its own row: its `selectable` uses the default indication and no fill, so
moving it would change its feedback; no `SessionDeviceRow` was extracted. Screenshots unchanged.

`SettingsGroup` (a `surface` column, `AppRadius.lg`, hairline border) and `SettingsCard` (the
same with `space5` padding) are the app's actual card: 20 call sites across Settings, Sessions, PlanUsage, the Sources tab and the
Vault's home, category, search and document pages. The design system's own `AppCard` draws something else
(`AppShapes.card` = 6dp radius, no border) and is used only by `SourceFetchingScreen`,
`SourcesTabScreen` and the gallery.

Their rows are rebuilt per screen with the same padding (`space5` × `space3`), spacing (`space4`)
and min heights:

| Row | Where | What it is |
|---|---|---|
| `SettingsLinkRow` | `SettingsScreen.kt:566` | icon, label over supporting, value, chevron; clickable or selectable |
| `ClickableRow` | `SourcesTabScreen.kt:1183` | clickable slot row with a min height and background |
| `SettingRow` + `SettingText` | `SourcesTabScreen.kt:1016` | label over a 2-line value, optional trailing |
| `FactRow` | `VaultDocumentPage.kt:254` | muted label, value at the end |
| `SessionRow` / `SessionChoice` | `SessionsSection.kt:153`, `SessionLimitScreen.kt:311` | device glyph, name over "since", trailing / radio |

**Proposal:**
- `components/content/AppGroup.kt`: `AppGroup` (was `SettingsGroup`) and `AppGroupCard` (was
  `SettingsCard`), same parameters.
- `components/content/AppGroupRow.kt`: `AppGroupRow`, the shared row container (padding, spacing,
  min height, optional `onClick` or `selected`, optional background) that `SettingsLinkRow` and
  `ClickableRow` become thin callers of; `SettingRow`/`FactRow` built on it. The two session rows
  share a `SessionDeviceRow` content piece but stay in their screens (they take `FydeOpenSession`).
- **Not** merged into `AppCard`/`AppListItem`: that would change radius, border, row heights and
  typography (M3 `ListItem`). Recorded below as a divergence for the token migration.

### 3. Skeleton list rows — medium value, low risk

**Done:** `AppListSkeleton`; `SkeletonRows`, `DocumentRowSkeletons` and `SessionsSkeleton` keep
their names and pass their proportions to it. Screenshots unchanged.

Three copies of "group of N rows: tile + two bars" plus one card-shaped skeleton:

| Composable | Rows | Row height | Tile | Bar widths | Bar gap |
|---|---|---|---|---|---|
| `SkeletonRows` (`SourcesTabScreen.kt:1225`) | 3 | comfortable (72) | `iconTile` 44, `AppShapes.iconTile` | 40% / 60% | `space2` |
| `DocumentRowSkeletons` (`VaultScreen.kt:562`) | 4 (param) | standard (64) | `iconTile` 44, default `AppRadius.sm` | 60% / 35% | `space3` |
| `SessionsSkeleton` (`SessionsSection.kt:190`) | 2 | comfortable (72) | 24dp | 160dp / 120dp, heights 18/14 | `space2` spacer |
| `PlanUsageSkeleton` (`PlanUsageSection.kt:128`) | — | card + 3 meters | — | — | — |

**Proposal:** `components/feedback/AppListSkeleton.kt`: `AppListSkeleton(rows, rowHeight, tileSize,
tileShape, firstLine, secondLine, lineGap)` with each difference kept as a parameter (the table's
values passed at each call site, no unification). `PlanUsageSkeleton` stays: one caller, shaped like
its page.

### 4. Centred page bar, three copies — medium value, low risk

**Done:** `AppPageBar`; `VaultPageBar` and `PageBar` are gone, `DrillDownBar` passes its height, fill
and margin as a modifier. Screenshots unchanged.

`VaultPageBar` (`VaultScreen.kt`), `PageBar` (`SourcesTabScreen.kt:1155`) and `DrillDownBar`
(`SettingsScreen.kt:426`): the back circle at the start, a centred `bodyEmphasis` title padded by
`backButton + space3`, an optional trailing action. Differences: `PageBar` takes `enabled`;
`DrillDownBar` has a fixed (not min) height, an `AppColors.background` fill and the Compact screen
margin, and no trailing slot.

**Proposal:** `components/navigation/AppPageBar.kt`: `AppPageBar(title, onBack, modifier,
enabled = true, trailing = null)`; `DrillDownBar` calls it with its own modifier (height,
background, padding). Distinct from `AppTopBar` (start-aligned title, size-class heights, divider):
both stay.

### 5. Section title, error note and retry — medium value, low risk

**Done:** `AppSectionTitle`, `AppErrorNote`, `AppRetryButton`; screenshots unchanged.

Defined in `SettingsScreen.kt` and imported by 3–4 other screens:

- `SectionTitle` (`section` style, `space7` above unless first, `space3` below) → used by
  Sources, Vault, Search.
- `SettingsErrorNote` and `SourcesTabScreen.kt`'s private `ErrorNote`: **identical** (`AppNote`,
  `Error`, `AlertGlyph` at `AppIconSize.sm`). Used by Settings, PlanUsage, Sessions, Vault, Sources.
- `RetryButton` (outlined `AppButton`, "Réessayer", `space4` above) → PlanUsage, Sessions, Vault.

**Proposal:** `AppSectionTitle` (`components/content/`), `AppErrorNote` (`components/feedback/`,
an `AppNote` overload), `AppRetryButton` (`components/action/`). Not merged: `SourcesScreen.kt`'s
`SourcesHeading` and `SettingsScreen.kt`'s `CompactPageTitle` (one caller each, different styles),
`ThemePreviews.kt`'s `GallerySectionTitle` (preview-only).

### 6. Meters — low-medium value, low risk

- `PasswordStrengthMeter` (`RegisterScreen.kt:443`, also used by Settings' Changer le mot de
  passe): four segments, hand-drawn at raw `4.dp` high with a raw `RoundedCornerShape(2.dp)`
  (= `AppRadius.xs`).
- `UsageMeter` + `MeterCaption` (`PlanUsageSection.kt`): label and "used / limit" over
  `AppProgressBar`, a caption past the limit. One caller (three meters on one page).

**Proposal:** move `PasswordStrengthMeter`'s bar row into `components/feedback/AppSegmentedMeter.kt`
(`segments`, `lit`, `color`), keeping the wording in the screen; the `4.dp` becomes
`AppDimensions.segmentedMeterHeight`, the shape `AppRadius.xs`. `UsageMeter` stays (one caller,
its levels are plan logic).

### 7. Other shared pieces in screen files — low value, low risk

- `ScriptIcon` (`SourcesTabScreen.kt:1324`, used by 4 screens): a script's icon tile from its
  bytes, falling back to a glyph → `components/content/AppIconTile.kt` already holds the tile;
  `ScriptIcon` goes beside it as `AppImageTile(bytes, size)` (it takes `ByteArray`, no SDK type).
- `CenteredScrollColumn` (`AuthBrandPanel.kt:509`, 9 screens: every sign-in and vault-creation
  form): a layout primitive → `layout/`.

### 8. Raw values outside the design system — medium value, low risk

Literals in screen files that have no token, or duplicate one:

| Value | Where | Proposal |
|---|---|---|
| `24.dp` glyph | `SessionsSection` ×2, `SessionLimitScreen`, `SettingsScreen:528`, `InsecureStorageSheet:83`, `AppSidebar` ×3 | `AppIconSize.lgRow = 24.dp` (comments call it "the mockup's `.ic.lg`, off AppIconSize's scale") |
| `display.copy(fontSize = 38.sp, lineHeight = 44.sp)` | `AuthBrandPanel.kt:128`, `:203` | `AppTypography.brandHeadline` |
| Sidebar `10.dp`, `14.dp`, `18.dp`, `6.dp`; `SidebarRowHeight 40`, `RailCellSize 48`, `RailCellShape 12`, `SelectionBarWidth 3` | `AppSidebar.kt` | `AppDimensions.sidebar*` named tokens (values unchanged; 10/14/18 are off the spacing scale) |
| `SidebarWidth 260`, `SidebarRailWidth 80` | `AppSidebar.kt:351` | `AppDimensions.sidebarWidth`, `railWidth` (the design names them) |
| `StepDoneFill #8FD1AE`, `StepDoneInk #0B2418` | `AuthBrandPanel.kt:297` | `AppColors` brand-panel step colors (both themes) |
| `micro.copy(SemiBold, letterSpacing 0.72.sp)` | `AppSidebar.kt:127` | `AppTypography.overline` |
| `RoundedCornerShape(16.dp)` source card, `340.dp` grid | `SourcesScreen.kt:457` | `AppShapes.sourceCard`, `AppDimensions.sourceGridHeight` |
| `RoundedCornerShape(12.dp)` word cell, cell sizes | `RecoveryKitScreen.kt:367–427` | leave: one screen, the recovery grid's own geometry |
| `AppearanceTileHeight 92` | `SettingsScreen.kt:884` | leave: one caller |
| Skeleton sizes `80/120/160.dp`, `14/16/18/22.dp` | `PlanUsageSection`, `SessionsSection` | covered by 3 (parameters at call sites) |
| `CircularProgressIndicator(strokeWidth = 2.dp)` | `RecoveryKitScreen:309`, `SourcesScreen:588` | `AppLoading` already exists, but with a different size: leave, note below |

## What stays in the screens, and why

- Screen-specific compositions: `SealCard`, `SealLine`, `SealRow` (three different statements of
  the seal, each one caller), `CategoryTile`, `RailCell`, `SidebarRow` (sidebar-only),
  `RecoveryWordCell`/`RecoveryWordGrid`, `SearchBar`/`SearchLauncher`, `SortButton`/`SortHeader`,
  `MonthHeading`, `TypeShortcuts`, `VaultDocumentRow` (takes `FydeDocument`).
- The two `SourceRow`s: `SourcesScreen`'s is a checkbox row with a 45% disabled state,
  `SourcesTabScreen`'s a navigation row with a status line. Different controls.
- The two `StepDisc`s: `AuthBrandPanel`'s is drawn on the brand panel's `primary` fill
  (white / outlined / mint), `RunStepper`'s is the run's five states. Different controls.
- Empty/failed/loading states per screen (`EmptySources`, `NoSources`, `SourcesFailed`,
  `PreviewUnavailable`, …): each already composes `AppEmptyState`/`AppErrorState`/`AppNote` with
  its own copy.
- Domain mappers (`CategoryGlyph`, `SourceStatusLine`, `sealedText`, …): they turn SDK types into
  design-system calls, which is the screen's job.

## Divergences recorded for the token migration (not touched here)

- `AppCard` (6dp, borderless) vs the `SettingsGroup` the screens use (10dp, hairline border).
- `AppListItem` (M3 `ListItem`, unused by screens) vs the hand-built rows of finding 2.
- `AppLoading` vs two inline `CircularProgressIndicator(strokeWidth = 2.dp)` at other sizes.
- `24.dp` glyphs off `AppIconSize`'s scale (finding 8).

## Order

1 → 5 → 2 → 3 → 4 → 8 → 6 → 7, one commit each, each checked against the baseline screenshots.
