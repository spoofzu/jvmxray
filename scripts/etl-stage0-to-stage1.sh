#!/usr/bin/env bash
#
# Promote STAGE0_EVENT data to STAGE1_EVENT and STAGE1_EVENT_KEYPAIR.
# Requires no parameters — auto-detects the SQLite database location.
# Optionally pass a database path: ./etl-stage0-to-stage1.sh /path/to/db.sqlite
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Build classpath from Maven dependencies
CLASSPATH="$PROJECT_DIR/target/classes:$(mvn -f "$PROJECT_DIR/pom.xml" dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)"

exec java -cp "$CLASSPATH" org.jvmxray.platform.shared.bin.EventPromoterCli "$@"
