#!/usr/bin/env bash
# Driver for the fyde-application Compose Desktop app under a headless X server.
# Run from application/ (the directory this skill lives under, two levels up
# from this script). State (display number, window id, PIDs) is kept in
# $STATE_DIR so repeated invocations of this script can share one session.
#
# Subcommands:
#   xvfb-start              start Xvfb on :99 (run with run_in_background / nohup'd by caller)
#   wm-start                start fluxbox on :99 (needed for click/focus/type to work — see Gotchas)
#   build                   gradlew :desktopApp:compileKotlin (sanity-check build only)
#   launch                  gradlew :desktopApp:run against :99, backgrounded by caller
#   wait-window [timeout]   poll until the "Fyde" window exists (default 90s — first launch
#                            triggers an sdkBridge cargo build of ../sdk, which is slow)
#   shot [name]             screenshot the app window only -> $SHOT_DIR/<name>.png
#                            (pixel (x,y) in this image == content-relative coords for click)
#   click X Y               move mouse to content-relative (X,Y) inside the window and click
#   type TEXT               type TEXT via xdotool (click a field first)
#   key KEYNAME              send a key, e.g. Return / Escape / Tab
#   geometry                print the window's position/size
#   quit                    kill the running app (MainKt) process
#   stop                    kill fluxbox + Xvfb
#
# All driving subcommands (shot/click/type/key/geometry/wm-start) pull
# xorg-server+xdotool+imagemagick+fluxbox from nix on demand — they are NOT
# part of application/shell.nix (that shell is for building, not for driving
# a GUI under Xvfb), so each call is wrapped in its own `nix-shell -p`.
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
STATE_DIR="${DRIVER_STATE_DIR:-/tmp/fyde-app-driver}"
SHOT_DIR="${SHOT_DIR:-/tmp/fyde-app-shots}"
DISPLAY_NUM="${FYDE_DISPLAY:-:99}"
mkdir -p "$STATE_DIR" "$SHOT_DIR"

DRIVE_PKGS="xorg-server xdotool imagemagick fluxbox"

drive() { # run a command with DISPLAY set and the X11 driving tools on PATH
  nix-shell -p $DRIVE_PKGS --run "DISPLAY=$DISPLAY_NUM $*"
}

find_window() {
  drive "xdotool search --name '^Fyde\$' | head -1"
}

cmd="${1:-}"; shift || true

case "$cmd" in
  xvfb-start)
    exec nix-shell -p xorg-server xvfb-run --run "Xvfb $DISPLAY_NUM -screen 0 1280x800x24 -nolisten tcp"
    ;;

  wm-start)
    exec nix-shell -p fluxbox --run "DISPLAY=$DISPLAY_NUM fluxbox"
    ;;

  build)
    cd "$APP_DIR"
    exec nix-shell --run "./gradlew :desktopApp:compileKotlin --console=plain"
    ;;

  launch)
    cd "$APP_DIR"
    exec nix-shell --run "DISPLAY=$DISPLAY_NUM ./gradlew :desktopApp:run --console=plain"
    ;;

  wait-window)
    timeout="${1:-90}"
    elapsed=0
    while [ -z "$(find_window)" ]; do
      sleep 2; elapsed=$((elapsed+2))
      if [ "$elapsed" -ge "$timeout" ]; then
        echo "TIMEOUT: no 'Fyde' window after ${timeout}s" >&2
        exit 1
      fi
    done
    echo "window found: $(find_window)"
    ;;

  shot)
    name="${1:-shot-$(date +%s)}"
    wid="$(find_window)"
    [ -n "$wid" ] || { echo "ERROR: no Fyde window — launch first" >&2; exit 1; }
    drive "import -window $wid $SHOT_DIR/$name.png"
    echo "screenshot: $SHOT_DIR/$name.png"
    ;;

  click)
    x="${1:?x}"; y="${2:?y}"
    wid="$(find_window)"
    [ -n "$wid" ] || { echo "ERROR: no Fyde window — launch first" >&2; exit 1; }
    drive "xdotool mousemove --window $wid $x $y click 1"
    ;;

  type)
    text="${1:?text}"
    drive "xdotool type --delay 50 -- '$text'"
    ;;

  key)
    key="${1:?keyname}"
    drive "xdotool key $key"
    ;;

  geometry)
    wid="$(find_window)"
    [ -n "$wid" ] || { echo "ERROR: no Fyde window — launch first" >&2; exit 1; }
    drive "xdotool getwindowgeometry --shell $wid"
    ;;

  quit)
    pkill -f com.peltoche.fyde.MainKt 2>/dev/null || true
    echo "quit sent"
    ;;

  stop)
    pkill -f "fluxbox" 2>/dev/null || true
    pkill -f "Xvfb $DISPLAY_NUM" 2>/dev/null || true
    echo "stopped"
    ;;

  *)
    echo "usage: $0 {xvfb-start|wm-start|build|launch|wait-window|shot|click|type|key|geometry|quit|stop}" >&2
    exit 1
    ;;
esac
