import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader

import java.nio.file.Path
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.toLocalDates

class JsonReaderTest {

  @TempDir
  Path tempDir

  @Test
  void testReadString() {
    def matrix = JsonReader.read('''[
        {
          "emp_id": 1,
          "emp_name": "Rick",
          "salary": 623.3,
          "start_date": "2012-01-01"
        },
        {
          "emp_id": 2,
          "emp_name": "Dan",
          "salary": 515.2,
          "start_date": "2013-09-23"
        },
        {
          "emp_id": 3,
          "emp_name": "Michelle",
          "salary": 611.0,
          "start_date": "2014-11-15"
        }
    ]''').convert([int, String, Number, LocalDate])

    Matrix empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick","Dan","Michelle"],
        salary: [623.3,515.2,611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
      .types(int, String, Number, LocalDate)
      .build()

    assertEquals(empData, matrix, empData.diff(matrix))
  }

  @Test
  void testReadFile() {
    String jsonContent = '[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]'
    File jsonFile = tempDir.resolve('test.json').toFile()
    jsonFile.text = jsonContent

    Matrix matrix = JsonReader.read(jsonFile)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['id', 'name'], matrix.columnNames(), "Column names")
    assertEquals([1, 'Alice'], matrix.row(0).toList(), "First row")
    assertEquals([2, 'Bob'], matrix.row(1).toList(), "Second row")
  }

  @Test
  void testReadFileFromPath() {
    String jsonContent = '[{"x":10,"y":20},{"x":30,"y":40}]'
    File jsonFile = tempDir.resolve('data.json').toFile()
    jsonFile.text = jsonContent

    String filePath = jsonFile.absolutePath
    Matrix matrix = JsonReader.readFile(filePath)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['x', 'y'], matrix.columnNames(), "Column names")
  }

  @Test
  void testReadFromReader() {
    String jsonContent = '[{"a":1},{"a":2},{"a":3}]'
    StringReader reader = new StringReader(jsonContent)

    Matrix matrix = JsonReader.read(reader)

    assertEquals(3, matrix.rowCount(), "Number of rows")
    assertEquals(['a'], matrix.columnNames(), "Column names")
  }

  @Test
  void testReadFromInputStream() {
    String jsonContent = '[{"name":"test","value":123}]'
    InputStream is = new ByteArrayInputStream(jsonContent.bytes)

    Matrix matrix = JsonReader.read(is)

    assertEquals(1, matrix.rowCount(), "Number of rows")
    assertEquals(['name', 'value'], matrix.columnNames(), "Column names")
    assertEquals(['test', 123], matrix.row(0).toList(), "Row content")
  }

  @Test
  void testReadFromPath() {
    String jsonContent = '[{"col1":"a","col2":"b"}]'
    Path jsonPath = tempDir.resolve('test.json')
    jsonPath.toFile().text = jsonContent

    Matrix matrix = JsonReader.read(jsonPath)

    assertEquals(1, matrix.rowCount(), "Number of rows")
  }

  @Test
  void testReadFromUrl() {
    String jsonContent = '[{"id":1},{"id":2}]'
    File jsonFile = tempDir.resolve('url_test.json').toFile()
    jsonFile.text = jsonContent

    URL url = jsonFile.toURI().toURL()
    Matrix matrix = JsonReader.read(url)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertEquals(['id'], matrix.columnNames(), "Column names")
  }

  @Test
  void testReadUrlFromString() {
    String jsonContent = '[{"key":"value"}]'
    File jsonFile = tempDir.resolve('string_url_test.json').toFile()
    jsonFile.text = jsonContent

    String urlString = jsonFile.toURI().toURL().toString()
    Matrix matrix = JsonReader.readUrl(urlString)

    assertEquals(1, matrix.rowCount(), "Number of rows")
  }

  @Test
  void testNestedObjects() {
    String json = '''[
      {
        "person": {
          "name": "Alice",
          "age": 30
        },
        "city": "NYC"
      },
      {
        "person": {
          "name": "Bob",
          "age": 25
        },
        "city": "LA"
      }
    ]'''

    Matrix matrix = JsonReader.read(json)

    assertEquals(2, matrix.rowCount(), "Number of rows")
    assertTrue(matrix.columnNames().contains('person.name'), "Should flatten nested objects")
    assertTrue(matrix.columnNames().contains('person.age'), "Should flatten nested objects")
    assertEquals('Alice', matrix.row(0)[matrix.columnNames().indexOf('person.name')], "Nested value")
  }

  @Test
  void testArrays() {
    String json = '''[
      {"items": [1, 2, 3]}
    ]'''

    Matrix matrix = JsonReader.read(json)

    assertEquals(1, matrix.rowCount(), "Number of rows")
    assertTrue(matrix.columnNames().contains('items[0]'), "Should flatten arrays")
    assertTrue(matrix.columnNames().contains('items[1]'), "Should flatten arrays")
    assertTrue(matrix.columnNames().contains('items[2]'), "Should flatten arrays")
  }

  @Test
  void testEmptyArray() {
    String json = '[]'

    Matrix matrix = JsonReader.read(json)

    assertEquals(0, matrix.rowCount(), "Empty array should produce empty matrix")
    assertEquals(0, matrix.columnCount(), "Empty array should have no columns")
  }

  @Test
  void testSparseData() {
    // Different rows have different keys
    String json = '''[
      {"a": 1, "b": 2},
      {"a": 3, "c": 4},
      {"b": 5, "c": 6}
    ]'''

    Matrix matrix = JsonReader.read(json)

    assertEquals(3, matrix.rowCount(), "Number of rows")
    assertEquals(3, matrix.columnCount(), "Should have all unique keys as columns")
    assertTrue(matrix.columnNames().contains('a'), "Should have column 'a'")
    assertTrue(matrix.columnNames().contains('b'), "Should have column 'b'")
    assertTrue(matrix.columnNames().contains('c'), "Should have column 'c'")

    // Check nulls for missing values
    assertNull(matrix.row(0)[matrix.columnNames().indexOf('c')], "Missing value should be null")
    assertNull(matrix.row(1)[matrix.columnNames().indexOf('b')], "Missing value should be null")
    assertNull(matrix.row(2)[matrix.columnNames().indexOf('a')], "Missing value should be null")
  }

  @Test
  void testDuplicateKeyAfterFlattening() {
    // This should throw because "a.b" exists both as a literal key and as a flattened path
    String json = '''[
      {
        "a.b": 1,
        "a": {"b": 2}
      }
    ]'''

    assertThrows(IllegalArgumentException.class, () -> {
      JsonReader.read(json)
    }, "Should throw on duplicate keys after flattening")
  }

  @Test
  void testCharsetHandling() {
    String jsonContent = '[{"text":"Grüße"}]'
    File jsonFile = tempDir.resolve('charset_test.json').toFile()
    jsonFile.write(jsonContent, 'UTF-8')

    Matrix matrix = JsonReader.read(jsonFile, java.nio.charset.StandardCharsets.UTF_8)

    assertEquals(1, matrix.rowCount(), "Number of rows")
    assertEquals("Grüße", matrix.row(0)[matrix.columnNames().indexOf('text')], "Should handle UTF-8 characters")
  }
}
