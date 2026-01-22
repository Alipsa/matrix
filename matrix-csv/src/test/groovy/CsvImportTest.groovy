import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvImporter

import java.text.NumberFormat
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*

class CsvImportTest {

  @Test
  void importCsv() {
    URL url = getClass().getResource("/basic.csv")
    CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
    Matrix basic = CsvImporter.importCsv(url, format)
    assertEquals(4, basic.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], basic.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], basic.row(3), "last row")

    Matrix b = CsvImporter.importCsv((CsvImporter.Format.Trim): true, url)
    assertEquals(4, b.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], b.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], b.row(3), "last row")

    Matrix b2 = CsvImporter.importCsv(Trim: true, url)
    assertEquals(4, b2.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], b2.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], b2.row(3), "last row")
  }

  @Test
  void csvWithSemiColonsQuoteAndEmptyLine() {
    URL url = getClass().getResource("/colonQuotesEmptyLine.csv")
    CSVFormat format = CSVFormat.Builder.create()
        .setTrim(true)
        .setDelimiter(';')
        .setIgnoreEmptyLines(true)
        .setQuote('"' as Character)
        .setHeader('id', 'name', 'date', 'amount')
        .get()
    Matrix matrix = CsvImporter.importCsv(url, format)
    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], matrix.row(3), "last row")

    Matrix m = CsvImporter.importCsv(
        (CsvImporter.Format.Trim): true,
        (CsvImporter.Format.Delimiter): ';',
        (CsvImporter.Format.IgnoreEmptyLines): true,
        (CsvImporter.Format.Quote): '"',
        (CsvImporter.Format.Header): ['id', 'name', 'date', 'amount'],
        url)
    assertEquals(4, m.rowCount(), "Number of rows \\n ${m.content()}")
    assertEquals(['id', 'name', 'date', 'amount'], m.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], m.row(3), "last row")

    Matrix m2 = CsvImporter.importCsv(
        Trim: true,
        Delimiter: ';',
        IgnoreEmptyLines: true,
        Quote: '"',
        Header: ['id', 'name', 'date', 'amount'],
        url)
    assertEquals(4, m2.rowCount(), "Number of rows: \n ${m2.content()}")
    assertEquals(['id', 'name', 'date', 'amount'], m2.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], m2.row(3), "last row")

    Matrix table = matrix.clone().convert(
        ["id": Integer,
         "name": String,
         "date": LocalDate,
         "amount": BigDecimal
        ],
        "yyyy-MMM-dd",
        NumberFormat.getInstance(Locale.GERMANY)
    )
    assertEquals(4, table.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], table.columnNames(), "Column names")
    assertEquals([4, 'Arne', LocalDate.parse('2023-07-01'), 222.99], table.row(3), "last row")
  }

  // Phase 2a: Critical Edge Cases

  @Test
  void testEmptyCsv() {
    URL url = getClass().getResource("/empty.csv")
    Matrix matrix = CsvImporter.importCsv(url)
    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(0, matrix.rowCount(), "Empty CSV should have 0 rows")
    assertEquals(0, matrix.columnCount(), "Empty CSV should have 0 columns")
  }

  @Test
  void testHeaderOnlyCsv() {
    URL url = getClass().getResource("/headerOnly.csv")
    Matrix matrix = CsvImporter.importCsv(url)
    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(0, matrix.rowCount(), "Header-only CSV should have 0 data rows")
    assertEquals(3, matrix.columnCount(), "Should have 3 columns")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames(), "Column names should be parsed from header")
  }

  @Test
  void testSingleColumnCsv() {
    URL url = getClass().getResource("/singleColumn.csv")
    Matrix matrix = CsvImporter.importCsv(url)
    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(4, matrix.rowCount(), "Should have 4 data rows")
    assertEquals(1, matrix.columnCount(), "Should have 1 column")
    assertEquals(['value'], matrix.columnNames(), "Column name should be 'value'")
    assertEquals(['10'], matrix.row(0), "First row should be ['10']")
    assertEquals(['40'], matrix.row(3), "Last row should be ['40']")
  }

  @Test
  void testSingleRowCsv() {
    URL url = getClass().getResource("/singleRow.csv")
    Matrix matrix = CsvImporter.importCsv(url)
    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(1, matrix.rowCount(), "Should have 1 data row")
    assertEquals(3, matrix.columnCount(), "Should have 3 columns")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['1', 'John', '100.50'], matrix.row(0), "Single row data")
  }

  @Test
  void testMismatchedColumns() {
    URL url = getClass().getResource("/mismatchedColumns.csv")
    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      CsvImporter.importCsv(url)
    }
    assertTrue(ex.message.contains("expected"), "Error message should mention expected column count")
  }

  @Test
  void testFirstRowAsHeaderFalse() {
    // Use basic.csv which has multiple rows
    URL url = getClass().getResource("/basic.csv")
    // Use trim but don't configure header in CSVFormat, let the firstRowAsHeader parameter handle it
    CSVFormat format = CSVFormat.Builder.create()
        .setTrim(true)
        .build()
    Matrix matrix = CsvImporter.importCsv(url, format, false)
    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(5, matrix.rowCount(), "Should have 5 rows (all rows as data)")
    assertEquals(4, matrix.columnCount(), "Should have 4 columns")
    assertEquals(['c0', 'c1', 'c2', 'c3'], matrix.columnNames(), "Auto-generated column names")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.row(0), "First row is original header")
  }

  @Test
  void testInvalidFormatOptions() {
    URL url = getClass().getResource("/basic.csv")

    // Trim must be Boolean
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException) {
      CsvImporter.importCsv((CsvImporter.Format.Trim): "invalid", url)
    }
    assertTrue(ex1.message.contains("Boolean"), "Error should mention Boolean type")

    // Delimiter must be String or Character
    IllegalArgumentException ex2 = assertThrows(IllegalArgumentException) {
      CsvImporter.importCsv((CsvImporter.Format.Delimiter): 123, url)
    }
    assertTrue(ex2.message.contains("Character"), "Error should mention Character type")
  }

  @Test
  void testCommentMarkerFormat() {
    // Create CSV content with comments
    String csvContent = """# This is a comment
id,name,amount
1,John,100.50
# Another comment
2,Jane,200.75"""

    Reader reader = new StringReader(csvContent)
    Matrix matrix = CsvImporter.importCsv(
        (CsvImporter.Format.CommentMarker): '#',
        reader)

    assertEquals(2, matrix.rowCount(), "Should have 2 data rows (comments ignored)")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames())
    assertEquals(['2', 'Jane', '200.75'], matrix.row(1), "Last data row")
  }

  @Test
  void testNullStringFormat() {
    String csvContent = """id,name,amount
1,John,100.50
2,NULL,200.75
3,Jane,NULL"""

    Reader reader = new StringReader(csvContent)
    Matrix matrix = CsvImporter.importCsv(
        (CsvImporter.Format.NullString): 'NULL',
        reader)

    assertEquals(3, matrix.rowCount(), "Should have 3 rows")
    assertNull(matrix.row(1)[1], "NULL string should be converted to null")
    assertNull(matrix.row(2)[2], "NULL string should be converted to null")
  }

  @Test
  void testCustomRecordSeparator() {
    // CSV with custom record separator (using actual newlines in the content)
    String csvContent = "id,name,amount\r\n1,John,100.50\r\n2,Jane,200.75"

    Reader reader = new StringReader(csvContent)
    Matrix matrix = CsvImporter.importCsv(
        (CsvImporter.Format.RecordSeparator): '\r\n',
        reader)

    assertEquals(2, matrix.rowCount(), "Should have 2 data rows")
    assertEquals(['1', 'John', '100.50'], matrix.row(0))
    assertEquals(['2', 'Jane', '200.75'], matrix.row(1))
  }

  // Phase 4: API Enhancements

  @Test
  void testImportCsvFromStringPath() {
    // Test the convenience method that takes a String file path
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvImporter.importCsvFromFile(file.absolutePath)
    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(4, matrix.rowCount(), "Should have 4 data rows")
    assertEquals(4, matrix.columnCount(), "Should have 4 columns")
    // Note: basic.csv has spaces after commas, default CSVFormat doesn't trim
    assertEquals(['id', ' name', ' date', ' amount'], matrix.columnNames(), "Column names with spaces")
    assertEquals(['1', ' Per', ' 2023-04-30', ' 234.12'], matrix.row(0), "First row")
  }

  // String Content Support Tests

  @Test
  void testImportCsvString() {
    String csvContent = """id,name,amount
1,Alice,100.50
2,Bob,200.75
3,Charlie,300.00"""

    Matrix matrix = CsvImporter.importCsvString(csvContent)

    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(3, matrix.rowCount(), "Should have 3 data rows")
    assertEquals(3, matrix.columnCount(), "Should have 3 columns")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames())
    assertEquals(['1', 'Alice', '100.50'], matrix.row(0))
    assertEquals(['2', 'Bob', '200.75'], matrix.row(1))
    assertEquals(['3', 'Charlie', '300.00'], matrix.row(2))
  }

  @Test
  void testImportCsvStringWithCustomFormat() {
    String csvContent = """id;name;amount
1;Alice;100.50
2;Bob;200.75"""

    CSVFormat format = CSVFormat.Builder.create()
        .setDelimiter(';')
        .setTrim(true)
        .build()

    Matrix matrix = CsvImporter.importCsvString(csvContent, format)

    assertEquals(2, matrix.rowCount(), "Should have 2 data rows")
    assertEquals(3, matrix.columnCount(), "Should have 3 columns")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames())
    assertEquals(['1', 'Alice', '100.50'], matrix.row(0))
  }

  @Test
  void testImportCsvStringWithoutHeader() {
    String csvContent = """1,Alice,100.50
2,Bob,200.75"""

    CSVFormat format = CSVFormat.Builder.create()
        .setTrim(true)
        .build()

    Matrix matrix = CsvImporter.importCsvString(csvContent, format, false)

    assertEquals(2, matrix.rowCount(), "Should have 2 data rows")
    assertEquals(3, matrix.columnCount(), "Should have 3 columns")
    assertEquals(['c0', 'c1', 'c2'], matrix.columnNames(), "Auto-generated column names")
    assertEquals(['1', 'Alice', '100.50'], matrix.row(0))
  }
}
