package se.alipsa.matrix.arff

import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Creates DATE formatters for ARFF parsing and writing.
 */
class ArffDateFormats {

  /** The ARFF default DATE pattern, used when an attribute declares no format. */
  static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

  static final TimeZone ARFF_TIME_ZONE = TimeZone.getTimeZone('UTC')

  /** Create a non-lenient ARFF date formatter in the default mode ({@link ArffDateMode#UTC}). */
  static SimpleDateFormat create(String pattern) {
    create(pattern, ArffDateMode.UTC)
  }

  /**
   * Create a non-lenient ARFF date formatter.
   *
   * @param pattern a {@link SimpleDateFormat} pattern
   * @param mode {@link ArffDateMode#UTC} for {@code Locale.ROOT} and UTC, {@link ArffDateMode#WEKA} for the JVM's
   *        default locale and time zone (what {@code weka.core.DateAttributeInfo} does)
   */
  static SimpleDateFormat create(String pattern, ArffDateMode mode) {
    // Weka calls new SimpleDateFormat(pattern), i.e. the JVM's FORMAT-category default locale
    SimpleDateFormat dateFormat = mode == ArffDateMode.WEKA
        ? new SimpleDateFormat(pattern, Locale.getDefault(Locale.Category.FORMAT))
        : new SimpleDateFormat(pattern, Locale.ROOT)
    dateFormat.lenient = false
    if (mode != ArffDateMode.WEKA) {
      dateFormat.timeZone = ARFF_TIME_ZONE
    }
    dateFormat
  }

  /**
   * Parse a DATE value. In {@link ArffDateMode#UTC} the whole value must be consumed; in {@link ArffDateMode#WEKA}
   * text after the date is ignored, as {@code SimpleDateFormat.parse(String)} (and therefore Weka) does.
   *
   * @throws ParseException when the value does not match the formatter's pattern
   */
  static Date parse(SimpleDateFormat format, String value, ArffDateMode mode) throws ParseException {
    ParsePosition position = new ParsePosition(0)
    Date parsed = format.parse(value, position)
    if (parsed == null || (mode != ArffDateMode.WEKA && position.index != value.length())) {
      int errorIndex = position.errorIndex >= 0 ? position.errorIndex : position.index
      throw new ParseException("Unparseable date: \"$value\"", errorIndex)
    }
    parsed
  }

  /** The zone in which a {@code LocalDate}/{@code LocalDateTime} is placed on the time line before formatting. */
  static ZoneId zone(ArffDateMode mode) {
    mode == ArffDateMode.WEKA ? ZoneId.systemDefault() : ZoneOffset.UTC
  }

  private ArffDateFormats() {
    // Utility class
  }

}
