#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "$0")/../backend"
mkdir -p target/audit-attempts

if [ -z "${NVD_API_KEY:-}" ]; then unset NVD_API_KEY; fi

for attempt in 1 2 3; do
  log="target/audit-attempts/attempt-$attempt.log"
  set +e
  ./mvnw -B -ntp -Psecurity dependency-check:check 2>&1 | tee "$log"
  status=${PIPESTATUS[0]}
  set -e
  if [ "$status" -eq 0 ]; then exit 0; fi
  if grep -Eq 'vulnerabilities (that have a CVSS|above the CVSS threshold)' "$log"; then
    exit "$status"
  fi
  if [ "$attempt" -eq 3 ] ||
    ! grep -Eq 'https://nvd\.nist\.gov/[^ ]+.*Server status: (404|408|429|5[0-9][0-9])' "$log"; then
    exit "$status"
  fi
  printf 'Falha HTTP transitória da NVD; nova auditoria completa após a tentativa %s.\n' "$attempt"
  sleep "$((attempt * 10))"
done
