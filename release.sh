#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository