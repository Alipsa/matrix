#!/usr/bin/env bash
# Publishes the se.alipsa.groovy:datasets relocation POM to Maven Central.
# Requires a "central" server entry (and gpg setup) in ~/.m2/settings.xml.
cd "$(dirname "$0")" || exit 1
mvn -Prelease clean deploy || exit 1
echo "see https://central.sonatype.com/publishing/deployments to review and publish"
