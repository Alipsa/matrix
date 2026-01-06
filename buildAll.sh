#!/usr/bin/env bash
set -e

# Check for Java version
check_java_version() {
  JAVA_VERSION_RAW=$(java -version 2>&1 | head -n 1)
  JAVA_VERSION=$(echo "$JAVA_VERSION_RAW" | sed -n 's/.*"\([^"]*\)".*/\1/p')
  JAVA_MAJOR_VERSION=${JAVA_VERSION%%.*}
  echo "Detected Java version: $JAVA_VERSION (major: $JAVA_MAJOR_VERSION)"
  if [[ "$JAVA_MAJOR_VERSION" == "21" ]]; then
    return 0
  else
    return 1
  fi
}

if ! check_java_version; then
  echo "Java version is not 21. Looking for jdk21 command..."
  if command -v jdk21 &> /dev/null; then
    echo "Found jdk21 command, executing it..."
    . jdk21
    if ! check_java_version; then
      echo "Error: Java version is still not 21 after running jdk21."
      exit 1
    fi
  else
    echo "Error: Java 21 is required. Please install Java 21 or create a command 'jdk21' to switch to it."
    exit 1
  fi
fi

# --- Updated Argument Handling ---
export RUN_EXTERNAL_TESTS=false
export RUN_SLOW_TESTS=false

if [[ "$1" == "--external" || "$1" == "-e" ]]; then
  export RUN_EXTERNAL_TESTS=true
  echo "External tests enabled"
elif [[ "$1" == "--all" || "$1" == "-a" ]]; then
  export RUN_EXTERNAL_TESTS=true
  export RUN_SLOW_TESTS=true
  echo "All tests (external and slow) enabled"
fi
# ---------------------------------

localRepo=$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)
if [[ -d "$localRepo/se/alipsa/matrix" ]]; then
  echo "Removing local cache in $localRepo/se/alipsa/matrix"
  rm -r "$localRepo/se/alipsa/matrix"
fi

echo "Locally publish bom"
pushd matrix-bom
  mvn -f bom.xml install
popd

echo "Building and locally publishing matrix"
./gradlew build publishToMavenLocal

echo "Verify bom"
pushd matrix-bom
  mvn verify
popd

# Clean up
unset RUN_EXTERNAL_TESTS
unset RUN_SLOW_TESTS

echo "Done!"