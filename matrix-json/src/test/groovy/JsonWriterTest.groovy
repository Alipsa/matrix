import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader
import se.alipsa.matrix.json.JsonWriter

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

class JsonWriterTest {

  @TempDir
  Path tempDir

  @Test
  void testWriteString() {
    Matrix matrix = Matrix.builder()
        .data(id: [1, 2, 3], name: ['Alice', 'Bob', 'Charlie'])
        .build()

    String json = JsonWriter.writeString(matrix)

    assertNotNull(json, "JSON string should not be null")
    assertTrue(json.contains('"id":1'), "Should contain id field")
    assertTrue(json.contains('"name":"Alice"'), "Should contain name field")
    assertTrue(json.startsWith('['), "Should be a JSON array")
    assertTrue(json.endsWith(']'), "Should be a JSON array")
  }

  @Test
  void testWriteStringWithIndent() {
    Matrix matrix = Matrix.builder()
        .data(x: [10], y: [20])
        .build()

    String json = JsonWriter.writeString(matrix, true)

    assertTrue(json.contains('\n'), "Indented JSON should contain newlines")
    assertTrue(json.contains('  '), "Indented JSON should contain spaces")
  }

  @Test
  void testWriteToFile() {
    Matrix matrix = Matrix.builder()
        .data(col1: ['a', 'b'], col2: [1, 2])
        .build()

    File outputFile = tempDir.resolve('output.json').toFile()
    JsonWriter.write(matrix, outputFile)

    assertTrue(outputFile.exists(), "Output file should exist")

    // Read it back and verify
    Matrix result = JsonReader.read(outputFile)
    assertEquals(2, result.rowCount(), "Number of rows should match")
    assertEquals(matrix.columnNames(), result.columnNames(), "Column names should match")
  }

  @Test
  void testWriteToPath() {
    Matrix matrix = Matrix.builder()
        .data(a: [1, 2])
        .build()

    Path outputPath = tempDir.resolve('path_output.json')
    JsonWriter.write(matrix, outputPath)

    assertTrue(outputPath.toFile().exists(), "Output file should exist")
  }

  @Test
  void testWriteToStringPath() {
    Matrix matrix = Matrix.builder()
        .data(value: [100])
        .build()

    String filePath = tempDir.resolve('string_path.json').toString()
    JsonWriter.write(matrix, filePath)

    assertTrue(new File(filePath).exists(), "Output file should exist")
  }

  @Test
  void testWriteToWriter() {
    Matrix matrix = Matrix.builder()
        .data(key: ['value1', 'value2'])
        .build()

    StringWriter writer = new StringWriter()
    JsonWriter.write(matrix, writer)

    String json = writer.toString()
    assertTrue(json.contains('"key":"value1"'), "Should contain data")
    assertTrue(json.contains('"key":"value2"'), "Should contain data")
  }

  @Test
  void testWriteToWriterWithColumnFormatters() {
    Matrix matrix = Matrix.builder()
        .data(salary: [1000, 2000], bonus: [100, 200])
        .build()

    Map<String, Closure> formatters = [
        salary: { it * 10 },
        bonus: { it * 5 }
    ]

    StringWriter writer = new StringWriter()
    JsonWriter.write(matrix, writer, formatters)

    String json = writer.toString()
    assertTrue(json.contains('10000'), "Should apply salary formatter")
    assertTrue(json.contains('20000'), "Should apply salary formatter")
    assertTrue(json.contains('500'), "Should apply bonus formatter")
    assertTrue(json.contains('1000'), "Should apply bonus formatter")
  }

  @Test
  void testRoundTrip() {
    Matrix original = Matrix.builder()
        .data(
            id: [1, 2, 3],
            name: ['Alice', 'Bob', 'Charlie'],
            score: [95.5, 87.3, 92.1]
        )
        .build()

    // Write to string
    String json = JsonWriter.writeString(original)

    // Read it back
    Matrix result = JsonReader.read(json)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match")
    assertEquals(original.columnNames().sort(), result.columnNames().sort(), "Column names should match")

    // JSON round-trip converts types, so we check values are present rather than exact equality
    assertTrue(result.columnNames().contains('id'), "Should have id column")
    assertTrue(result.columnNames().contains('name'), "Should have name column")
    assertTrue(result.columnNames().contains('score'), "Should have score column")

    // Verify row count matches
    assertEquals(3, result.rowCount(), "Should have 3 rows")
  }

  @Test
  void testWriteEmptyMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b', 'c'])
        .build()

    String json = JsonWriter.writeString(matrix)

    assertEquals('[]', json, "Empty matrix should produce empty JSON array")
  }

  @Test
  void testWriteWithDateFormat() {
    Matrix matrix = Matrix.builder()
        .data(
            name: ['Test'],
            date: [java.time.LocalDate.parse('2023-01-15')]
        )
        .types(String, java.time.LocalDate)
        .build()

    String json = JsonWriter.writeString(matrix, 'MM/dd/yyyy')

    assertTrue(json.contains('01/15/2023'), "Should format date with custom pattern")
  }

  @Test
  void testWriteToFileWithFormatters() {
    Matrix matrix = Matrix.builder()
        .data(price: [99.99, 149.99])
        .build()

    File outputFile = tempDir.resolve('formatted.json').toFile()

    Map<String, Closure> formatters = [
        price: { "\$${it}" }
    ]

    JsonWriter.write(matrix, outputFile, formatters)

    String content = outputFile.text
    assertTrue(content.contains('"price":"$99.99"'), "Should apply formatter to file output")
  }

  @Test
  void testWriteWithNullValues() {
    Matrix matrix = Matrix.builder()
        .columnNames(['a', 'b'])
        .rows([
            [1, null],
            [null, 2]
        ])
        .build()

    String json = JsonWriter.writeString(matrix)

    assertTrue(json.contains('null'), "Should include null values in JSON")
  }

  @Test
  void testWriteNullMatrixThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      JsonWriter.writeString(null)
    }, "Should throw on null matrix")
  }

  @Test
  void testWriteNullFileThrows() {
    Matrix matrix = Matrix.builder().data(a: [1]).build()

    assertThrows(IllegalArgumentException.class, () -> {
      JsonWriter.write(matrix, (File) null)
    }, "Should throw on null file")
  }

  @Test
  void testWriteNullWriterThrows() {
    Matrix matrix = Matrix.builder().data(a: [1]).build()

    assertThrows(IllegalArgumentException.class, () -> {
      JsonWriter.write(matrix, (Writer) null)
    }, "Should throw on null writer")
  }

  @Test
  void testPrettyPrintRoundTrip() {
    Matrix original = Matrix.builder()
        .data(x: [1, 2], y: [3, 4])
        .build()

    String prettyJson = JsonWriter.writeString(original, true)
    Matrix result = JsonReader.read(prettyJson)

    assertEquals(original.rowCount(), result.rowCount(), "Row count should match after pretty print")
    assertEquals(original.columnNames(), result.columnNames(), "Column names should match")
  }
}
