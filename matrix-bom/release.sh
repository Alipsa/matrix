#!/usr/bin/env bash
set -e
mvn -f bom.xml install
mvn verify
mvn -Prelease -f bom.xml deploy
