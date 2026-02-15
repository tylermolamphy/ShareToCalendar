#!/usr/bin/env bash
# parse-test-results.sh — Parse JUnit XML test results into GitHub Actions outputs.
#
# Usage: bash .github/scripts/parse-test-results.sh <glob-pattern> [<glob-pattern> ...]
#
# Outputs (via $GITHUB_OUTPUT):
#   total   — total number of tests
#   passed  — number of passing tests
#   failed  — number of failing tests
#   status  — human-readable status string with emoji
#   table   — Markdown table of per-test results (multiline heredoc)

set -euo pipefail

TOTAL=0
FAILED=0
TABLE_ROWS=""

for pattern in "$@"; do
  for f in $pattern; do
    [ -f "$f" ] || continue

    T=$(grep -oP 'tests="\K[0-9]+' "$f" | head -1)
    F=$(grep -oP 'failures="\K[0-9]+' "$f" | head -1)
    E=$(grep -oP 'errors="\K[0-9]+' "$f" | head -1)
    TOTAL=$((TOTAL + ${T:-0}))
    FAILED=$((FAILED + ${F:-0} + ${E:-0}))

    # Extract suite name from <testsuite> element
    SUITE=$(grep -oP ' name="\K[^"]+' "$f" | head -1)
    SHORT_SUITE="${SUITE##*.}"

    # Add suite header row
    TABLE_ROWS="${TABLE_ROWS}| | **${SHORT_SUITE}** | |\n"

    # Parse each <testcase> element
    while IFS= read -r line; do
      TC_NAME=$(echo "$line" | grep -oP ' name="\K[^"]+')
      TC_TIME=$(echo "$line" | grep -oP ' time="\K[^"]+')
      # Escape pipes in test names for Markdown
      TC_NAME="${TC_NAME//|/\\|}"

      # Self-closing /> means pass; otherwise has <failure> child
      if echo "$line" | grep -q '/>'; then
        TABLE_ROWS="${TABLE_ROWS}| ✅ | ${TC_NAME} | ${TC_TIME}s |\n"
      else
        TABLE_ROWS="${TABLE_ROWS}| ❌ | ${TC_NAME} | ${TC_TIME}s |\n"
      fi
    done < <(grep '<testcase ' "$f")
  done
done

PASSED=$((TOTAL - FAILED))
echo "total=${TOTAL}" >> "$GITHUB_OUTPUT"
echo "passed=${PASSED}" >> "$GITHUB_OUTPUT"
echo "failed=${FAILED}" >> "$GITHUB_OUTPUT"

if [ "$TOTAL" -eq 0 ]; then
  echo "status=⚠️ No tests found" >> "$GITHUB_OUTPUT"
elif [ "$FAILED" -gt 0 ]; then
  echo "status=❌ ${PASSED}/${TOTAL} passed (${FAILED} failed)" >> "$GITHUB_OUTPUT"
else
  echo "status=✅ ${PASSED}/${TOTAL} passed" >> "$GITHUB_OUTPUT"
fi

# Output multiline table
{
  echo "table<<ENDOFTABLE"
  echo "| Status | Test | Duration |"
  echo "|--------|------|----------|"
  echo -e "${TABLE_ROWS}"
  echo "ENDOFTABLE"
} >> "$GITHUB_OUTPUT"
