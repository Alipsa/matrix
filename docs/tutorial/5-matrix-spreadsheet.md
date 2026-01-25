# Matrix Spreadsheet Module

The matrix-spreadsheet module provides functionality for importing data from spreadsheets into Matrix objects and exporting Matrix objects to spreadsheets. It supports Excel (.xlsx) and LibreOffice/OpenOffice Calc (.ods) formats.

Note that indexing in this module follows the Excel convention of starting with 1 instead of the usual 0 in Groovy.
This means that the first row and the first column are both referenced as 1.

## Installation

To use the matrix-spreadsheet module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.3'
implementation platform('se.alipsa.matrix:matrix-bom:2.3.0')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-spreadsheet'
```

### Maven Configuration

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.3.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.3</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-spreadsheet</artifactId>
    </dependency>
  </dependencies>
</project>
```

## Importing Spreadsheets

The matrix-spreadsheet module provides a `SpreadsheetImporter` class that makes it easy to import data from Excel and OpenOffice Calc spreadsheets into a Matrix object.

### Basic Import

Here's a simple example of importing a spreadsheet:

```groovy
import se.alipsa.matrix.spreadsheet.*
import se.alipsa.matrix.core.Matrix

// Import a spreadsheet with default settings
Matrix table = SpreadsheetImporter.importSpreadsheet(file: "Book1.xlsx", endRow: 11, endCol: 4)
println(table.head(10))
```

### Import Parameters

The `SpreadsheetImporter.importSpreadsheet` method accepts several parameters to control the import process:

- **file**: The file path or File object pointing to the spreadsheet file
- **sheetName**: The name of the sheet to import (default is 'Sheet1')
- **startRow**: The starting row for the import (as you would see the row number in Excel), defaults to 1
- **endRow**: The last row to import
- **startCol**: The starting column name (A, B etc.) or column number (1, 2 etc.)
- **endCol**: The end column name (K, L etc.) or column number (11, 12 etc.)
- **firstRowAsColNames**: Whether the first row should be used for column names (default is true)

### Importing from Different Sheet

You can specify which sheet to import from:

```groovy
import se.alipsa.matrix.spreadsheet.*
import se.alipsa.matrix.core.Matrix

// Import data from a specific sheet
Matrix table = SpreadsheetImporter.importSpreadsheet(
    file: "SalesReport.xlsx", 
    sheetName: "Q1_Sales",
    startRow: 2,  // Skip header row
    endRow: 50,
    startCol: "B",
    endCol: "G",
    firstRowAsColNames: false
)

// Set column names manually if not using first row
table.columnNames = ["Date", "Product", "Region", "Units", "Price", "Revenue"]
```

### Importing from Input Stream

If you need to import from a stream (e.g., from a resource or network), you must use the importer specific to the type of spreadsheet:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.spreadsheet.fastods.FOdsImporter

// Importing an Excel spreadsheet
try (InputStream is = this.getClass().getResourceAsStream("/Book1.xlsx")) {
    Matrix table = FExcelImporter.create().importSpreadsheet(
        is, 'Sheet1', 1, 12, 'A', 'D', true
    )
    assert 3.0d == table[2, 0]
}

// Importing an OpenOffice spreadsheet
try (InputStream is = this.getClass().getResourceAsStream("/Book1.ods")) {
    Matrix table = FOdsImporter.create().importSpreadsheet(
        is, 'Sheet1', 1, 12, 'A', 'D', true
    )
    assert "3.0" == table[2, 0]
}
```

### Importing from URL
You can also import spreadsheets from a URL:

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.spreadsheet.fastexcel.*
import se.alipsa.matrix.spreadsheet.fastods.*

// Importing an Excel spreadsheet
URL url = this.getClass().getResource("/Book1.xlsx")
Matrix table = FExcelImporter.create().importSpreadsheet(
    url, 'Sheet1', 1, 12, 'A', 'D', true
)
assert 3.0d == table[2, 0]


// Importing an OpenOffice spreadsheet
URL odsUrl = this.getClass().getResource("/Book1.ods")
Matrix book1 = FOdsImporter.create().importSpreadsheet(
    odsUrl, 'Sheet1', 1, 12, 'A', 'D', true
)
assert 3.0 == book1[2, 0]

```

### Spreadsheet implementations
Matrix-spreadsheet uses FastExcel for .xlsx import/export and FastOds (streaming) for .ods import/reading/export to reduce memory usage. The SpreadsheetImporter and SpreadsheetWriter classes hide the underlying implementation details.


## Exporting to Spreadsheets

The matrix-spreadsheet module also provides functionality to export Matrix objects to spreadsheets using the `SpreadsheetWriter` class.

### Basic Export

Here's a simple example of exporting a Matrix to a spreadsheet:

```groovy
import static se.alipsa.matrix.core.ListConverter.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

// Create a Matrix with various data types
def dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
def table = Matrix.builder().data(
        id: [null, 2, 3, 4, -5],
        name: ['foo', 'bar', 'baz', 'bla', null],
        start: toLocalDates('2021-01-04', null, '2023-03-13', '2024-04-15', '2025-05-20'),
        end: toLocalDateTimes(dateFormat, '2021-02-04 12:01:22', '2022-03-12 13:14:15', 
                             '2023-04-13 15:16:17', null, '2025-06-20 17:18:19'),
        measure: [12.45, null, 14.11, 15.23, 10.99],
        active: [true, false, null, true, false]
    )
    .types(Integer, String, LocalDate, LocalDateTime, BigDecimal, Boolean)
    .build()

// Create a temporary file for the export
def file = File.createTempFile("matrix", ".xlsx")

// Export the Matrix to an Excel file
SpreadsheetWriter.write(table, file)

// Export the Matrix starting at a specific cell
SpreadsheetWriter.write(table, file, "Metrics", "B3")

println("Spreadsheet exported to: ${file.absolutePath}")
```

### Exporting to Multiple Sheets

You can export multiple Matrix objects to different sheets in the same spreadsheet:

```groovy
import se.alipsa.matrix.spreadsheet.*
import se.alipsa.matrix.core.Matrix
import static se.alipsa.matrix.core.ListConverter.*

// Create or obtain Matrix objects
Matrix salesByMonth = Matrix.builder().data(
    month: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
    revenue: [12500, 13200, 15400, 14800, 16700, 18200]
).build()

Matrix salesDetails = Matrix.builder().data(
    date: toLocalDates('2023-01-15', '2023-02-20', '2023-03-10', '2023-04-05', '2023-05-12', '2023-06-08'),
    product: ["Widget A", "Widget B", "Widget A", "Widget C", "Widget B", "Widget A"],
    units: [120, 85, 150, 95, 110, 180],
    revenue: [4800, 5100, 6000, 5700, 6600, 7200]
).build()

// Export both matrices to a single spreadsheet with multiple sheets
SpreadsheetWriter.writeSheets(
    file: new File("/path/to/sales_report.xlsx"),
    data: [salesByMonth, salesDetails],
    sheetNames: ['Monthly Summary', 'Sales Details']
)

// Export with per-sheet start positions (LinkedHashMap preserves order)
SpreadsheetWriter.writeSheets(
    file: new File("/path/to/sales_report.xlsx"),
    data: [salesByMonth, salesDetails],
    sheetNamesAndPositions: ['Monthly Summary': 'B2', 'Sales Details': 'D4']
)
```

The file extension (.xlsx or .ods) determines the type of spreadsheet that will be created (Excel or Calc).

## Reading Spreadsheet Information

The `SpreadsheetReader` class allows you to examine spreadsheet content without fully importing it, which can be useful for determining the structure of a spreadsheet before importing.

```groovy
import se.alipsa.matrix.spreadsheet.*

File spreadsheet = new File("/path/to/spreadsheet.xlsx")
try (SpreadsheetReader reader = SpreadsheetReader.Factory.create(spreadsheet)) {
    // Find the last row in the first sheet (index 1)
    def lastRow = reader.findLastRow(1)
    
    // Find the last column in the first sheet
    def endCol = reader.findLastCol(1)
    
    // Search for the first cell with the value 'Name' in sheet 1 in column A
    def firstRow = reader.findRowNum(1, 'A', 'Name')
    
    println("Last row: ${lastRow}, Last column: ${endCol}, 'Name' found at row: ${firstRow}")
    
    // Now we can use this information to import the data more precisely
    Matrix data = SpreadsheetImporter.importSpreadsheet(
        file: spreadsheet,
        startRow: firstRow,
        endRow: lastRow,
        endCol: endCol
    )
}
```

## Handling Large Files

FastExcel is the default Excel backend and uses streaming to keep memory usage down. If you're working with spreadsheets containing more than 150,000 rows, you might still encounter out-of-memory errors. Here are some strategies for dealing with large files:

1. **Export to CSV first**: If increasing RAM is not an option, consider exporting the content to a CSV file and use the matrix-csv module to import the data instead.

2. **Stream a single sheet**: Reading a single sheet at a time minimizes memory use:

```groovy
import se.alipsa.matrix.spreadsheet.fastexcel.FExcelImporter
import se.alipsa.matrix.core.Matrix

// Import a large Excel file (streaming)
Matrix data = FExcelImporter.create().importSpreadsheet(
    "/path/to/large_file.xlsx",
    "Data",
    1,
    200000,
    "A",
    "Z",
    true
)
```

FastExcel now supports appending new sheets (or replacing sheets with the same name) in existing Excel workbooks. The writer preserves existing sheets and metadata; new sheets inherit base column widths/default row heights from the first sheet when available. FastOds likewise supports appending/replacing sheets in existing .ods files via SpreadsheetWriter/FOdsAppender. Reading multiple sheets at once can still require re-streaming the workbook and may increase memory use; the streaming reader shines when reading a single sheet from a large spreadsheet.

## Best Practices

1. **Memory Management**: Be mindful of memory usage when working with large spreadsheets. Consider using the streaming implementation or importing in chunks.

2. **Data Types**: The importer will attempt to infer data types, but you may need to convert columns to specific types after import using the `convert` method of the Matrix class.

3. **Error Handling**: Wrap spreadsheet operations in try-catch blocks to handle potential errors, especially when working with files that might be corrupted or have unexpected formats.

4. **Sheet Names**: When working with multiple sheets, always verify that the sheet names exist in the spreadsheet to avoid errors.

5. **Column Headers**: Using the first row as column headers (the default) makes the resulting Matrix more intuitive to work with.

## Conclusion

The matrix-spreadsheet module provides a powerful and flexible way to interact with Excel and OpenOffice Calc spreadsheets in your Groovy applications. Whether you need to import data for analysis or export results to a shareable format, this module offers the tools you need.

In the next section, we'll explore the matrix-csv module, which provides similar functionality for CSV files with additional features for handling various CSV formats.

Go to [previous section](4-matrix-datasets.md) | Go to [next section](6-matrix-csv.md) | Back to [outline](outline.md)
