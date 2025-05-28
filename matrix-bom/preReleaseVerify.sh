#!/usr/bin/env bash
set -e
mvn -f bom.xml install
mvn clean verify