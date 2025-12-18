package se.alipsa.matrix.json

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Imports JSON arrays into Matrix format using Jackson streaming API.
 * This approach uses constant memory regardless of JSON size by processing
 * one row at a time instead of loading the entire document into memory.
 */
@CompileStatic
class JsonImporter {

  private static final ObjectMapper MAPPER = new ObjectMapper()
  private static final JsonFactory FACTORY = MAPPER.getFactory()

  private JsonImporter() {
    // Only static methods
  }

  /**
   * Parse a JSON string containing an array of objects into a Matrix.
   *
   * @param str a JSON string containing a list of rows (JSON objects)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix parse(String str) {
    FACTORY.createParser(str).withCloseable { JsonParser parser ->
      return parseStream(parser)
    }
  }

  /**
   * Parse JSON from an InputStream into a Matrix.
   *
   * @param is the input stream containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix parse(InputStream is, Charset charset = StandardCharsets.UTF_8) {
    FACTORY.createParser(new InputStreamReader(is, charset)).withCloseable { JsonParser parser ->
      return parseStream(parser)
    }
  }

  /**
   * Parse JSON from a File into a Matrix.
   *
   * @param file the file containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix parse(File file, Charset charset = StandardCharsets.UTF_8) {
    FACTORY.createParser(new InputStreamReader(new FileInputStream(file), charset)).withCloseable { JsonParser parser ->
      return parseStream(parser)
    }
  }

  /**
   * Parse JSON from a Reader into a Matrix.
   *
   * @param reader the reader providing JSON content
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix parse(Reader reader) {
    FACTORY.createParser(reader).withCloseable { JsonParser parser ->
      return parseStream(parser)
    }
  }

  /**
   * Stream-parse a JSON array into a Matrix.
   * Uses single-pass algorithm with dynamic column creation.
   * Memory usage is O(columns * rows) for the final Matrix only,
   * not O(rows) for intermediate parsed objects.
   */
  private static Matrix parseStream(JsonParser parser) {
    // Column tracking - maps column name to column index
    Map<String, Integer> columnIndex = new LinkedHashMap<>()
    List<String> columnNames = new ArrayList<>()
    List<List<Object>> columns = new ArrayList<>()
    int rowCount = 0

    // Expect array start
    JsonToken token = parser.nextToken()
    if (token != JsonToken.START_ARRAY) {
      throw new IllegalArgumentException("Expected JSON array, got ${token}")
    }

    // Process each object in the array
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      // Read single object as Map (this is the only object in memory at a time)
      Map<String, Object> rowObj = MAPPER.readValue(parser, Map)

      // Flatten nested structures to dot-notation keys
      Map<String, Object> flatRow = new LinkedHashMap<>()
      flatten('', rowObj, flatRow)

      // Track which columns received values in this row
      Set<Integer> columnsUpdated = new HashSet<>()

      // Process each key-value pair
      for (Map.Entry<String, Object> entry : flatRow.entrySet()) {
        String key = entry.getKey()
        Object value = entry.getValue()

        Integer colIdx = columnIndex.get(key)
        if (colIdx == null) {
          // New column discovered - create it and backfill with nulls
          colIdx = columnNames.size()
          columnIndex.put(key, colIdx)
          columnNames.add(key)
          List<Object> newColumn = new ArrayList<>()
          // Backfill nulls for previous rows
          for (int i = 0; i < rowCount; i++) {
            newColumn.add(null)
          }
          columns.add(newColumn)
        }
        columns.get(colIdx).add(value)
        columnsUpdated.add(colIdx)
      }

      // Fill nulls for columns not present in this row
      for (int colIdx = 0; colIdx < columnNames.size(); colIdx++) {
        if (!columnsUpdated.contains(colIdx)) {
          // This column wasn't in the current row, add null
          columns.get(colIdx).add(null)
        }
      }

      rowCount++
    }

    if (rowCount == 0) {
      return Matrix.builder().build()
    }

    return Matrix.builder()
        .columnNames(columnNames)
        .columns(columns)
        .build()
  }

  /**
   * Flatten a nested structure into dot-notation keys.
   * Example: {"a": {"b": 1}} becomes {"a.b": 1}
   * Arrays are indexed: {"arr": [1,2]} becomes {"arr[0]": 1, "arr[1]": 2}
   * 
   * @throws IllegalArgumentException if duplicate keys are detected after flattening
   */
  private static void flatten(String prefix, Object node, Map<String, Object> result) {
    if (node instanceof Map) {
      Map<String, Object> mapNode = (Map<String, Object>) node
      String pathPrefix = prefix.isEmpty() ? '' : prefix + '.'
      for (Map.Entry<String, Object> entry : mapNode.entrySet()) {
        flatten(pathPrefix + entry.getKey(), entry.getValue(), result)
      }
    } else if (node instanceof List) {
      List listNode = (List) node
      int size = listNode.size()
      for (int idx = 0; idx < size; idx++) {
        flatten(prefix + '[' + idx + ']', listNode.get(idx), result)
      }
    } else {
      // Check for duplicate keys before inserting
      if (result.containsKey(prefix)) {
        throw new IllegalArgumentException(
            "Duplicate key detected after flattening: '${prefix}'. " +
            "This can occur when JSON contains both a literal key and a nested structure " +
            "that flatten to the same path (e.g., {\"a.b\": 1, \"a\": {\"b\": 2}}). " +
            "Previous value: ${result.get(prefix)}, New value: ${node}"
        )
      }
      result.put(prefix, node)
    }
  }
}
