package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.NotNull

import java.sql.Date
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@CompileStatic
class ListConverter {

  static <T> List<T> convert(List<?> list, @NotNull Class<T> type, T valueIfNull = null,
                             String dateTimeFormat = null, NumberFormat numberFormat = null) {
    List<T> c = []
    list.eachWithIndex { it, idx ->
      try {
        if (it == null) {
          c.add(valueIfNull)
        } else if (it.class.isAssignableFrom(type)) {
          c.add(type.cast(it))
        } else {
          c.add(ValueConverter.convert(it, type, dateTimeFormat, numberFormat))
        }
      } catch (Exception e) {
        throw new ConversionException("Failed to convert \'$it\' (${it == null ? null : it.getClass().name}) to $type.name in index $idx: ${e.getMessage()}", e)
      }
    }
    return c
  }

  static List<java.util.Date> toDates(String... dates) {
    toDates(dates as List, null)
  }

  static List<java.util.Date> toDates(List<String> dates) {
    toDates(dates, null)
  }

  static List<java.util.Date> toDates(List<String> dates, Date valueIfNull) {
    toDates(dates, valueIfNull, 'yyyy-MM-dd')
  }

  static List<java.util.Date> toDates(List<String> dates, java.util.Date valueIfNull, String formatPattern) {
    def format = new SimpleDateFormat(formatPattern)
    def dat = new ArrayList<java.util.Date>(dates.size())
    dates.eachWithIndex { String d, int i ->
      try {
        def date = format.parse(d)
        dat.add(d == null ? valueIfNull : date)
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to java.util.Date in index $i", e)
      }
    }
    return dat
  }

  static List<Date> toSqlDates(String... dates) {
    toSqlDates(dates as List, null)
  }

  static List<Date> toSqlDates(List<String> dates) {
    toSqlDates(dates, null)
  }

  static List<Date> toSqlDates(List<String> dates, Date valueIfNull) {
    toSqlDates(dates, valueIfNull, 'yyyy-MM-dd')
  }

  static List<Date> toSqlDates(List<String> dates, Date valueIfNull, String formatPattern) {
    def format = new SimpleDateFormat(formatPattern)
    def dat = new ArrayList<Date>(dates.size())
    dates.eachWithIndex { String d, int i ->
      try {
        dat.add(d == null ? valueIfNull : new Date(format.parse(d).getTime()))
      } catch (Exception e) {
        throw new ConversionException("Failed to convert $d to java.sql.Date in index $i", e)
      }
    }
    return dat
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
