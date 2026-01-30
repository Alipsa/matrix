# Release history

## v2.3.0, In progress
**Major architectural refactoring with significant performance improvements**

### Breaking Changes
- removed POI and SODS implementations - FastExcel is now the single XLSX backend, FastOds is the single ODS backend
- removed ExcelImplementation and OdsImplementation enums - no implementation selection needed
- explicitly reject legacy .xls files (XLSX only)
- deprecated SpreadsheetExporter in favor of SpreadsheetWriter

### New Features
- add append/replace support for existing XLSX and ODS files (preserves sheets and metadata)
- add flexible start position support when writing data (e.g., write to cell B5)
- add map-based multi-sheet API: `writeSheets(Map<String, Position>)`
- add new ODS streaming writer/appender with table attributes/column reuse for styling
- add profiling support for ODS operations via `-Dmatrix.spreadsheet.ods.profile=true`
- XLSX append now inherits sheetFormatPr, column widths, page margins, and fixes relId collisions
- add comprehensive sheet name sanitization with automatic de-duplication

### Performance Improvements
- ODS read performance: 65-80% faster (medium files: 4.86s → 1.43s, large files: 262s → 53s)
- switched to Aalto StAX parser for 64% speedup
- adaptive row capacity sizing to minimize ArrayList resizing
- type-aware value extraction with switch dispatch
- optimized trailing empty row detection
- Null Object pattern for profiling eliminates branching overhead (~14% improvement)

### Bug Fixes
- fix missing return statement in ValueExtractor.getDouble() (percentage parsing)
- fix 1-based sheet indexing consistency across all importers
- fix sheet name collision prevention (sanitization could cause silent data loss)
- fix invalid sheet number handling in URL imports
- fix null row guards in FExcelReader
- fix percentage parsing to be locale-independent
- fix race condition in SpreadsheetExporter static field sharing

### Code Quality
- add column count validation to all write methods
- add robust cleanup for temp files and XML stream resources
- add XXE protection with hardened XML parsing
- replace 15+ println statements with proper logging
- remove ~500 lines of dead code (POI, SODS implementations)
- extract duplicate header building logic (DRY improvements)
- comprehensive test coverage: 79.74% (105 tests passing)
- add benchmarking suite for performance validation

### Dependencies
- remove org.apache.poi:poi and org.apache.poi:poi-ooxml
- remove com.github.miachm.sods:SODS
- remove org.apache.logging.log4j:log4j-api (migrated to matrix-core Logger)
- add com.fasterxml:aalto-xml 1.3.4 (high-performance StAX parser)
- upgrade com.github.javaparser:javaparser-core [3.26.4 -> 3.27.0]
- migrate from log4j to matrix-core Logger (supports slf4j if present, otherwise System.out/err)

## v2.2.1, 2025-07-19
- Upgrade dependencies
    - com.github.javaparser:javaparser-core [3.26.4 -> 3.27.0]
    - org.apache.logging.log4j:log4j-api [2.24.3 -> 2.25.1]
- Remove log4j impl and just use log4j api (it's a library should not have logging implementations).

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-spreadsheet/2.2.1/matrix-spreadsheet-2.2.1.jar)

## v2.2.0, 2025-05-29
- upgrade poi 5.4.0 -> 5.4.1
- getOrDefault() behaves oddly so switched to get() and elvis operator
- overload additional import options to SpreadsheetImporter to allow for more flexible import
- add importOds and importExcel method to SpreadsheetImporter to handle import from a URL
- upgrade SODS 1.6.7 -> 1.6.8
- Upgrade fastExcel 0.18.4 -> 0.19.0
- Fix groovydoc generation by using the groovydoc ant task to generate the docs (to be able to set javaVersion).
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-spreadsheet/2.2.0/matrix-spreadsheet-2.2.0.jar)

## v2.1.0, 2025-03-26
- Add several new import options (for file, input streams and URL)
- Add alternative excel implementation based on fastexcel to be able to handle very large Excel files
- Add home-brewed fast ods import option to be able to handle very large ods files
- Change methods from static to instance methods iand introduce a new Importer interface for consistency

## v2.0.0, 2025-03-12
- Require JDK 21

## v1.2.1, 2025-01-17
- Fix ODS import for Percentage and Currency columns by converting them to Doubles
- ExcelImporter now returns a LocalDate if no time info was specified to align with OdsImporter behavior 

## v1.2.0, 2025-01-16
- Default to 1:st sheet instead of 'Sheet1' if no sheet is given
- Change check for sheet param to detect Number instead of integer
- Breaking change: Change importOds to fetch as object instead of as string
- Upgrade POI 5.3.0 -> 5.4.0

## v1.1.0, 2025-01-08
- clarify which sheet we are on when trying to read outside boundaries
- change base package name to se.alipsa.matrix.spreadsheet
- adopt to matrix-core 2.2.0

## v1.0.3, 2024-10-31
- Add import ODS and XLSX from an InputStream
- Add import multiple sheets from an InputStream for both ods and xlsx
- fix bug when firstRowAsColumnNames is false (was off by one)
- adapt to matrix-core 2.0.0

## v1.0.2, 2024-07-07
- Consistently use index starting with 1 in the user api

## v1.0.1, 2023-11-21
- Change index of the SpreadsheetImporter to start with 1
- Name the imported Matrix after the sheet name

## v1.0.0, 2023-05-18
- initial version
