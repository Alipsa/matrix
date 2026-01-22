package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Path
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

  /** Read from an Arff file */
  static Matrix read(File file) {
    validateFile(file)
    InputStream is = new FileInputStream(file)
    try {
      return read(is, defaultName(file))
    } finally {
      is.close()
    }
  }

  /** Read from an Arff file */
  static Matrix read(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null")
    }
    read(path.toFile())
  }

  /**
   * Read arff from an InputStream. Stream will be closed by caller if needed.
   *
   * @param input the input stream containing ARFF content
   * @param defaultName fallback name if no @RELATION is present
   * @return a Matrix containing the parsed data
   */
  static Matrix read(InputStream input, String defaultName = "ArffMatrix") {
    if (input == null) {
      throw new IllegalArgumentException("InputStream cannot be null")
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
    parseArff(reader, defaultName)
  }

  /**
   * Read arff from a Reader. Reader will be closed by caller if needed.
   *
   * @param reader the reader containing ARFF content
   * @param defaultName fallback name if no @RELATION is present
   * @return a Matrix containing the parsed data
   */
  static Matrix read(Reader reader, String defaultName = "ArffMatrix") {
    if (reader == null) {
      throw new IllegalArgumentException("Reader cannot be null")
    }
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader)
    parseArff(buffered, defaultName)
  }

  /**
   * Read arff from a file path string.
   *
   * @param filePath path to the ARFF file
   * @return a Matrix containing the parsed data
   */
  static Matrix read(String filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException("File path cannot be null")
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
  static Matrix readString(String arffContent, String defaultName = "ArffMatrix") {
    if (arffContent == null) {
      throw new IllegalArgumentException("ARFF content cannot be null")
    }
    read(new StringReader(arffContent), defaultName)
  }

  /**
   * Read arff from a file path string (explicit convenience method).
   *
   * @param filePath path to the ARFF file
   * @return a Matrix containing the parsed data
   */
  static Matrix readFromFile(String filePath) {
    read(filePath)
  }

  /** Read from a URL */
  static Matrix read(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("URL cannot be null")
    }
    InputStream is = url.openStream()
    try {
      return read(is, defaultName(url))
    } finally {
      is.close()
    }
  }

  /**
   * Read arff from a URL string.
   *
   * @param urlString URL string pointing to ARFF content
   * @return a Matrix containing the parsed data
   */
  static Matrix readFromUrl(String urlString) {
    if (urlString == null) {
      throw new IllegalArgumentException("URL string cannot be null")
    }
    read(new URI(urlString).toURL())
  }

  private static Matrix parseArff(BufferedReader reader, String defaultName) {
    String relationName = defaultName
    List<String> attributeNames = []
    List<ArffAttribute> attributes = []
    List<List<Object>> rows = []
    boolean inDataSection = false

    String line
    while ((line = reader.readLine()) != null) {
      line = line.trim()

      // Skip empty lines and comments
      if (line.isEmpty() || line.startsWith('%')) {
        continue
      }

      if (inDataSection) {
        // Parse data row
        if (!line.isEmpty()) {
          List<Object> row = parseDataRow(line, attributes)
          rows.add(row)
        }
      } else {
        String upperLine = line.toUpperCase()

        if (upperLine.startsWith('@RELATION')) {
          relationName = parseRelationName(line)
        } else if (upperLine.startsWith('@ATTRIBUTE')) {
          ArffAttribute attr = parseAttribute(line)
          attributeNames.add(attr.name)
          attributes.add(attr)
        } else if (upperLine.startsWith('@DATA')) {
          inDataSection = true
        }
      }
    }

    // Build the Matrix
    List<Class> types = attributes.collect { it.javaType }

    // Transpose rows to columns
    List<List<Object>> columns = []
    if (!rows.isEmpty()) {
      int numCols = attributeNames.size()
      for (int i = 0; i < numCols; i++) {
        List<Object> col = []
        for (List<Object> row : rows) {
          col.add(i < row.size() ? row[i] : null)
        }
        columns.add(col)
      }
    } else {
      // Empty data - create empty columns
      for (int i = 0; i < attributeNames.size(); i++) {
        columns.add([])
      }
    }

    return Matrix.builder(relationName)
        .columnNames(attributeNames)
        .columns(columns)
        .types(types)
        .build()
  }

  private static String parseRelationName(String line) {
    String name = line.substring(9).trim() // Remove @RELATION
    return unescapeQuoted(name)
  }

  private static ArffAttribute parseAttribute(String line) {
    String trimmed = line.trim()
    int attrIndex = trimmed.toUpperCase().indexOf('@ATTRIBUTE')
    if (attrIndex < 0) {
      throw new IllegalArgumentException("Invalid @ATTRIBUTE line: $line")
    }
    String spec = trimmed.substring(attrIndex + 10).trim()
    if (spec.isEmpty()) {
      throw new IllegalArgumentException("Invalid @ATTRIBUTE line: $line")
    }

    String name
    String typeSpec
    char first = spec.charAt(0)
    if (first == '\'' || first == '"') {
      char quoteChar = first
      StringBuilder sb = new StringBuilder()
      boolean escape = false
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
          break
        }
        sb.append(c)
      }
      name = sb.toString()
      typeSpec = i < spec.length() ? spec.substring(i).trim() : ''
    } else {
      int splitIndex = -1
      for (int i = 0; i < spec.length(); i++) {
        if (Character.isWhitespace(spec.charAt(i))) {
          splitIndex = i
          break
        }
      }
      if (splitIndex < 0) {
        throw new IllegalArgumentException("Invalid @ATTRIBUTE line: $line")
      }
      name = spec.substring(0, splitIndex)
      typeSpec = spec.substring(splitIndex).trim()
    }

    return parseAttributeType(name, typeSpec)
  }

  private static ArffAttribute parseAttributeType(String name, String typeSpec) {
    String upperType = typeSpec.toUpperCase()

    // Check for nominal (categorical) type
    String nominalValuesStr = extractNominalValues(typeSpec)
    if (nominalValuesStr != null) {
      List<String> nominalValues = parseNominalValues(nominalValuesStr)
      return new ArffAttribute(name, ArffType.NOMINAL, String.class, nominalValues)
    }

    // Check for date type with optional format
    if (upperType.startsWith('DATE')) {
      String dateFormat = null
      Matcher dateMatcher = DATE_FORMAT_PATTERN.matcher(typeSpec)
      if (dateMatcher.find()) {
        dateFormat = dateMatcher.group(1) ?: dateMatcher.group(2)
      }
      return new ArffAttribute(name, ArffType.DATE, Date.class, null, dateFormat)
    }

    // Standard types
    switch (upperType) {
      case 'NUMERIC':
      case 'REAL':
        return new ArffAttribute(name, ArffType.NUMERIC, BigDecimal.class)
      case 'INTEGER':
        return new ArffAttribute(name, ArffType.INTEGER, Integer.class)
      case 'STRING':
        return new ArffAttribute(name, ArffType.STRING, String.class)
      default:
        // Default to string for unknown types
        return new ArffAttribute(name, ArffType.STRING, String.class)
    }
  }

  private static List<String> parseNominalValues(String valuesStr) {
    List<String> values = []
    parseDelimitedLine(valuesStr, ',' as char).each { ParsedToken token ->
      String value = token.quoted ? token.value : token.value.trim()
      values.add(value)
    }
    return values
  }

  private static List<Object> parseDataRow(String line, List<ArffAttribute> attributes) {
    List<ParsedToken> values = parseDelimitedLine(line, ',' as char)
    List<Object> row = []

    for (int i = 0; i < attributes.size(); i++) {
      ParsedToken token = i < values.size() ? values[i] : new ParsedToken(null, false)
      String value = token.value
      boolean quoted = token.quoted
      if (value != null && !quoted) {
        value = value.trim()
      }
      Object converted = convertValue(value, attributes[i], quoted)
      row.add(converted)
    }

    return row
  }

  private static List<ParsedToken> parseDelimitedLine(String line, char delimiter) {
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

    if (escape) {
      current.append('\\')
    }
    values.add(new ParsedToken(current.toString(), tokenQuoted))
    return values
  }

  private static Object convertValue(String value, ArffAttribute attr, boolean quoted) {
    if (value == null) {
      return null
    }
    if (!quoted && (value.isEmpty() || value == '?')) {
      return null
    }

    switch (attr.type) {
      case ArffType.NUMERIC:
        return new BigDecimal(value)
      case ArffType.INTEGER:
        return Integer.parseInt(value)
      case ArffType.STRING:
      case ArffType.NOMINAL:
        return value
      case ArffType.DATE:
        return parseDate(value, attr.dateFormat)
      default:
        return value
    }
  }

  private static Date parseDate(String value, String format) {
    if (format == null) {
      format = "yyyy-MM-dd'T'HH:mm:ss"
    }
    SimpleDateFormat sdf = new SimpleDateFormat(format)
    return sdf.parse(value)
  }

  private static String unescapeQuoted(String value) {
    boolean quoted = (value.startsWith("'") && value.endsWith("'")) ||
        (value.startsWith('"') && value.endsWith('"'))
    if (!quoted) {
      return value
    }
    if (value.length() == 2) {
      return ""
    }
    value = value.substring(1, value.length() - 1)
    StringBuilder result = new StringBuilder()
    boolean escape = false
    for (int i = 0; i < value.length(); i++) {
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
      result.append(c)
    }
    if (escape) {
      result.append('\\')
    }
    return result.toString()
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
    return null
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
    return name
  }

  private static String defaultName(URL url) {
    String name = url.getPath()
    if (name == null || name.isEmpty()) {
      name = url.getFile()
    }
    if (name == null || name.isEmpty()) {
      return "ArffMatrix"
    }
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1)
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name ?: "ArffMatrix"
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

/** Enum representing ARFF attribute types */
enum ArffType {
  NUMERIC,
  INTEGER,
  STRING,
  NOMINAL,
  DATE
}

/** Class representing an ARFF attribute definition */
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
