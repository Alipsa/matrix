package se.alipsa.matrix.charm.util

import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle

import static java.time.temporal.ChronoField.MONTH_OF_YEAR

class DateUtil {

  static List<String> getMonthNames() {
    getMonthNames(TextStyle.SHORT, Locale.default, 1, 12)
  }

  static List<String> getMonthNames(TextStyle style, Locale locale, int startMonth, int endMonth) {
    List<String> months = []
    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendText(MONTH_OF_YEAR, style).toFormatter(locale)
    (startMonth..endMonth).each {
      months << formatter.format(Month.of(it))
    }
    months
  }
}
