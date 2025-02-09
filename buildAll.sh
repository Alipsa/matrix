#!/usr/bin/env bash
set -e
localRepo=$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
rm -r "$localRepo/se/alipsa/matrix"
./gradlew build publishToMavenLocal
cd matrix-bom
mvn -f bom.xml install
mvn verify