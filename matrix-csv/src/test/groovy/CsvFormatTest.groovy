import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader
import se.alipsa.matrix.csv.CsvWriter

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

class CsvFormatTest {

  @TempDir
  Path tempDir

  // ── Read: fluent API defaults ──────────────────────────────

  @Test
  void readStringWithDefaults() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    Matrix matrix = CsvReader.read().fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Alice', '30'], matrix.row(0), "first row")
  }

  @Test
  void readStringWithCustomDelimiter() {
    String csvContent = '''id;name;amount
1;Alice;100.00
2;Bob;200.00'''

    Matrix matrix = CsvReader.read()
        .delimiter(';' as char)
        .fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['2', 'Bob', '200.00'], matrix.row(1), "last row")
  }

  @Test
  void readStringNoHeader() {
    String csvContent = '''1,Alice,100.00
2,Bob,200.00'''

    Matrix matrix = CsvReader.read()
        .firstRowAsHeader(false)
        .fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['c0', 'c1', 'c2'], matrix.columnNames(), "Auto-generated column names")
    assertEquals(['1', 'Alice', '100.00'], matrix.row(0), "first row")
  }

  @Test
  void readFromFile() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.read().from(file)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], matrix.row(3), "last row")
  }

  @Test
  void readFromPath() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.read().from(file.toPath())

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromUrl() {
    URL url = getClass().getResource("/basic.csv")

    Matrix matrix = CsvReader.read().from(url)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromUrlString() {
    URL url = getClass().getResource("/basic.csv")

    Matrix matrix = CsvReader.read().fromUrl(url.toString())

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromInputStream() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    InputStream is = new ByteArrayInputStream(csvContent.bytes)
    Matrix matrix = CsvReader.read().from(is)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Bob', '25'], matrix.row(1), "last row")
  }

  @Test
  void readFromReader() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    StringReader reader = new StringReader(csvContent)
    Matrix matrix = CsvReader.read().from(reader)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Alice', '30'], matrix.row(0), "first row")
  }

  @Test
  void readFromFilePath() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.read().fromFile(file.absolutePath)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readEmptyCsv() {
    Matrix matrix = CsvReader.read().fromString('')

    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(0, matrix.rowCount(), "Empty CSV should have 0 rows")
    assertEquals(0, matrix.columnCount(), "Empty CSV should have 0 columns")
  }

  // ── Read: presets ──────────────────────────────────────────

  @Test
  void readWithTsvPreset() {
    String tsvContent = "name\tage\nAlice\t30\nBob\t25"

    Matrix matrix = CsvReader.read().tsv().fromString(tsvContent)

    assertEquals(2, matrix.rowCount())
    assertEquals(['name', 'age'], matrix.columnNames())
    assertEquals(['Alice', '30'], matrix.row(0))
  }

  @Test
  void readWithExcelPreset() {
    String csvContent = "name,age\r\nAlice,30\r\nBob,25"

    Matrix matrix = CsvReader.read().excel().fromString(csvContent)

    assertEquals(2, matrix.rowCount())
    assertEquals(['name', 'age'], matrix.columnNames())
  }

  @Test
  void readWithRfc4180Preset() {
    String csvContent = "name,age\r\nAlice,30\r\nBob,25"

    Matrix matrix = CsvReader.read().rfc4180().fromString(csvContent)

    assertEquals(2, matrix.rowCount())
    assertEquals(['name', 'age'], matrix.columnNames())
  }

  // ── Read: reader-specific options ──────────────────────────

  @Test
  void readWithExplicitHeader() {
    String csvContent = '''1,Alice,100.00
2,Bob,200.00'''

    Matrix matrix = CsvReader.read()
        .header(['id', 'name', 'amount'])
        .fromString(csvContent)

    assertEquals(2, matrix.rowCount())
    assertEquals(['id', 'name', 'amount'], matrix.columnNames())
    assertEquals(['1', 'Alice', '100.00'], matrix.row(0))
  }

  @Test
  void readWithCharset() {
    String csvContent = '''name,city
Alice,Malmö
Bob,Göteborg'''

    Matrix matrix = CsvReader.read()
        .charset(StandardCharsets.UTF_8)
        .fromString(csvContent)

    assertEquals(2, matrix.rowCount())
    assertEquals('Malmö', matrix.row(0)[1])
  }

  @Test
  void readWithMatrixName() {
    String csvContent = '''name,age
Alice,30'''

    Matrix matrix = CsvReader.read()
        .matrixName('people')
        .fromString(csvContent)

    assertEquals('people', matrix.matrixName)
    assertEquals(1, matrix.rowCount())
  }

  // ── Write: fluent API ──────────────────────────────────────

  @Test
  void writeToFile() {
    Matrix matrix = Matrix.builder()
        .matrixName('test')
        .columnNames(['id', 'name', 'amount'])
        .rows([
            ['1', 'Alice', '100.00'],
            ['2', 'Bob', '200.00']
        ])
        .build()

    File outputFile = tempDir.resolve('output.csv').toFile()
    CsvWriter.write(matrix).to(outputFile)

    assertTrue(outputFile.exists(), "Output file should exist")

    Matrix result = CsvReader.read().from(outputFile)
    assertEquals(2, result.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'amount'], result.columnNames(), "Column names")
    assertEquals(['2', 'Bob', '200.00'], result.row(1), "Last row")
  }

  @Test
  void writeToPath() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    Path outputPath = tempDir.resolve('output_path.csv')
    CsvWriter.write(matrix).to(outputPath)

    assertTrue(outputPath.toFile().exists(), "Output file should exist")
  }

  @Test
  void writeToFilePath() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b'])
        .rows([['1', '2']])
        .build()

    String filePath = tempDir.resolve('output_strpath.csv').toString()
    CsvWriter.write(matrix).toFile(filePath)

    assertTrue(new File(filePath).exists(), "Output file should exist")
  }

  @Test
  void writeAsString() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'age'])
        .rows([
            ['Alice', '30'],
            ['Bob', '25']
        ])
        .build()

    String csvContent = CsvWriter.write(matrix).asString()

    assertNotNull(csvContent, "CSV content should not be null")
    assertTrue(csvContent.contains('name,age'), "Should contain header")
    assertTrue(csvContent.contains('Alice,30'), "Should contain first row")
    assertTrue(csvContent.contains('Bob,25'), "Should contain second row")
  }

  @Test
  void writeAsStringWithoutHeader() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    String csvContent = CsvWriter.write(matrix).withHeader(false).asString()

    assertFalse(csvContent.contains('x,y'), "Should not contain header")
    assertTrue(csvContent.contains('1,2'), "Should contain first row")
    assertTrue(csvContent.contains('3,4'), "Should contain second row")
  }

  @Test
  void writeWithCustomDelimiter() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'value'])
        .rows([['Alice', '100'], ['Bob', '200']])
        .build()

    String csvContent = CsvWriter.write(matrix)
        .delimiter(';' as char)
        .asString()

    assertTrue(csvContent.contains('name;value'), "Should use semicolon delimiter")
    assertTrue(csvContent.contains('Alice;100'), "Data should use semicolon delimiter")
  }

  @Test
  void writeToWriter() {
    Matrix matrix = Matrix.builder()
        .columnNames(['col1', 'col2'])
        .rows([['a', 'b'], ['c', 'd']])
        .build()

    StringWriter writer = new StringWriter()
    CsvWriter.write(matrix).to(writer)

    String csvContent = writer.toString()
    assertTrue(csvContent.contains('col1,col2'), "Should contain header")
    assertTrue(csvContent.contains('a,b'), "Should contain first row")
    assertTrue(csvContent.contains('c,d'), "Should contain second row")
  }

  // ── Write: presets ─────────────────────────────────────────

  @Test
  void writeWithExcelPreset() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2']])
        .build()

    String csvContent = CsvWriter.write(matrix).excel().asString()

    assertTrue(csvContent.contains('\r\n'), "Excel preset should use CRLF")
  }

  @Test
  void writeWithTsvPreset() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'value'])
        .rows([['Alice', '100']])
        .build()

    String csvContent = CsvWriter.write(matrix).tsv().asString()

    assertTrue(csvContent.contains("name\tvalue"), "TSV preset should use tab delimiter")
    assertTrue(csvContent.contains("Alice\t100"), "Data should use tab delimiter")
  }

  @Test
  void writeWithRfc4180Preset() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2']])
        .build()

    String csvContent = CsvWriter.write(matrix).rfc4180().asString()

    assertTrue(csvContent.contains('\r\n'), "RFC 4180 preset should use CRLF")
  }

  // ── Round-trip tests ───────────────────────────────────────

  @Test
  void roundTripDefault() {
    Matrix original = Matrix.builder()
        .columnNames(['id', 'name', 'amount'])
        .rows([
            ['1', 'Alice', '100.00'],
            ['2', 'Bob', '200.00'],
            ['3', 'Charlie', '300.00']
        ])
        .build()

    String csvContent = CsvWriter.write(original).asString()
    Matrix result = CsvReader.read().fromString(csvContent)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }

  @Test
  void roundTripSemicolonDelimited() {
    Matrix original = Matrix.builder()
        .columnNames(['name', 'city', 'score'])
        .rows([
            ['Alice', 'New York', '95'],
            ['Bob', 'Los Angeles', '88']
        ])
        .build()

    String csvContent = CsvWriter.write(original).delimiter(';' as char).asString()
    Matrix result = CsvReader.read().delimiter(';' as char).fromString(csvContent)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }

  @Test
  void roundTripTsv() {
    Matrix original = Matrix.builder()
        .columnNames(['a', 'b', 'c'])
        .rows([['1', '2', '3'], ['4', '5', '6']])
        .build()

    String tsvContent = CsvWriter.write(original).tsv().asString()
    Matrix result = CsvReader.read().tsv().fromString(tsvContent)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
  }

  @Test
  void roundTripExcel() {
    Matrix original = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    String csvContent = CsvWriter.write(original).excel().asString()
    Matrix result = CsvReader.read().excel().fromString(csvContent)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
  }

  @Test
  void roundTripFile() {
    Matrix original = Matrix.builder()
        .matrixName('roundtrip')
        .columnNames(['id', 'desc', 'value'])
        .rows([
            ['1', 'First item', '10.5'],
            ['2', 'Second item', '20.0']
        ])
        .build()

    File outputFile = tempDir.resolve('roundtrip.csv').toFile()
    CsvWriter.write(original).to(outputFile)

    Matrix result = CsvReader.read().from(outputFile)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }

  // ── Edge cases ─────────────────────────────────────────────

  @Test
  void readQuotedFieldsWithEmbeddedNewlines() {
    String csvContent = '"name","description"\n"Alice","Line one\nLine two"\n"Bob","Single line"'

    Matrix matrix = CsvReader.read().fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals('Line one\nLine two', matrix.row(0)[1], "Embedded newline should be preserved")
  }

  @Test
  void readQuotedFieldsWithEmbeddedDelimiters() {
    String csvContent = '''name,description
"Alice","Contains, a comma"
"Bob","No comma here"'''

    Matrix matrix = CsvReader.read().fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals('Contains, a comma', matrix.row(0)[1], "Embedded delimiter should be preserved")
  }

  @Test
  void readEmptyFields() {
    String csvContent = '''a,b,c
1,,3
,2,'''

    Matrix matrix = CsvReader.read().fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['1', '', '3'], matrix.row(0), "Empty fields should be empty strings")
    assertEquals(['', '2', ''], matrix.row(1), "Empty fields should be empty strings")
  }

  @Test
  void readWithNullStringOption() {
    String csvContent = '''name,value
Alice,NA
Bob,100'''

    Matrix matrix = CsvReader.read()
        .nullString('NA')
        .fromString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertNull(matrix.row(0)[1], "NA should be converted to null")
    assertEquals('100', matrix.row(1)[1], "Non-NA values should remain")
  }

  @Test
  void writeEmptyMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b', 'c'])
        .build()

    String csvContent = CsvWriter.write(matrix).asString()

    assertTrue(csvContent.contains('a,b,c'), "Should contain header even for empty matrix")
    assertFalse(csvContent.contains('null'), "Should not contain null values")
  }

  @Test
  void roundTripWithComplexData() {
    Matrix original = Matrix.builder()
        .columnNames(['id', 'name', 'description', 'amount'])
        .rows([
            ['1', 'Alice', 'Works at "Tech Corp"', '1000.50'],
            ['2', 'Bob', 'Loves comma, semicolon; and quotes"', '2000.75'],
            ['3', 'Charlie', 'Simple text', '500.00']
        ])
        .build()

    String csvContent = CsvWriter.write(original).asString()
    Matrix result = CsvReader.read().fromString(csvContent)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }
}
