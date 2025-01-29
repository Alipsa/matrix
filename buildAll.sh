#!/usr/bin/env bash
set -e
./gradlew build publishToMavenLocal
cd matrix-bom
mvn -f bom.xml install
mvn verify