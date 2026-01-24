# Release history

## v2.3.0, 2026-01-24
- Simplified spreadsheet backends: removed POI and SODS implementations; FastExcel is now the single XLSX path and FastOds is the single ODS path (import/export/read).
- Added append/replace support for existing XLSX and ODS files; preserves existing sheets and metadata.
- New ODS streaming writer/appender that reuses table attributes/columns for base styling.
- XLSX append now inherits sheetFormatPr, column widths, and page margins, and fixes relId collisions in workbook relationships.
- Updated tests to cover append/replace and style inheritance. API (no implementation selection)
- Explicitly reject legacy .xls files (xlsx only)

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
