package se.alipsa.matrix.arff

import java.text.SimpleDateFormat

/**
 * Creates DATE formatters for ARFF parsing and writing.
 */
class ArffDateFormats {

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
