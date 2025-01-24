#!/usr/bin/env bash
set -e
mvn -f bom.xml install
mvn verify
mvn -f bom.xml deploy
