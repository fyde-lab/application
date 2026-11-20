---
name: run-fyde-application
description: Build, run, and drive the fyde-application Compose Multiplatform desktop app under a headless X server (Xvfb). Use when asked to run, build, launch, screenshot, or interact with the fyde desktop app, or to confirm a UI change works.
---

Fyde's desktop app is Compose Multiplatform (JVM/Skia, via `:desktopApp:run`,
entry point `com.peltoche.fyde.MainKt`). It has no headless/API mode — for
agent use, drive it as a real GUI under Xvfb via
`.claude/skills/run-fyde-application/driver.sh`, which wraps `xdotool` +
`imagemagick` for input/screenshots.

All paths below are relative to `application/` (this skill's grandparent
directory). The build itself needs `application/shell.nix` (JDK 21, the
pinned Rust toolchain `sdkBridge` uses to cross-compile `../sdk`); the driver's
X11 tooling (`xorg-server`, `xdotool`, `imagemagick`, `fluxbox`) is pulled
from nix on demand by the script itself and is NOT part of `shell.nix`.

## Prerequisites

Nothing to install manually — `nix-shell -p xorg-server xdotool imagemagick fluxbox`
(used internally by the driver) and `nix-shell` (application/shell.nix, used
for the Gradle build) fetch everything needed.

## Build

```bash
cd application
nix-shell --run "./gradlew :desktopApp:compileKotlin --console=plain"
```

First run triggers an `sdkBridge` cargo build of `../sdk` (~2-4 min); cached
after that. If this fails with an rustc-version error, `application/shell.nix`'s
pinned Rust toolchain is older than `../sdk`'s dependencies need — bump it to
match `../sdk/shell.nix`.

## Run (agent path)

Four steps, each backgrounded by the caller (the driver subcommands run in
the foreground and block):

```bash
cd application
rm -f /tmp/.X11-unix/X99 /tmp/.X99-lock   # clear a stale Xvfb socket/lock, see Gotchas

# 1. Start the X server (background; runs until killed)
./.claude/skills/run-fyde-application/driver.sh xvfb-start &

# 2. Start a window manager — REQUIRED, see Gotchas (background; runs until killed)
./.claude/skills/run-fyde-application/driver.sh wm-start &

# 3. Launch the app (background; runs until killed)
./.claude/skills/run-fyde-application/driver.sh launch &

# 4. Wait for the window, then drive it
./.claude/skills/run-fyde-application/driver.sh wait-window 90
./.claude/skills/run-fyde-application/driver.sh shot landing
```

(In a real agent harness, run each of 1-3 via a background-process tool
rather than shell `&`, so you get a clean way to check output/kill them —
that's how this skill was built and verified.)

### Driver commands

| command | what it does |
|---|---|
| `xvfb-start` | start Xvfb on `:99` (foreground — background it) |
| `wm-start` | start fluxbox on `:99` (foreground — background it) |
| `build` | `gradlew :desktopApp:compileKotlin` sanity build |
| `launch` | `gradlew :desktopApp:run` against `:99` (foreground — background it) |
| `wait-window [timeout]` | poll until the "Fyde" window exists (default 90s) |
| `shot [name]` | screenshot **the app window only** → `/tmp/fyde-app-shots/<name>.png`. Because it captures just the window (not the full desktop), pixel `(x,y)` in the saved image *is* the content-relative coordinate to pass to `click`. |
| `click X Y` | move mouse to content-relative `(X,Y)` and click — read X,Y off a `shot` image |
| `type TEXT` | type `TEXT` via keyboard (click a field first) |
| `key KEYNAME` | send a key, e.g. `Return`, `Escape`, `Tab` |
| `geometry` | print window position/size |
| `quit` | kill the running app process |
| `stop` | kill fluxbox + Xvfb |

Override the display (`FYDE_DISPLAY`, default `:99`) or screenshot dir
(`SHOT_DIR`, default `/tmp/fyde-app-shots`) via env vars if needed.

### Example: search box interaction

Verified this session — screenshot, click into the search field, type, see
the reactive "no matches" state, click "Clear search":

```bash
./.claude/skills/run-fyde-application/driver.sh shot landing
# open /tmp/fyde-app-shots/landing.png, read off the search field's center, e.g. (195,100)
./.claude/skills/run-fyde-application/driver.sh click 195 100
./.claude/skills/run-fyde-application/driver.sh type "invoice"
./.claude/skills/run-fyde-application/driver.sh shot after-search   # shows "No matches"
```

## Run (human path)

```bash
cd application
nix-shell --run "./gradlew :desktopApp:run"
```

Opens a real window. Useless on a headless machine — use the agent path.

## Gotchas

- **Without a window manager, clicks and typed text silently go nowhere.**
  AWT/Skiko needs something to grant the window input focus; a bare Xvfb
  (no WM) never does. `wm-start` (fluxbox) fixes this — and once it's
  running, fluxbox's default "focus new windows" behavior means you do
  **not** need `xdotool windowactivate`/`windowfocus` at all. Those two
  commands were tried and both throw a `BadWindow` X protocol error against
  the app's own content window here (`getwindowname`/`getwindowgeometry`
  on the same id succeed seconds apart — root cause unconfirmed, possibly
  a window recreation during the GL→software fallback below). `click` in
  this driver therefore skips activation entirely and just does
  `mousemove --window <id> X Y click 1`, which works reliably.

- **`org.jetbrains.skiko.RenderException: Cannot create Linux GL context`**
  on every launch under Xvfb — expected, not fatal. Xvfb has no GPU; Skiko
  logs `[SKIKO] warn: Fallback to next API` and renders in software. The
  app comes up fully functional a couple seconds later.

- **Screenshot the window, not the root display.** `shot` uses
  `import -window <id>`, not `-window root`. This was the single biggest
  time-sink while building this driver: with a full-desktop screenshot you
  must subtract the window's `(X,Y)` offset (which *changes* once a WM adds
  a title bar) from every click coordinate, and it's easy to get it wrong.
  Capturing just the window makes screenshot pixels == click coordinates,
  no math, no WM-decoration drift.

- **Stale Xvfb socket/lock** (`Fatal server error: Server is already active
  for display 99` or `Cannot establish any listening sockets`) after a
  previous session didn't exit cleanly: `rm -f /tmp/.X11-unix/X99
  /tmp/.X99-lock` before `xvfb-start`.

- **The app opens on "Fyde is already open"** instead of its usual screens:
  fyde-sdk refused to start (`ClientAlreadyRunning`, FYDE-1012) because
  another instance holds the `flock` on `fyde.db.lock` next to the desktop
  database (`~/.local/share/fyde/desktop/`). Only one instance may run at a
  time: kill the other `MainKt` process (`pgrep -f MainKt`), then press
  Retry. The OS drops the lock with its process, so a stale lock file left
  by a crash never blocks a launch.

## Troubleshooting

- **`rustc X.Y.Z is not supported by …`** during `build`/`launch`:
  `application/shell.nix`'s pinned rustc is older than one of `../sdk`'s
  dependencies requires. Bump the version in `application/shell.nix` to match
  `../sdk/shell.nix`.
- **`wait-window` times out:** check the `launch` background task's output —
  a real build error looks like a Gradle `BUILD FAILED` block, not the
  harmless GL warning above.
- **Click/type has no visible effect:** you probably skipped `wm-start`, or
  coordinates were read off a full-desktop screenshot instead of a
  window-only one from `shot`. Re-screenshot with `shot` and re-read the
  coordinates from that image.
