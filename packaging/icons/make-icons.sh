#!/usr/bin/env bash
# Makes the app icons from packaging/icons/kormcrete-order-desk-1024.png (the kormcrete tile): the macOS .icns, the
# Windows .ico and the window icon PNGs. Run on a Mac when the logo changes; the outputs are committed.
set -euo pipefail
cd "$(dirname "$0")/../.."
JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 1.8)}"
WORK=target/icons
rm -rf "$WORK"
mkdir -p "$WORK/classes"
"$JAVA_HOME/bin/javac" -d "$WORK/classes" packaging/icons/MakeIcons.java
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$WORK/classes" MakeIcons \
  packaging/icons/kormcrete-order-desk-1024.png "$WORK/kormcrete-order-desk.iconset" \
  packaging/icons/kormcrete-order-desk.ico src/main/resources/io/github/kormcrete/orderdesk/icons/app
iconutil -c icns "$WORK/kormcrete-order-desk.iconset" -o packaging/icons/kormcrete-order-desk.icns
