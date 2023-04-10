package se.alipsa.groovy.matrix

import java.sql.Timestamp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

class ValueConverter {

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
            return new BigDecimal(n)
        }
        return format.parse(num) as BigDecimal
    }

    static BigDecimal asBigDecimal(Number num) {
        return num as BigDecimal
    }

    static BigDecimal asBigDecimal(Object num, NumberFormat format = null) {
        if (num instanceof BigDecimal) {
            return asBigDecimal(num as BigDecimal)
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
        if (String.valueOf(obj).toLowerCase() in ["1", "true"])
            return true
        if (String.valueOf(obj).toLowerCase() in ["0", "false"])
            return false
        throw new ConversionException("Failed to convert $obj (${obj == null ? null : obj.getClass()} to Boolean")
    }

    static Double asDouble(Double num) {
        return num
    }


    static Double asDouble(String num, NumberFormat format = null) {
        // Maybe Double.NaN instead of null?
        if (num == null  || 'null' == num || num.isBlank()) return null
        if (format == null) return Double.valueOf(num)
        return format.parse(num) as Double
    }

    static Double asDouble(Number num) {
        return num as Double
    }

    static Double asDouble(Object obj, NumberFormat format = null) {
        if (obj == null ) return null
        if (obj instanceof Double) return asDouble(obj as Double)
        if (obj instanceof BigDecimal) return asDouble(obj as BigDecimal)
        if (obj instanceof Number) return asDouble(obj as Number)
        if (obj instanceof String) return asDouble(obj as String, format)
        return asDouble(String.valueOf(obj), format)
    }

    static LocalDate asLocalDate(String date) {
        return LocalDate.parse(date)
    }

    static LocalDate asLocalDate(String date, DateTimeFormatter formatter) {
        if (formatter == null) return asLocalDate(date)
        return date == null ? null : LocalDate.parse(date, formatter)
    }

    static LocalDate asLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate()
    }

    static LocalDate asLocalDate(Object date, DateTimeFormatter formatter = null) {
        if (date == null) return null
        if (date instanceof LocalDate) {
            return date
        }
        if (date instanceof LocalDateTime) {
            return date.toLocalDate()
        }
        if (date instanceof Date) {
            return new java.sql.Date(date.getTime()).toLocalDate()
        }
        if (formatter == null) {
            return LocalDate.parse(String.valueOf(date))
        }
        return LocalDate.parse(String.valueOf(date), formatter)
    }

    static <T> T convert(Object o, Class<T> type,
                         DateTimeFormatter dateTimeFormatter = null,
                         NumberFormat numberFormat = null) {
        return switch (type) {
            case String -> (T)String.valueOf(o)
            case LocalDate -> (T)asLocalDate(o, dateTimeFormatter)
            case LocalDateTime -> (T)asLocalDateTime(o, dateTimeFormatter)
            case YearMonth -> (T)asYearMonth(o)
            case BigDecimal -> (T)asBigDecimal(o, numberFormat)
            case Double, double -> (T)asDouble(o, numberFormat)
            case Integer, int -> (T)asInteger(o)
            case BigInteger -> (T)asBigInteger(o)
            default -> try {
                type.cast(o)
            } catch (ClassCastException e) {
                o.asType(type)
            }
        }
    }

    static LocalDateTime asLocalDateTime(Object o, DateTimeFormatter dateTimeFormatter = null) {
        if (o == null) return null
        if (o instanceof LocalDate) return o as LocalDateTime
        if (o instanceof LocalDateTime) return o
        if (o instanceof Date) return new Timestamp(o.getTime()).toLocalDateTime()
        if (o instanceof Number) {
            return LocalDateTime.ofEpochSecond(o.toLong(), 0, OffsetDateTime.now().getOffset())
        }
        if (dateTimeFormatter == null)
            return LocalDateTime.parse(String.valueOf(o))
        return LocalDateTime.parse(String.valueOf(o), dateTimeFormatter)
    }

    static Integer asInteger(Object o) {
        if (o == null ) return null
        if (o instanceof Number) return o.intValue()
        try {
            return (o as BigDecimal).intValue()
        } catch (NumberFormatException e) {
            String val = asDecimalNumber(String.valueOf(o))
            if (val.isBlank()) return null
            return Integer.valueOf(val)
        }
    }

    static BigInteger asBigInteger(Object o) {
        if (o == null) return null
        if (o instanceof Number) return o.toBigInteger()
        return new BigInteger(String.valueOf(o))
    }

    /** strips off any non mumeric char from the string. */
    static String asDecimalNumber(String txt, char decimalSeparator = '.') {
        StringBuilder result = new StringBuilder()
        txt.chars().mapToObj(i -> i as char)
                .filter(c -> Character.isDigit(c) || decimalSeparator == c || '-' == c)
                .forEach(c -> result.append(c));
        return result.toString();
    }

    static YearMonth asYearMonth(Object o) {
        if (o == null) return null
        if (o instanceof TemporalAccessor) {
            return YearMonth.from(o as TemporalAccessor)
        }
        if (o instanceof CharSequence) {
            return YearMonth.parse(o as CharSequence)
        }
        if (o instanceof Date) {
            Date date = o as Date
            return YearMonth.of(date.year + 1900, date.month +1)
        }
        throw new IllegalArgumentException("Failed to convert ${o.class}, $o to a YearMonth")
    }

    static YearMonth asYearMonth(Object o, DateTimeFormatter formatter) {
        if (o instanceof String) {
            return YearMonth.from(formatter.parse(o as String))
        }
        return asYearMonth(o)
    }
}
