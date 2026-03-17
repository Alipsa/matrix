# Matrix-arff release history

## v0.2.0 In progress
- add `ArffFormatProvider` and service registration so `.arff` files work with the generic `Matrix.read(...)` / `matrix.write(...)` SPI API
- add `ArffReadOptions` with `matrixName` fallback support when an ARFF file has no `@RELATION`
- add `ArffWriteOptions` with `nominalMappings` support for explicit nominal value definitions when writing
- document the generic `Matrix.read(...)` / `matrix.write(...)` ARFF usage in the README

## v0.1.0 2026-01-30
Initial release
Support for reading arff files into a matrix and writing a matrix to an arff file
