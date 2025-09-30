[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-spreadsheet/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-spreadsheet)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-spreadsheet/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-spreadsheet)
# Matrix Spreadsheet
Spreadsheet import to a Matrix and Matrix export to a spreadsheet. 

This Groovy library enables you to import and export Excel and Libre/Open Office Calc spreadsheets.
It is heavily inspired by the [Spreadsheets](https://github.com/Alipsa/spreadsheets) library 
for [Renjin R](https://github.com/bedatadriven/renjin).

To use it, add the following to your gradle build script: 
```groovy
implementation 'org.apache.groovy:groovy:5.0.5'
implementation 'se.alipsa.matrix:matrix-core:3.4.1'
implementation 'se.alipsa.matrix:matrix-spreadsheet:2.2.1'
```
or if you use maven:
```xml
<dependencies>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.1</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.4.1</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-spreadsheet</artifactId>
    <version>2.2.1</version>
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
(ExcelImporter or OdsImporter respectively) e.g.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.poi.ExcelImporter
import se.alipsa.matrix.spreadsheet.sods.OdsImporter

// Importing an excel spreadsheet
try (InputStream is = this.getClass().getResourceAsStream("/Book1.xlsx")) {
  Matrix table = ExcelImporter.create().importExcel(
      is, 'Sheet1', 1, 12, 'A', 'D', true
  )
  assert 3.0d == table[2, 0]
}

// importing an open document spreadsheet
try (InputStream is = this.getClass().getResourceAsStream("/Book1.ods")) {
  Matrix table = OdsImporter.importOds(
      is, 'Sheet1', 1, 12, 'A', 'D', true
  )
  assert "3.0" == table[2, 0]
}
```

## Export a spreadsheet

```groovy
import static se.alipsa.matrix.core.ListConverter.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetExporter
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
import se.alipsa.matrix.spreadsheet.*

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
Apache POI which is used to handle Excel sheets requires quite a lot of memory. If you have Excel sheets with more than 150,000 rows you might encounter out of memory errors. If increasing RAM is not an option, consider exporting the content to a csv and use matrix-csv to import the data instead. There is also an early alternative implementation based on fastexcel which uses streaming and MUCH less memory. It has a very similar API as the poi one except for one difference: it is unable to append sheets to an existing Excel document. See [FExcelImporterTest](https://github.com/Alipsa/matrix/blob/main/matrix-spreadsheet/src/test/groovy/spreadsheet/FExcelImporterTest.groovy) and [FExporterTest](https://github.com/Alipsa/matrix/blob/main/matrix-spreadsheet/src/test/groovy/spreadsheet/FExporterTest.groovy) for example usage. You can also switch between implementations when using the Spreadsheet API (e.g. SpreadsheetImporter) by setting the static enum variable `excelImplementation` e.g. `SpreadsheetImporter.excelImplementation = ExcelImplementation.FastExcel`. See [SpreadsheetImporterTest](https://github.com/Alipsa/matrix/blob/main/matrix-spreadsheet/src/test/groovy/spreadsheet/SpreadsheetImporterTest.groovy) for examples.

# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix-csv and matrix core

| Matrix spreadsheet |    Matrix core |
|-------------------:|---------------:|
|              2.1.0 |          3.1.0 |
|              2.0.0 |          3.0.0 |
|     1.2.0 -> 1.2.1 |          2.3.0 |
|              1.1.0 |          2.2.0 |
|              1.0.3 | 2.0.0 -> 2.1.1 |
|              1.0.2 |          1.2.4 |
|              1.0.1 | 1.2.1 -> 1.2.3 |
|              2.0.0 |          3.0.0 |
|              2.1.0 |          3.1.0 |
|              2.2.0 | 3.2.0 -> 3.3.0 |


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