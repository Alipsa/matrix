#!/usr/bin/env bash
# Shared tool and portable filesystem preflight; does not create or remove files.
release_tools_preflight() {
  (( BASH_VERSINFO[0] >= 4 )) || { echo 'Use Bash 4+ (macOS: brew install bash)' >&2; return 1; }
  local tool
  for tool in java mvn groovy grep; do
    command -v "$tool" >/dev/null || { echo "Required tool missing: $tool" >&2; return 1; }
  done
  if [[ "${1:-release}" == release ]]; then
    command -v git >/dev/null || { echo 'Required tool missing: git' >&2; return 1; }
    java -version 2>&1 | grep -Eq 'version "21([."]|$)' || { echo 'Java 21 is required' >&2; return 1; }
  else
    local java_version
    java_version=$(java -version 2>&1)
    if [[ ! "$java_version" =~ version\ \"([0-9]+) ]]; then
      echo 'Java 21+ is required' >&2; return 1
    fi
    (( BASH_REMATCH[1] >= 21 )) || { echo 'Java 21+ is required' >&2; return 1; }
  fi
  local maven_version
  maven_version=$(mvn --version)
  maven_version=${maven_version%%$'\n'*}
  [[ "$maven_version" =~ Apache\ Maven\ ([0-9]+)\.([0-9]+)\.([0-9]+) ]] || return 1
  (( BASH_REMATCH[1] > 3 || (BASH_REMATCH[1] == 3 && (BASH_REMATCH[2] > 9 || (BASH_REMATCH[2] == 9 && BASH_REMATCH[3] >= 9))) )) || {
    echo 'Maven 3.9.9+ is required' >&2; return 1;
  }
  RELEASE_PATHS_STRICT_COUNT=0 groovy "$RELEASE_ROOT/scripts/release-all/Paths.groovy" "$RELEASE_ROOT" "$RELEASE_ROOT/matrix-bom" "$HOME" "$HOME/.m2/repository" >/dev/null
}
