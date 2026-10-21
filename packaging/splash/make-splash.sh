#!/usr/bin/env bash
# Makes the splash's still image (src/main/resources/io/github/kormcrete/orderdesk/splash.png and splash@2x.png): white,
# the website's hairline around it, the website's wordmark bottom-right (packaging/splash/wordmark*.png: the header's
# "kormcrete" with "Solutions" under it, rendered from the website at 1x and 2x). Run when one of them changes; the
# outputs are committed.
set -euo pipefail
cd "$(dirname "$0")/../.."
JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 1.8)}"
WORK=target/splash
rm -rf "$WORK"
mkdir -p "$WORK/classes"
"$JAVA_HOME/bin/javac" -d "$WORK/classes" packaging/splash/MakeSplash.java
"$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$WORK/classes" MakeSplash \
  packaging/splash src/main/resources/io/github/kormcrete/orderdesk
