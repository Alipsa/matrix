package se.alipsa.matrix.csv

/**
 * Configuration options for the Map-based CSV reading API.
 *
 * <p>These enum values are used as keys in the format Map passed to
 * {@link CsvReader#read(Map, URL)} and related methods. Keys are
 * case-insensitive, so both {@code Delimiter: ';'} and {@code delimiter: ';'}
 * work when using Groovy named arguments.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * // Explicit enum keys
 * Matrix m = CsvReader.read(
 *     (CsvOption.Delimiter): ';',
 *     (CsvOption.Trim): true,
 *     (CsvOption.Header): ['id', 'name', 'amount'],
 *     file)
 *
 * // Or with Groovy named arguments (camelCase recommended, case-insensitive)
 * Matrix m = CsvReader.read(delimiter: ';', trim: true, file)
 *
 * // With type conversion
 * Matrix m = CsvReader.read(
 *     types: [Integer, String, LocalDate, BigDecimal],
 *     dateTimeFormat: 'yyyy-MM-dd',
 *     file)
 * </pre>
 *
 * @see CsvReader
 * @see CsvFormat
 */
enum CsvOption {
  /** Trim values inside the quote. Default: {@code true}. */
  Trim,
  /** The character used to separate values. Default: {@code ','}. */
  Delimiter,
  /** Skip blank lines. Default: {@code true}. */
  IgnoreEmptyLines,
  /** The char surrounding dates and strings. Default: {@code '"'}. */
  Quote,
  /** The char to designate a line comment. Default: {@code null} (no comments). */
  CommentMarker,
  /** The escape character. Default: {@code null} (no escape character). */
  Escape,
  /** List of strings containing the header. Overrides whatever is set for FirstRowAsHeader. */
  Header,
  /** Determines how duplicate header fields should be handled. Default: {@code 'ALLOW_EMPTY'}. Accepts a String value. */
  DuplicateHeaderMode,
  /** Ignore spaces around the quotes. Default: {@code true}. */
  IgnoreSurroundingSpaces,
  /** Converts strings equal to the given nullString to null when reading records. Default: {@code null} (no substitution). */
  NullString,
  /** The marker for a new line. Default: {@code '\n'}. */
  RecordSeparator,
  /** Whether the first row contains the header. Default: {@code true} unless Header is set. */
  FirstRowAsHeader,
  /** The charset used. Default: {@code UTF-8}. */
  Charset,
  /** The name of the Matrix. Default: {@code ''}. */
  TableName,
  /** {@code List<Class>} of column types by position. Used for automatic type conversion after reading. */
  Types,
  /** {@code String} date/time parse pattern (e.g. {@code 'yyyy-MM-dd'}). Used with {@link #Types} for date/time conversion. */
  DateTimeFormat,
  /** {@code java.text.NumberFormat} instance for locale-aware number parsing. Used with {@link #Types}. */
  NumberFormat
}
