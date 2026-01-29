# Matrix-Tablesaw Version history

## v0.2.2, In progress

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
- Deprecated `OdsReadOptions.builderFromUrl(String url)` - ODS is a binary format, not text-based
- Note: These deprecated methods will be removed in v0.3.0

### Dependency Updates
- com.github.miachm.sods:SODS [1.6.8 -> 1.7.0]
- org.apache.poi:poi-ooxml [5.4.1 -> 5.5.1]

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