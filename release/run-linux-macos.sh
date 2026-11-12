#!/usr/bin/env bash
# ===========================================================================
#  FEMZYK Enterprise Fleet & Transport Management System - desktop launcher
#
#  Usage:  ./run-linux-macos.sh [--data DIR] [--demo] [--headless-check]
#
#  Needs Java 17 or newer (sudo apt install openjdk-17-jre, or
#  brew install openjdk@17). On a server without a display, use
#  --headless-check to verify the database and demo data without the UI.
# ===========================================================================
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$HERE/FEMZYK-Fleet-Management.jar"

if [[ ! -f "$APP" ]]; then
  echo "ERROR: FEMZYK-Fleet-Management.jar not found next to this script." >&2
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA="java"
else
  echo "ERROR: Java was not found. Install Java 17+ (e.g. 'sudo apt install openjdk-17-jre')." >&2
  exit 1
fi

exec "$JAVA" -jar "$APP" "$@"
