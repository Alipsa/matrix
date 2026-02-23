import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvFormat
import se.alipsa.matrix.csv.CsvReader
import se.alipsa.matrix.csv.CsvWriter

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

class CsvFormatTest {

  @TempDir
  Path tempDir

  // ── Predefined constants ──────────────────────────────────────

  @Test
  void testDefaultConstant() {
    assertNotNull(CsvFormat.DEFAULT)
    assertEquals(',' as char, CsvFormat.DEFAULT.delimiter)
    assertEquals('"' as Character, CsvFormat.DEFAULT.quoteCharacter)
    assertNull(CsvFormat.DEFAULT.escapeCharacter)
    assertNull(CsvFormat.DEFAULT.commentMarker)
    assertTrue(CsvFormat.DEFAULT.trim)
    assertTrue(CsvFormat.DEFAULT.ignoreEmptyLines)
    assertTrue(CsvFormat.DEFAULT.ignoreSurroundingSpaces)
    assertNull(CsvFormat.DEFAULT.nullString)
    assertEquals('\n', CsvFormat.DEFAULT.recordSeparator)
  }

  @Test
  void testExcelConstant() {
    assertEquals(',' as char, CsvFormat.EXCEL.delimiter)
    assertEquals('\r\n', CsvFormat.EXCEL.recordSeparator)
  }

  @Test
  void testTdfConstant() {
    assertEquals('\t' as char, CsvFormat.TDF.delimiter)
  }

  @Test
  void testRfc4180Constant() {
    assertEquals('\r\n', CsvFormat.RFC4180.recordSeparator)
  }

  // ── Builder ───────────────────────────────────────────────────

  @Test
  void testBuilderCustomDelimiter() {
    CsvFormat format = CsvFormat.builder()
        .delimiter(';' as char)
        .build()

    assertEquals(';' as char, format.delimiter)
    // Other defaults remain unchanged
    assertEquals('"' as Character, format.quoteCharacter)
    assertTrue(format.trim)
  }

  @Test
  void testBuilderAllOptions() {
    CsvFormat format = CsvFormat.builder()
        .delimiter('\t' as char)
        .quoteCharacter('\'' as Character)
        .escapeCharacter('\\' as Character)
        .commentMarker('#' as Character)
        .trim(false)
        .ignoreEmptyLines(false)
        .ignoreSurroundingSpaces(false)
        .nullString('NA')
        .recordSeparator('\r\n')
        .build()

    assertEquals('\t' as char, format.delimiter)
    assertEquals('\'' as Character, format.quoteCharacter)
    assertEquals('\\' as Character, format.escapeCharacter)
    assertEquals('#' as Character, format.commentMarker)
    assertFalse(format.trim)
    assertFalse(format.ignoreEmptyLines)
    assertFalse(format.ignoreSurroundingSpaces)
    assertEquals('NA', format.nullString)
    assertEquals('\r\n', format.recordSeparator)
  }

  @Test
  void testBuilderNullQuote() {
    CsvFormat format = CsvFormat.builder()
        .quoteCharacter(null)
        .build()

    assertNull(format.quoteCharacter)
  }

  // ── toCSVFormat conversion ────────────────────────────────────

  @Test
  void testToCSVFormatEquivalence() {
    CsvFormat csvFormat = CsvFormat.builder()
        .delimiter(';' as char)
        .trim(true)
        .ignoreEmptyLines(true)
        .build()

    CSVFormat apacheFormat = csvFormat.toCSVFormat()
    assertEquals(';', apacheFormat.delimiterString)
    assertTrue(apacheFormat.trim)
    assertTrue(apacheFormat.ignoreEmptyLines)
  }

  // ── Read with CsvFormat ───────────────────────────────────────

  @Test
  void readStringWithCsvFormat() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    Matrix matrix = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Alice', '30'], matrix.row(0), "first row")
  }

  @Test
  void readStringWithCustomCsvFormat() {
    String csvContent = '''id;name;amount
1;Alice;100.00
2;Bob;200.00'''

    CsvFormat format = CsvFormat.builder()
        .delimiter(';' as char)
        .build()

    Matrix matrix = CsvReader.readString(csvContent, format)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['2', 'Bob', '200.00'], matrix.row(1), "last row")
  }

  @Test
  void readStringNoHeaderWithCsvFormat() {
    String csvContent = '''1,Alice,100.00
2,Bob,200.00'''

    Matrix matrix = CsvReader.readString(csvContent, CsvFormat.DEFAULT, false)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['c0', 'c1', 'c2'], matrix.columnNames(), "Auto-generated column names")
    assertEquals(['1', 'Alice', '100.00'], matrix.row(0), "first row")
  }

  @Test
  void readFromFileWithCsvFormat() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.read(file, CsvFormat.DEFAULT)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], matrix.row(3), "last row")
  }

  @Test
  void readFromPathWithCsvFormat() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.read(file.toPath(), CsvFormat.DEFAULT)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromUrlWithCsvFormat() {
    URL url = getClass().getResource("/basic.csv")

    Matrix matrix = CsvReader.read(url, CsvFormat.DEFAULT)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromUrlStringWithCsvFormat() {
    URL url = getClass().getResource("/basic.csv")

    Matrix matrix = CsvReader.readUrl(url.toString(), CsvFormat.DEFAULT)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromInputStreamWithCsvFormat() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    InputStream is = new ByteArrayInputStream(csvContent.bytes)
    Matrix matrix = CsvReader.read(is, CsvFormat.DEFAULT)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Bob', '25'], matrix.row(1), "last row")
  }

  @Test
  void readFromReaderWithCsvFormat() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    StringReader reader = new StringReader(csvContent)
    Matrix matrix = CsvReader.read(reader, CsvFormat.DEFAULT)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Alice', '30'], matrix.row(0), "first row")
  }

  @Test
  void readFileFromStringPathWithCsvFormat() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.readFile(file.absolutePath, CsvFormat.DEFAULT)

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readEmptyCsvWithCsvFormat() {
    String csvContent = ''

    Matrix matrix = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(0, matrix.rowCount(), "Empty CSV should have 0 rows")
    assertEquals(0, matrix.columnCount(), "Empty CSV should have 0 columns")
  }

  // ── Write with CsvFormat ──────────────────────────────────────

  @Test
  void writeToFileWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .matrixName('test')
        .columnNames(['id', 'name', 'amount'])
        .rows([
            ['1', 'Alice', '100.00'],
            ['2', 'Bob', '200.00']
        ])
        .build()

    File outputFile = tempDir.resolve('output.csv').toFile()
    CsvWriter.write(matrix, outputFile, CsvFormat.DEFAULT)

    assertTrue(outputFile.exists(), "Output file should exist")

    Matrix result = CsvReader.read(outputFile, CsvFormat.DEFAULT)
    assertEquals(2, result.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'amount'], result.columnNames(), "Column names")
    assertEquals(['2', 'Bob', '200.00'], result.row(1), "Last row")
  }

  @Test
  void writeToPathWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    Path outputPath = tempDir.resolve('output_path.csv')
    CsvWriter.write(matrix, outputPath, CsvFormat.DEFAULT)

    assertTrue(outputPath.toFile().exists(), "Output file should exist")
  }

  @Test
  void writeToStringPathWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b'])
        .rows([['1', '2']])
        .build()

    String filePath = tempDir.resolve('output_strpath.csv').toString()
    CsvWriter.write(matrix, filePath, CsvFormat.DEFAULT)

    assertTrue(new File(filePath).exists(), "Output file should exist")
  }

  @Test
  void writeToStringWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'age'])
        .rows([
            ['Alice', '30'],
            ['Bob', '25']
        ])
        .build()

    String csvContent = CsvWriter.writeString(matrix, CsvFormat.DEFAULT)

    assertNotNull(csvContent, "CSV content should not be null")
    assertTrue(csvContent.contains('name,age'), "Should contain header")
    assertTrue(csvContent.contains('Alice,30'), "Should contain first row")
    assertTrue(csvContent.contains('Bob,25'), "Should contain second row")
  }

  @Test
  void writeToStringWithoutHeaderWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    String csvContent = CsvWriter.writeString(matrix, CsvFormat.DEFAULT, false)

    assertFalse(csvContent.contains('x,y'), "Should not contain header")
    assertTrue(csvContent.contains('1,2'), "Should contain first row")
    assertTrue(csvContent.contains('3,4'), "Should contain second row")
  }

  @Test
  void writeWithCustomCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'value'])
        .rows([['Alice', '100'], ['Bob', '200']])
        .build()

    CsvFormat format = CsvFormat.builder()
        .delimiter(';' as char)
        .build()

    String csvContent = CsvWriter.writeString(matrix, format)

    assertTrue(csvContent.contains('name;value'), "Should use semicolon delimiter")
    assertTrue(csvContent.contains('Alice;100'), "Data should use semicolon delimiter")
  }

  @Test
  void writeToWriterWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['col1', 'col2'])
        .rows([['a', 'b'], ['c', 'd']])
        .build()

    StringWriter writer = new StringWriter()
    CsvWriter.write(matrix, writer, CsvFormat.DEFAULT)

    String csvContent = writer.toString()
    assertTrue(csvContent.contains('col1,col2'), "Should contain header")
    assertTrue(csvContent.contains('a,b'), "Should contain first row")
    assertTrue(csvContent.contains('c,d'), "Should contain second row")
  }

  // ── Round-trip tests ──────────────────────────────────────────

  @Test
  void roundTripWithCsvFormat() {
    Matrix original = Matrix.builder()
        .columnNames(['id', 'name', 'amount'])
        .rows([
            ['1', 'Alice', '100.00'],
            ['2', 'Bob', '200.00'],
            ['3', 'Charlie', '300.00']
        ])
        .build()

    String csvContent = CsvWriter.writeString(original, CsvFormat.DEFAULT)
    Matrix result = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

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

    CsvFormat format = CsvFormat.builder()
        .delimiter(';' as char)
        .build()

    String csvContent = CsvWriter.writeString(original, format)
    Matrix result = CsvReader.readString(csvContent, format)

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

    String tsvContent = CsvWriter.writeString(original, CsvFormat.TDF)
    Matrix result = CsvReader.readString(tsvContent, CsvFormat.TDF)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
  }

  @Test
  void roundTripExcelFormat() {
    Matrix original = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    String csvContent = CsvWriter.writeString(original, CsvFormat.EXCEL)
    Matrix result = CsvReader.readString(csvContent, CsvFormat.EXCEL)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
  }

  @Test
  void roundTripFileWithCsvFormat() {
    Matrix original = Matrix.builder()
        .matrixName('roundtrip')
        .columnNames(['id', 'desc', 'value'])
        .rows([
            ['1', 'First item', '10.5'],
            ['2', 'Second item', '20.0']
        ])
        .build()

    File outputFile = tempDir.resolve('roundtrip.csv').toFile()
    CsvWriter.write(original, outputFile, CsvFormat.DEFAULT)

    Matrix result = CsvReader.read(outputFile, CsvFormat.DEFAULT)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }

  // ── Edge cases ────────────────────────────────────────────────

  @Test
  void readQuotedFieldsWithEmbeddedNewlines() {
    String csvContent = '"name","description"\n"Alice","Line one\nLine two"\n"Bob","Single line"'

    Matrix matrix = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals('Line one\nLine two', matrix.row(0)[1], "Embedded newline should be preserved")
  }

  @Test
  void readQuotedFieldsWithEmbeddedDelimiters() {
    String csvContent = '''name,description
"Alice","Contains, a comma"
"Bob","No comma here"'''

    Matrix matrix = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals('Contains, a comma', matrix.row(0)[1], "Embedded delimiter should be preserved")
  }

  @Test
  void readEmptyFieldsWithCsvFormat() {
    String csvContent = '''a,b,c
1,,3
,2,'''

    Matrix matrix = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['1', '', '3'], matrix.row(0), "Empty fields should be empty strings")
    assertEquals(['', '2', ''], matrix.row(1), "Empty fields should be empty strings")
  }

  @Test
  void readWithNullStringOption() {
    String csvContent = '''name,value
Alice,NA
Bob,100'''

    CsvFormat format = CsvFormat.builder()
        .nullString('NA')
        .build()

    Matrix matrix = CsvReader.readString(csvContent, format)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertNull(matrix.row(0)[1], "NA should be converted to null")
    assertEquals('100', matrix.row(1)[1], "Non-NA values should remain")
  }

  @Test
  void writeEmptyMatrixWithCsvFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b', 'c'])
        .build()

    String csvContent = CsvWriter.writeString(matrix, CsvFormat.DEFAULT)

    assertTrue(csvContent.contains('a,b,c'), "Should contain header even for empty matrix")
    assertFalse(csvContent.contains('null'), "Should not contain null values")
  }

  @Test
  void roundTripWithComplexDataAndCsvFormat() {
    Matrix original = Matrix.builder()
        .columnNames(['id', 'name', 'description', 'amount'])
        .rows([
            ['1', 'Alice', 'Works at "Tech Corp"', '1000.50'],
            ['2', 'Bob', 'Loves comma, semicolon; and quotes"', '2000.75'],
            ['3', 'Charlie', 'Simple text', '500.00']
        ])
        .build()

    String csvContent = CsvWriter.writeString(original, CsvFormat.DEFAULT)
    Matrix result = CsvReader.readString(csvContent, CsvFormat.DEFAULT)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }

  @Test
  void testToString() {
    CsvFormat format = CsvFormat.builder()
        .delimiter(';' as char)
        .build()

    String str = format.toString()
    assertTrue(str.contains('CsvFormat'), "toString should contain class name")
    assertTrue(str.contains(';'), "toString should contain delimiter")
  }
}
