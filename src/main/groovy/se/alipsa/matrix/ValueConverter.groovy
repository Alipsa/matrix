package se.alipsa.matrix

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
}
