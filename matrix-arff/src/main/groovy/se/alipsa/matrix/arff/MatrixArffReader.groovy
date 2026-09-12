package se.alipsa.matrix.arff

import se.alipsa.matrix.core.Matrix

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.text.ParseException
import java.text.ParsePosition
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
class MatrixArffReader {

  private static final Pattern DATE_TYPE_PATTERN = Pattern.compile(/(?i)^date(?:\s+(.*))?$/)
  private static final String INVALID_DATE_FORMAT = 'Invalid @ATTRIBUTE DATE format'
  private static final String DEFAULT_MATRIX_NAME = 'ArffMatrix'
  private static final String RELATION_KEYWORD = '@RELATION'
  private static final String ATTRIBUTE_KEYWORD = '@ATTRIBUTE'
  private static final String END_KEYWORD = '@END'
  private static final String DATA_KEYWORD = '@DATA'
  private static final String INVALID_ATTRIBUTE_LINE = 'Invalid @ATTRIBUTE line'
  private static final String INVALID_RELATION_LINE = 'Invalid @RELATION line'
  private static final char BACKSLASH_CHAR = '\\'
  private static final char SINGLE_QUOTE_CHAR = '\''
  private static final char DOUBLE_QUOTE_CHAR = '"'
  private static final char COMMA_CHAR = ','
  private static final char OPEN_BRACE_CHAR = '{'
  private static final char CLOSE_BRACE_CHAR = '}'
  private static final char PERCENT_CHAR = '%'
  private static final String DOT = '.'
  private static final String SLASH = '/'
  private static final String OPEN_BRACE = '{'
  private static final String CLOSE_BRACE = '}'
  /** Marks a STRING cell omitted from a sparse row until {@link #resolveOmittedValues} resolves it. */
  private static final Object OMITTED = new Object()

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
      throw new IllegalArgumentException('Path cannot be null')
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
  static Matrix read(InputStream input, String defaultName = DEFAULT_MATRIX_NAME) {
    read(input, defaultName, new ArffReadOptions())
  }

  /** Read arff from an InputStream with typed options. */
  static Matrix read(InputStream input, ArffReadOptions options) {
    read(input, DEFAULT_MATRIX_NAME, options)
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
  static Matrix read(Reader reader, String defaultName = DEFAULT_MATRIX_NAME) {
    read(reader, defaultName, new ArffReadOptions())
  }

  /** Read arff from a Reader with typed options. */
  static Matrix read(Reader reader, ArffReadOptions options) {
    read(reader, DEFAULT_MATRIX_NAME, options)
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
  static Matrix readString(String arffContent, String defaultName = DEFAULT_MATRIX_NAME) {
    readString(arffContent, defaultName, new ArffReadOptions())
  }

  /** Read arff content from a String with typed options. */
  static Matrix readString(String arffContent, ArffReadOptions options) {
    readString(arffContent, DEFAULT_MATRIX_NAME, options)
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
    List<AttributeScope> scopes = [new AttributeScope(null)]
    List<ArffAttribute> attributes = scopes[0].attributes
    List<List<Object>> rows = []
    List<BigDecimal> weights = []
    boolean inDataSection = false
    int lineNumber = 0

    String rawLine
    while ((rawLine = reader.readLine()) != null) {
      lineNumber++
      String line = stripComment(rawLine).trim()

      if (line.isEmpty()) {
        continue
      }

      if (inDataSection) {
        DataLine dataLine = splitInstanceWeight(line)
        rows.add(parseDataRow(dataLine.content, attributes, options, lineNumber, rawLine))
        weights.add(dataLine.weight)
        continue
      }

      String upperLine = line.toUpperCase()
      if (upperLine.startsWith(RELATION_KEYWORD)) {
        relationName = parseRelationName(line, lineNumber, rawLine)
      } else if (upperLine.startsWith(ATTRIBUTE_KEYWORD)) {
        ArffAttribute attr = parseAttribute(line, options, lineNumber, rawLine)
        if (attr.type == ArffType.RELATIONAL) {
          scopes.add(new AttributeScope(attr.name))
        } else {
          addAttribute(scopes.last(), attr, lineNumber, rawLine)
        }
      } else if (upperLine.startsWith(END_KEYWORD)) {
        closeRelationalScope(line, scopes, options, lineNumber, rawLine)
      } else if (upperLine.startsWith(DATA_KEYWORD)) {
        if (scopes.size() > 1) {
          throw unterminatedRelationalError(scopes.last().name, lineNumber, rawLine)
        }
        rejectInstanceWeightAttribute(scopes[0], relationName, options)
        inDataSection = true
      }
    }

    if (scopes.size() > 1) {
      throw unterminatedRelationalError(scopes.last().name, lineNumber, rawLine)
    }
    rejectInstanceWeightAttribute(scopes[0], relationName, options)
    buildMatrix(relationName, attributes, rows, weights, options)
  }

  private static IllegalArgumentException unterminatedRelationalError(String scopeName, int lineNumber, String rawLine) {
    parseError("Relational attribute '$scopeName' is not terminated by @END before @DATA", lineNumber, rawLine)
  }

  private static void addAttribute(AttributeScope scope, ArffAttribute attr, int lineNumber, String rawLine) {
    if (!scope.names.add(attr.name)) {
      throw parseError("Duplicate @ATTRIBUTE name '${attr.name}'", lineNumber, rawLine)
    }
    scope.attributes.add(attr)
  }

  private static void closeRelationalScope(String line, List<AttributeScope> scopes, ArffReadOptions options,
                                           int lineNumber, String rawLine) {
    if (scopes.size() == 1) {
      throw parseError('@END without an open relational attribute', lineNumber, rawLine)
    }
    String endName = parseQuotedName(line, END_KEYWORD.length(), 'Invalid @END line', lineNumber, rawLine)
    AttributeScope scope = scopes.removeLast()
    if (!endName.equalsIgnoreCase(scope.name)) {
      throw parseError("Relational attribute '${scope.name}' must be terminated by @END ${scope.name} but found @END $endName",
          lineNumber, rawLine)
    }
    if (scope.attributes.isEmpty()) {
      throw parseError("Relational attribute '${scope.name}' declares no attributes", lineNumber, rawLine)
    }
    rejectInstanceWeightAttribute(scope, scope.name, options)
    ArffAttribute attr = new ArffAttribute(scope.name, ArffType.RELATIONAL, Matrix, null, null, scope.attributes)
    addAttribute(scopes.last(), attr, lineNumber, rawLine)
  }

  /**
   * The instance-weight column name is reserved in every relation: an {@code @ATTRIBUTE} of that name would otherwise
   * be silently merged with the weights that {@link #buildMatrix} appends.
   */
  private static void rejectInstanceWeightAttribute(AttributeScope scope, String relationName, ArffReadOptions options) {
    String weightColumn = options.instanceWeightColumn
    if (weightColumn != null && scope.names.contains(weightColumn)) {
      throw new IllegalArgumentException(instanceWeightClashMessage(weightColumn, relationName))
    }
  }

  private static String instanceWeightClashMessage(String weightColumn, String relationName) {
    "instanceWeightColumn '$weightColumn' clashes with an @ATTRIBUTE of the same name in relation '$relationName'"
  }

  private static Matrix buildMatrix(String name, List<ArffAttribute> attributes, List<List<Object>> rows,
                                    List<BigDecimal> weights, ArffReadOptions options) {
    resolveOmittedValues(rows, attributes, options.omittedStringFallback)
    List<String> columnNames = attributes*.name
    List<Class> types = attributes*.javaType
    List<List<Object>> columns = []
    for (int i = 0; i < attributes.size(); i++) {
      List<Object> col = []
      for (List<Object> row : rows) {
        col.add(row[i])
      }
      columns.add(col)
    }
    String weightColumn = options.instanceWeightColumn
    if (weightColumn != null) {
      if (columnNames.contains(weightColumn)) {
        throw new IllegalArgumentException(instanceWeightClashMessage(weightColumn, name))
      }
      columnNames.add(weightColumn)
      types.add(BigDecimal)
      // weight == null means "no {w} on the row" (ARFF: weight 1); an explicit {0} must stay 0, so no ?: here
      columns.add(weights.collect { BigDecimal weight -> (Object) (weight == null ? BigDecimal.ONE : weight) })
    }
    Matrix.builder(name)
        .columnNames(columnNames)
        .columns(columns)
        .types(types)
        .build()
  }

  /**
   * Split a trailing instance weight {@code {w}} off a data line: {@code a,b,{2}} or {@code {0 a}, {2}}. The braces must
   * be the last top-level brace group, must not be the sparse row itself, and must enclose a number; otherwise the line
   * is returned unchanged (Weka quietly ignores a non-numeric group too).
   */
  private static DataLine splitInstanceWeight(String line) {
    if (!line.endsWith(CLOSE_BRACE)) {
      return new DataLine(line, null)
    }
    int lastOpen = ArffScanner.lastIndexOfOutsideQuotes(line, OPEN_BRACE_CHAR)
    if (lastOpen <= 0) {
      return new DataLine(line, null)
    }
    BigDecimal weight
    try {
      weight = new BigDecimal(line.substring(lastOpen + 1, line.length() - 1).trim())
    } catch (NumberFormatException ignored) {
      return new DataLine(line, null)
    }
    String content = line.substring(0, lastOpen).trim()
    if (!content.isEmpty() && content.charAt(content.length() - 1) == COMMA_CHAR) {
      content = content.substring(0, content.length() - 1).trim()
    }
    new DataLine(content, weight)
  }

  /** Cut the line at the first {@code %} that is outside a quoted token, as Weka's tokenizer treats it as a comment. */
  private static String stripComment(String line) {
    int comment = ArffScanner.indexOfOutsideQuotes(line, PERCENT_CHAR, 0)
    comment < 0 ? line : line.substring(0, comment)
  }

  private static String parseRelationName(String line, int lineNumber, String rawLine) {
    parseQuotedName(line, RELATION_KEYWORD.length(), INVALID_RELATION_LINE, lineNumber, rawLine)
  }

  /** Parse the (optionally quoted) name that follows a keyword of {@code prefixLength} characters. */
  private static String parseQuotedName(String line, int prefixLength, String invalidMessage, int lineNumber, String rawLine) {
    if (line.length() < prefixLength) {
      throw parseError(invalidMessage, lineNumber, rawLine)
    }
    String name = line.substring(prefixLength).trim()
    if (name.isEmpty() || !ArffScanner.isQuoteChar(name.charAt(0))) {
      return name
    }
    ArffScanner.QuotedToken token = ArffScanner.readQuotedToken(name, 0)
    if (token == null) {
      throw parseError("$invalidMessage (missing closing quote)", lineNumber, rawLine)
    }
    if (!name.substring(token.end).trim().isEmpty()) {
      throw parseError("$invalidMessage (unexpected text after quoted name)", lineNumber, rawLine)
    }
    token.value
  }

  private static ArffAttribute parseAttribute(String line, ArffReadOptions options, int lineNumber, String rawLine) {
    String trimmed = line.trim()
    int attrIndex = trimmed.toUpperCase().indexOf(ATTRIBUTE_KEYWORD)
    if (attrIndex < 0) {
      throw parseError(INVALID_ATTRIBUTE_LINE, lineNumber, rawLine)
    }
    String spec = trimmed.substring(attrIndex + ATTRIBUTE_KEYWORD.length()).trim()
    if (spec.isEmpty()) {
      throw parseError(INVALID_ATTRIBUTE_LINE, lineNumber, rawLine)
    }

    String name
    String typeSpec
    char first = spec.charAt(0)
    if (ArffScanner.isQuoteChar(first)) {
      ArffScanner.QuotedToken token = ArffScanner.readQuotedToken(spec, 0)
      if (token == null) {
        throw parseError("$INVALID_ATTRIBUTE_LINE (missing closing quote)", lineNumber, rawLine)
      }
      name = token.value
      typeSpec = spec.substring(token.end).trim()
      if (typeSpec.isEmpty()) {
        throw parseError("$INVALID_ATTRIBUTE_LINE (missing type)", lineNumber, rawLine)
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
        throw parseError(INVALID_ATTRIBUTE_LINE, lineNumber, rawLine)
      }
      name = spec.substring(0, splitIndex)
      typeSpec = spec.substring(splitIndex).trim()
    }

    parseAttributeType(name, typeSpec, options, lineNumber, rawLine)
  }

  private static ArffAttribute parseAttributeType(String name, String typeSpec, ArffReadOptions options, int lineNumber, String rawLine) {
    typeSpec = stripAttributeWeight(typeSpec)
    String upperType = typeSpec.toUpperCase()

    String nominalValuesStr = extractNominalValues(typeSpec)
    if (nominalValuesStr != null) {
      if (nominalValuesStr.trim().isEmpty()) {
        throw parseError('Nominal attribute must declare at least one value', lineNumber, rawLine)
      }
      List<String> nominalValues = parseNominalValues(nominalValuesStr)
      return new ArffAttribute(name, ArffType.NOMINAL, String, nominalValues)
    }

    Matcher dateMatcher = DATE_TYPE_PATTERN.matcher(typeSpec)
    if (dateMatcher.matches()) {
      String dateFormat = parseDateFormat(dateMatcher.group(1), lineNumber, rawLine)
      return new ArffAttribute(name, ArffType.DATE, Date, null, dateFormat)
    }

    switch (upperType) {
      case 'NUMERIC', 'REAL' -> new ArffAttribute(name, ArffType.NUMERIC, BigDecimal)
      case 'INTEGER' -> new ArffAttribute(name, ArffType.INTEGER, Integer)
      case 'STRING' -> new ArffAttribute(name, ArffType.STRING, String)
      case 'RELATIONAL' -> new ArffAttribute(name, ArffType.RELATIONAL, Matrix)
      default -> {
        if (options.failOnUnknownAttributeType) {
          throw parseError("Unknown @ATTRIBUTE type '$typeSpec'", lineNumber, rawLine)
        }
        yield new ArffAttribute(name, ArffType.STRING, String)
      }
    }
  }

  /**
   * Remove a trailing Weka attribute weight such as {@code numeric {0.5}} (a Weka 3.8 extension outside the ARFF
   * specification). A leading brace group is a nominal declaration and is left untouched; a trailing group after a
   * nominal declaration is already ignored by {@link #extractNominalValues}.
   */
  private static String stripAttributeWeight(String typeSpec) {
    if (typeSpec.startsWith(OPEN_BRACE) || !typeSpec.endsWith(CLOSE_BRACE)) {
      return typeSpec
    }
    int open = ArffScanner.lastIndexOfOutsideQuotes(typeSpec, OPEN_BRACE_CHAR)
    if (open <= 0) {
      return typeSpec
    }
    try {
      new BigDecimal(typeSpec.substring(open + 1, typeSpec.length() - 1).trim())
    } catch (NumberFormatException ignored) {
      return typeSpec
    }
    typeSpec.substring(0, open).trim()
  }

  /**
   * Parse the optional format after the {@code date} keyword. Weka reads it as a single token, so both
   * {@code date 'yyyy-MM-dd\'T\'HH:mm:ss'} (escaped quotes) and {@code date yyyy-MM-dd} (unquoted) are valid.
   *
   * @return the format pattern, or null when none was given (the default pattern applies)
   */
  private static String parseDateFormat(String formatSpec, int lineNumber, String rawLine) {
    String spec = formatSpec?.trim()
    if (spec == null || spec.isEmpty()) {
      return null
    }
    if (ArffScanner.isQuoteChar(spec.charAt(0))) {
      ArffScanner.QuotedToken token = ArffScanner.readQuotedToken(spec, 0)
      if (token == null) {
        throw parseError("$INVALID_DATE_FORMAT (missing closing quote)", lineNumber, rawLine)
      }
      if (!spec.substring(token.end).trim().isEmpty()) {
        throw parseError("$INVALID_DATE_FORMAT (unexpected text after format)", lineNumber, rawLine)
      }
      return token.value
    }
    if (indexOfWhitespace(spec) >= 0) {
      throw parseError("$INVALID_DATE_FORMAT (unexpected text after format)", lineNumber, rawLine)
    }
    spec
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
    if (line.startsWith(OPEN_BRACE)) {
      return parseSparseDataRow(line, attributes, options, lineNumber, rawLine)
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
      row.add(convertValue(value, attributes[i], token.quoted, options, lineNumber, rawLine))
    }
    row
  }

  /** Parse a sparse ARFF row in `{index value, ...}` format. */
  private static List<Object> parseSparseDataRow(String line, List<ArffAttribute> attributes, ArffReadOptions options,
                                                 int lineNumber, String rawLine) {
    if (!line.endsWith(CLOSE_BRACE)) {
      throw parseError('Invalid sparse ARFF row (missing closing brace)', lineNumber, rawLine)
    }
    List<Object> row = attributes.collect { ArffAttribute attr -> sparseDefaultValue(attr) }
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

      int splitIndex = indexOfWhitespace(entry)
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
      row[attributeIndex] = convertValue(value, attributes[attributeIndex], valueToken.quoted, options, lineNumber, rawLine)
    }

    row
  }

  /**
   * The value an attribute has when a sparse row omits it. ARFF defines it as {@code 0}: numeric zero, the first
   * declared nominal value and the epoch for DATE. STRING cells get a marker that {@link #resolveOmittedValues} replaces
   * once the whole data section is known, because Weka resolves {@code 0} through a dictionary filled while reading.
   */
  private static Object sparseDefaultValue(ArffAttribute attr) {
    switch (attr.type) {
      case ArffType.NUMERIC -> BigDecimal.ZERO
      case ArffType.INTEGER -> 0
      case ArffType.NOMINAL -> attr.nominalValues[0]
      case ArffType.DATE -> new Date(0L)
      default -> OMITTED
    }
  }

  /**
   * Resolve STRING cells omitted from sparse rows the way Weka does: value index 0 of the attribute's dictionary, i.e.
   * the first explicit value read for that attribute anywhere in the data. When the attribute never has an explicit
   * value there is nothing index 0 can denote and the cell becomes {@code fallback} (null unless
   * {@code ArffReadOptions.omittedStringFallback} is set).
   */
  private static void resolveOmittedValues(List<List<Object>> rows, List<ArffAttribute> attributes, String fallback) {
    for (int col = 0; col < attributes.size(); col++) {
      ArffType type = attributes[col].type
      if (type != ArffType.STRING && type != ArffType.RELATIONAL) {
        continue
      }
      Object replacement = type == ArffType.STRING ? fallback : null
      for (List<Object> row : rows) {
        Object value = row[col]
        if (value != null && !OMITTED.is(value)) {
          replacement = value
          break
        }
      }
      for (List<Object> row : rows) {
        if (OMITTED.is(row[col])) {
          row[col] = replacement
        }
      }
    }
  }

  private static List<String> splitSparseEntries(String body, int lineNumber, String rawLine) {
    if (ArffScanner.hasUnterminatedQuote(body)) {
      throw parseError('Unterminated quoted sparse value', lineNumber, rawLine)
    }
    List<String> entries = []
    int start = 0
    int comma = ArffScanner.indexOfOutsideQuotes(body, COMMA_CHAR, 0)
    while (comma >= 0) {
      entries.add(body.substring(start, comma))
      start = comma + 1
      comma = ArffScanner.indexOfOutsideQuotes(body, COMMA_CHAR, start)
    }
    entries.add(body.substring(start))
    entries
  }

  private static int indexOfWhitespace(String text) {
    for (int i = 0; i < text.length(); i++) {
      if (Character.isWhitespace(text.charAt(i))) {
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
    if (!ArffScanner.isQuoteChar(first)) {
      return new ParsedToken(value, false)
    }
    ArffScanner.QuotedToken token = ArffScanner.readQuotedToken(value, 0)
    if (token == null || !value.substring(token.end).trim().isEmpty()) {
      throw parseError("Invalid sparse ARFF value '$valuePart'", lineNumber, rawLine)
    }
    new ParsedToken(token.value, true)
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
          current.append(ArffEscapes.unescape(c))
          escape = false
          continue
        }
        if (c == BACKSLASH_CHAR) {
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

      if (c == SINGLE_QUOTE_CHAR || c == DOUBLE_QUOTE_CHAR) {
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
      current.append(BACKSLASH_CHAR)
    }
    values.add(new ParsedToken(current.toString(), tokenQuoted))
    values
  }

  private static Object convertValue(String value, ArffAttribute attr, boolean quoted, ArffReadOptions options,
                                     int lineNumber, String rawLine) {
    if (value == null) {
      return null
    }
    if (!quoted && (value.isEmpty() || value == '?')) {
      return null
    }

    try {
      return switch (attr.type) {
        case ArffType.NUMERIC -> new BigDecimal(value)
        case ArffType.INTEGER -> new BigDecimal(value).intValueExact()
        case ArffType.STRING, ArffType.NOMINAL -> value
        case ArffType.DATE -> parseDate(value, attr)
        case ArffType.RELATIONAL -> parseRelationalValue(value, attr, options, lineNumber, rawLine)
        default -> value
      }
    } catch (NumberFormatException | ArithmeticException e) {
      throw parseError("Invalid ${attr.type} value '$value' for attribute '${attr.name}'", lineNumber, rawLine, e)
    } catch (ParseException e) {
      throw parseError("Invalid DATE value '$value' for attribute '${attr.name}'", lineNumber, rawLine, e)
    }
  }

  /**
   * Parse a relational cell: the (unescaped) token holds the nested rows, one per line, dense or sparse, optionally
   * weighted, exactly like a @DATA section for the sub-relation.
   */
  private static Matrix parseRelationalValue(String value, ArffAttribute attr, ArffReadOptions options,
                                             int lineNumber, String rawLine) {
    List<List<Object>> rows = []
    List<BigDecimal> weights = []
    value.readLines().each { String nestedLine ->
      String content = stripComment(nestedLine).trim()
      if (content.isEmpty()) {
        return
      }
      DataLine dataLine = splitInstanceWeight(content)
      rows.add(parseDataRow(dataLine.content, attr.relationalAttributes, options, lineNumber, rawLine))
      weights.add(dataLine.weight)
    }
    buildMatrix(attr.name, attr.relationalAttributes, rows, weights, options)
  }

  private static Date parseDate(String value, ArffAttribute attr) throws ParseException {
    ParsePosition position = new ParsePosition(0)
    Date parsed = attr.dateFormatter().parse(value, position)
    if (parsed == null || position.index != value.length()) {
      int errorIndex = position.errorIndex >= 0 ? position.errorIndex : position.index
      throw new ParseException("Unparseable date: \"$value\"", errorIndex)
    }
    parsed
  }

  private static String extractNominalValues(String typeSpec) {
    int openIndex = typeSpec.indexOf(OPEN_BRACE)
    if (openIndex < 0) {
      return null
    }
    int closeIndex = ArffScanner.indexOfOutsideQuotes(typeSpec, CLOSE_BRACE_CHAR, openIndex + 1)
    closeIndex < 0 ? null : typeSpec.substring(openIndex + 1, closeIndex)
  }

  private static void validateFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException('File cannot be null')
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
    if (name.contains(DOT)) {
      name = name.substring(0, name.lastIndexOf(DOT))
    }
    name
  }

  private static String defaultName(URL url) {
    String name = url.getPath()
    if (name == null || name.isEmpty()) {
      name = url.getFile()
    }
    if (name == null || name.isEmpty()) {
      return DEFAULT_MATRIX_NAME
    }
    if (name.contains(SLASH)) {
      name = name.substring(name.lastIndexOf(SLASH) + 1)
    }
    if (name.contains(DOT)) {
      name = name.substring(0, name.lastIndexOf(DOT))
    }
    name ?: DEFAULT_MATRIX_NAME
  }

  private static String fallbackName(String defaultName, ArffReadOptions options) {
    options?.fallbackMatrixName ?: defaultName
  }

  private static IllegalArgumentException parseError(String message, int lineNumber, String rawLine) {
    new IllegalArgumentException("$message at line $lineNumber: ${rawLine?.trim()}")
  }

  private static IllegalArgumentException parseError(String message, int lineNumber, String rawLine, Throwable cause) {
    new IllegalArgumentException("$message at line $lineNumber: ${rawLine?.trim()}", cause)
  }

  /** Attributes declared in one scope: the top level, or an open relational declaration named {@code name}. */
  private static final class AttributeScope {
    final String name
    final List<ArffAttribute> attributes = []
    final Set<String> names = [] as Set<String>

    AttributeScope(String name) {
      this.name = name
    }
  }

  /** A data line with its trailing instance weight removed; {@code weight} is null when the line declared none. */
  private static final class DataLine {
    final String content
    final BigDecimal weight

    DataLine(String content, BigDecimal weight) {
      this.content = content
      this.weight = weight
    }
  }

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
  DATE,
  RELATIONAL
}

/** Class representing an ARFF attribute definition. */
class ArffAttribute {
  String name
  ArffType type
  Class javaType
  List<String> nominalValues
  String dateFormat
  /** Declarations of the sub-relation for a RELATIONAL attribute, otherwise null. */
  List<ArffAttribute> relationalAttributes

  ArffAttribute(String name, ArffType type, Class javaType,
                List<String> nominalValues = null, String dateFormat = null, List<ArffAttribute> relationalAttributes = null) {
    this.name = name
    this.type = type
    this.javaType = javaType
    this.nominalValues = nominalValues
    this.dateFormat = dateFormat
    this.relationalAttributes = relationalAttributes
  }

  private SimpleDateFormat dateFormatter

  /**
   * The strict UTC formatter for this attribute's DATE pattern (or the default pattern), created on first use.
   *
   * @return the formatter, or null when this is not a DATE attribute
   */
  SimpleDateFormat dateFormatter() {
    if (dateFormatter == null && type == ArffType.DATE) {
      dateFormatter = ArffDateFormats.create(dateFormat ?: ArffDateFormats.DEFAULT_PATTERN)
    }
    dateFormatter
  }
}
