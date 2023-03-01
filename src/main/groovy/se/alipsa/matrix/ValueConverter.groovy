package se.alipsa.matrix

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

    static BigDecimal toBigDecimal(Number num) {
        return num as BigDecimal
    }

    static BigDecimal toBigDecimal(Object num) {
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
            return toBigDecimal(num as String)
        }
        return null
    }

    static Double toDouble(Double num) {
        return num
    }

    static Double toDouble(String num) {
        return Double.valueOf(num)
    }

    static Double toDouble(Number num) {
        return num as Double
    }

    static List<LocalDate> toLocalDate(String... dates) {
        def dat = []
        for (d in dates) {
            dat.add(LocalDate.parse(d))
        }
        return dat
    }

    static List<LocalDate> toLocalDate(DateTimeFormatter formatter, String ... dates) {
        def dat = []
        for (d in dates) {
            dat.add(LocalDate.parse(d, formatter))
        }
        return dat
    }
}
