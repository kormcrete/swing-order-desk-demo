#!/usr/bin/env bash
# Checks a download the way a user gets it: starts the packaged app (from the mounted .dmg, or the unzipped folder),
# checks that its agent loaded Order Desk's plugin and keeps the recorded tests that came with it read-only, runs the
# UI tests against it on port 8770 (all Java ones and every recorded test in uitest/), then the recorded tests the
# download carries, again on the same app (the plugin's reset makes that work), and checks the agent's listening line
# in its log file (log=), or in the console for the Java zip. Needs a built target/ (mvn verify).
# macOS: bash. Windows: Git Bash (the runner's "shell: bash"). The Java zip: any OS with a display.
# Usage: check-download.sh <kormcrete-order-desk-macos-<arch>.dmg | kormcrete-order-desk-windows-x64.zip
#                           | kormcrete-order-desk-java.zip>
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
    BUNDLED="$VOLUME/$NAME.app/Contents/Resources/uitest"
    ;;
  *-java.zip)
    DIR=$(mktemp -d)
    unzip -q "$FILE" -d "$DIR"
    RUN="$DIR/$NAME/order-desk"
    [ -x "$RUN" ] || { echo "no executable $NAME/order-desk in $FILE" >&2; exit 1; }
    LOG="$DIR/console.log"
    # APP_JAVA_HOME: the Java the app runs on (the tests stay on JAVA_HOME)
    JAVA_HOME="${APP_JAVA_HOME:-${JAVA_HOME:-}}" "$RUN" > "$LOG" 2>&1 &
    APP=$!
    trap 'echo "--- $LOG"; cat "$LOG" 2>/dev/null || true; kill "$APP" 2>/dev/null || true' EXIT
    BUNDLED="$DIR/$NAME/uitest"
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
    BUNDLED="$DIR/$NAME/app/uitest"
    ;;
  *) echo "usage: $0 <.dmg | .zip>" >&2; exit 2 ;;
esac

# the recorded tests the download carries: the same files as uitest/
RECORDED=target/download-uitest
rm -rf "$RECORDED"
mkdir -p "$RECORDED"
cp "$BUNDLED"/*.json "$RECORDED/"
diff -r uitest "$RECORDED" || { echo "the download's recorded tests differ from uitest/" >&2; exit 1; }
echo "Recorded tests in the download:" $(cd "$RECORDED" && ls | sed 's/\.json$//')

SWINGUI_PORT=8770 .github/scripts/ui-tests.sh --attach
# what the agent was started with: Order Desk's plugin ready, the bundled recorded tests read-only
SESSION=$(java -jar target/swing-ui-agent-agent.jar --port 8770 session)
echo "$SESSION" | grep -q '"recordedReadOnly"[[:space:]]*:[[:space:]]*true' \
  || { echo "the download's recorded tests are not read-only: $SESSION" >&2; exit 1; }
echo "$SESSION" | grep -q 'OrderDeskPlugin' && echo "$SESSION" | grep -q '"state"[[:space:]]*:[[:space:]]*"ready"' \
  || { echo "the download did not load Order Desk's plugin: $SESSION" >&2; exit 1; }
# again, on the app the tests above left behind: each starts from the plugin's reset
SWINGUI_PORT=8770 .github/scripts/ui-tests.sh --attach -Dit.test=RecordedUiIT -Dswingui.recordedDir="$RECORDED"
if ! grep -q "swing-ui-agent listening on http://127.0.0.1:8770/mcp" "$LOG"; then
  echo "the agent's line is missing from $LOG:"
  cat "$LOG" || true
  exit 1
fi
echo "Download checked: $FILE"
