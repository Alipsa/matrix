#!/usr/bin/env bash
set -e
localRepo=$(mvn -f bom.xml help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
if [[ -d "$localRepo/se/alipsa/matrix" ]]; then
  echo "if the artifacts are not available in central we cannot release, so"
  echo "removing local cache in $localRepo/se/alipsa/matrix"
  rm -r "$localRepo/se/alipsa/matrix"
fi
mvn -f bom.xml install
mvn clean verify
echo "Releasing matrix-bom..."
mvn -DstagingProcessTimeoutMinutes=10 -Prelease -f bom.xml clean deploy
echo "Releasing matrix-all..."
mvn -DstagingProcessTimeoutMinutes=10 -Prelease clean deploy
echo "Done!"