package tech.tablesaw.column.numbers;

import tech.tablesaw.columns.numbers.NumberColumnFormatter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * A formatter for BigDecimalColumn that can be configured to use different formats for printing values.
 *
 * <p>This formatter provides various factory methods for common formatting scenarios including:
 * <ul>
 *   <li>Percentage formatting with configurable precision</li>
 *   <li>Floating point formatting with full precision</li>
 *   <li>Integer-only formatting</li>
 *   <li>Currency formatting for specific locales</li>
 *   <li>Custom formatting using any NumberFormat</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Format as percentage with 2 decimal places
 * BigDecimalColumnFormatter formatter = BigDecimalColumnFormatter.percent(2);
 * column.setPrintFormatter(formatter);
 *
 * // Format as currency
 * BigDecimalColumnFormatter currencyFormatter = BigDecimalColumnFormatter.currency("en", "US");
 * column.setPrintFormatter(currencyFormatter);
 * }</pre>
 *
 * @see NumberColumnFormatter
 * @see tech.tablesaw.api.BigDecimalColumn
 */
public class BigDecimalColumnFormatter extends NumberColumnFormatter {

  protected NumberFormat format;

  /**
   * Returns a formatter that prints floating point numbers as percentages with a fixed number of fractional digits.
   *
   * @param fractionalDigits the number of digits after the decimal point
   * @return a BigDecimalColumnFormatter configured for percentage formatting
   */
  public static BigDecimalColumnFormatter percent(int fractionalDigits) {
    NumberFormat format = NumberFormat.getPercentInstance();
    format.setGroupingUsed(false);
    format.setMinimumFractionDigits(fractionalDigits);
    format.setMaximumFractionDigits(fractionalDigits);
    return new BigDecimalColumnFormatter(format);
  }

  /**
   * Returns a formatter that prints floating point numbers with all precision (up to 340 digits).
   *
   * <p>This formatter preserves the full precision of BigDecimal values without using scientific notation.
   *
   * @return a BigDecimalColumnFormatter configured for maximum precision
   */
  public static BigDecimalColumnFormatter floatingPointDefault() {
    NumberFormat format =
        new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.getDefault()));
    format.setMaximumFractionDigits(340);
    format.setMaximumIntegerDigits(340);
    format.setGroupingUsed(false);
    return new BigDecimalColumnFormatter(format);
  }

  /**
   * Formats numbers using Java's default BigDecimal toString() method.
   *
   * <p>This may use scientific notation for very large or very small numbers.
   *
   * @return a BigDecimalColumnFormatter using default formatting
   */
  public static BigDecimalColumnFormatter standard() {
    return new BigDecimalColumnFormatter();
  }

  /**
   * Returns a formatter that prints floating point numbers as integers (no decimal places).
   *
   * <p>Values are rounded according to the NumberFormat's rounding mode.
   *
   * @return a BigDecimalColumnFormatter configured for integer-only formatting without grouping
   */
  public static BigDecimalColumnFormatter ints() {
    NumberFormat format = new DecimalFormat();
    format.setGroupingUsed(false);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(0);
    return new BigDecimalColumnFormatter(format);
  }

  /**
   * Returns a formatter that prints floating point numbers as integers with digit grouping (e.g., 1,000,000).
   *
   * <p>Values are rounded according to the NumberFormat's rounding mode.
   *
   * @return a BigDecimalColumnFormatter configured for integer-only formatting with grouping
   */
  public static BigDecimalColumnFormatter intsWithGrouping() {
    NumberFormat format = new DecimalFormat();
    format.setGroupingUsed(true);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(0);
    return new BigDecimalColumnFormatter(format);
  }

  /**
   * Returns a formatter that prints floating point numbers with a fixed number of fractional digits and digit grouping.
   *
   * @param fractionalDigits the number of digits after the decimal point
   * @return a BigDecimalColumnFormatter configured for fixed precision with grouping
   */
  public static BigDecimalColumnFormatter fixedWithGrouping(int fractionalDigits) {
    NumberFormat format = new DecimalFormat();
    format.setGroupingUsed(true);
    format.setMinimumFractionDigits(fractionalDigits);
    format.setMaximumFractionDigits(fractionalDigits);
    return new BigDecimalColumnFormatter(format);
  }

  /**
   * Returns a formatter that prints floating point numbers in the currency format
   * of the language and country provided.
   *
   * @param language the ISO 639 alpha-2 or alpha-3 language code
   * @param country the ISO 3166 alpha-2 country code
   */
  public static BigDecimalColumnFormatter currency(String language, String country) {
    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.of(language, country));
    return new BigDecimalColumnFormatter(format);
  }

  /**
   * Constructs a formatter that uses BigDecimal's default toString() formatting with empty missing value string.
   */
  public BigDecimalColumnFormatter() {
    super("");
    this.format = null;
  }

  /**
   * Constructs a formatter with a specific NumberFormat and empty missing value string.
   *
   * @param format the NumberFormat to use for formatting values
   */
  public BigDecimalColumnFormatter(NumberFormat format) {
    super("");
    this.format = format;
  }

  /**
   * Constructs a formatter with a specific NumberFormat and custom missing value string.
   *
   * @param format the NumberFormat to use for formatting values
   * @param missingString the string to use for missing/null values
   */
  public BigDecimalColumnFormatter(NumberFormat format, String missingString) {
    super(missingString);
    this.format = format;
  }

  /**
   * Constructs a formatter that uses BigDecimal's default toString() formatting with custom missing value string.
   *
   * @param missingString the string to use for missing/null values
   */
  public BigDecimalColumnFormatter(String missingString) {
    super(missingString);
    this.format = null;
  }

  /**
   * Formats a BigDecimal value as a string.
   *
   * <p>If the value is null or missing, returns the configured missing value string.
   * Otherwise, formats using the configured NumberFormat, or uses BigDecimal.toString() if no format is set.
   *
   * @param value the BigDecimal value to format
   * @return the formatted string representation of the value
   */
  public String format(BigDecimal value) {
    if (isMissingValue(value)) {
      return getMissingString();
    }
    if (format == null) {
      return value.toString();
    }
    return format.format(value);
  }

  /**
   * Returns the NumberFormat used by this formatter.
   *
   * @return the NumberFormat instance, or null if using default formatting
   */
  @Override
  public NumberFormat getFormat() {
    return format;
  }

  private boolean isMissingValue(BigDecimal value) {
    return BigDecimalColumnType.valueIsMissing(value);
  }
}
