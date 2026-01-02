#!/bin/bash

RED='\033[1;31m'
NC='\033[0m' # No Color (Reset)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

fail_count=0
total_run=0

for file in "$SCRIPT_DIR"/*.groovy; do
  if [ -f "$file" ]; then
    ((total_run++))
    echo "Running: $(basename "$file")"
    echo "----------------------------------------"

    groovy "$file"

    exit_code=$?

    if [ $exit_code -ne 0 ]; then
      echo -e "${RED}>>> ERROR:${NC} $(basename "$file") failed with exit code $exit_code"
      ((fail_count++))
    fi
    echo ""
  fi
done

echo "----------------------------------------"
echo "Done running all examples."
echo "Total scripts run: $total_run"

if [ $fail_count -gt 0 ]; then
  echo -e "${RED}${fail_count} scripts failed!${NC}"
  exit 1
else
  echo "All scripts ran successfully!"
  exit 0
fi