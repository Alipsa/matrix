package se.alipsa.matrix

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ValueConverter {

    static BigDecimal toBigDecimal(BigDecimal num) {
        return num
    }

    static BigDecimal toBigDecimal(BigInteger num) {
        return num.toBigDecimal()
    }

    static BigDecimal toBigDecimal(String num) {
        return new BigDecimal(num)
    }

    static BigDecimal toBigDecimal(String num, NumberFormat format) {
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
        return null
    }

    static Double toDouble(Double num) {
        return num
    }

    static Double toDouble(String num) {
        return Double.valueOf(num)
    }

    static Double toDouble(String num, NumberFormat format) {
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
        if (obj instanceof String) {
            if (format == null) {
                return toDouble(obj as String)
            } else {
                return toDouble(obj as String, format)
            }
        }
        return null
    }

    static LocalDate toLocalDate(String date) {
        return LocalDate.parse(date)
    }

    static LocalDate toLocalDate(String date, DateTimeFormatter formatter) {
        return LocalDate.parse(date, formatter)
    }

    static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate()
    }

    static LocalDate toLocalDate(Object date) {
        return LocalDate.parse(String.valueOf(date))
    }

    static <T> T convert(Object o, Class<T> type) {
        return switch (type) {
            case String -> (T)String.valueOf(o)
            case LocalDate -> (T)toLocalDate(o)
            case LocalDateTime -> (T)toLocalDateTime(o)
            case BigDecimal -> (T)toBigDecimal(o)
            case Double -> (T)toDouble(o)
            case Integer -> (T)toInteger(o)
            case BigInteger -> (T)toBigInteger(o)
            default -> try {
                type.cast(o)
            } catch (ClassCastException e) {
                o.asType(type)
            }
        }
    }

    static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null
        if (o instanceof LocalDate) return o as LocalDateTime
        return LocalDateTime.parse(String.valueOf(o))
    }

    static Integer toInteger(Object o) {
        if (o == null) return null
        if (o instanceof Number) return o.intValue()
        return Integer.valueOf(String.valueOf(o))
    }

    static BigInteger toBigInteger(Object o) {
        if (o == null) return null
        if (o instanceof Number) return o.toBigInteger()
        return new BigInteger(String.valueOf(o))
    }
}
