#!/usr/bin/env bash
# Builds kormcrete-order-desk-java.zip from target/ (mvn package) and order-desk-plugin/target/: a "Kormcrete Order
# Desk" folder with the jars, Order Desk's plugin, its recorded tests (uitest, read-only for the agent), the order-desk
# (macOS, Linux) and order-desk.cmd (Windows) launchers, LICENSE and NOTICE. Runs on your own Java 8+.
# Usage: packaging/java/build-java-zip.sh <out-dir>
set -euo pipefail
fail() { echo "build-java-zip: $*" >&2; exit 2; }
[ $# -eq 1 ] || fail "usage: $0 <out-dir>"
mkdir -p "$1"
OUT=$(cd "$1" && pwd)
cd "$(dirname "$0")/../.."
for f in target/order-desk.jar target/swing-ui-agent-agent.jar order-desk-plugin/target/order-desk-plugin.jar; do
  [ -f "$f" ] || fail "missing $f (run mvn -B -f order-desk-plugin/pom.xml install and mvn -B package first)"
done

NAME="Kormcrete Order Desk"
WORK=target/java
rm -rf "$WORK"
mkdir -p "$WORK/$NAME"
cp target/order-desk.jar target/swing-ui-agent-agent.jar order-desk-plugin/target/order-desk-plugin.jar \
  packaging/java/order-desk packaging/java/order-desk.cmd LICENSE NOTICE "$WORK/$NAME/"
mkdir "$WORK/$NAME/uitest"
cp uitest/*.json "$WORK/$NAME/uitest/"
chmod 755 "$WORK/$NAME/order-desk"
ZIP="$OUT/kormcrete-order-desk-java.zip"
rm -f "$ZIP"
(cd "$WORK" && zip -q -r -X "$ZIP" "$NAME")
echo "$ZIP"
