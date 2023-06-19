package se.alipsa.groovy.matrix

import groovyjarjarantlr4.v4.runtime.misc.NotNull

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class ListConverter {

  static <T> List<T> convert(List<?> list, @NotNull Class<T> type,
                             DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    List<T> c = []
    list.eachWithIndex { it, idx ->
      try {
        c.add(ValueConverter.convert(it, type, dateTimeFormatter, numberFormat))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert \'$it\' (${it == null ? null : it.getClass().name}) to $type.name in index $idx", e)
      }
    }
    return c
  }

  static List<LocalDate> toLocalDates(String[] dates) {
    def dat = new ArrayList<LocalDate>(dates.length)
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(d == null ? null : LocalDate.parse(d))
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
        dat.add(d == null ? null : LocalDateTime.parse(d))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to LocalDateTime in index $i, maybe you need to provide a DateTimeFormatter?", e)
      }
    }
    return dat
  }

  static List<LocalDateTime> toLocalDateTimes(DateTimeFormatter formatter, String[] dates) {
    def dat = new ArrayList<LocalDateTime>(dates.length)
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(d == null ? null : LocalDateTime.parse(d, formatter))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to LocalDate in index $i", e)
      }
    }
    return dat
  }

  static List<YearMonth> toYearMonth(List<?> localDates) {
    List<YearMonth> list = []
    localDates.each {
      list.add(ValueConverter.asYearMonth(it))
    }
    return list
  }

  static List<String> toString(List<?> objList) {
    List<String> list = []
    objList.each {
      list.add(ValueConverter.asString(it))
    }
    return list
  }

  static List<Float> toFloats(List<? extends Number> numbers) {
    List<Float> list = []
    numbers.each {
      list.add(ValueConverter.asFloat(it))
    }
    return list
  }

  static List<Double> toDoubles(List<? extends Number> numbers) {
    List<Double> list = []
    numbers.each {
      list.add(ValueConverter.asDouble(it))
    }
    return list
  }

  /**
   * Commons math uses double arrays, so this makes usage of commons math simple
   */
  static double[] toDoubleArray(List<? extends Number> numbers) {
    double[] list = new double[numbers.size()]
    for (int i = 0; i < list.length; i++) {
      list[i] = numbers.get(i).doubleValue()
    }
    return list
  }

  static List<BigDecimal> toBigDecimals(List<? extends Number> numbers) {
    List<BigDecimal> list = []
    numbers.each {
      list.add(ValueConverter.asBigDecimal(it))
    }
    return list
  }
}
