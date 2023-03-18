#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk11
./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository