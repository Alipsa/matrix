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

    static BigDecimal toBigDecimal(BigDecimal num) {
        return num
    }

    static BigDecimal toBigDecimal(BigInteger num) {
        return num.toBigDecimal()
    }

    static BigDecimal toBigDecimal(String num, NumberFormat format = null) {
        if (num == null || 'null' == num || num.isBlank()) return null
        if (format == null) {
            def n = toDecimalNumber(num)
            if (n.isBlank()) return null
            return new BigDecimal(n)
        }
        return format.parse(num) as BigDecimal
    }

    static BigDecimal toBigDecimal(Number num) {
        return num as BigDecimal
    }

    static BigDecimal toBigDecimal(Object num, NumberFormat format = null) {
        if (num instanceof BigDecimal) {
            return toBigDecimal(num as BigDecimal)
        }
        if (num instanceof BigInteger) {
            return toBigDecimal(num as BigInteger)
        }
        if (num instanceof Number) {
            return toBigDecimal(num as Number)
        }
        if (num instanceof String) {
            if (format == null) {
                return toBigDecimal(num as String)
            } else {
                return toBigDecimal(num as String, format)
            }
        }
        return toBigDecimal(String.valueOf(num), format)
    }

    static Boolean toBoolean(Object obj) {
        if (obj == null) return null
        if (obj instanceof Boolean) return obj as Boolean
        if (String.valueOf(obj).toLowerCase() in ["1", "true"])
            return true
        if (String.valueOf(obj).toLowerCase() in ["0", "false"])
            return false
        throw new ConversionException("Failed to convert $obj (${obj == null ? null : obj.getClass()} to Boolean")
    }

    static Double toDouble(Double num) {
        return num
    }


    static Double toDouble(String num, NumberFormat format = null) {
        // Maybe Double.NaN instead of null?
        if (num == null  || 'null' == num || num.isBlank()) return null
        if (format == null) return Double.valueOf(num)
        return format.parse(num) as Double
    }

    static Double toDouble(Number num) {
        return num as Double
    }

    static Double toDouble(Object obj, NumberFormat format = null) {
        if (obj == null ) return null
        if (obj instanceof Double) return toDouble(obj as Double)
        if (obj instanceof BigDecimal) return toDouble(obj as BigDecimal)
        if (obj instanceof Number) return toDouble(obj as Number)
        if (obj instanceof String) return toDouble(obj as String, format)
        return toDouble(String.valueOf(obj), format)
    }

    static LocalDate toLocalDate(String date) {
        return LocalDate.parse(date)
    }

    static LocalDate toLocalDate(String date, DateTimeFormatter formatter) {
        if (formatter == null) return toLocalDate(date)
        return date == null ? null : LocalDate.parse(date, formatter)
    }

    static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate()
    }

    static LocalDate toLocalDate(Object date, DateTimeFormatter formatter = null) {
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
            case LocalDate -> (T)toLocalDate(o, dateTimeFormatter)
            case LocalDateTime -> (T)toLocalDateTime(o, dateTimeFormatter)
            case BigDecimal -> (T)toBigDecimal(o, numberFormat)
            case Double, double -> (T)toDouble(o, numberFormat)
            case Integer, int -> (T)toInteger(o)
            case BigInteger -> (T)toBigInteger(o)
            default -> try {
                type.cast(o)
            } catch (ClassCastException e) {
                o.asType(type)
            }
        }
    }

    static LocalDateTime toLocalDateTime(Object o, DateTimeFormatter dateTimeFormatter = null) {
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

    static Integer toInteger(Object o) {
        if (o == null ) return null
        if (o instanceof Number) return o.intValue()
        try {
            return (o as BigDecimal).intValue()
        } catch (NumberFormatException e) {
            String val = toDecimalNumber(String.valueOf(o))
            if (val.isBlank()) return null
            return Integer.valueOf(val)
        }
    }

    static BigInteger toBigInteger(Object o) {
        if (o == null) return null
        if (o instanceof Number) return o.toBigInteger()
        return new BigInteger(String.valueOf(o))
    }

    /** strips off any non mumeric char from the string. */
    static String toDecimalNumber(String txt, char decimalSeparator = '.') {
        StringBuilder result = new StringBuilder()
        txt.chars().mapToObj(i -> i as char)
                .filter(c -> Character.isDigit(c) || decimalSeparator == c || '-' == c)
                .forEach(c -> result.append(c));
        return result.toString();
    }

    static YearMonth toYearMonth(Object o) {
        if (o == null) return null;
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

    static YearMonth toYearMonth(Object o, DateTimeFormatter formatter) {
        if (o instanceof String) {
            return YearMonth.from(formatter.parse(o as String))
        }
        return toYearMonth(o)
    }
}
