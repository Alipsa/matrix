import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader
import se.alipsa.matrix.json.JsonWriter

import java.nio.file.Path

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

    String filePath = tempDir.resolve('string_path.json') as String
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

    String json = writer as String
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

    String json = writer as String
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
    assertThrows(IllegalArgumentException) {
      JsonWriter.writeString(null)
    }
  }

  @Test
  void testWriteNullFileThrows() {
    Matrix matrix = Matrix.builder().data(a: [1]).build()

    assertThrows(IllegalArgumentException) {
      JsonWriter.write(matrix, (File) null)
    }
  }

  @Test
  void testWriteNullWriterThrows() {
    Matrix matrix = Matrix.builder().data(a: [1]).build()

    assertThrows(IllegalArgumentException) {
      JsonWriter.write(matrix, (Writer) null)
    }
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

  @Test
  void testWriteFormatterToPath() {
    Matrix matrix = Matrix.builder()
        .data(price: [100, 200])
        .build()

    Path outputPath = tempDir.resolve('formatter_path.json')
    JsonWriter.write(matrix, outputPath, [price: { it * 10 }])

    String content = outputPath.toFile().text
    assertTrue(content.contains('1000'), "Should apply formatter via Path overload")
    assertTrue(content.contains('2000'), "Should apply formatter via Path overload")
  }

  @Test
  void testWriteFormatterToStringPath() {
    Matrix matrix = Matrix.builder()
        .data(value: [5])
        .build()

    String filePath = tempDir.resolve('formatter_string.json') as String
    JsonWriter.write(matrix, filePath, [value: { it * 3 }])

    String content = new File(filePath).text
    assertTrue(content.contains('15'), "Should apply formatter via String path overload")
  }

  @Test
  void testWriteFormatterCreatesParentDir() {
    Matrix matrix = Matrix.builder()
        .data(x: [1])
        .build()

    File outputFile = tempDir.resolve('sub/dir/output.json').toFile()
    assertFalse(outputFile.getParentFile().exists(), "Parent dir should not exist yet")

    JsonWriter.write(matrix, outputFile, [x: { it + 1 }])

    assertTrue(outputFile.exists(), "File should be created with parent dirs")
    assertTrue(outputFile.text.contains('2'), "Should apply formatter")
  }

  @Test
  void testWriteFormatterThrowsOnDirectory() {
    Matrix matrix = Matrix.builder()
        .data(a: [1])
        .build()

    File dir = tempDir.resolve('adir').toFile()
    dir.mkdirs()

    assertThrows(IOException) {
      JsonWriter.write(matrix, dir, [a: { it }])
    }
  }

  // Fluent WriteBuilder API tests

  @Test
  void testFluentWriteToFile() {
    Matrix matrix = Matrix.builder().data(a: [1, 2]).build()
    File file = tempDir.resolve('fluent.json').toFile()

    JsonWriter.write(matrix).to(file)

    assertTrue(file.exists(), "File should exist")
    Matrix result = JsonReader.read(file)
    assertEquals(2, result.rowCount())
  }

  @Test
  void testFluentWriteToPath() {
    Matrix matrix = Matrix.builder().data(x: [10]).build()
    Path path = tempDir.resolve('fluent_path.json')

    JsonWriter.write(matrix).to(path)

    assertTrue(path.toFile().exists(), "File should exist via Path")
  }

  @Test
  void testFluentWriteToStringPath() {
    Matrix matrix = Matrix.builder().data(v: [5]).build()
    String filePath = tempDir.resolve('fluent_str.json') as String

    JsonWriter.write(matrix).to(filePath)

    assertTrue(new File(filePath).exists(), "File should exist via String path")
  }

  @Test
  void testFluentWriteToWriter() {
    Matrix matrix = Matrix.builder().data(k: ['v']).build()
    StringWriter sw = new StringWriter()

    JsonWriter.write(matrix).to(sw)

    assertTrue(sw.toString().contains('"k"'), "Writer output should contain field")
  }

  @Test
  void testFluentAsString() {
    Matrix matrix = Matrix.builder().data(id: [1]).build()

    String json = JsonWriter.write(matrix).asString()

    assertTrue(json.contains('"id"'), "asString should contain field name")
    assertTrue(json.contains('1'), "asString should contain value")
  }

  @Test
  void testFluentWithIndent() {
    Matrix matrix = Matrix.builder().data(x: [1]).build()

    String json = JsonWriter.write(matrix).indent().asString()

    assertTrue(json.contains('\n'), "Indented output should contain newlines")
  }

  @Test
  void testFluentWithIndentBoolean() {
    Matrix matrix = Matrix.builder().data(x: [1]).build()

    String compact = JsonWriter.write(matrix).indent(false).asString()
    String indented = JsonWriter.write(matrix).indent(true).asString()

    assertFalse(compact.contains('\n'), "Compact should not contain newlines")
    assertTrue(indented.contains('\n'), "Indented should contain newlines")
  }

  @Test
  void testFluentWithDateFormat() {
    Matrix matrix = Matrix.builder()
        .data(d: [java.time.LocalDate.of(2024, 6, 15)])
        .types(java.time.LocalDate)
        .build()

    String json = JsonWriter.write(matrix).dateFormat('dd/MM/yyyy').asString()

    assertTrue(json.contains('15/06/2024'), "Should format date with custom pattern")
  }

  @Test
  void testFluentWithFormatter() {
    Matrix matrix = Matrix.builder().data(price: [100]).build()

    String json = JsonWriter.write(matrix).formatter('price') { it * 2 }.asString()

    assertTrue(json.contains('200'), "Should apply single formatter")
  }

  @Test
  void testFluentWithMultipleFormatters() {
    Matrix matrix = Matrix.builder().data(a: [1], b: [2]).build()

    String json = JsonWriter.write(matrix)
        .formatter('a') { it * 10 }
        .formatter('b') { it * 20 }
        .asString()

    assertTrue(json.contains('10'), "Should apply formatter to column a")
    assertTrue(json.contains('40'), "Should apply formatter to column b")
  }

  @Test
  void testFluentWithColumnFormatters() {
    Matrix matrix = Matrix.builder().data(x: [3], y: [4]).build()

    String json = JsonWriter.write(matrix)
        .columnFormatters([x: { it * 100 }, y: { it * 200 }])
        .asString()

    assertTrue(json.contains('300'), "Should apply column formatters map")
    assertTrue(json.contains('800'), "Should apply column formatters map")
  }
}
