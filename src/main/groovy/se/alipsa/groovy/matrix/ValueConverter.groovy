package se.alipsa.groovy.matrix

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

    static BigDecimal toBigDecimal(String num, NumberFormat format = null) {
        if (num == null || 'null' == num) return null
        if (format == null) return new BigDecimal(num)
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

    static Double toDouble(Double num) {
        return num
    }


    static Double toDouble(String num, NumberFormat format = null) {
        // Maybe Double.NaN instead of null?
        if (num == null  || 'null' == num) return null
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
        return LocalDate.parse(date, formatter)
    }

    static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate()
    }

    static LocalDate toLocalDate(Object date, DateTimeFormatter formatter) {
        if (formatter == null)
            return LocalDate.parse(String.valueOf(date))
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
            case Double -> (T)toDouble(o, numberFormat)
            case Integer -> (T)toInteger(o)
            case BigInteger -> (T)toBigInteger(o)
            default -> try {
                type.cast(o)
            } catch (ClassCastException e) {
                o.asType(type)
            }
        }
    }

    static LocalDateTime toLocalDateTime(Object o, DateTimeFormatter dateTimeFormatter) {
        if (o == null) return null
        if (o instanceof LocalDate) return o as LocalDateTime
        if (dateTimeFormatter == null)
            return LocalDateTime.parse(String.valueOf(o))
        return LocalDateTime.parse(String.valueOf(o), dateTimeFormatter)
    }

    static Integer toInteger(Object o) {
        if (o == null) return null
        if (o instanceof Number) return o.intValue()
        try {
            return (o as BigDecimal).intValue()
        } catch (NumberFormatException e) {
            return Integer.valueOf(String.valueOf(o))
        }
    }

    static BigInteger toBigInteger(Object o) {
        if (o == null) return null
        if (o instanceof Number) return o.toBigInteger()
        return new BigInteger(String.valueOf(o))
    }
}