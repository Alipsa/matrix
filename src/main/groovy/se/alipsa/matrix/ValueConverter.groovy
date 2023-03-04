package se.alipsa.matrix

import java.text.NumberFormat
import java.time.LocalDate
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

    static List<LocalDate> toLocalDate(String... dates) {
        def dat = new ArrayList<LocalDate>(dates.length)
        for (d in dates) {
            dat.add(LocalDate.parse(d))
        }
        return dat
    }

    static List<LocalDate> toLocalDate(DateTimeFormatter formatter, String... dates) {
        def dat = new ArrayList<LocalDate>(dates.length)
        for (d in dates) {
            dat.add(LocalDate.parse(d, formatter))
        }
        return dat
    }
}
