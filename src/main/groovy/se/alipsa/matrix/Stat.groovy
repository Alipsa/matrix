package se.alipsa.matrix

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

import static se.alipsa.matrix.ValueConverter.toBigDecimal

class Stat {
    static BigDecimal sum(List<List<?>> matrix, int colNum) {
        def s = 0g
        def value
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                s = s + value
            }
        }
        return s
    }

    static BigDecimal mean(List<List<?>> matrix, int colNum) {
        def s = 0g
        def value
        def count = 0
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                count++
                s = s + value
            }
        }
        return s / count
    }

    static BigDecimal median(List<List<?>> matrix, int colNum) {
        def valueList = []
        def value
        def count = 0
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                valueList.add(value)
            }
        }
        if (valueList.size() % 2 > 0) {
            def index = valueList.size()/2 as int
            def val1 = valueList.get(index)
            def val2 = valueList.get(index +1)
            return (val1 + val2) / 2
        } else {
            return toBigDecimal(valueList.get(valueList.size()/2 as int))
        }
    }

    static BigDecimal min(List<List<?>> matrix, int colNum) {
        def value
        def minVal = null
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                if (minVal == null || minVal < value) {
                    minVal = value
                }
            }
        }
        return minVal
    }

    static BigDecimal max(List<List<?>> matrix, int colNum) {
        def value
        def maxVal = null
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                if (maxVal == null || maxVal > value) {
                    maxVal = value
                }
            }
        }
        return maxVal
    }

    /**
     *
     * @param matrix the matrix containing the column to compute
     * @param colNum the column index for the column
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static BigDecimal sd(List<List<?>> matrix, int colNum, boolean... isBiasCorrected) {
        def s = 0g
        def value
        def numbers = []
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                numbers.add(value.doubleValue())
            }
        }
        return sd(numbers as double[], isBiasCorrected)
    }

    /**
     *
     * @param column the List containing the values to compute
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static BigDecimal sd(List<?> column, boolean... isBiasCorrected) {
        def s = 0g
        def numbers = []
        for (value in column) {
            if (value instanceof Number) {
                numbers.add(value.doubleValue())
            }
        }
        return sd(numbers as double[], isBiasCorrected)
    }

    /**
     * @param numbers the array containing the values to compute
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static BigDecimal sd(double[] numbers, boolean... isBiasCorrected) {
        if (isBiasCorrected.length > 0) {
            return new StandardDeviation(isBiasCorrected[0]).evaluate(numbers as double[])
        }
        return new StandardDeviation().evaluate(numbers)
    }
}
