#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk17
cd ..
./gradlew :matrix-core:clean :matrix-core:publishToSonatype closeAndReleaseSonatypeStagingRepository