package se.alipsa.matrix

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ListConverter {

  static <T> List<T> convert(List<?> list, Class<T> type) {
    List<T> c = []
    list.each {
      c.add(ValueConverter.convert(it, type))
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
