#!/usr/bin/env bash
# Builds "Kormcrete Order Desk.app" and kormcrete-order-desk-macos-<arch>.dmg from target/ (mvn package) and a Zulu
# JRE 8 for that architecture.
# Usage: packaging/macos/build-macos.sh <jre-home> <arm64|x64> <version> <out-dir>
#   <jre-home>: the JRE folder with bin/java and lib/rt.jar, or the .jre/.jdk bundle holding it in Contents/Home.
set -euo pipefail
fail() { echo "build-macos: $*" >&2; exit 2; }
[ $# -eq 4 ] || fail "usage: $0 <jre-home> <arm64|x64> <version> <out-dir>"
JRE=$(cd "$1" && pwd) || fail "no such folder: $1"
[ -x "$JRE/Contents/Home/bin/java" ] && JRE="$JRE/Contents/Home"
ARCH=$2
VERSION=$3
mkdir -p "$4"
OUT=$(cd "$4" && pwd)
cd "$(dirname "$0")/../.."

case "$ARCH" in
  arm64) MACHO=arm64 ;;
  x64) MACHO=x86_64 ;;
  *) fail "the architecture is arm64 or x64, not $ARCH" ;;
esac
[ -x "$JRE/bin/java" ] && [ -f "$JRE/lib/rt.jar" ] || fail "$JRE is not a Java 8 JRE (needs bin/java and lib/rt.jar)"
file "$JRE/bin/java" | grep -q "$MACHO" || fail "$JRE/bin/java is not $MACHO: $(file -b "$JRE/bin/java")"
for f in target/order-desk.jar target/swing-ui-agent-agent.jar packaging/icons/kormcrete-order-desk.icns; do
  [ -f "$f" ] || fail "missing $f (run mvn -B package first)"
done

NAME="Kormcrete Order Desk"
WORK="target/macos-$ARCH"
APP="$WORK/dmg/$NAME.app"
rm -rf "$WORK"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Java" "$APP/Contents/Resources"
sed "s/@VERSION@/$VERSION/g" packaging/macos/Info.plist > "$APP/Contents/Info.plist"
install -m 755 packaging/macos/kormcrete-order-desk "$APP/Contents/MacOS/kormcrete-order-desk"
ditto "$JRE" "$APP/Contents/Java/jre"
cp target/order-desk.jar target/swing-ui-agent-agent.jar "$APP/Contents/Java/"
cp packaging/icons/kormcrete-order-desk.icns "$APP/Contents/Resources/"
# ad-hoc signature: not trusted by Gatekeeper, but Apple Silicon reports an unsigned bundle as "damaged"
codesign --force --deep --sign - "$APP"
ln -s /Applications "$WORK/dmg/Applications"

DMG="$OUT/kormcrete-order-desk-macos-$ARCH.dmg"
rm -f "$DMG"
hdiutil create -quiet -volname "$NAME" -srcfolder "$WORK/dmg" -fs HFS+ -format UDZO "$DMG"
echo "$DMG"
