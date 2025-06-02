#!/usr/bin/env bash
set -e
localRepo=$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
if [[ -d "$localRepo/se/alipsa/matrix" ]]; then
  echo "Removing local cache in $localRepo/se/alipsa/matrix"
  rm -r "$localRepo/se/alipsa/matrix"
fi
echo "Locally publish bom"
pushd matrix-bom
  mvn -f bom.xml install
popd
echo "Building and locally publishing matrix"
./gradlew build publishToMavenLocal
echo "Verify bom"
pushd matrix-bom
  mvn verify
popd
echo "Done!"