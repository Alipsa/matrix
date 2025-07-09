#!/usr/bin/env bash
set -e

sonatypeUsername=$(grep '^centralPublishingUsername=' ~/.gradle/gradle.properties | cut -d'=' -f2-)
sonatypePassword=$(grep '^centralPublishingToken=' ~/.gradle/gradle.properties | cut -d'=' -f2-)
token=$(printf "$sonatypeUsername:$sonatypePassword" | base64)

pushd build/zips
  echo "upload the bundle"
  zipFile=$(ls matrix-core*.zip)
  deploymentId=$(curl --no-progress-meter --header "Authorization: Bearer $token" \
    --form bundle=@${zipFile} \
    https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC)
popd

echo "deploymentId = $deploymentId"
echo "check https://central.sonatype.com/publishing/deployments for progress"
echo "This might also work:"
echo "./checkStatus.sh '$deploymentId'"