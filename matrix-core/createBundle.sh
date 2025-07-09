#!/usr/bin/env bash
set -e
artifact=$(basename "$PWD")
version="3.4.0"

pushd build
  for file in publications/maven/pom-default.xml*; do
    suffix="${file#publications/maven/pom-default.xml}"  # e.g., '', '.asc', '.md5'
    cp "$file" "libs/${artifact}-${version}.pom${suffix}"
  done

  prefix="se/alipsa/matrix/${artifact}/$version"
  mkdir -p tmp/"$prefix"
  cp libs/* tmp/"$prefix"/

  mkdir -p zips
popd

pushd build/tmp
  zipFile="../zips/${artifact}-$version-bundle.zip"
  if [[ -f "$zipFile" ]]; then
    rm "$zipFile"
  fi
  zip -r "$zipFile" "$prefix"
popd

rm -r build/tmp