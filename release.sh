#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk17
./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository