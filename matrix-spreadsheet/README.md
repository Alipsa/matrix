[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-spreadsheet/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-spreadsheet)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-spreadsheet/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-spreadsheet)
# Matrix Spreadsheet
Spreadsheet import to a Matrix and Matrix export to a spreadsheet. 

This Groovy library enables you to import and export Excel and Libre/Open Office Calc spreadsheets.
It is heavily inspired by the [Spreadsheets](https://github.com/Alipsa/spreadsheets) library 
for [Renjin R](https://github.com/bedatadriven/renjin).

To use it, add the following to your gradle build script:
```groovy
implementation 'org.apache.groovy:groovy:5.0.4'
implementation 'se.alipsa.matrix:matrix-core:3.4.1'
implementation 'se.alipsa.matrix:matrix-spreadsheet:2.3.0'
```
or if you use maven:
```xml
<dependencies>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.3</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.4.1</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-spreadsheet</artifactId>
    <version>2.3.0</version>
  </dependency>
</dependencies>
```

## Import a spreadsheet
```groovy
import se.alipsa.matrix.spreadsheet.*
import se.alipsa.matrix.core.Matrix

Matrix table = SpreadsheetImporter.importSpreadsheet(file: "Book1.xlsx", endRow: 11, endCol: 4)
println(table.head(10))
```
The SpreadSheetImporter.importSpreadSheetSheet takes the following parameters:
- _file_ the filePath or the file object pointing to the Excel file
- _sheetName_ the name of the sheet to import, default is 'Sheet1'
- _startRow_ the starting row for the import (as you would see the row number in Excel), defaults to 1
- _endRow_ the last row to import
- _startCol_ the starting column name (A, B etc.) or column number (1, 2 etc.)
- _endCol_ the end column name (K, L etc) or column number (11, 12 etc.)
- _firstRowAsColNames_ whether the first row should be used for the names of each column, if false the column names will be v1, v2 etc. Defaults to true

Note: there are seveal overloaded versions of the importSpreadsheet method e.g taking a sheet index instead of a sheet name,
using column index instead of column name etc.

See [the Matrix package](https://github.com/Alipsa/matrix) for more information on what you can do with a Matrix.

If you need to import from a stream you must use the importer specific to the type of spreadsheet you are reading
(FExcelImporter or FOdsImporter respectively) e.g.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastods.FOdsImporter

// Importing an excel spreadsheet
try (InputStream is = this.getClass().getResourceAsStream("/Book1.xlsx")) {
  Matrix table = FExcelImporter.create().importSpreadsheet(
      is, 'Sheet1', 1, 12, 'A', 'D', true
  )
  assert 3.0d == table[2, 0]
}

// importing an open document spreadsheet
try (InputStream is = this.getClass().getResourceAsStream("/Book1.ods")) {
  Matrix table = FOdsImporter.create().importSpreadsheet(
      is, 'Sheet1', 1, 12, 'A', 'D', true
  )
  assert "3.0" == table[2, 0]
}
```

## Export a spreadsheet

```groovy
import static se.alipsa.matrix.core.ListConverter.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter
import java.time.format.DateTimeFormatter

def dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
def table = Matrix.builder().data(
        id: [null,2,3,4,-5],
        name: ['foo', 'bar', 'baz', 'bla', null],
        start: toLocalDates('2021-01-04', null, '2023-03-13', '2024-04-15', '2025-05-20'),
        end: toLocalDateTimes(dateFormat, '2021-02-04 12:01:22', '2022-03-12 13:14:15', '2023-04-13 15:16:17', null, '2025-06-20 17:18:19'),
        measure: [12.45, null, 14.11, 15.23, 10.99],
        active: [true, false, null, true, false]
    )
    .types(int, String, LocalDate, LocalDateTime, BigDecimal, Boolean)
    .build()
def file = File.createTempFile("matrix", ".xlsx")

// Export the Matrix to an excel file
SpreadsheetWriter.write(table, file)

// Export the Matrix starting at a specific cell
SpreadsheetWriter.write(table, file, "Metrics", "B3")
```

## Export to multiple sheets

```groovy
import se.alipsa.matrix.spreadsheet.*

// get data from somewhere
Matrix revenuePerYearMonth = getRevenue() 
Matrix details = getSalesDetails()

SpreadsheetWriter.writeSheets(
    // The file extension (.xlsx or .ods) determines the type (Excel or Calc)
  file: new File("/some/path/sales.ods"),
  data: [revenuePerYearMonth, details],
  sheetNames: ['monthly', 'details']
)

// Export with per-sheet start positions (LinkedHashMap preserves order)
SpreadsheetWriter.writeSheets(
  data: [revenuePerYearMonth, details],
  file: new File("/some/path/sales.xlsx"),
  sheetNamesAndPositions: ['monthly': 'B2', 'details': 'D4']
)
```

## Inquire about spreadsheet content
The SpreadsheetReader is an autocloseable class that can help you find various information about the
content e.g. where certain rows and columns are located. Here's an example:
```groovy
import se.alipsa.matrix.spreadsheet.*

File spreadsheet = new File("/some/path/to/excel_or_ods_file")
try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(spreadsheet)) {
  lastRow = reader.findLastRow(1)
  endCol = reader.findLastCol(1)
  // search For the first cell with the value 'Name' in sheet 1 in the A column:
  firstRow = reader.findRowNum(1, 'A', 'Name') 
}
```

See [the tests](https://github.com/Alipsa/spreadsheet/tree/main/src/test/groovy/spreadsheet) for more usage examples!

# Handling large files
Matrix-spreadsheet uses FastExcel for .xlsx import/export and FastOds (streaming) for .ods import/reading/export by default to keep memory usage low. If you have Excel sheets with more than 150,000 rows you might still encounter out of memory errors; if increasing RAM is not an option, consider exporting the content to CSV and use matrix-csv to import the data instead. Note that the FastExcel backend only supports .xlsx (not legacy .xls). Appending/replacing sheets in existing .xlsx files is supported via SpreadsheetWriter/FExcelAppender, and appending/replacing sheets in existing .ods files is supported via SpreadsheetWriter/FOdsAppender.

# Performance

Version 2.3.0 introduced significant ODS performance optimizations:

**ODS Read Performance (v2.3.0)**
- Medium files (50k rows × 12 columns): **1.43s** (70% faster than v2.2.0)
- Large files (900k+ rows Crime_Data): **53s** (80% faster than v2.2.0)

**Key Optimizations**
1. **Aalto StAX Parser**: Switched from JDK default to high-performance Aalto XML parser (10-30% faster)
2. **Type-aware Cell Parsing**: Optimized extractValue with switch dispatch and separate methods per type
3. **Adaptive Capacity**: Dynamic ArrayList sizing based on learned row width
4. **Null Object Pattern**: Eliminated profiling branch overhead for production use

**Benchmarking**

Run performance benchmarks:
```bash
./gradlew :matrix-spreadsheet:spreadsheetBenchmark --rerun-tasks
```

Enable ODS profiling (detailed timing breakdown):
```bash
./gradlew :matrix-spreadsheet:spreadsheetBenchmark -Dmatrix.spreadsheet.ods.profile=true
```

For more details on the optimizations, see `docs/fastexcel-analysis.md` and `docs/ods-optimization-opportunities.md`.

# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix-spreadsheet and matrix-core

| Matrix spreadsheet |    Matrix core |
|-------------------:|---------------:|
|              2.3.0 |          3.4.1 |
|              2.2.0 | 3.2.0 -> 3.3.0 |
|              2.1.0 |          3.1.0 |
|              2.0.0 |          3.0.0 |
|     1.2.0 -> 1.2.1 |          2.3.0 |
|              1.1.0 |          2.2.0 |
|              1.0.3 | 2.0.0 -> 2.1.1 |
|              1.0.2 |          1.2.4 |
|              1.0.1 | 1.2.1 -> 1.2.3 |


# Third party libraries used
Note: only direct dependencies are listed below.

### Groovy
The environment this library is for. Note that there is no inclusion of Groovy in the jar leaving you free to use
any (modern) version of Groovy you prefer.
- URL: https://groovy-lang.org/
- License: Apache 2.0

### FastExcel
Used to handle Excel (.xlsx) import and export
- URL: https://github.com/dhatim/fastexcel
- License: Apache 2.0

### FastOds
Internal streaming ODS implementation bundled with matrix-spreadsheet (not an external dependency).
- URL: https://github.com/Alipsa/matrix (matrix-spreadsheet fastods package)
- License: MIT

### Matrix-core
Used to define the data format i.e. the result from an import or the data to export
- URL: https://github.com/Alipsa/matrix
- License: MIT

### Log4j
Used to handle logging
- URL: https://logging.apache.org/log4j/2.x/
- License: Apache 2.0
