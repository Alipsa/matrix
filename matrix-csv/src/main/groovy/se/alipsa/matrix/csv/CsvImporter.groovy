package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.DuplicateHeaderMode
import org.apache.commons.io.input.ReaderInputStream
import se.alipsa.matrix.core.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@CompileStatic
class CsvImporter {

  enum Format {
    Trim, // trim values inside the quote, default to true
    Delimiter, // the character used to separate values, default to ,
    IgnoreEmptyLines, // skip blank lines, default to true
    Quote, // The char surrounding dates and strings, default to "
    CommentMarker, // The char to designate a line comment, defaults to null i.e. not comments allowed
    Escape, // Sets the escape character. defaults to null (no escape character)
    Header, // List of strings containing the header, overrides whatever is set for FirstRowAsHeader
    DuplicateHeaderMode, // Determines how duplicate header fields should be handled, default to ALLOW_EMPTY
    IgnoreSurroundingSpaces, // ignore spaces around the quotes, default to true
    NullString, // Converts strings equal to the given nullString to null when reading records, default null (no substitution)
    RecordSeparator, // the marker for a new line, defaults to \n
    FirstRowAsHeader, // if the first row contains the header, defaults to true unless Header is set
    Charset, // the charset used, deafults to UTF-8
    TableName // the name of the Matrix, defaults to ''
  }

  static Matrix importCsv(Map format, URL url) throws IOException {
    Map r = parseMap(format)
    importCsv(url, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  static Matrix importCsv(Map format, String url) throws IOException {
    Map r = parseMap(format)
    importCsv(url, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  static Matrix importCsv(Map format, InputStream is) throws IOException {
    Map r = parseMap(format)
    importCsv(is, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset, r.tableName as String)
  }

  static Matrix importCsv(Map format, Reader reader) throws IOException {
    Map r = parseMap(format)
    importCsv(reader, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset, r.tableName as String)
  }

  static Matrix importCsv(Map format, File file) throws IOException {
    Map r = parseMap(format)
    importCsv(file, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  static Matrix importCsv(Map format, Path path) throws IOException {
    Map r = parseMap(format)
    importCsv(path, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  private static Map parseMap(Map format) {
    Map r = [:]
    r.charset = format.getOrDefault(Format.Charset, StandardCharsets.UTF_8) as Charset
    r.tableName = format.getOrDefault(Format.TableName, '')
    r.builder = createFormatBuilder(format).build()
    boolean firstRowAsHeader
    if (format.containsKey(Format.Header)) {
      firstRowAsHeader = false
    } else {
      firstRowAsHeader = format.getOrDefault(Format.FirstRowAsHeader, true)
    }
    r.firstRowAsHeader = firstRowAsHeader
    r
  }


  static Matrix importCsv(InputStream is, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String tableName = '') throws IOException {
    try (CSVParser parser = CSVParser.parse(is, charset, format)) {
      return parse(tableName, parser, firstRowAsHeader)
    }
  }

  static Matrix importCsv(Reader reader, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String tableName = '') throws IOException {
    try(ReaderInputStream readerInputStream = ReaderInputStream.builder()
    .setCharset(charset).setReader(reader).get()) {
      return importCsv(readerInputStream, format, firstRowAsHeader, charset, tableName)
    }
  }

  static Matrix importCsv(URL url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (CSVParser parser = CSVParser.parse(url, charset, format)) {
      return parse(tableName(url), parser, firstRowAsHeader)
    }
  }

  static Matrix importCsv(String url, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    importCsv(new URI(url).toURL(), format, firstRowAsHeader, charset)
  }

  static Matrix importCsv(Path path, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
   importCsv(path.toFile(), format, firstRowAsHeader, charset)
  }

  static Matrix importCsv(File file, CSVFormat format = CSVFormat.DEFAULT, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) throws IOException {
    try (CSVParser parser = CSVParser.parse(file, charset, format)) {
      return parse(tableName(file), parser, firstRowAsHeader)
    }
  }

  private static Matrix parse(String tableName, CSVParser parser, boolean firstRowAsHeader) {
    List<List<String>> rows = parser.records*.toList()
    int rowCount = 0
    int ncols = rows[0].size()
    for (List<String> row in rows) {
      if (row.size() != ncols) {
        throw new IllegalArgumentException("This csv file does not have an equal number of columns on each row, error on row $rowCount: extected $ncols but was ${row.size()}")
      }
      rowCount++
    }
    List<String> headerRow = []
    if (parser.headerNames != null && parser.headerNames.size() > 0) {
      headerRow = parser.headerNames
    } else if (firstRowAsHeader) {
      headerRow = rows.remove(0)
    } else {
      for (int i = 0; i < ncols; i++) {
        headerRow << "c" + i
      }
    }
    def types = [String] * ncols
    return Matrix.builder()
        .matrixName(tableName)
        .columnNames(headerRow)
        .rows(rows)
        .types(types)
        .build()
  }

  static String tableName(URL url) {
    def name = url.getFile() == null ? url.getPath() : url.getFile()
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1, name.length())
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name
  }

  static String tableName(File file) {
    def name = file.getName()
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name
  }

  private static CSVFormat.Builder createFormatBuilder(Map format) {
    format = convertKeysToEnums(format)
    CSVFormat.Builder f = CSVFormat.Builder.create()

    // Boolean format options
    setTrim(f, format)
    setIgnoreEmptyLines(f, format)
    setIgnoreSurroundingSpaces(f, format)

    // Character format options
    setDelimiter(f, format)
    setQuote(f, format)
    setCommentMarker(f, format)
    setEscape(f, format)

    // String format options
    setNullString(f, format)
    setRecordSeparator(f, format)

    // Header format option (special handling)
    if (format.containsKey(Format.Header)) {
      def val = format.get(Format.Header)
      if (val instanceof String[]) {
        f.setHeader(val as String[])
        format.put(Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof List) {
        f.setHeader(val as String[])
        format.put(Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else if (val instanceof String) {
        f.setHeader([val] as String[])
        format.put(Format.FirstRowAsHeader, false)
        f.setSkipHeaderRecord(false)
      } else {
        throw new IllegalArgumentException("The value for Header must be a List or array of Strings but was $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    // DuplicateHeaderMode (enum/string handling)
    if (format.containsKey(Format.DuplicateHeaderMode)) {
      def val = format.get(Format.DuplicateHeaderMode)
      if (val instanceof DuplicateHeaderMode) {
        f.setDuplicateHeaderMode(val)
      } else if (val instanceof String) {
        f.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(val))
      } else {
        throw new IllegalArgumentException("The value for DuplicateHeaderMode must be a String or DuplicateHeaderMode enum but was $val")
      }
    } else {
      f.setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
    }

    // FirstRowAsHeader (special boolean with header interaction)
    if (format.containsKey(Format.FirstRowAsHeader)) {
      def val = format.get(Format.FirstRowAsHeader)
      if (val instanceof Boolean) {
        if (!format.containsKey(Format.Header)) {
          if (val) {
            f.setHeader()
            f.setSkipHeaderRecord(true)
          } else {
            f.setHeader((String) null)
            f.setSkipHeaderRecord(false)
          }
        } else {
          // will be set in the Header section
        }
      } else {
        throw new IllegalArgumentException("The value for FirstRowAsHeader must be a Boolean but was ${val.class} = $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    return f
  }

  // Boolean format options
  private static void setTrim(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.Trim)) {
      def val = format.get(Format.Trim)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for Trim must be a Boolean but was $val")
      }
      builder.setTrim(val as Boolean)
    } else {
      builder.setTrim(true)
    }
  }

  private static void setIgnoreEmptyLines(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.IgnoreEmptyLines)) {
      def val = format.get(Format.IgnoreEmptyLines)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for IgnoreEmptyLines must be a Boolean but was $val")
      }
      builder.setIgnoreEmptyLines(val as Boolean)
    } else {
      builder.setIgnoreEmptyLines(true)
    }
  }

  private static void setIgnoreSurroundingSpaces(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.IgnoreSurroundingSpaces)) {
      def val = format.get(Format.IgnoreSurroundingSpaces)
      if (!(val instanceof Boolean)) {
        throw new IllegalArgumentException("The value for IgnoreSurroundingSpaces must be a Boolean but was $val")
      }
      builder.setIgnoreSurroundingSpaces(val as Boolean)
    } else {
      builder.setIgnoreSurroundingSpaces(true)
    }
  }

  // Character format options
  private static void setDelimiter(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.Delimiter)) {
      def val = format.get(Format.Delimiter)
      if (val instanceof String || val instanceof Character) {
        builder.setDelimiter(String.valueOf(val))
      } else {
        throw new IllegalArgumentException("The value for Delimiter must be a String or Character but was $val")
      }
    } else {
      builder.setDelimiter(',')
    }
  }

  private static void setQuote(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.Quote)) {
      def val = format.get(Format.Quote)
      if (val instanceof String) {
        builder.setQuote(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        builder.setQuote(val as Character)
      } else {
        throw new IllegalArgumentException("The value for Quote must be a String or Character but was $val")
      }
    } else {
      builder.setQuote('"' as Character)
    }
  }

  private static void setCommentMarker(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.CommentMarker)) {
      def val = format.get(Format.CommentMarker)
      if (val instanceof String) {
        builder.setCommentMarker(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        builder.setCommentMarker(val as Character)
      } else {
        throw new IllegalArgumentException("The value for CommentMarker must be a String or Character but was $val")
      }
    } else {
      builder.setCommentMarker(null)
    }
  }

  private static void setEscape(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.Escape)) {
      def val = format.get(Format.Escape)
      if (val instanceof String) {
        builder.setEscape(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        builder.setEscape(val as Character)
      } else {
        throw new IllegalArgumentException("The value for Escape must be a String or Character but was $val")
      }
    } else {
      builder.setEscape(null)
    }
  }

  // String format options
  private static void setNullString(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.NullString)) {
      def val = format.get(Format.NullString)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for NullString must be a String but was $val")
      }
      builder.setNullString(val as String)
    } else {
      builder.setNullString(null)
    }
  }

  private static void setRecordSeparator(CSVFormat.Builder builder, Map format) {
    if (format.containsKey(Format.RecordSeparator)) {
      def val = format.get(Format.RecordSeparator)
      if (!(val instanceof String)) {
        throw new IllegalArgumentException("The value for RecordSeparator must be a String but was $val")
      }
      builder.setRecordSeparator(val as String)
    } else {
      builder.setRecordSeparator('\n')
    }
  }

  private static Map convertKeysToEnums(Map map) {
    Map m = [:]
    map.each {k,v ->
      if (k instanceof Format) {
        m.put(k, v)
      } else {
        def key = Format.valueOf(String.valueOf(k))
        m.put(key, v)
      }
    }
    return m
  }
}
