#!/usr/bin/env bash

mvn -f bom.xml install || exit
mvn verify || exit
mvn -f bom.xml deploy
