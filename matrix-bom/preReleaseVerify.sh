#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
if [[ ! -x "$SCRIPT_DIR/verifyBomApi.sh" ]]; then
  echo "verifyBomApi.sh is missing or not executable" >&2
  exit 1
fi
exec "$SCRIPT_DIR/verifyBomApi.sh" "$@"
