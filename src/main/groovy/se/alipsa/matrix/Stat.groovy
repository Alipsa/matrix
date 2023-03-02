package se.alipsa.matrix

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation

import static se.alipsa.matrix.ValueConverter.toBigDecimal

class Stat {

    static BigDecimal[] sum(List<List<?>> matrix, int... colNums) {
        def s = [0.0g] * colNums.length
        def value
        def idx
        for (row in matrix) {
            idx = 0
            for (colNum in colNums) {
                value = row[colNum]
                if (value instanceof Number) {
                    s.set(idx, s.get(colNum) + value)
                }
                idx++
            }
        }
        return s
    }

    static BigDecimal[] mean(List<List<?>> matrix, int... colNums) {
        def sums = [0.0g] * colNums.length
        def ncols = [0.0g] * colNums.length
        def value
        def idx
        for (row in matrix) {
            idx = 0
            for (colNum in colNums) {
                value = row[colNum]
                if (value != null && value instanceof Number) {
                    sums[idx] = sums[idx] + value
                    ncols[idx] = ncols[idx] + 1
                }
                idx++
            }
        }
        def means = new ArrayList<BigDecimal>(colNums.length)
        for (int i = 0; i < colNums.length; i++) {
            means[i] = sums[i] / ncols[i]
        }
        return means
    }

    static BigDecimal[] median(List<List<?>> matrix, int... colNums) {
        Map<String, List<? extends Number>> valueList = [:].withDefault{key -> return []}
        def value
        for (row in matrix) {
            for (colNum in colNums) {
                value = row[colNum]
                if (value != null && value instanceof Number) {
                    valueList[String.valueOf(colNum)].add(value)
                }
            }
        }
        def medians = new ArrayList<BigDecimal>(colNums.length)
        def m
        def list
        //println "Valuelist = ${valueList}"
        for (colNum in colNums) {
            //list = valueList.get(String.valueOf(colNum)).sort()
            list = valueList[String.valueOf(colNum)].sort()
            m = median(list)
            //println "${list} has median ${m}"
            medians.add(m)
        }

        return medians
    }

    static BigDecimal median(List<? extends Number> valueList) {
        if (valueList == null || valueList.size() == 0) {
            return null
        }
        if (valueList.size() == 1) {
            return valueList[0]
        }
        if (valueList.size() % 2 == 0) {
            def index = valueList.size()/2 as int
            def val1 = valueList[index -1] as Number
            def val2 = valueList[index] as Number
            def median = (val1 + val2) / 2
            //println("Returning $val1 plus $val2 / 2 = $median")
            return median
        } else {
            return toBigDecimal(valueList[valueList.size()/2 as int])
        }
    }

    static Number[] min(List<List<?>> matrix, int... colNums) {
        def value
        def minVal
        def minVals = new ArrayList<Number>(colNums.length)
        def idx
        for (row in matrix) {
            idx = 0
            for (colNum in colNums) {
                value = row[colNum]
                if (value instanceof Number) {
                    minVal = minVals[idx]
                    if (minVal == null || value < minVal) {
                        minVals[idx] = value
                    }
                }
                idx++
            }
        }
        return minVals
    }

    static Number[] max(List<List<?>> matrix, int... colNums) {
        def value
        def maxVal
        def maxVals = new ArrayList<Number>(colNums.length)
        def idx
        for (row in matrix) {
            idx = 0
            for (colNum in colNums) {
                value = row[colNum]
                if (value instanceof Number) {
                    maxVal = maxVals[idx]
                    if (maxVal == null || value > maxVal) {
                        maxVals[idx] = value
                    }
                }
                idx++
            }
        }
        return maxVals
    }

    /**
     *
     * @param matrix the matrix containing the column to compute
     * @param colNum the column index for the column
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static BigDecimal[] sd(List<List<?>> matrix, boolean isBiasCorrected = true, int... colNums) {
        def value
        def numberMap = [:].withDefault{key -> return []}
        for (row in matrix) {
            for (colNum in colNums) {
                value = row[colNum]
                if (value instanceof Number) {
                    numberMap[String.valueOf(colNum)].add(value.doubleValue())
                }
            }
        }
        def list
        def std
        def stds = []
        for (colNum in colNums) {
            list = numberMap[String.valueOf(colNum)]
            std = sd(list as double[], isBiasCorrected)
            stds.add(std)
        }
        return stds
    }

    /**
     *
     * @param column the List containing the values to compute
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static BigDecimal sd(List<?> column, boolean isBiasCorrected = true) {
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
    static BigDecimal sd(double[] numbers, boolean isBiasCorrected = true) {
        return new StandardDeviation(isBiasCorrected).evaluate(numbers)
    }
}
