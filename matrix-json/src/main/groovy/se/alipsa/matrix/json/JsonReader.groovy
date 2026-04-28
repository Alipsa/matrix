package se.alipsa.matrix.json

import groovy.transform.CompileStatic

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

import se.alipsa.matrix.core.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Reads JSON arrays into Matrix format using Jackson streaming API.
 *
 * <p>This class uses constant memory regardless of JSON size by processing
 * one row at a time instead of loading the entire document into memory.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * // Read JSON string
 * Matrix m = JsonReader.read('[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]')
 *
 * // Read from file
 * Matrix m = JsonReader.read(new File("data.json"))
 *
 * // Read from file path
 * Matrix m = JsonReader.readFile("/path/to/data.json")
 *
 * // Read from URL
 * Matrix m = JsonReader.readUrl("https://example.com/data.json")
 * </pre>
 *
 * <h3>Nested Structure Handling</h3>
 * <p>Nested objects are automatically flattened to dot-notation keys:</p>
 * <ul>
 *   <li><code>{"a": {"b": 1}}</code> becomes <code>{"a.b": 1}</code></li>
 *   <li><code>{"arr": [1, 2]}</code> becomes <code>{"arr[0]": 1, "arr[1]": 2}</code></li>
 * </ul>
 *
 * <h3>Duplicate Key Detection</h3>
 * <p>Throws {@link IllegalArgumentException} if keys collide after flattening:</p>
 * <pre>
 * // This will throw: "a.b" literal vs "a": {"b": ...} flattened
 * {"a.b": 1, "a": {"b": 2}}
 * </pre>
 *
 * @see JsonWriter
 */
@CompileStatic
class JsonReader {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
  private static final JsonFactory FACTORY = MAPPER.getFactory()

  private JsonReader() {
    // Only static methods
  }

  /**
   * Read a JSON string containing an array of objects into a Matrix.
   *
   * @param str a JSON string containing a list of rows (JSON objects)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix read(String str) {
    FACTORY.createParser(str).withCloseable { JsonParser parser ->
      parseStream(parser)
    }
  }

  /**
   * Read JSON from an InputStream into a Matrix.
   *
   * @param is the input stream containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix read(InputStream is, Charset charset = StandardCharsets.UTF_8) {
    FACTORY.createParser(new InputStreamReader(is, charset)).withCloseable { JsonParser parser ->
      parseStream(parser)
    }
  }

  /**
   * Read JSON from a File into a Matrix.
   *
   * @param file the file containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix read(File file, Charset charset = StandardCharsets.UTF_8) {
    Matrix result = file.newReader(charset.name()).withCloseable { Reader reader ->
      FACTORY.createParser(reader).withCloseable { JsonParser parser ->
        parseStream(parser)
      }
    } as Matrix
    result.matrixName = tableName(file)
    result
  }

  /**
   * Read JSON from a Reader into a Matrix.
   *
   * @param reader the reader providing JSON content
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix read(Reader reader) {
    FACTORY.createParser(reader).withCloseable { JsonParser parser ->
      parseStream(parser)
    }
  }

  /**
   * Read JSON from a URL into a Matrix.
   *
   * @param url URL pointing to JSON content
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @throws IOException if reading the URL fails
   */
  static Matrix read(URL url, Charset charset = StandardCharsets.UTF_8) {
    Matrix result = url.openStream().withCloseable { InputStream is ->
      read(is, charset)
    } as Matrix
    result.matrixName = tableName(url)
    result
  }

  /**
   * Read JSON from a URL string into a Matrix.
   *
   * @param urlString String URL pointing to JSON content
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @throws IOException if reading the URL fails or URL is invalid
   */
  static Matrix readUrl(String urlString, Charset charset = StandardCharsets.UTF_8) {
    read(new URI(urlString).toURL(), charset)
  }

  /**
   * Read JSON from a Path into a Matrix.
   *
   * @param path Path to the file containing JSON array
   * @param charset character encoding (default UTF-8)
   * @return a Matrix with columns derived from JSON keys
   * @throws IOException if reading the file fails
   */
  static Matrix read(Path path, Charset charset = StandardCharsets.UTF_8) {
    read(path.toFile(), charset)
  }

  /**
   * Read JSON from a file path string (convenience method).
   *
   * <p>This method provides a convenient way to read JSON using a String file path
   * instead of creating a File object. It uses default UTF-8 encoding.</p>
   *
   * @param filePath path to the JSON file as a String
   * @param charset character encoding (default UTF-8)
   * @return Matrix containing the parsed data
   * @throws IOException if reading the file fails or file not found
   */
  static Matrix readFile(String filePath, Charset charset = StandardCharsets.UTF_8) {
    read(new File(filePath), charset)
  }

  /**
   * Read a JSON string containing an array of objects into a Matrix.
   *
   * <p>This is an alias for {@link #read(String)} provided for API symmetry
   * with other format readers (e.g. CsvReader.readString).</p>
   *
   * @param jsonContent a JSON string containing a list of rows (JSON objects)
   * @return a Matrix with columns derived from JSON keys
   */
  static Matrix readString(String jsonContent) {
    read(jsonContent)
  }

  /**
   * Derive a table name from a File by stripping the extension.
   */
  private static String tableName(File file) {
    String name = file.getName()
    int dot = name.lastIndexOf('.')
    dot > 0 ? name.substring(0, dot) : name
  }

  /**
   * Derive a table name from a URL by extracting the filename and stripping the extension.
   */
  private static String tableName(URL url) {
    String path = url.getFile() ?: url.getPath()
    int slash = path.lastIndexOf('/')
    String name = slash >= 0 ? path.substring(slash + 1) : path
    int dot = name.lastIndexOf('.')
    dot > 0 ? name.substring(0, dot) : name
  }

  /**
   * Stream-parse a JSON array into a Matrix.
   * Uses single-pass algorithm with dynamic column creation.
   * Memory usage is O(columns * rows) for the final Matrix only,
   * not O(rows) for intermediate parsed objects.
   */
  private static Matrix parseStream(JsonParser parser) {
    // Column tracking - maps column name to column index
    Map<String, Integer> columnIndex = [:]
    List<String> columnNames = []
    List<List<Object>> columns = []
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
      Map<String, Object> flatRow = [:]
      flatten('', rowObj, flatRow)

      // Track which columns received values in this row
      Set<Integer> columnsUpdated = [] as Set<Integer>

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
          List<Object> newColumn = []
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

    Matrix.builder()
        .columnNames(columnNames)
        .columns(columns)
        .build()
  }

  /**
   * Flatten a nested structure into dot-notation keys.
   *
   * <p><b>Examples:</b></p>
   * <ul>
   *   <li>Objects: <code>{"a": {"b": 1}}</code> → <code>{"a.b": 1}</code></li>
   *   <li>Arrays: <code>{"arr": [1, 2]}</code> → <code>{"arr[0]": 1, "arr[1]": 2}</code></li>
   *   <li>Deep nesting: <code>{"x": {"y": {"z": 3}}}</code> → <code>{"x.y.z": 3}</code></li>
   *   <li>Mixed: <code>{"a": [{"b": 1}, {"b": 2}]}</code> → <code>{"a[0].b": 1, "a[1].b": 2}</code></li>
   * </ul>
   *
   * <p><b>Collision Detection:</b></p>
   * <p>Throws {@link IllegalArgumentException} if a literal key collides with a flattened path:</p>
   * <pre>
   * // Error: Both flatten to "a.b"
   * {"a.b": 1, "a": {"b": 2}}
   * </pre>
   *
   * @param prefix the current path prefix (empty string at root level)
   * @param node the current node to flatten (Map, List, or leaf value)
   * @param result output map collecting flattened key-value pairs
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
