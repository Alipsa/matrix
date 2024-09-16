package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.NotNull

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@CompileStatic
class ListConverter {

  static <T> List<T> convert(List<?> list, @NotNull Class<T> type,
                             DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
    List<T> c = []
    list.eachWithIndex { it, idx ->
      try {
        if (it == null) {
          c.add(null)
        } else if (it.class.isAssignableFrom(type)) {
          c.add(type.cast(it))
        } else {
          c.add(ValueConverter.convert(it, type, dateTimeFormatter, numberFormat))
        }
      } catch (Exception e) {
        throw new ConversionException("Failed to convert \'$it\' (${it == null ? null : it.getClass().name}) to $type.name in index $idx: ${e.getMessage()}", e)
      }
    }
    return c
  }

  static List<LocalDate> toLocalDates(List<String> dates) {
    toLocalDates(dates as String[])
  }

  static List<LocalDate> toLocalDates(String... dates) {
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

  static List<LocalDate> toLocalDates(DateTimeFormatter formatter, String[] dates) {
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

  static List<LocalDateTime> toLocalDateTimes(String... dates) {
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

  static List<YearMonth> toYearMonths(List<?> localDates) {
    localDates.collect({ValueConverter.asYearMonth(it)})
  }

  static List<YearMonth> toYearMonths(Object... localDates) {
    localDates.collect({ValueConverter.asYearMonth(it)})
  }

  static List<String> toStrings(Collection<?> objList) {
    objList.collect({ValueConverter.asString(it)})
  }

  static List<String> toStrings(Object... objList) {
    objList.collect({ValueConverter.asString(it)})
  }

  static List<Float> toFloats(List<? extends Number> numbers) {
    numbers.collect({ValueConverter.asFloat(it)})
  }

  static List<Float> toFloats(Object... numbers) {
    numbers.collect({ValueConverter.asFloat(it)})
  }

  static List<Double> toDoubles(Object... numbers) {
    numbers.collect({ValueConverter.asDouble(it)})
  }

  static List<Double> toDoubles(List<? extends Number> numbers) {
    numbers.collect({ValueConverter.asDouble(it)})
  }

  static List<Integer> toIntegers(Object... numbers) {
    numbers.collect({ValueConverter.asInteger(it)})
  }

  static List<Integer> toIntegers(List<?> numbers) {
    numbers.collect({ValueConverter.asInteger(it)})
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

  static List<BigDecimal> toBigDecimals(List<?> numbers) {
    List<BigDecimal> list = []
    numbers.each {
      list.add(ValueConverter.asBigDecimal(it))
    }
    return list
  }

  static List<BigDecimal> toBigDecimals(Object... numbers) {
    List<BigDecimal> list = []
    numbers.each {
      list.add(ValueConverter.asBigDecimal(it))
    }
    return list
  }
}
