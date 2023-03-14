package se.alipsa.groovy.matrix

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ListConverter {

  static <T> List<T> convert(List<?> list, Class<T> type,
                             DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    List<T> c = []
    list.each {
      c.add(ValueConverter.convert(it, type, dateTimeFormatter, numberFormat))
    }
    return c
  }

  static List<LocalDate> toLocalDates(String[] dates) {
    def dat = new ArrayList<LocalDate>(dates.length)
    for (d in dates) {
      dat.add(LocalDate.parse(d))
    }
    return dat
  }

  static List<LocalDate> toLocalDates(String[] dates, DateTimeFormatter formatter) {
    def dat = new ArrayList<LocalDate>(dates.length)
    for (d in dates) {
      dat.add(LocalDate.parse(d, formatter))
    }
    return dat
  }
}
