import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvOption
import se.alipsa.matrix.csv.CsvReader

import static org.junit.jupiter.api.Assertions.*

class CsvReaderTest {

  @Test
  void readCsvFromFile() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())
    CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()

    Matrix basic = CsvReader.read(file, format)
    assertEquals(4, basic.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], basic.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], basic.row(3), "last row")
  }

  @Test
  void readCsvFromUrl() {
    URL url = getClass().getResource("/basic.csv")
    CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()

    Matrix basic = CsvReader.read(url, format)
    assertEquals(4, basic.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], basic.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], basic.row(3), "last row")
  }

  @Test
  void readCsvWithMapFormat() {
    URL url = getClass().getResource("/basic.csv")

    Matrix b = CsvReader.read((CsvOption.Trim): true, url)
    assertEquals(4, b.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], b.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], b.row(3), "last row")

    // Test beautiful named arguments syntax
    Matrix b2 = CsvReader.read(Trim: true, url)
    assertEquals(4, b2.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], b2.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-07-01', '222.99'], b2.row(3), "last row")
  }

  @Test
  void readStringBasicCsv() {
    String csvContent = '''id,name,date,amount
1,Alice,2023-01-01,100.00
2,Bob,2023-01-02,200.00
3,Charlie,2023-01-03,150.50'''

    Matrix matrix = CsvReader.readString(csvContent)

    assertEquals(3, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['1', 'Alice', '2023-01-01', '100.00'], matrix.row(0), "first row")
    assertEquals(['3', 'Charlie', '2023-01-03', '150.50'], matrix.row(2), "last row")
  }

  @Test
  void readStringWithCustomFormat() {
    String csvContent = '''id;name;date;amount
1;Alice;2023-01-01;100,00
2;Bob;2023-01-02;200,00'''

    CSVFormat format = CSVFormat.Builder.create()
        .setDelimiter(';' as char)
        .setTrim(true)
        .build()

    Matrix matrix = CsvReader.readString(csvContent, format)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
    assertEquals(['2', 'Bob', '2023-01-02', '200,00'], matrix.row(1), "last row")
  }

  @Test
  void readStringNoHeader() {
    String csvContent = '''1,Alice,100.00
2,Bob,200.00'''

    Matrix matrix = CsvReader.readString(csvContent, CSVFormat.DEFAULT, false)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['c0', 'c1', 'c2'], matrix.columnNames(), "Auto-generated column names")
    assertEquals(['1', 'Alice', '100.00'], matrix.row(0), "first row")
  }

  @Test
  void readFileFromStringPath() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())
    String filePath = file.absolutePath

    Matrix matrix = CsvReader.readFile(filePath, CSVFormat.Builder.create().setTrim(true).build())

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readUrlFromString() {
    URL url = getClass().getResource("/basic.csv")
    String urlString = url.toString()

    Matrix matrix = CsvReader.readUrl(urlString, CSVFormat.Builder.create().setTrim(true).build())

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void readFromReader() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    StringReader reader = new StringReader(csvContent)
    Matrix matrix = CsvReader.read(reader)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Alice', '30'], matrix.row(0), "first row")
  }

  @Test
  void readFromInputStream() {
    String csvContent = '''name,age
Alice,30
Bob,25'''

    InputStream is = new ByteArrayInputStream(csvContent.bytes)
    Matrix matrix = CsvReader.read(is)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], matrix.columnNames(), "Column names")
    assertEquals(['Bob', '25'], matrix.row(1), "last row")
  }

  @Test
  void readFromPath() {
    URL url = getClass().getResource("/basic.csv")
    File file = new File(url.toURI())

    Matrix matrix = CsvReader.read(file.toPath(), CSVFormat.Builder.create().setTrim(true).build())

    assertEquals(4, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void testEmptyCsv() {
    String csvContent = ''

    Matrix matrix = CsvReader.readString(csvContent)

    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(0, matrix.rowCount(), "Empty CSV should have 0 rows")
    assertEquals(0, matrix.columnCount(), "Empty CSV should have 0 columns")
  }

  @Test
  void testHeaderOnlyCsv() {
    String csvContent = 'id,name,amount'

    Matrix matrix = CsvReader.readString(csvContent)

    assertNotNull(matrix, "Matrix should not be null")
    assertEquals(0, matrix.rowCount(), "Header-only CSV should have 0 data rows")
    assertEquals(3, matrix.columnCount(), "Should have 3 columns")
    assertEquals(['id', 'name', 'amount'], matrix.columnNames(), "Column names")
  }

  @Test
  void testSingleRow() {
    String csvContent = '''name,score
Alice,95'''

    Matrix matrix = CsvReader.readString(csvContent)

    assertEquals(1, matrix.rowCount(), "Should have 1 row")
    assertEquals(['name', 'score'], matrix.columnNames(), "Column names")
    assertEquals(['Alice', '95'], matrix.row(0), "Single row content")
  }

  @Test
  void testQuotedFields() {
    String csvContent = '''"name","description","value"
"Alice","Works at ""Tech Corp""",100
"Bob","Said: ""Hello, World!""",200'''

    Matrix matrix = CsvReader.readString(csvContent)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['Alice', 'Works at "Tech Corp"', '100'], matrix.row(0), "Quoted values handled correctly")
    assertEquals(['Bob', 'Said: "Hello, World!"', '200'], matrix.row(1), "Escaped quotes handled correctly")
  }

  @Test
  void readCsvWithMapFormatAndComplexOptions() {
    URL url = getClass().getResource("/colonQuotesEmptyLine.csv")

    Matrix m = CsvReader.read(
        Trim: true,
        Delimiter: ';',
        IgnoreEmptyLines: true,
        Quote: '"',
        Header: ['id', 'name', 'date', 'amount'],
        url)

    assertEquals(4, m.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'date', 'amount'], m.columnNames(), "Column names")
    assertEquals(['4', 'Arne', '2023-Jul-01', '222,99'], m.row(3), "last row")
  }

  @Test
  void readStringWithMatrixName() {
    String csvContent = '''x,y
1,2
3,4'''

    InputStream is = new ByteArrayInputStream(csvContent.bytes)
    Matrix matrix = CsvReader.read(is, CSVFormat.DEFAULT, true, java.nio.charset.StandardCharsets.UTF_8, 'TestMatrix')

    assertEquals('TestMatrix', matrix.matrixName, "Matrix name should be set")
    assertEquals(2, matrix.rowCount(), "Number of rows")
  }
}
