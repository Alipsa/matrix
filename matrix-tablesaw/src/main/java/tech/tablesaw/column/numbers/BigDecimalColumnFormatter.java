package tech.tablesaw.column.numbers;

import tech.tablesaw.columns.numbers.NumberColumnFormatter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * A formatter for BigDecimalColumn that can be configured to use different formats for printing
 * values.
 */
public class BigDecimalColumnFormatter extends NumberColumnFormatter {

  protected NumberFormat format;

  /** Returns a formatter that prints floating point numbers with a fixed number of fractional digits */
  public static BigDecimalColumnFormatter percent(int fractionalDigits) {
    NumberFormat format = NumberFormat.getPercentInstance();
    format.setGroupingUsed(false);
    format.setMinimumFractionDigits(fractionalDigits);
    format.setMaximumFractionDigits(fractionalDigits);
    return new BigDecimalColumnFormatter(format);
  }

  /** Returns a formatter that prints floating point numbers with all precision */
  public static BigDecimalColumnFormatter floatingPointDefault() {
    NumberFormat format =
        new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.getDefault()));
    format.setMaximumFractionDigits(340);
    format.setMaximumIntegerDigits(340);
    format.setGroupingUsed(false);
    return new BigDecimalColumnFormatter(format);
  }

  /** Formats numbers using java default, so sometimes in scientific notation, sometimes not */
  public static BigDecimalColumnFormatter standard() {
    return new BigDecimalColumnFormatter();
  }

  /** Returns a formatter that prints floating point numbers as if they were integers */
  public static BigDecimalColumnFormatter ints() {
    NumberFormat format = new DecimalFormat();
    format.setGroupingUsed(false);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(0);
    return new BigDecimalColumnFormatter(format);
  }

  public static BigDecimalColumnFormatter intsWithGrouping() {
    NumberFormat format = new DecimalFormat();
    format.setGroupingUsed(true);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(0);
    return new BigDecimalColumnFormatter(format);
  }

  /** Returns a formatter that prints floating point numbers with a fixed number of fractional digits */
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

  public BigDecimalColumnFormatter() {
    super("");
    this.format = null;
  }

  public BigDecimalColumnFormatter(NumberFormat format) {
    super("");
    this.format = format;
  }

  public BigDecimalColumnFormatter(NumberFormat format, String missingString) {
    super(missingString);
    this.format = format;
  }

  public BigDecimalColumnFormatter(String missingString) {
    super(missingString);
    this.format = null;
  }

  public String format(BigDecimal value) {
    if (isMissingValue(value)) {
      return getMissingString();
    }
    if (format == null) {
      return value.toString();
    }
    return format.format(value);
  }

  @Override
  public NumberFormat getFormat() {
    return format;
  }

  private boolean isMissingValue(BigDecimal value) {
    return BigDecimalColumnType.valueIsMissing(value);
  }
}
