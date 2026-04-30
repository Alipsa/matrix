# Matrix-arff release history

## v0.2.1 - 2026-04-30
- Fix nominal sentinel values (`?`, empty string, `%`-prefixed) being written unquoted, causing lossy ARFF round-trips
- Add `ArffDateFormats` utility to share strict (`lenient=false`), UTC, `Locale.ROOT` date formatter creation between reader and writer
- Fix date parsing to reject invalid dates (e.g. `2026-02-31`) instead of silently normalizing them
- Fix published SCM URL in POM (`matrix-arff/tree/master` → `tree/main/matrix-arff`)
- Apply module-wide `@CompileStatic` via `compileStatic.groovy` build configuration
- Add regression tests for nominal sentinel round-trips, invalid date rejection, and UTC date output
- Fixed all codenarc warnings and change the build to fail on any new warnings.

## v0.2.0 - 2026-03-18
- add sparse ARFF data row read support with validation for duplicate and out-of-range attribute indices
- add `ArffFormatProvider` and service registration so `.arff` files work with the generic `Matrix.read(...)` / `matrix.write(...)` SPI API
- expand `ArffReadOptions` with `fallbackMatrixName` support, strict validation toggles, and richer parser error messages with line context
- expand `ArffWriteOptions` with configurable schema generation, including nominal inference controls, forced per-column ARFF types, and global/per-column DATE formats
- add typed options-first direct API overloads for ARFF reads and writes, and align the SPI provider with those typed paths
- refactor `MatrixArffWriter` so direct API and SPI writes share one typed schema-resolution path
- document the current ARFF API surface and defaults in the README and tutorial, including sparse input, strict mode, explicit schema control, and SPI round-tripping examples

## v0.1.0 - 2026-01-30
Initial release
Support for reading arff files into a matrix and writing a matrix to an arff file
