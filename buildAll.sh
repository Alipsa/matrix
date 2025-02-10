#!/usr/bin/env bash
set -e
localRepo=$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
if [[ -d "$localRepo/se/alipsa/matrix" ]]; then
  echo "Removing local cache in $localRepo/se/alipsa/matrix"
  rm -r "$localRepo/se/alipsa/matrix"
fi
echo "Building and locally publishing matrix"
./gradlew build publishToMavenLocal
echo "Locally publish bom and verify"
cd matrix-bom
mvn -f bom.xml install
mvn verify
echo "Done!"