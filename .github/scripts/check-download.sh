#!/usr/bin/env bash
# Checks a download the way a user gets it: starts the packaged app (from the mounted .dmg, or the unzipped folder),
# runs the UI tests against its agent on port 8770, and checks the agent's listening line in its log file (log=).
# Needs a built target/ (mvn verify). macOS: bash. Windows: Git Bash (the runner's "shell: bash").
# Usage: check-download.sh <kormcrete-order-desk-macos-<arch>.dmg | kormcrete-order-desk-windows-x64.zip>
set -euo pipefail
cd "$(dirname "$0")/../.."
[ $# -eq 1 ] && [ -f "$1" ] || { echo "usage: $0 <.dmg | .zip>" >&2; exit 2; }
FILE=$1
NAME="Kormcrete Order Desk"

case "$FILE" in
  *.dmg)
    VOLUME=$(hdiutil attach -nobrowse -readonly "$FILE" | tail -1 | awk -F'\t' '{print $NF}')
    LOG="$HOME/Library/Logs/$NAME/agent.log"
    rm -f "$LOG"
    stop() {
      pkill -f "$NAME.app/Contents/Java" || true
      sleep 1
      hdiutil detach "$VOLUME" > /dev/null || true
    }
    trap 'echo "--- $LOG"; cat "$LOG" 2>/dev/null || true; stop' EXIT
    open "$VOLUME/$NAME.app"
    ;;
  *.zip)
    DIR=$(mktemp -d)
    unzip -q "$FILE" -d "$DIR"
    EXE="$DIR/$NAME/$NAME.exe"
    [ -f "$EXE" ] || { echo "no $NAME/$NAME.exe in $FILE" >&2; exit 1; }
    LOG="$(cygpath -u "$LOCALAPPDATA")/$NAME/agent.log"
    rm -f "$LOG"
    stop() {
      taskkill //F //T //IM "$NAME.exe" > /dev/null 2>&1 || true
      taskkill //F //IM javaw.exe > /dev/null 2>&1 || true
    }
    trap 'echo "--- $LOG"; cat "$LOG" 2>/dev/null || true; stop' EXIT
    powershell -NoProfile -Command "Start-Process -FilePath '$(cygpath -w "$EXE")'"
    ;;
  *) echo "usage: $0 <.dmg | .zip>" >&2; exit 2 ;;
esac

SWINGUI_PORT=8770 .github/scripts/ui-tests.sh --attach
if ! grep -q "swing-ui-agent listening on http://127.0.0.1:8770/mcp" "$LOG"; then
  echo "the agent's line is missing from $LOG:"
  cat "$LOG" || true
  exit 1
fi
echo "Download checked: $FILE"
