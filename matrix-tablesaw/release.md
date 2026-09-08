# Matrix-Tablesaw Version history

## v0.4.0, unreleased

### Security
- `XmlReader` now rejects any XML document containing a `DOCTYPE` declaration, and disables
  external general entities, external parameter entities, and external DTD loading as defense in
  depth. Previously an XML file referencing an external entity could cause the parser to read
  arbitrary files (XML external entity injection).
- XML parsing failures and rejected documents surface as `RuntimeIOException` with a useful
  message instead of leaking parser internals.

### Breaking changes
- `BigDecimalColumn.create(String, int...)` is replaced by `createFromInts(String, int...)`.
  `create(String, int)` retains its Tablesaw-compatible meaning of creating that many missing
  rows, so `create("c", 5)` still yields five missing rows while `createFromInts("c", 5, 6)`
  yields two values.
- `XlsxWriteOptions.builder(Writer)` is deprecated and its `build()` now fails at write time:
  XLSX is a binary-only format, so `XlsxWriter` rejects character destinations with
  `IllegalArgumentException("XLSX requires a binary OutputStream destination")`. Use the
  `OutputStream`, `File`, or filename overloads instead.
- `TableUtil.createColumn` throws a named `IllegalArgumentException` (column name, zero-based row
  index, expected type, actual type) for values whose runtime type is incompatible with the
  requested `ColumnType`, instead of silently inserting missing values. Accepted conversions:
  lossless numeric widenings (narrower integers to wider integer types; exactly representable
  integers and `Float` to `Double`; exactly representable `Short`/`Byte`/`Integer` to `Float`),
  any `CharSequence` (including Groovy `GString`) to `String`, and floating-point values to
  `BigDecimal` through `BigDecimalColumn.toBigDecimal`, where NaN becomes missing and infinities
  are rejected. Values that would lose precision or overflow (for example a `Long` beyond 2^53
  in a `DOUBLE` column, or 10^400) are rejected. This keeps `TableUtil.fromMatrix`/`toTablesaw`
  working for Matrix columns declared wider than their stored values (for example integer
  salaries in a `BigDecimal`-typed column). Note that 0.3.2 coerced floating-point values into
  `BigDecimal` columns through the Groovy cast; that conversion is retained but now follows the
  column's documented rules. Unsupported and `SKIP` types now also throw instead of returning
  `null`.
- `TableUtil.round(NumberColumn, int)` no longer mutates the source column: it returns an
  independent rounded copy (HALF_EVEN by default). Callers must use the return value.
- ODS (and XML) reads now preserve interior all-missing rows instead of dropping them, so tables
  containing such rows report a higher row count than 0.3.2. Trailing all-missing rows are still
  dropped by default; see the I/O fixes below for the opt-out.

### Numeric behavior
- `Double.NaN` and `Float.NaN` appended to a `BigDecimalColumn` become missing values; positive
  and negative infinity are rejected with `IllegalArgumentException` because `BigDecimal` cannot
  represent them.
- Finite `Float` values convert via `new BigDecimal(Float.toString(value))`, so `append(1.1f)`
  produces `1.1` rather than the widened-double representation `1.100000023841858`.
- No-context `divide`/`divideBy` now use `MathContext.DECIMAL64` instead of dividend-scale
  rounding; overloads accepting an explicit `MathContext` were added.
- `BigDecimalColumn` equality, hashing, and distinct-value operations are now numeric:
  `1.0` and `1.00` are equal, hash identically, and appear once in `unique()`/`asSet()`.
  `asSet()` uses numeric comparator equality, so `asSet().contains(new BigDecimal("1.000"))`
  is true even though an ordinary hash-based set containing `1.0` would not contain it.
- Parser-based string mutation (`set`/`appendCell` with a parser) preserves the exact decimal
  text only when it agrees with the parser's numeric interpretation; otherwise the parser's
  custom semantics win. `BigDecimalParser` bypasses this agreement probe.
- Empty or all-missing columns return `null` from mean, median, coefficient of variation, range,
  min, and max; sum retains its existing empty-input convention.

### I/O fixes
- XML, ODS, and XLSX writers now emit missing cells as blank/empty cells instead of serializing
  Tablesaw numeric/boolean sentinels.
- Interior all-missing rows now round-trip through ODS (and XML) instead of being dropped or —
  worse — being corrupted into the following row's values by the ODS writer. Trailing all-missing
  rows are still dropped by default, as in 0.3.2; pass `trimTrailingMissingRows(false)` to
  `OdsReadOptions.builder(...)` to preserve a legitimate trailing all-missing data row, for
  example when round-tripping a file written by this module's own writer.
- XML output is deterministic UTF-8: stream destinations get an explicit
  `encoding="UTF-8"` declaration written through an explicit `OutputStreamWriter`, while a
  caller-supplied `Writer` is used as supplied and the declaration omits the encoding attribute.
- XML duplicate column names are compared case-insensitively. With `allowDuplicateColumnNames(false)`
  they are rejected; with it enabled, later occurrences are renamed deterministically to
  collision-safe `name-2`, `name-3`, ... Unlike Tablesaw CSV, pre-suffixed names such as `name-2`
  cannot cause the generated names to collide.
- XML tables are shape-validated before indexing: every first-row `<td>` needs a non-blank `name`,
  and every later row must have exactly as many `<td>` elements as the first row; violations
  raise `RuntimeIOException` naming the one-based data row and expected/actual cell count.
- XLSX worksheet names are sanitized with `WorkbookUtil.createSafeSheetName`, falling back to
  `Sheet1` when the result is null or blank.
- File-backed `builder(File)`/`builder(String)` write options for XLSX, ODS, and XML defer opening
  the output file until writing starts: creating or building options no longer creates or
  truncates the target. I/O errors surface as `RuntimeIOException` from the write call. A
  successful write closes the stream.
- `Reader` and `InputStream` sources passed to `XmlReader` are closed after parsing, on both
  success and failure, matching the module's ODS reader behavior.
- Shared `FormatWriteOptionsBuilder` base class added for the XLSX/ODS/XML write-option builders,
  owning common destination construction (including the lazy file destination). All existing
  builder entry points and signatures are preserved.

### Validation
- `Gtable.create(data, columnTypes)` validates up front that the type list is non-null, matches
  the data size, and contains no null, `SKIP`, or unsupported entries, identifying the bad type
  index and column name.
- `Gtable.create(data, typeOverrides)` rejects unknown override keys and routes null/`SKIP`/
  unsupported override values through the same named error instead of silently ignoring them.
- `Normalizer.logNorm` for `DoubleColumn` and `FloatColumn` skips missing rows instead of
  normalizing the missing sentinel.

### Build/test changes
- New `testNonUtf8DefaultEncoding` Gradle task verifies XML stream output is UTF-8 under a
  non-UTF-8 JVM default (`-Dfile.encoding=ISO-8859-1`); wired into `check`.

## v0.3.2, 2026-07-06
- matrix-tablesaw/src/main/java/tech/tablesaw/api/NumberAggregateFunction.java: BigDecimal aggregate functions now only declare compatibility with BigDecimalColumnType,
  preventing Tablesaw from dispatching them to DoubleColumn, IntColumn, etc.
- matrix-tablesaw/src/main/java/tech/tablesaw/io/xlsx/XlsxWriter.java: BigDecimal values now export as numeric XLSX cells; null BigDecimal values remain blank.
- Added regression tests in matrix-tablesaw/src/test/java/tech/tablesaw/api/BigDecimalColumnTest.java and matrix-tablesaw/src/test/java/io/ExportDataTest.java.

### Dependency updates
- com.github.miachm.sods:SODS 1.8.3 -> 1.10.1

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
