package se.alipsa.matrix

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ValueConverter {

    static BigDecimal toBigDecimal(BigDecimal num) {
        return num
    }

    static BigDecimal toBigDecimal(String num) {
        return new BigDecimal(num)
    }

    static BigDecimal toBigDecimal(Number num) {
            return num as BigDecimal
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

    static List<LocalDate> asLocalDate(String... dates) {
        def dat = []
        for (d in dates) {
            dat.add(LocalDate.parse(d))
        }
        return dat
    }

    static List<LocalDate> asLocalDate(DateTimeFormatter formatter, String ... dates) {
        def dat = []
        for (d in dates) {
            dat.add(LocalDate.parse(d, formatter))
        }
        return dat
    }
}
