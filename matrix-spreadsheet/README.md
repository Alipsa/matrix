# spreadsheet
Groovy spreadsheet import/export

This Groovy library enables you to import and export Excel and Libre/Open Office Calc spreadsheets.
It is based on (heavily inspired by) the [Spreadsheets](https://github.com/Alipsa/spreadsheets) library 
for [Renjin R](https://github.com/bedatadriven/renjin).

To use it, add the following to your gradle build script: 
```groovy
implementation 'org.apache.groovy:groovy:4.0.23'
implementation 'se.alipsa.groovy:matrix-core:2.0.0'
implementation 'se.alipsa.groovy:matrix-spreadsheet:1.0.3'
```
or if you use maven:
```xml
<dependencies>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>4.0.23</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.groovy</groupId>
      <artifactId>matrix-core</artifactId>
      <version>2.0.0</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.groovy</groupId>
    <artifactId>matrix-spreadsheet</artifactId>
    <version>1.0.3</version>
  </dependency>
</dependencies>
```

## Import a spreadsheet
```groovy
import se.alipsa.groovy.spreadsheet.*
import se.alipsa.groovy.matrix.Matrix

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
(ExcelImporter or OdsImporter respectively) e.g.

```groovy
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.spreadsheet.excel.ExcelImporter
import se.alipsa.groovy.spreadsheet.ods.OdsImporter

// Importing an excel spreadsheet
try(InputStream is = this.getClass().getResourceAsStream("/Book1.xlsx")) {
  Matrix table = ExcelImporter.importExcel(
      is, 'Sheet1', 1, 12, 'A', 'D', true
  )
  assert 3.0d == table[2, 0]
}

// importing an open document spreadsheet
try(InputStream is = this.getClass().getResourceAsStream("/Book1.ods")) {
  Matrix table = OdsImporter.importOds(
      is, 'Sheet1', 1, 12, 'A', 'D', true
  )
  assert "3.0" == table[2, 0]
}
```

## Export a spreadsheet

```groovy
import static se.alipsa.groovy.matrix.ListConverter.*
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.spreadsheet.SpreadsheetExporter
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
SpreadsheetExporter.exportSpreadsheet(file, table)
```

## Export to multiple sheets

```groovy
import se.alipsa.groovy.spreadsheet.*

// get data from somewhere
Matrix revenuePerYearMonth = getRevenue() 
Matrix details = getSalesDetails()

SpreadsheetExporter.exportSpreadsheets(
    // The file extension (.xls, .xlsx, .ods) determines the type (Excel or Calc)
  file: new File("/some/path/sales.ods"),
  data: [revenuePerYearMonth, details],
  sheetNames: ['monthly', 'details']
)
```

## Inquire about spreadsheet content
The SpreadsheetReader is an autocloseable class that can help you find various information about the
content e.g. where certain rows and columns are located. Here's an example:
```groovy
import se.alipsa.groovy.spreadsheet.*

File spreadsheet = new File("/some/path/to/excel_or_ods_file")
try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(spreadsheet)) {
  lastRow = reader.findLastRow(1)
  endCol = reader.findLastCol(1)
  // search For the first cell with the value 'Name' in sheet 1 in the A column:
  firstRow = reader.findRowNum(1, 'A', 'Name') 
}
```

See [the tests](https://github.com/Alipsa/spreadsheet/tree/main/src/test/groovy/spreadsheet) for more usage examples!

# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix-csv and matrix core

| Matrix spreadsheet |    Matrix core | 
|-------------------:|---------------:|
|              1.0.3 |          2.0.0 |
|              1.0.2 |          1.2.4 |
|              1.0.1 | 1.2.1 -> 1.2.3 |

# Third party libraries used
Note: only direct dependencies are listed below.

### Groovy
The environment this library is for. Note that there is no inclusion of Groovy in the jar leaving you free to use
any (modern) version of Groovy you prefer.
- URL: https://groovy-lang.org/
- License: Apache 2.0

### SODS 
Used to handle ODS file import and export
- URL: https://github.com/miachm/SODS
- License: Unlicense

### POI
Used to handle Excel import and export
- URL: https://poi.apache.org/
- License: Apache 2.0

### Matrix-core
Used to define the data format i.e. the result from an import or the data to export
- URL: https://github.com/Alipsa/matrix
- License: MIT

### Log4j
Used to handle logging
- URL: https://logging.apache.org/log4j/2.x/
- License: Apache 2.0