package tech.tablesaw.io.ods;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.io.ReadOptions;
import tech.tablesaw.io.Source;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read options for ODS (OpenDocument Spreadsheet) format.
 *
 * <p>This class provides configuration options for reading Tablesaw tables from ODS files.
 * It extends {@link ReadOptions} to support standard parsing options like date/time formats,
 * column types, and missing value indicators, and adds ODS-specific options like sheet selection.
 *
 * <p>Example usage:
 * <pre>{@code
 * OdsReadOptions options = OdsReadOptions.builder("input.ods")
 *     .sheetIndex(0)
 *     .dateFormat(DateTimeFormatter.ISO_LOCAL_DATE)
 *     .missingValueIndicator("N/A", "")
 *     .build();
 * Table table = new OdsReader().read(options);
 * }</pre>
 *
 * @see OdsReader
 * @see ReadOptions
 */
public class OdsReadOptions extends ReadOptions {

  /** The sheet to read. Null means no specific index was set. First sheet has index 0. */
  protected Integer sheetIndex;

  /**
   * Creates a builder with a File source.
   * The table name is automatically set to the file name.
   *
   * @param file the file to read from
   * @return a new builder
   */
  public static Builder builder(File file) {
    return new Builder(file).tableName(file.getName());
  }

  /**
   * Creates a builder with an InputStream source.
   *
   * @param stream the input stream to read from
   * @return a new builder
   */
  public static Builder	builder(InputStream stream) {
    return new Builder(stream);
  }

  /**
   * Creates a builder with a Reader source.
   *
   * @param reader the reader to read from
   * @return a new builder
   */
  public static Builder	builder(Reader reader) {
    return new Builder(reader);
  }

  /**
   * Creates a builder with a file name source.
   *
   * @param fileName the name of the file to read from
   * @return a new builder
   */
  public static Builder	builder(String fileName) {
    return new Builder(new File(fileName));
  }

  /**
   * Creates a builder with a URL source.
   *
   * @param url the URL to read from
   * @return a new builder
   * @throws IOException if the URL cannot be accessed
   */
  public static Builder	builder(URL url) throws IOException {
    return new Builder(url);
  }

  /**
   * Creates a builder with the specified source.
   *
   * @param source the source to read from
   * @return a new builder
   */
  public static Builder	builder(Source source) {
    return new Builder(source);
  }

  /**
   * Creates a builder that reads from a string containing ODS content.
   *
   * @param contents the ODS content as a string
   * @return a new builder
   */
  public static Builder	builderFromString(String contents) {
    return new Builder(new StringReader(contents));
  }

  /**
   * Creates a builder with a URL string source.
   *
   * @param url the URL string to read from
   * @return a new builder
   * @throws IOException if the URL cannot be accessed
   * @throws URISyntaxException if the URL string is not a valid URI
   */
  public static Builder builderFromUrl(String url) throws IOException, URISyntaxException {
    return new Builder(new URI(url).toURL());
  }

  /**
   * Constructs read options from a builder.
   *
   * @param builder the builder containing the configuration
   */
  protected OdsReadOptions(Builder builder) {
    super(builder);
    sheetIndex = builder.sheetIndex;
  }

  /**
   * Builder for {@link OdsReadOptions}.
   *
   * <p>Provides a fluent API for configuring ODS read options including sheet selection,
   * date/time formats, column types, missing value indicators, and other parsing settings.
   */
  public static class Builder extends ReadOptions.Builder {

    protected Integer sheetIndex;

    /**
     * Constructs a builder with the specified source.
     *
     * @param source the source to read from
     */
    protected Builder(Source source) {
      super(source);
    }

    /**
     * Constructs a builder with a File source.
     *
     * @param file the file to read from
     */
    protected Builder(File file) {
      super(file);
    }

    /**
     * Constructs a builder with an InputStream source.
     *
     * @param stream the input stream to read from
     */
    protected Builder(InputStream stream) {
      super(stream);
    }

    /**
     * Constructs a builder with a Reader source.
     *
     * @param reader the reader to read from
     */
    protected Builder(Reader reader) {
      super(reader);
    }

    /**
     * Constructs a builder with a URL source.
     *
     * @param url the URL to read from
     * @throws IOException if the URL cannot be accessed
     */
    protected Builder(URL url) throws IOException {
      super(url);
    }

    /**
     * Sets whether the first row contains column headers.
     *
     * @param header true if the first row contains headers
     * @return this builder
     */
    @Override
    public Builder header(boolean header) {
      super.header(header);
      return this;
    }

    /**
     * Sets the name for the table.
     *
     * @param tableName the table name
     * @return this builder
     */
    @Override
    public Builder tableName(String tableName) {
      super.tableName(tableName);
      return this;
    }

    /**
     * Sets whether to sample the data for type detection.
     *
     * @param sample true to sample the data
     * @return this builder
     */
    @Override
    public Builder sample(boolean sample) {
      super.sample(sample);
      return this;
    }

    /**
     * Sets the date format for parsing date columns.
     *
     * @param dateFormat the date formatter
     * @return this builder
     */
    @Override
    public Builder dateFormat(DateTimeFormatter dateFormat) {
      super.dateFormat(dateFormat);
      return this;
    }

    /**
     * Sets the time format for parsing time columns.
     *
     * @param timeFormat the time formatter
     * @return this builder
     */
    @Override
    public Builder timeFormat(DateTimeFormatter timeFormat) {
      super.timeFormat(timeFormat);
      return this;
    }

    /**
     * Sets the date-time format for parsing date-time columns.
     *
     * @param dateTimeFormat the date-time formatter
     * @return this builder
     */
    @Override
    public Builder dateTimeFormat(DateTimeFormatter dateTimeFormat) {
      super.dateTimeFormat(dateTimeFormat);
      return this;
    }

    /**
     * Sets the locale for parsing locale-sensitive data.
     *
     * @param locale the locale
     * @return this builder
     */
    @Override
    public Builder locale(Locale locale) {
      super.locale(locale);
      return this;
    }

    /**
     * Sets the strings that should be treated as missing values.
     *
     * @param missingValueIndicators the missing value indicators
     * @return this builder
     */
    @Override
    public Builder missingValueIndicator(String... missingValueIndicators) {
      super.missingValueIndicator(missingValueIndicators);
      return this;
    }

    /**
     * Enables column size minimization to use the smallest appropriate data types.
     *
     * @return this builder
     */
    @Override
    public Builder minimizeColumnSizes() {
      super.minimizeColumnSizes();
      return this;
    }

    /**
     * Sets the column types for all columns.
     *
     * @param columnTypes array of column types
     * @return this builder
     */
    @Override
    public Builder columnTypes(ColumnType[] columnTypes) {
      super.columnTypes(columnTypes);
      return this;
    }

    /**
     * Sets a function to determine column types by column name.
     *
     * @param columnTypeFunction function mapping column names to types
     * @return this builder
     */
    @Override
    public Builder columnTypes(Function<String, ColumnType> columnTypeFunction) {
      super.columnTypes(columnTypeFunction);
      return this;
    }

    /**
     * Sets a partial function to determine column types by column name.
     * Columns not mapped by the function will use automatic type detection.
     *
     * @param columnTypeFunction partial function mapping column names to types
     * @return this builder
     */
    @Override
    public Builder columnTypesPartial(Function<String, Optional<ColumnType>> columnTypeFunction) {
      super.columnTypesPartial(columnTypeFunction);
      return this;
    }

    /**
     * Sets column types for specific columns by name.
     * Columns not in the map will use automatic type detection.
     *
     * @param columnTypeByName map of column names to types
     * @return this builder
     */
    @Override
    public Builder columnTypesPartial(Map<String, ColumnType> columnTypeByName) {
      super.columnTypesPartial(columnTypeByName);
      return this;
    }

    /**
     * Sets the index of the sheet to read from the ODS file.
     * Sheet indices are 0-based (first sheet is index 0).
     *
     * @param sheetIndex the zero-based index of the sheet to read
     * @return this builder
     */
    public Builder sheetIndex(int sheetIndex) {
      this.sheetIndex = sheetIndex;
      return this;
    }

    /**
     * Builds the ODS read options.
     *
     * @return the configured read options
     */
    @Override
    public OdsReadOptions build() {
      return new OdsReadOptions(this);
    }
  }
}
