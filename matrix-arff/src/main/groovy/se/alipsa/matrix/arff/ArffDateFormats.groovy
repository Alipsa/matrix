package se.alipsa.matrix.arff

import java.text.SimpleDateFormat

/**
 * Creates DATE formatters for ARFF parsing and writing.
 */
class ArffDateFormats {

  /** The ARFF default DATE pattern, used when an attribute declares no format. */
  static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

  static final TimeZone ARFF_TIME_ZONE = TimeZone.getTimeZone('UTC')

  /** Create a strict ARFF date formatter. */
  static SimpleDateFormat create(String pattern) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ROOT)
    dateFormat.lenient = false
    dateFormat.timeZone = ARFF_TIME_ZONE
    dateFormat
  }

  private ArffDateFormats() {
    // Utility class
  }

}
