#!/usr/bin/env bash

set -euo pipefail

# ADC can come from a service account key file pointed to by
# GOOGLE_APPLICATION_CREDENTIALS; if not set, use gcloud's application-default flow.
if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
  if ! command -v gcloud >/dev/null; then
    echo 'No GOOGLE_APPLICATION_CREDENTIALS set and gcloud is not installed;' >&2
    echo 'provide one of them to authenticate the external Google Sheets tests.' >&2
    exit 1
  fi

  if ! gcloud auth application-default print-access-token >/dev/null 2>&1; then
    echo 'Application Default Credentials are unavailable; starting the gcloud login flow.'
    gcloud auth application-default login
  fi

  echo 'Google application default credentials are valid.'
else
  if [[ ! -f "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
    echo "GOOGLE_APPLICATION_CREDENTIALS points to ${GOOGLE_APPLICATION_CREDENTIALS} which does not exist." >&2
    exit 1
  fi
  echo "Using credentials from GOOGLE_APPLICATION_CREDENTIALS=${GOOGLE_APPLICATION_CREDENTIALS}"
fi

echo 'Running all matrix-gsheets integration tests...'
RUN_EXTERNAL_TESTS=true ./gradlew :matrix-gsheets:test --rerun-tasks
