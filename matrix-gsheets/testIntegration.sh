#!/usr/bin/env bash

set -euo pipefail

REQUIRED_SCOPES='https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/spreadsheets,https://www.googleapis.com/auth/drive.file'

# ADC can come from a service account key file pointed to by
# GOOGLE_APPLICATION_CREDENTIALS; if not set, use gcloud's application-default flow.
if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
  if ! command -v gcloud >/dev/null; then
    echo 'No GOOGLE_APPLICATION_CREDENTIALS set and gcloud is not installed;' >&2
    echo 'provide one of them to authenticate the external Google Sheets tests.' >&2
    exit 1
  fi

  if ! gcloud auth application-default print-access-token --scopes="$REQUIRED_SCOPES" >/dev/null 2>&1; then
    echo 'Application Default Credentials are unavailable or lack the required Sheets scopes; starting the gcloud login flow.'
    gcloud auth application-default login --scopes="$REQUIRED_SCOPES"
    gcloud auth application-default print-access-token --scopes="$REQUIRED_SCOPES" >/dev/null
  fi

  echo 'Google application default credentials grant the required integration-test scopes.'
else
  if [[ ! -f "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
    echo "GOOGLE_APPLICATION_CREDENTIALS points to ${GOOGLE_APPLICATION_CREDENTIALS} which does not exist." >&2
    exit 1
  fi
  echo "Using credentials from GOOGLE_APPLICATION_CREDENTIALS=${GOOGLE_APPLICATION_CREDENTIALS}"
  echo 'Those credentials must grant spreadsheets and drive.file access to the test spreadsheets.'
fi

echo 'Running all matrix-gsheets integration tests...'
RUN_EXTERNAL_TESTS=true ./gradlew :matrix-gsheets:test --rerun-tasks
