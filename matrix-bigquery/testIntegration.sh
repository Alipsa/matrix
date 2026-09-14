#!/usr/bin/env bash

set -euo pipefail

if ! command -v gcloud >/dev/null; then
  echo 'gcloud is required to run the external BigQuery tests.' >&2
  exit 1
fi

if ! gcloud auth application-default print-access-token >/dev/null 2>&1; then
  echo 'Application Default Credentials are unavailable; starting the gcloud login flow.'
  gcloud auth application-default login
fi

project_id="$(gcloud config get-value project 2>/dev/null || true)"
if [[ -z "${project_id}" || "${project_id}" == '(unset)' ]]; then
  echo 'No active Google Cloud project is configured. Run: gcloud config set project PROJECT_ID' >&2
  exit 1
fi

echo "Google application default credentials are valid and project is set to $project_id"
echo 'Running all matrix-bigquery integration tests...'
GOOGLE_CLOUD_PROJECT="${project_id}" ./gradlew :matrix-bigquery:externalTest --rerun-tasks
