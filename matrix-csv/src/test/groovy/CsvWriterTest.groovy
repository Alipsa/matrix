import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReader
import se.alipsa.matrix.csv.CsvWriter

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

class CsvWriterTest {

  @TempDir
  Path tempDir

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
    CsvWriter.write(matrix, outputFile)

    assertTrue(outputFile.exists(), "Output file should exist")

    // Read it back and verify
    Matrix result = CsvReader.read(outputFile)
    assertEquals(2, result.rowCount(), "Number of rows")
    assertEquals(['id', 'name', 'amount'], result.columnNames(), "Column names")
    assertEquals(['2', 'Bob', '200.00'], result.row(1), "Last row")
  }

  @Test
  void writeToFilePath() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    Path outputPath = tempDir.resolve('output2.csv')
    CsvWriter.write(matrix, outputPath)

    assertTrue(outputPath.toFile().exists(), "Output file should exist")

    Matrix result = CsvReader.read(outputPath)
    assertEquals(2, result.rowCount(), "Number of rows")
  }

  @Test
  void writeToStringPath() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b'])
        .rows([['1', '2']])
        .build()

    String filePath = tempDir.resolve('output3.csv').toString()
    CsvWriter.write(matrix, filePath)

    assertTrue(new File(filePath).exists(), "Output file should exist")
  }

  @Test
  void writeToString() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'age'])
        .rows([
            ['Alice', '30'],
            ['Bob', '25'],
            ['Charlie', '35']
        ])
        .build()

    String csvContent = CsvWriter.writeString(matrix)

    assertNotNull(csvContent, "CSV content should not be null")
    assertTrue(csvContent.contains('name,age'), "Should contain header")
    assertTrue(csvContent.contains('Alice,30'), "Should contain first row")
    assertTrue(csvContent.contains('Bob,25'), "Should contain second row")
    assertTrue(csvContent.contains('Charlie,35'), "Should contain third row")

    // Verify by parsing it back
    Matrix result = CsvReader.readString(csvContent)
    assertEquals(3, result.rowCount(), "Number of rows")
    assertEquals(['name', 'age'], result.columnNames(), "Column names")
  }

  @Test
  void writeToStringWithoutHeader() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([['1', '2'], ['3', '4']])
        .build()

    String csvContent = CsvWriter.writeString(matrix, CSVFormat.DEFAULT, false)

    assertFalse(csvContent.contains('x,y'), "Should not contain header")
    assertTrue(csvContent.contains('1,2'), "Should contain first row")
    assertTrue(csvContent.contains('3,4'), "Should contain second row")
  }

  @Test
  void writeExcelCsv() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'value'])
        .rows([['Test', '123']])
        .build()

    File outputFile = tempDir.resolve('excel.csv').toFile()
    CsvWriter.writeExcelCsv(matrix, outputFile)

    assertTrue(outputFile.exists(), "Excel CSV file should exist")

    // Read it back
    Matrix result = CsvReader.read(outputFile, CSVFormat.EXCEL)
    assertEquals(1, result.rowCount(), "Number of rows")
    assertEquals(['name', 'value'], result.columnNames(), "Column names")
  }

  @Test
  void writeExcelCsvString() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b'])
        .rows([['1', '2']])
        .build()

    String csvContent = CsvWriter.writeExcelCsvString(matrix)

    assertNotNull(csvContent, "Excel CSV string should not be null")
    assertTrue(csvContent.contains('a,b'), "Should contain header")
    assertTrue(csvContent.contains('1,2'), "Should contain data")
  }

  @Test
  void writeTsv() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'age', 'city'])
        .rows([
            ['Alice', '30', 'NYC'],
            ['Bob', '25', 'LA']
        ])
        .build()

    File outputFile = tempDir.resolve('output.tsv').toFile()
    CsvWriter.writeTsv(matrix, outputFile)

    assertTrue(outputFile.exists(), "TSV file should exist")

    // Read it back
    Matrix result = CsvReader.read(outputFile, CSVFormat.TDF)
    assertEquals(2, result.rowCount(), "Number of rows")
    assertEquals(['name', 'age', 'city'], result.columnNames(), "Column names")
  }

  @Test
  void writeTsvString() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([['1', '2', '3']])
        .build()

    String tsvContent = CsvWriter.writeTsvString(matrix)

    assertNotNull(tsvContent, "TSV string should not be null")
    assertTrue(tsvContent.contains("x\ty\tz"), "Should contain tab-separated header")
    assertTrue(tsvContent.contains("1\t2\t3"), "Should contain tab-separated data")
  }

  @Test
  void writeToWriter() {
    Matrix matrix = Matrix.builder()
        .columnNames(['col1', 'col2'])
        .rows([['a', 'b'], ['c', 'd']])
        .build()

    StringWriter writer = new StringWriter()
    CsvWriter.write(matrix, writer)

    String csvContent = writer.toString()
    assertTrue(csvContent.contains('col1,col2'), "Should contain header")
    assertTrue(csvContent.contains('a,b'), "Should contain first row")
    assertTrue(csvContent.contains('c,d'), "Should contain second row")
  }

  @Test
  void writeWithCustomFormat() {
    Matrix matrix = Matrix.builder()
        .columnNames(['name', 'value'])
        .rows([['Alice', '100'], ['Bob', '200']])
        .build()

    CSVFormat format = CSVFormat.Builder.create()
        .setDelimiter(';' as char)
        .build()

    String csvContent = CsvWriter.writeString(matrix, format)

    assertTrue(csvContent.contains('name;value'), "Should use semicolon delimiter")
    assertTrue(csvContent.contains('Alice;100'), "Data should use semicolon delimiter")
  }

  @Test
  void writeToDirectory() {
    Matrix matrix = Matrix.builder()
        .matrixName('TestMatrix')
        .columnNames(['a', 'b'])
        .rows([['1', '2']])
        .build()

    File directory = tempDir.toFile()
    CsvWriter.write(matrix, directory)

    File expectedFile = new File(directory, 'TestMatrix.csv')
    assertTrue(expectedFile.exists(), "File should be created in directory with matrix name")

    Matrix result = CsvReader.read(expectedFile)
    assertEquals(1, result.rowCount(), "Number of rows")
  }

  @Test
  void writeEmptyMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b', 'c'])
        .build()

    String csvContent = CsvWriter.writeString(matrix)

    assertTrue(csvContent.contains('a,b,c'), "Should contain header even for empty matrix")
    assertFalse(csvContent.contains('null'), "Should not contain null values")
  }

  @Test
  void writeMatrixWithNullMatrixName() {
    Matrix matrix = Matrix.builder()
        .columnNames(['x'])
        .rows([['1']])
        .build()

    File directory = tempDir.toFile()
    CsvWriter.write(matrix, directory)

    File expectedFile = new File(directory, 'matrix.csv')
    assertTrue(expectedFile.exists(), "Should use 'matrix' as default name")
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

    // Write to string
    String csvContent = CsvWriter.writeString(original)

    // Read it back
    Matrix result = CsvReader.readString(csvContent)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original.row(i).toList(), result.row(i).toList(), "Row $i should match")
    }
  }
}
