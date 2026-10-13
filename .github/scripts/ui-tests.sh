#!/usr/bin/env bash
# Runs the UI tests (*UiIT) the way CI does: Order Desk starts with the swing-ui-agent agent, the tests drive it.
# Needs a built target/ (mvn verify). On Linux without a display it starts a virtual one (Xvfb).
set -euo pipefail
cd "$(dirname "$0")/../.."
PORT="${SWINGUI_PORT:-8770}"
AGENT=target/swing-ui-agent-agent.jar

if [ "$(uname)" = Linux ] && [ -z "${DISPLAY:-}" ]; then
  Xvfb :99 -screen 0 1600x1000x24 > target/xvfb.log 2>&1 &
  XVFB=$!
  export DISPLAY=:99
  for i in $(seq 1 50); do [ -e /tmp/.X11-unix/X99 ] && break; sleep 0.2; done   # the app needs the display up
  if [ ! -e /tmp/.X11-unix/X99 ]; then echo "Xvfb did not start:"; cat target/xvfb.log; exit 1; fi
fi
java -javaagent:"$AGENT"=port="$PORT" -Dorderdesk.today=2026-10-12 -jar target/order-desk.jar > target/app.log 2>&1 &
APP=$!
trap 'kill "$APP" 2>/dev/null || true; if [ -n "${XVFB:-}" ]; then kill "$XVFB" 2>/dev/null || true; fi' EXIT

for i in $(seq 1 60); do
  if java -jar "$AGENT" --port "$PORT" session > /dev/null 2>&1; then echo "Order Desk is ready (${i}s)"; break; fi
  if ! kill -0 "$APP" 2>/dev/null; then echo "Order Desk stopped:"; cat target/app.log; exit 1; fi
  if [ "$i" = 60 ]; then echo "Order Desk did not answer on port $PORT in 60 s:"; cat target/app.log; exit 1; fi
  sleep 1
done

mvn -B -Pui -Dswingui.port="$PORT" failsafe:integration-test failsafe:verify
