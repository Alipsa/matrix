# Matrix-Tablesaw Version history

## v0.3.1, 2026-07-03

### Bug Fixes
- **Preserve missing floating values during rounding**
  - `TableUtil.round(NumberColumn, int)` now skips missing rows for `DoubleColumn` and `FloatColumn` before rounding non-missing values.
  - Direct scalar rounding remains strict and does not special-case `NaN`.
- **Return Gtable from CSV reader overloads**
  - `GdataFrameReader.csv(...)` now wraps all public Tablesaw CSV read overloads in `Gtable`, including path, file, stream, URL, reader, and `CsvReadOptions` variants.
- **Preserve Gtable through fluent joins**
  - `GdataFrameJoiner` fluent builder methods now return `GdataFrameJoiner`, and terminal `join()` returns `Gtable`.
  - Existing direct convenience joins continue to return `Gtable`.
- **Render frequency-table missing values explicitly**
  - `TableUtil.frequency(Column<?>)` now detects missing entries with `column.isMissing(i)` and groups them under the reserved `"<missing>"` marker.
  - Literal string values such as `"null"` remain distinct from true missing values.
  - A real non-missing value that would display as `"<missing>"` now fails with a clear `IllegalArgumentException` instead of silently merging with missing values.

### Testing
- Final matrix-tablesaw verification passed in repository-required order.
- Full repository test suite passed.

## v0.3.0, 2026-05-05

### Breaking Changes
- **Removed previously deprecated `OdsReadOptions` factory methods**
  - `OdsReadOptions.builder(Reader)` and `OdsReadOptions.builderFromString(String)` have been removed as promised in the v0.2.2 release notes.
- **BigDecimalColumn arithmetic is now non-mutating by default.**
  - `plus()`, `subtract()`, `multiply()`, and `divide()` return **new** columns instead of mutating the receiver.
  - Use the new `addTo()`, `subtractBy()`, `multiplyBy()`, and `divideBy()` methods for in-place mutation.
  - This makes Groovy operator overloading (`+`, `-`, `*`, `/`) behave intuitively.

### New Features
- **Friendlier Gtable factory APIs**
  - `Gtable.create(Map)` infers column types from the first non-null value in each list.
  - `Gtable.create(Map, Map<String, ColumnType>)` allows named type overrides while inferring the rest.
  - All map-based factories now validate that every list has the same length and throw a clear `IllegalArgumentException` on mismatch.
- **Table-level normalization convenience**
  - `Gtable.normalizeMinMax(columnName, outputColumnName?, decimals?)`
  - `Gtable.normalizeMean(columnName, outputColumnName?, decimals?)`
  - `Gtable.normalizeStdScale(columnName, outputColumnName?, decimals?)`
  - `Gtable.normalizeLog(columnName, outputColumnName?, decimals?)`
  - Supports `DoubleColumn`, `FloatColumn`, and `BigDecimalColumn`.
  - Non-destructive by default (returns a new Gtable). Omit `outputColumnName` to replace the source column.
- **Explicit unsupported-column handling in Matrix → Tablesaw conversion**
  - `TableUtil.toTablesaw(Matrix)` now throws `IllegalArgumentException` for unsupported column types instead of silently skipping them.
  - `TableUtil.toTablesaw(Matrix, boolean skipUnsupported)` provides an explicit opt-in to skip unsupported columns.

### Bug Fixes
- **Preserve Matrix type metadata during Tablesaw conversion**
  - `TableUtil.classForColumnType` now compares against `ColumnType` constants and `BigDecimalColumnType.instance()` directly, fixing cases where type metadata was lost.
- **Fix ODS missing cell handling**
  - Null cells in ODS spreadsheets are now imported as missing values instead of the literal string `"null"`.
- **Fix XLSX DateTime export**
  - `LOCAL_DATE_TIME` columns now preserve both date and time components when written to XLSX.
- **Gtable.copy() now deep-copies columns**
  - Previously `copy()` reused the original column objects, allowing mutations to leak back to the source table. It now creates independent column copies.
- **XmlReader now throws RuntimeIOException on parse failures**
  - `DocumentException` from dom4j was previously wrapped in a raw `RuntimeException`; it is now consistently wrapped in `RuntimeIOException`.
- **BigDecimalAggregateFunctions.cv guards against zero mean**
  - Dividing by a zero mean now throws a clear `IllegalArgumentException` instead of an opaque `ArithmeticException`.

### Documentation
- Updated `readme.md` with current dependency guidance (use `matrix-bom` or `matrix-all`), quick examples for conversion, Gtable factories, BigDecimal arithmetic, and normalization.
- Fixed incorrect BOM version references in `readme.md` (was `3.7.0`, corrected to `2.5.0`).
- Fixed GroovyDoc typos (`extansion` → `extension`, `tgble` → `Gtable`) and added missing method documentation to public API surface in `Gtable.groovy`.
- Added missing Javadoc to `BigDecimalColumn.add(BigDecimalColumn)`.

### Code Quality
- `BigDecimalColumnType.INSTANCE` is now `final`.
- Extracted `assertSameSize(BigDecimalColumn)` to eliminate duplicated size-check logic across `BigDecimalColumn` arithmetic methods.

### Build & Publishing
- Corrected POM `url` and added a module-local `LICENSE` file (Apache License 2.0).
- Updated license metadata in published POM from MIT to Apache 2.0 to align with Tablesaw licensing.

### Dependency Updates
- com.github.miachm.sods:SODS 1.8.2 -> 1.8.3

### Testing
- All 115 tests passing.
- `:matrix-tablesaw:check` (including JaCoCo coverage verification) passes.

## v0.2.2, 2026-01-31
- Dependency updates:
  - com.github.miachm.sods:SODS 1.8.1 -> 1.8.2

### Build Configuration Improvements
- Added `compileTestJava` configuration with deprecation and unchecked warnings enabled
- Added `-Xlint:unchecked` flag to `compileGroovy` for improved Groovy code quality checks
- Build configuration now consistent with matrix-core module standards

### Code Quality Improvements
- Added `@SuppressWarnings("unchecked")` annotation to `TableUtil.createColumn()` method to properly handle intentional unchecked generic casts
- Removed duplicate `BigDecimalColumn` type check in `classForColumnType()` method (dead code removal)

### Bug Fixes & Improvements
- **BigDecimalColumn enhancements:**
  - Fixed `asBytes()` method to use UTF-8 encoding explicitly instead of platform default charset, ensuring consistent byte representation across all platforms
  - Cleaned up `asBytes()` method documentation and removed outdated TODO comments
  - Extended `toBigDecimal()` method to handle additional Number subtypes:
    - `BigDecimal` - now returns the value as-is without conversion (prevents precision loss from unnecessary double conversion)
    - `AtomicInteger` - converted via `get()` for precision
    - `AtomicLong` - converted via `get()` for precision
    - `DoubleAccumulator` - converted via `doubleValue()`
  - Added comprehensive Javadoc explaining conversion behavior for all Number types
  - Improved test coverage to properly exercise toBigDecimal(Number) conversion path for BigDecimal inputs
  - Updated test assertions to use UTF-8 encoding for deterministic byte array comparisons

### Documentation
- Updated README with v0.2.2 version reference
- Enhanced Javadoc/Groovydoc documentation:
  - **BigDecimalColumnFormatter** - Added comprehensive class documentation with usage examples, documented all factory methods, constructors, and formatting methods
  - **GdataFrameJoiner** - Added class documentation explaining join types, documented all join method variants with parameter descriptions
  - Verified existing documentation in BigDecimalComparator, XlsxWriteOptions, and GdataFrameReader
- All public APIs now have production-quality documentation

### Testing
- Added JaCoCo code coverage reporting infrastructure
  - Current coverage: 54% instruction coverage, 58% branch coverage
  - Coverage thresholds: 50% overall, 15% per class (baseline to prevent regression)
  - Coverage reports available in HTML and XML formats
  - Excluded low-coverage infrastructure classes from strict requirements
- Added test coverage for atomic type conversions in BigDecimalColumn
- Added test for BigDecimal precision preservation
- All 85 tests passing (2 new tests added)

### Deprecations
- Deprecated `OdsReadOptions.builder(Reader reader)` - ODS is a binary format, not text-based
- Deprecated `OdsReadOptions.builderFromString(String contents)` - ODS is a binary format, not text-based
- Note: These deprecated methods will be removed in v0.3.0

> **Correction (v0.3.0):** `OdsReadOptions.builderFromUrl(String url)` was incorrectly listed here as deprecated. It was never deprecated and remains part of the public API.

### Build Configuration Improvements

## v0.2.1, 2025-07-19
- Upgrade dependencies
  - com.github.miachm.sods:SODS [1.6.7 -> 1.6.8]
  - org.apache.poi:poi-ooxml [5.4.0 -> 5.4.1]
  - org.dom4j:dom4j [2.1.4 -> 2.2.0]
  
## v0.2.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-tablesaw/0.2.0/matrix-tablesaw-0.2.0.jar)

- Add BigDecimalAggregateFunctions to GTable
- Add column creation methods to Gtable to enable fluent interaction.
- Add from and toMatrix static factory methods to TableUtil

## v0.1
- moved from data-utils 1.0.5-SNAPSHOT
- add putAt method in GTable allowing the shorthand syntax `table[0,1] = 12` and `table[0, 'columnName'] = 'foo'` to change data.
- add possibility to cast a GTable to a Grid
