#!/usr/bin/env bash
set -e
if [[ "$1" == "--external" || "$1" == "-e" ]]; then
  export RUN_EXTERNAL_TESTS=true
  echo "External tests enabled"
fi
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
if [[ "$1" == "--external" || "$1" == "-e" ]]; then
  unset RUN_EXTERNAL_TESTS
fi
echo "Done!"