package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

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

  private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
      /(?i)@attribute\s+('[^']*'|"[^"]*"|\S+)\s+(.+)/)
  private static final Pattern NOMINAL_PATTERN = Pattern.compile(/\{([^}]*)\}/)
  private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile(/(?i)date\s*(?:'([^']*)'|"([^"]*)")?/)

  /** Read from an Arff file */
  static Matrix read(File file) {
    InputStream is = new FileInputStream(file)
    try {
      return read(is, file.name)
    } finally {
      is.close()
    }
  }

  /** Read from an Arff file */
  static Matrix read(Path path) {
    read(path.toFile())
  }

  /** Read arff from an InputStream. Stream will be closed by caller if needed. */
  static Matrix read(InputStream input, String defaultName = "ArffMatrix") {
    BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))
    parseArff(reader, defaultName)
  }

  /** Read from a URL */
  static Matrix read(URL url) {
    InputStream is = url.openStream()
    try {
      return read(is, url.getFile() ?: "ArffMatrix")
    } finally {
      is.close()
    }
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
    // Handle quoted names
    if ((name.startsWith("'") && name.endsWith("'")) ||
        (name.startsWith('"') && name.endsWith('"'))) {
      name = name.substring(1, name.length() - 1)
    }
    return name
  }

  private static ArffAttribute parseAttribute(String line) {
    Matcher matcher = ATTRIBUTE_PATTERN.matcher(line)
    if (!matcher.find()) {
      throw new IllegalArgumentException("Invalid @ATTRIBUTE line: $line")
    }

    String name = matcher.group(1).trim()
    // Remove quotes from attribute name if present
    if ((name.startsWith("'") && name.endsWith("'")) ||
        (name.startsWith('"') && name.endsWith('"'))) {
      name = name.substring(1, name.length() - 1)
    }

    String typeSpec = matcher.group(2).trim()
    return parseAttributeType(name, typeSpec)
  }

  private static ArffAttribute parseAttributeType(String name, String typeSpec) {
    String upperType = typeSpec.toUpperCase()

    // Check for nominal (categorical) type
    Matcher nominalMatcher = NOMINAL_PATTERN.matcher(typeSpec)
    if (nominalMatcher.find()) {
      String valuesStr = nominalMatcher.group(1)
      List<String> nominalValues = parseNominalValues(valuesStr)
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
    StringBuilder current = new StringBuilder()
    boolean inQuote = false
    char quoteChar = 0

    for (int i = 0; i < valuesStr.length(); i++) {
      char c = valuesStr.charAt(i)

      if (!inQuote && (c == '\'' || c == '"')) {
        inQuote = true
        quoteChar = c
      } else if (inQuote && c == quoteChar) {
        inQuote = false
      } else if (!inQuote && c == ',') {
        values.add(current.toString().trim())
        current = new StringBuilder()
      } else {
        current.append(c)
      }
    }

    if (current.length() > 0) {
      values.add(current.toString().trim())
    }

    return values
  }

  private static List<Object> parseDataRow(String line, List<ArffAttribute> attributes) {
    List<String> values = parseCSVLine(line)
    List<Object> row = []

    for (int i = 0; i < attributes.size(); i++) {
      String value = i < values.size() ? values[i].trim() : null
      Object converted = convertValue(value, attributes[i])
      row.add(converted)
    }

    return row
  }

  private static List<String> parseCSVLine(String line) {
    List<String> values = []
    StringBuilder current = new StringBuilder()
    boolean inQuote = false
    char quoteChar = 0

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i)

      if (!inQuote && (c == '\'' || c == '"')) {
        inQuote = true
        quoteChar = c
      } else if (inQuote && c == quoteChar) {
        inQuote = false
      } else if (!inQuote && c == ',') {
        values.add(current.toString())
        current = new StringBuilder()
      } else {
        current.append(c)
      }
    }

    values.add(current.toString())
    return values
  }

  private static Object convertValue(String value, ArffAttribute attr) {
    if (value == null || value.isEmpty() || value == '?' || value == '?') {
      return null
    }

    // Remove quotes if present
    if ((value.startsWith("'") && value.endsWith("'")) ||
        (value.startsWith('"') && value.endsWith('"'))) {
      value = value.substring(1, value.length() - 1)
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
