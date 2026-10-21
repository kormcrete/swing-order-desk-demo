#!/usr/bin/env bash
# Runs the UI tests (*UiIT) the way CI does: Order Desk starts with the swing-ui-agent agent, the tests drive it.
# Needs a built target/ (mvn verify) and order-desk-plugin/target/ (mvn -f order-desk-plugin/pom.xml install).
# On Linux without a display it starts a virtual one (Xvfb).
# The tests start once Order Desk's window is up: with -jar it opens after the splash (the k building itself).
# --attach: start nothing, test the Order Desk already running with the agent on $SWINGUI_PORT (a download).
# Other arguments go to Maven, e.g. -Dswingui.recordedDir=<folder>: the recorded tests to run.
# Usage: ui-tests.sh [--attach] [maven args...]
set -euo pipefail
cd "$(dirname "$0")/../.."
PORT="${SWINGUI_PORT:-8770}"
AGENT=target/swing-ui-agent-agent.jar
PLUGIN=order-desk-plugin/target/classes   # Order Desk's domain steps, for the recorded tests' do steps
APP=
ATTACH=
if [ "${1:-}" = --attach ]; then ATTACH=1; shift; fi

if [ -z "$ATTACH" ]; then
  if [ "$(uname)" = Linux ] && [ -z "${DISPLAY:-}" ]; then
    Xvfb :99 -screen 0 1600x1000x24 > target/xvfb.log 2>&1 &
    XVFB=$!
    export DISPLAY=:99
    for i in $(seq 1 50); do [ -e /tmp/.X11-unix/X99 ] && break; sleep 0.2; done   # the app needs the display up
    if [ ! -e /tmp/.X11-unix/X99 ]; then echo "Xvfb did not start:"; cat target/xvfb.log; exit 1; fi
  fi
  java -javaagent:"$AGENT"=port="$PORT",palette=off,plugins="$PLUGIN" -Dorderdesk.today=2026-10-12 -jar target/order-desk.jar > target/app.log 2>&1 &
  APP=$!
  trap 'kill "$APP" 2>/dev/null || true; if [ -n "${XVFB:-}" ]; then kill "$XVFB" 2>/dev/null || true; fi' EXIT
fi

for i in $(seq 1 60); do
  if java -jar "$AGENT" --port "$PORT" session > /dev/null 2>&1; then echo "Order Desk is ready (${i}s)"; break; fi
  if [ -n "$APP" ] && ! kill -0 "$APP" 2>/dev/null; then echo "Order Desk stopped:"; cat target/app.log; exit 1; fi
  if [ "$i" = 60 ]; then
    echo "Order Desk did not answer on port $PORT in 60 s"
    if [ -n "$APP" ]; then cat target/app.log; fi
    exit 1
  fi
  sleep 1
done

# the agent answers before main() runs; the window opens after the splash
for i in $(seq 1 30); do
  SNAPSHOT=$(java -jar "$AGENT" --port "$PORT" snapshot 2>&1 || true)
  if echo "$SNAPSHOT" | grep -q '"role":"window","name":"Order Desk"'; then echo "Order Desk's window is up (${i}s)"; break; fi
  if [ "$i" = 30 ]; then
    echo "Order Desk's window did not open in 30 s:"
    echo "$SNAPSHOT"
    if [ -n "$APP" ]; then cat target/app.log; fi
    exit 1
  fi
  sleep 1
done

mvn -B -Pui -Dswingui.agents="127.0.0.1:$PORT" "$@" failsafe:integration-test failsafe:verify
