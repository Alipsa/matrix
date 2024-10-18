package se.alipsa.groovy.matrixcsv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.DuplicateHeaderMode
import se.alipsa.groovy.matrix.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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

  static Matrix importCsv(Map format, URL url) {
    Map r = parseMap(format)
    importCsv(url, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
  }

  static Matrix importCsv(Map format, InputStream is) {
    Map r = parseMap(format)
    importCsv(is, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset, r.tableName as String)
  }

  static Matrix importCsv(Map format, File file) {
    Map r = parseMap(format)
    importCsv(file, r.builder as CSVFormat, r.firstRowAsHeader as boolean, r.charset as Charset)
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


  static Matrix importCsv(InputStream is, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String tableName = '') {
    try (CSVParser parser = CSVParser.parse(is, charset, format)) {
      return parse(tableName, parser, firstRowAsHeader)
    }
  }

  static Matrix importCsv(URL url, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) {
    try (CSVParser parser = CSVParser.parse(url, charset, format)) {
      return parse(tableName(url), parser, firstRowAsHeader)
    }
  }

  static Matrix importCsv(File file, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) {
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
    List<Class<?>> types = [String] * ncols
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

    if (format.containsKey(Format.Trim)) {
      def val = format.get(Format.Trim)
      if (val instanceof Boolean) {
        f.setTrim(val as Boolean)
      } else {
        throw new IllegalArgumentException("The value for Trim must be a Boolean but was $val")
      }
    } else {
      f.setTrim(true)
    }

    if (format.containsKey(Format.Delimiter)) {
      def val = format.get(Format.Delimiter)
      if (val instanceof String || val instanceof Character) {
        f.setDelimiter(String.valueOf(val))
      } else {
        throw new IllegalArgumentException("The value for Delimeter must be a String or Character but was $val")
      }
    } else {
      f.setDelimiter(',')
    }

    if (format.containsKey(Format.IgnoreEmptyLines)) {
      def val = format.get(Format.IgnoreEmptyLines)
      if (val instanceof Boolean) {
        f.setIgnoreEmptyLines(val as Boolean)
      } else {
        throw new IllegalArgumentException("The value for IgnoreEmptyLines must be a Boolean but was $val")
      }
    } else {
      f.setIgnoreEmptyLines(true)
    }

    if (format.containsKey(Format.Quote)) {
      def val = format.get(Format.Quote)
      if (val instanceof String) {
        f.setQuote(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        f.setQuote(val as Character)
      } else {
        throw new IllegalArgumentException("The value for Quote must be a String or Character but was $val")
      }
    } else {
      f.setQuote('"' as Character)
    }

    if (format.containsKey(Format.CommentMarker)) {
      def val = format.get(Format.CommentMarker)
      if (val instanceof String) {
        f.setCommentMarker(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        f.setCommentMarker(val as Character)
      } else {
        throw new IllegalArgumentException("The value for CommentMarker must be a String or Character but was $val")
      }
    } else {
      f.setCommentMarker(null)
    }

    if (format.containsKey(Format.Escape)) {
      def val = format.get(Format.Escape)
      if (val instanceof String) {
        f.setEscape(String.valueOf(val).substring(0, 1) as Character)
      } else if (val instanceof Character) {
        f.setEscape(val as Character)
      } else {
        throw new IllegalArgumentException("The value for Escape must be a String or Character but was $val")
      }
    } else {
      f.setEscape(null)
    }

    if (format.containsKey(Format.Header)) {
      def val = format.get(Format.Header)
      if (val instanceof List || val instanceof String[]) {
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

    if (format.containsKey(Format.IgnoreSurroundingSpaces)) {
      def val = format.get(Format.IgnoreSurroundingSpaces)
      if (val instanceof Boolean) {
        f.setIgnoreSurroundingSpaces(val as Boolean)
      } else {
        throw new IllegalArgumentException("The value for IgnoreSurroundingSpaces must be a Boolean but was $val")
      }
    } else {
      f.setIgnoreSurroundingSpaces(true)
    }

    if (format.containsKey(Format.NullString)) {
      def val = format.get(Format.NullString)
      if (val instanceof String) {
        f.setNullString(val)
      } else {
        throw new IllegalArgumentException("The value for NullString must be a String but was $val")
      }
    } else {
      f.setNullString(null)
    }

    if (format.containsKey(Format.RecordSeparator)) {
      def val = format.get(Format.RecordSeparator)
      if (val instanceof String) {
        f.setRecordSeparator(val)
      } else {
        throw new IllegalArgumentException("The value for RecordSeparator must be a String but was $val")
      }
    } else {
      f.setRecordSeparator('\n')
    }

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
        throw new IllegalArgumentException("The value for IgnoreEmptyLines must be a Boolean but was ${val.class} = $val")
      }
    } else {
      f.setHeader()
      f.setSkipHeaderRecord(true)
    }

    return f
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
