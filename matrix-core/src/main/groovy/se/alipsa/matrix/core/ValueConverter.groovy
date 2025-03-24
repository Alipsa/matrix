package se.alipsa.matrix.core

import groovy.transform.CompileStatic

import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@CompileStatic
class ValueConverter {

  private static final char C_HYPHEN = '\u002D'
  private static final String S_HYPHEN = String.valueOf(C_HYPHEN)
  private static final char C_MINUS = '\u2212'
  private static final String S_MINUS = String.valueOf(C_MINUS)

  private static final char C_PLUS = '+'
  private static final char C_E = 'E'
  private static final char C_e = 'e'

  static Map<String, SimpleDateFormat> simpleDateCache = [:]
  static Map<String, DateTimeFormatter> dateTimeFormatterCache = [:]

  static <E> E convert(Object o, Class<E> type,
                       String dateTimePattern = null,
                       NumberFormat numberFormat = null,
                       E valueIfNull = null) {
    if (o == null) {
      return valueIfNull
    }
    if (type == o.class) {
      return (E)o
    }
    return switch (type) {
      case String -> (E) asString(o, dateTimeFormatter(dateTimePattern), numberFormat)
      case LocalDate -> (E) asLocalDate(o,dateTimeFormatter(dateTimePattern))
      case LocalDateTime -> (E) asLocalDateTime(o, dateTimeFormatter(dateTimePattern))
      case LocalTime -> (E) asLocalTime(o)
      case YearMonth -> (E) asYearMonth(o)
      case BigDecimal -> (E) asBigDecimal(o, numberFormat)
      case Double, double -> (E) asDouble(o, numberFormat)
      case Byte, byte -> (E) asByte(o)
      case Short, short -> (E) asShort(o)
      case Integer, int -> (E) asInteger(o)
      case Long, long -> (E) asLong(o)
      case BigInteger -> (E) asBigInteger(o)
      case Float -> (E) asFloat(o)
      case Date -> (E) asSqlDate(o)
      case Time -> (E) asSqlTIme(o)
      case Timestamp -> (E) asTimestamp(o)
      case java.util.Date -> (E) asDate(o, dateTimePattern)
      case Number -> (E) asNumber(o)
      default -> try {
        type.cast(o)
      } catch (ClassCastException ignored) {
        o.asType(type)
      }
    }
  }

  static BigDecimal asBigDecimal(BigDecimal num) {
    return num
  }

  static BigDecimal asBigDecimal(BigInteger num) {
    return num.toBigDecimal()
  }

  static BigDecimal asBigDecimal(String num, NumberFormat format = null) {
    if (num == null || 'null' == num || num.isBlank()) return null
    if (format == null) {
      def n = asDecimalNumber(num)
      if (n.isBlank()) return null
      return new BigDecimal(num)
    }
    return format.parse(fixNegationFormat(format, num)) as BigDecimal
  }

  static BigDecimal asBigDecimal(Number num) {
    return num as BigDecimal
  }

  static BigDecimal asBigDecimal(Object num, NumberFormat format = null) {
    if (num == null) {
      return null
    }
    if (num instanceof BigDecimal) {
      return num
    }
    if (num instanceof BigInteger) {
      return asBigDecimal(num as BigInteger)
    }
    if (num instanceof Number) {
      return asBigDecimal(num as Number)
    }
    if (num instanceof String) {
      if (format == null) {
        return asBigDecimal(num as String)
      } else {
        return asBigDecimal(num as String, format)
      }
    }
    return asBigDecimal(String.valueOf(num), format)
  }

  static Boolean asBoolean(Object obj) {
    if (obj == null) return null
    if (obj instanceof Boolean) return obj as Boolean
    if (obj instanceof Number) return 1 == (obj as Number)
    return asBoolean(String.valueOf(obj))
  }

  static Boolean asBoolean(String val) {
    if (val == null) return null
    return val.toLowerCase() in ['1', 'true', 'on', 'yes']
  }

  static Double asDouble(Double num) {
    return num
  }


  static Double asDouble(String num, NumberFormat format = null, Double valueIfNull = null) {
    // Maybe Double.NaN instead of null?
    if (num == null || 'null' == num || num.isBlank()) return valueIfNull

    if (format == null) return Double.valueOf(num)

    return format.parse(fixNegationFormat(format, num)) as Double
  }

  static Double asDouble(Number num, Double valueIfNull = null) {
    return num == null ? valueIfNull : num as Double
  }

  static Double asDouble(Object obj, NumberFormat format = null, Double valueIfNull = null) {
    if (obj == null) return valueIfNull
    if (obj instanceof Number) return asDouble(obj as Number)
    if (obj instanceof String) return asDouble(obj as String, format)
    return asDouble(String.valueOf(obj), format)
  }

  static LocalDate asLocalDate(String date, LocalDate valueIfNull = null) {
    if (date == null) return valueIfNull
    return LocalDate.parse(date)
  }

  static LocalDate asLocalDate(String date, DateTimeFormatter formatter) {
    if (formatter == null) return asLocalDate(date)
    return date == null ? null : LocalDate.parse(date, formatter)
  }

  static LocalDate asLocalDate(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.toLocalDate()
  }

  static LocalDate asLocalDate(Object date, DateTimeFormatter formatter = null, LocalDate valueIfNull = null) {
    if (date == null) return valueIfNull
    if (date instanceof LocalDate) {
      return date
    }
    if (date instanceof LocalDateTime) {
      return date.toLocalDate()
    }
    if (date instanceof java.util.Date) {
      return new Date(date.getTime()).toLocalDate()
    }
    if (date instanceof Number) {
      // Number of days since 1970-01-01
      LocalDate.ofEpochDay(date.longValue())
    }
    if (formatter == null) {
      return LocalDate.parse(String.valueOf(date))
    }
    return LocalDate.parse(String.valueOf(date), formatter)
  }

  static LocalDateTime asLocalDateTime(Object o, DateTimeFormatter dateTimeFormatter = null, LocalDateTime valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof LocalDate) return o.atStartOfDay()
    if (o instanceof LocalDateTime) return o
    if (o instanceof Date) return new Timestamp(o.getTime()).toLocalDateTime()
    if (o instanceof Timestamp) return o.toLocalDateTime()
    if (o instanceof Number) {
      return LocalDateTime.ofEpochSecond(o.toLong(), 0, OffsetDateTime.now().getOffset())
    }
    if (dateTimeFormatter == null)
      return LocalDateTime.parse(String.valueOf(o))
    return LocalDateTime.parse(String.valueOf(o), dateTimeFormatter)
  }

  static LocalDateTime asLocalDateTime(Object o, String pattern, LocalDateTime valueIfNull = null) {
    asLocalDateTime(o, DateTimeFormatter.ofPattern(pattern), valueIfNull)
  }

  static Byte asByte(Object o, Byte valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) return o.byteValue()
    try {
      return (o as BigDecimal).byteValue()
    } catch (NumberFormatException ignored) {
      String val = asDecimalNumber(String.valueOf(o))
      if (val.isBlank()) return null
      return Byte.valueOf(val)
    }
  }

  static Short asShort(Object o, Short valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) return o.shortValue()
    try {
      return (o as BigDecimal).shortValue()
    } catch (NumberFormatException ignored) {
      String val = asDecimalNumber(String.valueOf(o))
      if (val.isBlank()) return null
      return Short.valueOf(val)
    }
  }

  static Integer asInteger(Object o, Integer valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) {
      return o.intValue()
    } else if (o instanceof Boolean) {
      return o ? 1 : 0
    }
    String strVal = String.valueOf(o).toLowerCase()
    if (strVal == 'true') {
      return 1
    } else if (strVal == 'false') {
      return 0
    }
    String val = asDecimalNumber(strVal)
    if (val.isBlank()) return null
    return Double.parseDouble(val).intValue()
  }

  static Integer asIntegerRound(Object o, Integer valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) {
      return Math.round(o.doubleValue()).intValue()
    }

    String val = asDecimalNumber(String.valueOf(o))
    if (val.isBlank()) return null
    return Math.round(Double.parseDouble(val)).intValue()
  }

  static BigInteger asBigInteger(Object o, BigInteger valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) return o.toBigInteger()
    return new BigInteger(String.valueOf(o))
  }

  /**
   * Checks whether object is a Number or a CharSequence containing numbers
   *
   * @param o the Object to test
   * @param numberFormatOpt an optional NumberFormat to use
   * @return true if it is numeric otherwise false
   */
  static boolean isNumeric(Object o, NumberFormat... numberFormatOpt) {
    if (o == null) return false
    if (o instanceof Number) return true
    if (o instanceof CharSequence) {
      ParsePosition pos = new ParsePosition(0)
      String str = o.toString()
      NumberFormat format = numberFormatOpt.length == 0 ? NumberFormat.getInstance() : numberFormatOpt[0]
      format.parse(str, pos)
      //  if, after parsing the string, the parser position is at the end of the string,
      //  we can safely assume that the entire string is numeric
      return str.length() == pos.getIndex()
    }
    return false
  }

  /** strips off any non numeric char from the string. */
  static String asDecimalNumber(String txt, char decimalSeparator = '.', String valueIfNull = null) {
    if (txt == null) return valueIfNull
    StringBuilder result = new StringBuilder()
    txt.chars().mapToObj(i -> i as char)
        .filter(c -> c.isDigit() || decimalSeparator == c || C_MINUS == c || C_HYPHEN == c
            || C_PLUS == c || C_e == c || C_E == c)
        .forEach(c -> result.append(c))
    return result.toString()
  }

  static YearMonth asYearMonth(Object o, YearMonth valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof TemporalAccessor) {
      return YearMonth.from(o as TemporalAccessor)
    }
    if (o instanceof CharSequence) {
      return YearMonth.parse(o as CharSequence)
    }
    if (o instanceof java.util.Date) {
      java.util.Date date = o as java.util.Date
      Calendar calendar = Calendar.getInstance()
      calendar.setTime(date)
      return YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
    if (o instanceof Calendar) {
      Calendar calendar = o as Calendar
      return YearMonth.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
    if (o instanceof Number) {
      def val = ((Number)o).intValue()
      return YearMonth.of(val / 100 as int, val % 100)
    }
    throw new IllegalArgumentException("Failed to convert ${o.class}, $o to a YearMonth")
  }

  static YearMonth asYearMonth(Object o, DateTimeFormatter formatter, YearMonth valueIfNull = null) {
    if (o instanceof CharSequence) {
      return YearMonth.from(formatter.parse(o as String))
    }
    return asYearMonth(o, valueIfNull)
  }

  static String asString(Object o, DateTimeFormatter formatter = null, NumberFormat numberFormat = null, String valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof TemporalAccessor && formatter != null) {
      return formatter.format(o)
    } else if (o instanceof Number && numberFormat != null) {
      return numberFormat.format(o)
    }
    return String.valueOf(o)
  }

  static Float asFloat(Object o, Float valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) {
      return o.toFloat()
    }
    return Float.valueOf(String.valueOf(o))
  }

  static Long asLong(Object o, Long valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Number) {
      return o.longValue()
    }
    Double.valueOf(String.valueOf(o)).longValue()
  }

  static java.util.Date asDate(java.util.Date o, java.util.Date valueIfNull = null) {
    if (o == null) return valueIfNull
    return o as java.util.Date
  }

  static java.util.Date asDate(Number o, java.util.Date valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o < 22991231) {
      return new Date(simpleDateFormatCache('yyyyMMdd').parse(String.valueOf(o.longValue())).getTime())
    }
    // assume millis since 1970-01-01
    return new Date(o.longValue())
  }

  static java.util.Date asDate(LocalDate o, java.util.Date valueIfNull = null) {
    if (o == null) return valueIfNull
    return Date.from(o.atStartOfDay(ZoneId.systemDefault()).toInstant())
  }

  static java.util.Date asDate(LocalDateTime o, java.util.Date valueIfNull = null) {
    if (o == null) return valueIfNull
    return Date.from(o.atZone(ZoneId.systemDefault()).toInstant())
  }


  static java.util.Date asDate(Object o, java.util.Date valueIfNull = null) {
    if (o == null) return valueIfNull
    String s = String.valueOf(o)
    if (s.size() == 10) {
      return simpleDateFormatCache('yyyy-MM-dd').parse(s)
    }
    if (s.size() == 8 && isNumeric(s)) {
      return simpleDateFormatCache('yyyyMMdd').parse(s)
    }
    throw new IllegalArgumentException("Failed to convert $o of type ${o.class} to java.util.Date")
  }

  static java.util.Date asDate(String val, String pattern, java.util.Date valueIfNull = null) {
    if (val == null) return valueIfNull
    return simpleDateFormatCache(pattern).parse(val)
  }

  static java.util.Date asDate(Object val, String pattern, java.util.Date valueIfNull = null) {
    if (val instanceof String && pattern != null) {
      return asDate(val as String, pattern)
    } else {
      return asDate(val, valueIfNull)
    }
  }

  static Timestamp asTimestamp(Object o, Timestamp valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Timestamp) {
      return o
    }
    if (o instanceof Date) {
      return new Timestamp(o.getTime())
    }
    if (o instanceof LocalDateTime) {
      return Timestamp.valueOf(o)
    }
    if (o instanceof ZonedDateTime) {
      return Timestamp.valueOf(o.toLocalDateTime())
    }
    if (o instanceof LocalDate) {
      return Timestamp.valueOf(o.atStartOfDay())
    }
    if (o instanceof String) {
      return Timestamp.valueOf(o)
    }
    throw new IllegalArgumentException("Failed to convert $o of type ${o.class} to java.sql.Timestamp")
  }

  static Date asSqlDate(Object o, Date valueIfNull = null) {
    if (o == null) return valueIfNull
    if (o instanceof Date) {
      return o
    }
    if (o instanceof LocalDate) {
      return Date.valueOf(o)
    }
    if (o instanceof LocalDateTime) {
      return Date.valueOf(o.toLocalDate())
    }
    if (o instanceof String) {
      return Date.valueOf(o)
    }
    if (o instanceof Number) {
      if (o < 22991231) {
        return new Date(simpleDateFormatCache('yyyyMMdd').parse(String.valueOf(o.longValue())).getTime())
      }
      // assume millis since 1970-01-01
      return new Date(o.longValue())
    }
    throw new IllegalArgumentException("Failed to convert $o of type ${o.class} to java.sql.Date")
  }

  private static SimpleDateFormat simpleDateFormatCache(String pattern) {
    if (pattern == null) return null

    if (!simpleDateCache.containsKey(pattern)) {
      simpleDateCache.put(pattern, new SimpleDateFormat(pattern))
    }
    return simpleDateCache.get(pattern)
  }

  static DateTimeFormatter dateTimeFormatter(String pattern) {
    if (pattern == null) return null
    if (!dateTimeFormatterCache.containsKey(pattern)) {
      dateTimeFormatterCache.put(pattern, DateTimeFormatter.ofPattern(pattern))
    }
    dateTimeFormatterCache.get(pattern)
  }

  static Time asSqlTIme(Object o, Time valueIfNull = null) {
    if (o == null) {
      return valueIfNull
    }
    if (o instanceof Time) {
      return o
    }
    if (o instanceof LocalTime) {
      return Time.valueOf(o)
    }
    return Time.valueOf(String.valueOf(o))
  }

  static LocalTime asLocalTime(Object o, LocalTime valueIfNull = null) {
    if (o == null) {
      return valueIfNull
    }
    if (o instanceof LocalTime) {
      return o
    }
    if (o instanceof Time) {
      return o.toLocalTime()
    }
    return LocalTime.parse(String.valueOf(o))
  }

  static Number asNumber(Object o, Number valueIfNull = null) {
    if (o == null) {
      return valueIfNull
    }
    if (o instanceof Number) {
      return o as Number
    }
    asBigDecimal(o)
  }

  static String fixNegationFormat(NumberFormat format, String val) {
    // Some formats e.g. Sweden requires minus and throws an error on hyphen
    // Other formats e.g. US does the opposite
    // here we make sure hyphen and minus both mean the negative prefix
    String neg = ((DecimalFormat)format).negativePrefix
    val.replace(S_HYPHEN, neg).replace(S_MINUS, neg)
  }
}
