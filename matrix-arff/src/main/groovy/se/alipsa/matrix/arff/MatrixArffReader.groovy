package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Reads ARFF (Attribute-Relation File Format) files into Matrix objects.
 *
 * ARFF format specification:
 * - Comments start with %
 * - @RELATION defines the dataset name
 * - @ATTRIBUTE name type defines columns (NUMERIC, REAL, INTEGER, STRING, DATE, or {nominal values})
 * - @DATA marks the start of data rows
 * - Data rows are comma-separated values
 */
@CompileStatic
class MatrixArffReader {

  private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile(/(?i)date\s*(?:'([^']*)'|"([^"]*)")?/)

  /** Read from an Arff file. */
  static Matrix read(File file) {
    read(file, new ArffReadOptions())
  }

  /** Read from an Arff file with typed options. */
  static Matrix read(File file, ArffReadOptions options) {
    validateFile(file)
    new FileInputStream(file).withCloseable { InputStream is ->
      read(is, fallbackName(defaultName(file), options), options)
    }
  }

  /** Read from an Arff file. */
  static Matrix read(Path path) {
    read(path, new ArffReadOptions())
  }

  /** Read from an Arff file with typed options. */
  static Matrix read(Path path, ArffReadOptions options) {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null")
    }
    read(path.toFile(), options)
  }

  /**
   * Read arff from an InputStream. Stream will be closed by caller if needed.
   *
   * @param input the input stream containing ARFF content
   * @param defaultName fallback name if no @RELATION is present
   * @return a Matrix containing the parsed data
   */
  static Matrix read(InputStream input, String defaultName = 'ArffMatrix') {
    read(input, defaultName, new ArffReadOptions())
  }

  /** Read arff from an InputStream with typed options. */
  static Matrix read(InputStream input, ArffReadOptions options) {
    read(input, 'ArffMatrix', options)
  }

  /**
   * Read arff from an InputStream with typed options. Stream will be closed by caller if needed.
   *
   * @param input the input stream containing ARFF content
   * @param defaultName fallback name if no @RELATION is present
   * @param options typed read options
   * @return a Matrix containing the parsed data
   */
  static Matrix read(InputStream input, String defaultName, ArffReadOptions options) {
    if (input == null) {
      throw new IllegalArgumentException('InputStream cannot be null')
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
    parseArff(reader, fallbackName(defaultName, options), options ?: new ArffReadOptions())
  }

  /**
   * Read arff from a Reader. Reader will be closed by caller if needed.
   *
   * @param reader the reader containing ARFF content
   * @param defaultName fallback name if no @RELATION is present
   * @return a Matrix containing the parsed data
   */
  static Matrix read(Reader reader, String defaultName = 'ArffMatrix') {
    read(reader, defaultName, new ArffReadOptions())
  }

  /** Read arff from a Reader with typed options. */
  static Matrix read(Reader reader, ArffReadOptions options) {
    read(reader, 'ArffMatrix', options)
  }

  /**
   * Read arff from a Reader with typed options. Reader will be closed by caller if needed.
   *
   * @param reader the reader containing ARFF content
   * @param defaultName fallback name if no @RELATION is present
   * @param options typed read options
   * @return a Matrix containing the parsed data
   */
  static Matrix read(Reader reader, String defaultName, ArffReadOptions options) {
    if (reader == null) {
      throw new IllegalArgumentException('Reader cannot be null')
    }
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader)
    parseArff(buffered, fallbackName(defaultName, options), options ?: new ArffReadOptions())
  }

  /**
   * Read arff from a file path string.
   *
   * @param filePath path to the ARFF file
   * @return a Matrix containing the parsed data
   */
  static Matrix readFile(String filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException('File path cannot be null')
    }
    read(new File(filePath))
  }

  /**
   * Read arff content from a String.
   *
   * @param arffContent ARFF content as a string
   * @param defaultName fallback name if no @RELATION is present
   * @return a Matrix containing the parsed data
   */
  static Matrix readString(String arffContent, String defaultName = 'ArffMatrix') {
    readString(arffContent, defaultName, new ArffReadOptions())
  }

  /** Read arff content from a String with typed options. */
  static Matrix readString(String arffContent, ArffReadOptions options) {
    readString(arffContent, 'ArffMatrix', options)
  }

  /**
   * Read arff content from a String with typed options.
   *
   * @param arffContent ARFF content as a string
   * @param defaultName fallback name if no @RELATION is present
   * @param options typed read options
   * @return a Matrix containing the parsed data
   */
  static Matrix readString(String arffContent, String defaultName, ArffReadOptions options) {
    if (arffContent == null) {
      throw new IllegalArgumentException('ARFF content cannot be null')
    }
    read(new StringReader(arffContent), defaultName, options)
  }

  /** Read from a URL. */
  static Matrix read(URL url) {
    read(url, new ArffReadOptions())
  }

  /** Read from a URL with typed options. */
  static Matrix read(URL url, ArffReadOptions options) {
    if (url == null) {
      throw new IllegalArgumentException('URL cannot be null')
    }
    url.openStream().withCloseable { InputStream is ->
      read(is, fallbackName(defaultName(url), options), options)
    }
  }

  /**
   * Read arff from a URL string.
   *
   * @param urlString URL string pointing to ARFF content
   * @return a Matrix containing the parsed data
   */
  static Matrix readUrl(String urlString) {
    if (urlString == null) {
      throw new IllegalArgumentException('URL string cannot be null')
    }
    try {
      return read(new URI(urlString).toURL())
    } catch (URISyntaxException | MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL string: $urlString", e)
    }
  }

  private static Matrix parseArff(BufferedReader reader, String defaultName, ArffReadOptions options) {
    String relationName = defaultName
    List<String> attributeNames = []
    List<ArffAttribute> attributes = []
    List<List<Object>> rows = []
    boolean inDataSection = false
    int lineNumber = 0

    String rawLine
    while ((rawLine = reader.readLine()) != null) {
      lineNumber++
      String line = rawLine.trim()

      if (line.isEmpty() || line.startsWith('%')) {
        continue
      }

      if (inDataSection) {
        List<Object> row = parseDataRow(line, attributes, options, lineNumber, rawLine)
        rows.add(row)
        continue
      }

      String upperLine = line.toUpperCase()
      if (upperLine.startsWith('@RELATION')) {
        relationName = parseRelationName(line, lineNumber, rawLine)
      } else if (upperLine.startsWith('@ATTRIBUTE')) {
        ArffAttribute attr = parseAttribute(line, options, lineNumber, rawLine)
        attributeNames.add(attr.name)
        attributes.add(attr)
      } else if (upperLine.startsWith('@DATA')) {
        inDataSection = true
      }
    }

    List<Class> types = attributes.collect { ArffAttribute attr -> attr.javaType }
    List<List<Object>> columns = []
    if (!rows.isEmpty()) {
      int numCols = attributeNames.size()
      for (int i = 0; i < numCols; i++) {
        List<Object> col = []
        for (List<Object> row : rows) {
          col.add(row[i])
        }
        columns.add(col)
      }
    } else {
      for (int i = 0; i < attributeNames.size(); i++) {
        columns.add([])
      }
    }

    Matrix.builder(relationName)
        .columnNames(attributeNames)
        .columns(columns)
        .types(types)
        .build()
  }

  private static String parseRelationName(String line, int lineNumber, String rawLine) {
    if (line.length() < 9) {
      throw parseError('Invalid @RELATION line', lineNumber, rawLine)
    }
    String name = line.substring(9).trim()
    unescapeQuoted(name)
  }

  private static ArffAttribute parseAttribute(String line, ArffReadOptions options, int lineNumber, String rawLine) {
    String trimmed = line.trim()
    int attrIndex = trimmed.toUpperCase().indexOf('@ATTRIBUTE')
    if (attrIndex < 0) {
      throw parseError('Invalid @ATTRIBUTE line', lineNumber, rawLine)
    }
    String spec = trimmed.substring(attrIndex + 10).trim()
    if (spec.isEmpty()) {
      throw parseError('Invalid @ATTRIBUTE line', lineNumber, rawLine)
    }

    String name
    String typeSpec
    char first = spec.charAt(0)
    if (first == '\'' || first == '"') {
      char quoteChar = first
      StringBuilder sb = new StringBuilder()
      boolean escape = false
      boolean closed = false
      int i = 1
      for (; i < spec.length(); i++) {
        char c = spec.charAt(i)
        if (escape) {
          sb.append(c)
          escape = false
          continue
        }
        if (c == '\\') {
          escape = true
          continue
        }
        if (c == quoteChar) {
          i++
          closed = true
          break
        }
        sb.append(c)
      }
      if (!closed) {
        throw parseError('Invalid @ATTRIBUTE line (missing closing quote)', lineNumber, rawLine)
      }
      name = sb.toString()
      typeSpec = i < spec.length() ? spec.substring(i).trim() : ''
      if (typeSpec.isEmpty()) {
        throw parseError('Invalid @ATTRIBUTE line (missing type)', lineNumber, rawLine)
      }
    } else {
      int splitIndex = -1
      for (int i = 0; i < spec.length(); i++) {
        if (Character.isWhitespace(spec.charAt(i))) {
          splitIndex = i
          break
        }
      }
      if (splitIndex < 0) {
        throw parseError('Invalid @ATTRIBUTE line', lineNumber, rawLine)
      }
      name = spec.substring(0, splitIndex)
      typeSpec = spec.substring(splitIndex).trim()
    }

    parseAttributeType(name, typeSpec, options, lineNumber, rawLine)
  }

  private static ArffAttribute parseAttributeType(String name, String typeSpec, ArffReadOptions options, int lineNumber, String rawLine) {
    String upperType = typeSpec.toUpperCase()

    String nominalValuesStr = extractNominalValues(typeSpec)
    if (nominalValuesStr != null) {
      List<String> nominalValues = parseNominalValues(nominalValuesStr)
      return new ArffAttribute(name, ArffType.NOMINAL, String.class, nominalValues)
    }

    if (upperType.startsWith('DATE')) {
      String dateFormat = null
      Matcher dateMatcher = DATE_FORMAT_PATTERN.matcher(typeSpec)
      if (dateMatcher.find()) {
        dateFormat = dateMatcher.group(1) ?: dateMatcher.group(2)
      }
      return new ArffAttribute(name, ArffType.DATE, Date.class, null, dateFormat)
    }

    switch (upperType) {
      case 'NUMERIC', 'REAL' -> new ArffAttribute(name, ArffType.NUMERIC, BigDecimal.class)
      case 'INTEGER' -> new ArffAttribute(name, ArffType.INTEGER, Integer.class)
      case 'STRING' -> new ArffAttribute(name, ArffType.STRING, String.class)
      default -> {
        if (options.failOnUnknownAttributeType) {
          throw parseError("Unknown @ATTRIBUTE type '$typeSpec'", lineNumber, rawLine)
        }
        yield new ArffAttribute(name, ArffType.STRING, String.class)
      }
    }
  }

  private static List<String> parseNominalValues(String valuesStr) {
    List<String> values = []
    parseDelimitedLine(valuesStr, ',' as char).each { ParsedToken token ->
      values.add(token.quoted ? token.value : token.value.trim())
    }
    values
  }

  private static List<Object> parseDataRow(String line, List<ArffAttribute> attributes, ArffReadOptions options,
                                           int lineNumber, String rawLine) {
    if (line.startsWith('{')) {
      return parseSparseDataRow(line, attributes, lineNumber, rawLine)
    }

    List<ParsedToken> values = parseDelimitedLine(line, ',' as char, lineNumber, rawLine, 'data row')
    if (options.failOnRowLengthMismatch && values.size() != attributes.size()) {
      throw parseError(
          "Row length mismatch: expected ${attributes.size()} values but found ${values.size()}",
          lineNumber,
          rawLine
      )
    }

    List<Object> row = []
    for (int i = 0; i < attributes.size(); i++) {
      ParsedToken token = i < values.size() ? values[i] : new ParsedToken(null, false)
      String value = token.value
      if (value != null && !token.quoted) {
        value = value.trim()
      }
      row.add(convertValue(value, attributes[i], token.quoted, lineNumber, rawLine))
    }
    row
  }

  /** Parse a sparse ARFF row in `{index value, ...}` format. */
  private static List<Object> parseSparseDataRow(String line, List<ArffAttribute> attributes, int lineNumber, String rawLine) {
    if (!line.endsWith('}')) {
      throw parseError('Invalid sparse ARFF row (missing closing brace)', lineNumber, rawLine)
    }
    List<Object> row = [null] * attributes.size()
    String body = line.substring(1, line.length() - 1).trim()
    if (body.isEmpty()) {
      return row
    }

    Set<Integer> assignedIndices = [] as Set<Integer>
    splitSparseEntries(body, lineNumber, rawLine).each { String entry ->
      entry = entry?.trim()
      if (entry == null || entry.isEmpty()) {
        throw parseError('Invalid sparse ARFF row (empty entry)', lineNumber, rawLine)
      }

      int splitIndex = findSparseEntryValueStart(entry)
      if (splitIndex < 0) {
        throw parseError("Invalid sparse ARFF sparse entry '$entry'", lineNumber, rawLine)
      }

      String indexPart = entry.substring(0, splitIndex).trim()
      String valuePart = entry.substring(splitIndex).trim()
      if (valuePart.isEmpty()) {
        throw parseError("Invalid sparse ARFF sparse entry '$entry'", lineNumber, rawLine)
      }

      int attributeIndex
      try {
        attributeIndex = Integer.parseInt(indexPart)
      } catch (NumberFormatException e) {
        throw parseError("Invalid sparse ARFF attribute index '$indexPart'", lineNumber, rawLine, e)
      }

      if (attributeIndex < 0 || attributeIndex >= attributes.size()) {
        throw parseError(
            "Sparse ARFF attribute index $attributeIndex is out of bounds for ${attributes.size()} attributes",
            lineNumber,
            rawLine
        )
      }
      if (!assignedIndices.add(attributeIndex)) {
        throw parseError("Duplicate sparse ARFF attribute index $attributeIndex", lineNumber, rawLine)
      }

      ParsedToken valueToken = parseSparseValue(valuePart, lineNumber, rawLine)
      String value = valueToken.value
      if (value != null && !valueToken.quoted) {
        value = value.trim()
      }
      row[attributeIndex] = convertValue(value, attributes[attributeIndex], valueToken.quoted, lineNumber, rawLine)
    }

    row
  }

  private static List<String> splitSparseEntries(String body, int lineNumber, String rawLine) {
    List<String> entries = []
    StringBuilder current = new StringBuilder()
    boolean inQuote = false
    boolean escape = false
    char quoteChar = 0

    for (int i = 0; i < body.length(); i++) {
      char c = body.charAt(i)
      if (inQuote) {
        current.append(c)
        if (escape) {
          escape = false
          continue
        }
        if (c == '\\') {
          escape = true
          continue
        }
        if (c == quoteChar) {
          inQuote = false
        }
        continue
      }

      if (c == '\'' || c == '"') {
        inQuote = true
        quoteChar = c
        current.append(c)
        continue
      }
      if (c == ',' as char) {
        entries.add(current.toString())
        current = new StringBuilder()
        continue
      }
      current.append(c)
    }

    if (inQuote) {
      throw parseError('Unterminated quoted sparse value', lineNumber, rawLine)
    }
    if (escape) {
      current.append('\\')
    }
    entries.add(current.toString())
    entries
  }

  private static int findSparseEntryValueStart(String entry) {
    for (int i = 0; i < entry.length(); i++) {
      if (Character.isWhitespace(entry.charAt(i))) {
        return i
      }
    }
    -1
  }

  private static ParsedToken parseSparseValue(String valuePart, int lineNumber, String rawLine) {
    String value = valuePart.trim()
    if (value.isEmpty()) {
      throw parseError("Invalid sparse ARFF value '$valuePart'", lineNumber, rawLine)
    }

    char first = value.charAt(0)
    if (first != '\'' && first != '"') {
      return new ParsedToken(value, false)
    }

    StringBuilder result = new StringBuilder()
    boolean escape = false
    for (int i = 1; i < value.length(); i++) {
      char c = value.charAt(i)
      if (escape) {
        result.append(c)
        escape = false
        continue
      }
      if (c == '\\') {
        escape = true
        continue
      }
      if (c == first) {
        String trailing = i + 1 < value.length() ? value.substring(i + 1).trim() : ''
        if (!trailing.isEmpty()) {
          throw parseError("Invalid sparse ARFF value '$valuePart'", lineNumber, rawLine)
        }
        return new ParsedToken(result.toString(), true)
      }
      result.append(c)
    }

    throw parseError("Invalid sparse ARFF value '$valuePart'", lineNumber, rawLine)
  }

  private static List<ParsedToken> parseDelimitedLine(String line, char delimiter) {
    parseDelimitedLine(line, delimiter, 0, line, null)
  }

  private static List<ParsedToken> parseDelimitedLine(String line, char delimiter, int lineNumber, String rawLine, String context) {
    List<ParsedToken> values = []
    StringBuilder current = new StringBuilder()
    boolean inQuote = false
    char quoteChar = 0
    boolean tokenQuoted = false
    boolean escape = false

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i)

      if (inQuote) {
        if (escape) {
          current.append(c)
          escape = false
          continue
        }
        if (c == '\\') {
          escape = true
          continue
        }
        if (c == quoteChar) {
          inQuote = false
          tokenQuoted = true
          continue
        }
        current.append(c)
        continue
      }

      if (c == '\'' || c == '"') {
        if (current.toString().trim().isEmpty()) {
          current.setLength(0)
        } else {
          current.append(c)
          continue
        }
        inQuote = true
        quoteChar = c
        tokenQuoted = true
        continue
      }
      if (c == delimiter) {
        values.add(new ParsedToken(current.toString(), tokenQuoted))
        current = new StringBuilder()
        tokenQuoted = false
        continue
      }
      current.append(c)
    }

    if (inQuote) {
      String detail = context == null ? 'Unterminated quoted value' : "Unterminated quoted $context"
      if (lineNumber > 0) {
        throw parseError(detail, lineNumber, rawLine)
      }
      throw new IllegalArgumentException(detail)
    }
    if (escape) {
      current.append('\\')
    }
    values.add(new ParsedToken(current.toString(), tokenQuoted))
    values
  }

  private static Object convertValue(String value, ArffAttribute attr, boolean quoted, int lineNumber, String rawLine) {
    if (value == null) {
      return null
    }
    if (!quoted && (value.isEmpty() || value == '?')) {
      return null
    }

    try {
      return switch (attr.type) {
        case ArffType.NUMERIC -> new BigDecimal(value)
        case ArffType.INTEGER -> Integer.parseInt(value)
        case ArffType.STRING, ArffType.NOMINAL -> value
        case ArffType.DATE -> parseDate(value, attr.dateFormat)
        default -> value
      }
    } catch (NumberFormatException e) {
      throw parseError("Invalid ${attr.type} value '$value' for attribute '${attr.name}'", lineNumber, rawLine, e)
    } catch (ParseException e) {
      throw parseError("Invalid DATE value '$value' for attribute '${attr.name}'", lineNumber, rawLine, e)
    }
  }

  private static Date parseDate(String value, String format) throws ParseException {
    String resolvedFormat = format ?: "yyyy-MM-dd'T'HH:mm:ss"
    SimpleDateFormat sdf = new SimpleDateFormat(resolvedFormat)
    sdf.parse(value)
  }

  private static String unescapeQuoted(String value) {
    boolean quoted = (value.startsWith("'") && value.endsWith("'")) ||
        (value.startsWith('"') && value.endsWith('"'))
    if (!quoted) {
      return value
    }
    if (value.length() == 2) {
      return ''
    }
    String unquoted = value.substring(1, value.length() - 1)
    StringBuilder result = new StringBuilder()
    boolean escape = false
    for (int i = 0; i < unquoted.length(); i++) {
      char c = unquoted.charAt(i)
      if (escape) {
        result.append(c)
        escape = false
        continue
      }
      if (c == '\\') {
        escape = true
        continue
      }
      result.append(c)
    }
    if (escape) {
      result.append('\\')
    }
    result.toString()
  }

  private static String extractNominalValues(String typeSpec) {
    int openIndex = typeSpec.indexOf('{')
    if (openIndex < 0) {
      return null
    }
    boolean inQuote = false
    boolean escape = false
    char quoteChar = 0
    for (int i = openIndex + 1; i < typeSpec.length(); i++) {
      char c = typeSpec.charAt(i)
      if (inQuote) {
        if (escape) {
          escape = false
          continue
        }
        if (c == '\\') {
          escape = true
          continue
        }
        if (c == quoteChar) {
          inQuote = false
        }
        continue
      }
      if (c == '\'' || c == '"') {
        inQuote = true
        quoteChar = c
        continue
      }
      if (c == '}') {
        return typeSpec.substring(openIndex + 1, i)
      }
    }
    null
  }

  private static void validateFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exist: ${file.absolutePath}")
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Expected a file but got a directory: ${file.absolutePath}")
    }
  }

  private static String defaultName(File file) {
    String name = file.name
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    name
  }

  private static String defaultName(URL url) {
    String name = url.getPath()
    if (name == null || name.isEmpty()) {
      name = url.getFile()
    }
    if (name == null || name.isEmpty()) {
      return 'ArffMatrix'
    }
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1)
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    name ?: 'ArffMatrix'
  }

  private static String fallbackName(String defaultName, ArffReadOptions options) {
    options?.matrixName ?: defaultName
  }

  private static IllegalArgumentException parseError(String message, int lineNumber, String rawLine) {
    new IllegalArgumentException("$message at line $lineNumber: ${rawLine?.trim()}")
  }

  private static IllegalArgumentException parseError(String message, int lineNumber, String rawLine, Throwable cause) {
    new IllegalArgumentException("$message at line $lineNumber: ${rawLine?.trim()}", cause)
  }

  @CompileStatic
  private static final class ParsedToken {
    final String value
    final boolean quoted

    ParsedToken(String value, boolean quoted) {
      this.value = value
      this.quoted = quoted
    }
  }
}

/** Enum representing ARFF attribute types. */
enum ArffType {
  NUMERIC,
  INTEGER,
  STRING,
  NOMINAL,
  DATE
}

/** Class representing an ARFF attribute definition. */
@CompileStatic
class ArffAttribute {
  String name
  ArffType type
  Class javaType
  List<String> nominalValues
  String dateFormat

  ArffAttribute(String name, ArffType type, Class javaType,
                List<String> nominalValues = null, String dateFormat = null) {
    this.name = name
    this.type = type
    this.javaType = javaType
    this.nominalValues = nominalValues
    this.dateFormat = dateFormat
  }
}
