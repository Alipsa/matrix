package se.alipsa.groovy.matrix

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ListConverter {

  static <T> List<T> convert(List<?> list, Class<T> type,
                             DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    List<T> c = []
    list.eachWithIndex { it, idx ->
      try {
        c.add(ValueConverter.convert(it, type, dateTimeFormatter, numberFormat))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $it to $type.simpleName in index $idx", e)
      }
    }
    return c
  }

  static List<LocalDate> toLocalDates(String[] dates) {
    def dat = new ArrayList<LocalDate>(dates.length)
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(LocalDate.parse(d))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to LocalDate in index $i", e)
      }
    }
    return dat
  }

  static List<LocalDate> toLocalDates(String[] dates, DateTimeFormatter formatter) {
    def dat = new ArrayList<LocalDate>(dates.length)
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(LocalDate.parse(d, formatter))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to LocalDate in index $i", e)
      }
    }
    return dat
  }

  static List<LocalDateTime> toLocalDateTimes(String[] dates) {
    def dat = new ArrayList<LocalDateTime>(dates.length)
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(LocalDateTime.parse(d))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to LocalDateTime in index $i", e)
      }
    }
    return dat
  }

  static List<LocalDateTime> toLocalDateTimes(String[] dates, DateTimeFormatter formatter) {
    def dat = new ArrayList<LocalDateTime>(dates.length)
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(LocalDateTime.parse(d, formatter))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to LocalDate in index $i", e)
      }
    }
    return dat
  }
}
