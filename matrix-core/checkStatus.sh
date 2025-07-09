#!/usr/bin/env bash

set -e

sonatypeUsername=$(grep '^centralPublishingUsername=' ~/.gradle/gradle.properties | cut -d'=' -f2-)
sonatypePassword=$(grep '^centralPublishingToken=' ~/.gradle/gradle.properties | cut -d'=' -f2-)
token=$(printf "$sonatypeUsername:$sonatypePassword" | base64)

curl --no-progress-meter --header "Authorization: Bearer $token" \
  "https://central.sonatype.com/api/v1/publisher/status?id=$1" \
  | jq
