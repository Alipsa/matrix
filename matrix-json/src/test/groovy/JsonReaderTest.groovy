import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.toLocalDates

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader

import java.nio.file.Path
import java.time.LocalDate

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
        emp_name: ['Rick','Dan','Michelle'],
        salary: [623.3,515.2,611.0],
        start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15'))
      .types(int, String, Number, LocalDate)
      .build()

    assertEquals(empData, matrix, empData.diff(matrix))
    assertEquals(BigDecimal, matrix.row(0)[2].class, 'Decimal JSON values should deserialize as BigDecimal')
  }

  @Test
  void testReadFile() {
    String jsonContent = '[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]'
    File jsonFile = tempDir.resolve('test.json').toFile()
    jsonFile.text = jsonContent

    Matrix matrix = JsonReader.read(jsonFile)

    assertEquals(2, matrix.rowCount(), 'Number of rows')
    assertEquals(['id', 'name'], matrix.columnNames(), 'Column names')
    assertEquals([1, 'Alice'], matrix.row(0).toList(), 'First row')
    assertEquals([2, 'Bob'], matrix.row(1).toList(), 'Second row')
  }

  @Test
  void testReadFileFromPath() {
    String jsonContent = '[{"x":10,"y":20},{"x":30,"y":40}]'
    File jsonFile = tempDir.resolve('data.json').toFile()
    jsonFile.text = jsonContent

    String filePath = jsonFile.absolutePath
    Matrix matrix = JsonReader.readFile(filePath)

    assertEquals(2, matrix.rowCount(), 'Number of rows')
    assertEquals(['x', 'y'], matrix.columnNames(), 'Column names')
  }

  @Test
  void testReadFromReader() {
    String jsonContent = '[{"a":1},{"a":2},{"a":3}]'
    StringReader reader = new StringReader(jsonContent)

    Matrix matrix = JsonReader.read(reader)

    assertEquals(3, matrix.rowCount(), 'Number of rows')
    assertEquals(['a'], matrix.columnNames(), 'Column names')
  }

  @Test
  void testReadFromInputStream() {
    String jsonContent = '[{"name":"test","value":123}]'
    InputStream is = new ByteArrayInputStream(jsonContent.bytes)

    Matrix matrix = JsonReader.read(is)

    assertEquals(1, matrix.rowCount(), 'Number of rows')
    assertEquals(['name', 'value'], matrix.columnNames(), 'Column names')
    assertEquals(['test', 123], matrix.row(0).toList(), 'Row content')
  }

  @Test
  void testReadFromPath() {
    String jsonContent = '[{"col1":"a","col2":"b"}]'
    Path jsonPath = tempDir.resolve('test.json')
    jsonPath.toFile().text = jsonContent

    Matrix matrix = JsonReader.read(jsonPath)

    assertEquals(1, matrix.rowCount(), 'Number of rows')
  }

  @Test
  void testReadFromUrl() {
    String jsonContent = '[{"id":1},{"id":2}]'
    File jsonFile = tempDir.resolve('url_test.json').toFile()
    jsonFile.text = jsonContent

    URL url = jsonFile.toURI().toURL()
    Matrix matrix = JsonReader.read(url)

    assertEquals(2, matrix.rowCount(), 'Number of rows')
    assertEquals(['id'], matrix.columnNames(), 'Column names')
  }

  @Test
  void testReadUrlFromString() {
    String jsonContent = '[{"key":"value"}]'
    File jsonFile = tempDir.resolve('string_url_test.json').toFile()
    jsonFile.text = jsonContent

    String urlString = jsonFile.toURI().toURL() as String
    Matrix matrix = JsonReader.readUrl(urlString)

    assertEquals(1, matrix.rowCount(), 'Number of rows')
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

    assertEquals(2, matrix.rowCount(), 'Number of rows')
    assertTrue(matrix.columnNames().contains('person.name'), 'Should flatten nested objects')
    assertTrue(matrix.columnNames().contains('person.age'), 'Should flatten nested objects')
    assertEquals('Alice', matrix.row(0)[matrix.columnNames().indexOf('person.name')], 'Nested value')
  }

  @Test
  void testArrays() {
    String json = '''[
      {"items": [1, 2, 3]}
    ]'''

    Matrix matrix = JsonReader.read(json)

    assertEquals(1, matrix.rowCount(), 'Number of rows')
    assertTrue(matrix.columnNames().contains('items[0]'), 'Should flatten arrays')
    assertTrue(matrix.columnNames().contains('items[1]'), 'Should flatten arrays')
    assertTrue(matrix.columnNames().contains('items[2]'), 'Should flatten arrays')
  }

  @Test
  void testEmptyArray() {
    String json = '[]'

    Matrix matrix = JsonReader.read(json)

    assertEquals(0, matrix.rowCount(), 'Empty array should produce empty matrix')
    assertEquals(0, matrix.columnCount(), 'Empty array should have no columns')
  }

  @Test
  void testEmptyObjectRowPreservesRowCount() {
    Matrix matrix = JsonReader.read('[{}]')

    assertEquals(1, matrix.rowCount(), 'A single empty object should still count as one row')
    assertEquals(0, matrix.columnCount(), 'An empty object has no columns')
  }

  @Test
  void testAllEmptyObjectRowsPreserveRowCount() {
    Matrix matrix = JsonReader.read('[{}, {}, {}]')

    assertEquals(3, matrix.rowCount(), 'Every row should be counted even though none has any keys')
    assertEquals(0, matrix.columnCount(), 'No keys anywhere in the document means no columns')
  }

  @Test
  void testRowWithOnlyEmptyNestedContainersPreservesRowCount() {
    // Neither an empty array nor an empty object produces a leaf key/value pair after flattening
    Matrix matrix = JsonReader.read('[{"a": [], "b": {}}]')

    assertEquals(1, matrix.rowCount(), 'Row with only empty nested containers should still count as one row')
    assertEquals(0, matrix.columnCount(), 'Empty nested containers do not produce any columns')
  }

  @Test
  void testMixedEmptyAndNonEmptyObjectRows() {
    // Regression guard: once a column exists, trailing/leading empty-object rows must still be
    // backfilled with nulls rather than dropped - this already worked before the row-count fix.
    Matrix matrix = JsonReader.read('[{"a":1},{},{}]')

    assertEquals(3, matrix.rowCount(), 'Number of rows')
    assertEquals(['a'], matrix.columnNames(), 'Column names')
    assertEquals([1, null, null], matrix.column('a'), 'Empty rows should be backfilled with null')
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

    assertEquals(3, matrix.rowCount(), 'Number of rows')
    assertEquals(3, matrix.columnCount(), 'Should have all unique keys as columns')
    assertTrue(matrix.columnNames().contains('a'), "Should have column 'a'")
    assertTrue(matrix.columnNames().contains('b'), "Should have column 'b'")
    assertTrue(matrix.columnNames().contains('c'), "Should have column 'c'")

    // Check nulls for missing values
    assertNull(matrix.row(0)[matrix.columnNames().indexOf('c')], 'Missing value should be null')
    assertNull(matrix.row(1)[matrix.columnNames().indexOf('b')], 'Missing value should be null')
    assertNull(matrix.row(2)[matrix.columnNames().indexOf('a')], 'Missing value should be null')
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

    assertThrows(IllegalArgumentException) {
      JsonReader.read(json)
    }
  }

  @Test
  void testCharsetHandling() {
    String jsonContent = '[{"text":"Grüße"}]'
    File jsonFile = tempDir.resolve('charset_test.json').toFile()
    jsonFile.write(jsonContent, 'UTF-8')

    Matrix matrix = JsonReader.read(jsonFile, java.nio.charset.StandardCharsets.UTF_8)

    assertEquals(1, matrix.rowCount(), 'Number of rows')
    assertEquals('Grüße', matrix.row(0)[matrix.columnNames().indexOf('text')], 'Should handle UTF-8 characters')
  }

  @Test
  void testMatrixNameDerivedFromFile() {
    String jsonContent = '[{"id":1}]'
    File jsonFile = tempDir.resolve('employees.json').toFile()
    jsonFile.text = jsonContent

    Matrix matrix = JsonReader.read(jsonFile)

    assertEquals('employees', matrix.matrixName, 'Matrix name should be derived from filename without extension')
  }

  @Test
  void testMatrixNameDerivedFromUrl() {
    String jsonContent = '[{"id":1}]'
    File jsonFile = tempDir.resolve('people.json').toFile()
    jsonFile.text = jsonContent

    URL url = jsonFile.toURI().toURL()
    Matrix matrix = JsonReader.read(url)

    assertEquals('people', matrix.matrixName, 'Matrix name should be derived from URL filename')
  }

  @Test
  void testReadStringAlias() {
    String json = '[{"id":1,"name":"Test"}]'
    Matrix fromRead = JsonReader.read(json)
    Matrix fromReadString = JsonReader.readString(json)

    assertEquals(fromRead.rowCount(), fromReadString.rowCount(), 'readString should produce same row count as read')
    assertEquals(fromRead.columnNames(), fromReadString.columnNames(), 'readString should produce same columns as read')
  }

  @Test
  void testNestedPojoSerialization() {
    ObjectMapper mapper = new ObjectMapper()

    Family f1 = new Family('Nyfelt-Wersäll')
    f1.members.add(new Person('Per'))
    f1.members.add(new Person('Louise'))

    Family f2 = new Family('Nyfelt-Anselius')
    f2.members.add(new Person('Jonas'))
    f2.members.add(new Person('Kristin'))
    f2.members.add(new Person('Frida'))
    f2.members.add(new Person('Oliver'))

    String json = mapper.writeValueAsString([f1, f2])
    JsonNode root = mapper.readTree(json)
    Matrix jacksonMatrix = toMatrix(root)

    Matrix m = JsonReader.read(json)
    assertEquals(jacksonMatrix, m, jacksonMatrix.diff(m))
  }

  Matrix toMatrix(JsonNode root) {
    List<Map<String, String>> flatMaps = []
    Set<String> keySet = new LinkedHashSet<>()
    root.each {
      Map<String, String> flatMap = [:]
      addKeys('', it, flatMap)
      flatMaps << flatMap
      keySet.addAll(flatMap.keySet())
    }
    List rows = []
    for (int i = 0; i < flatMaps.size(); i++) {
      def m = flatMaps.get(i)
      keySet.each {
        if (!m.containsKey(it)) {
          m.put(it, null)
        }
      }
      rows << new ArrayList<>(m.values())
    }
    Matrix.builder()
        .rows(rows)
        .columnNames(keySet)
        .build()
  }

  def addKeys(String currentPath, JsonNode jsonNode, Map<String, Object> map) {
    if (jsonNode.isObject()) {
      ObjectNode objectNode = (ObjectNode) jsonNode
      Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields()
      String pathPrefix = currentPath.isEmpty() ? '' : currentPath + '.'

      while (iter.hasNext()) {
        Map.Entry<String, JsonNode> entry = iter.next()
        addKeys(pathPrefix + entry.getKey(), entry.getValue(), map)
      }
    } else if (jsonNode.isArray()) {
      ArrayNode arrayNode = (ArrayNode) jsonNode
      for (int i = 0; i < arrayNode.size(); i++) {
        addKeys(currentPath + '[' + i + ']', arrayNode.get(i), map)
      }
    } else if (jsonNode.isValueNode()) {
      if (jsonNode instanceof NumericNode) {
        NumericNode node = jsonNode as NumericNode
        map.put(currentPath, node.numberValue())
      } else if (jsonNode instanceof BooleanNode) {
        BooleanNode node = jsonNode as BooleanNode
        map.put(currentPath, node.booleanValue())
      } else {
        ValueNode valueNode = (ValueNode) jsonNode
        map.put(currentPath, valueNode.asText())
      }
    }
  }

  class Person {
    Person(String name) {
      this.name = name
    }
    String name
  }

  class Family {
    Family(String name) {
      this.name = name
    }
    String name
    List<Person> members = []
  }

  @Test
  void testReadObjectInsteadOfArrayThrows() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      JsonReader.read('{"not": "an array"}')
    }
    assertTrue(ex.message.contains('Expected JSON array'), 'Error should mention array expectation')
  }

  @Test
  void testMalformedJsonThrows() {
    assertThrows(Exception) {
      JsonReader.read('[{"a": 1,}]')  // Trailing comma
    }
  }

  @Test
  void testReadNonexistentFileThrows() {
    assertThrows(FileNotFoundException) {
      JsonReader.read(new File('/nonexistent/path/file.json'))
    }
  }
}
