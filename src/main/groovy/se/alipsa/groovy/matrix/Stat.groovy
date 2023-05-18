package se.alipsa.groovy.matrix

import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicInteger

import static ValueConverter.asBigDecimal

class Stat {

    private static final primitives = ['double', 'float', 'int', 'long', 'short', 'byte']

    static Structure str(Matrix table) {
        Structure map = new Structure()
        def name = table.name == null ? '' : table.name + ', '
        map["Matrix"] = ["${name}${table.rowCount()} observations of ${table.columnCount()} variables".toString()]
        for (colName in table.columnNames()) {
            def vals = [table.columnType(colName).getSimpleName()]
            def endRow = Math.min(4, table.rowCount()-1)
            def samples = ListConverter.convert(table.column(colName).subList(0, endRow), String.class)
            vals.addAll(samples)
            map[colName] = vals
        }
        return map
    }

    static Summary summary(Matrix table) {
        def map = new Summary()
        for (colName in table.columnNames()) {
            def column = table.column(colName)
            def type = table.columnType(colName)
            if (Number.isAssignableFrom(type) || primitives.contains(type.getTypeName())) {
                map[colName] = addNumericSummary(column, type)
            } else {
                map[colName] = addCategorySummary(column, type)
            }
        }
        return map
    }

    static Map<String, Object> addNumericSummary(List<Object> objects, Class<?> type) {
        def numbers = objects as List<Number>
        def quarts = quartiles(numbers)
        [
            'Type': type.getSimpleName(),
            'Min': min(numbers),
            '1st Q': quarts[0],
            'Median': median(numbers),
            'Mean': mean(numbers),
            '3rd Q': quarts[1],
            'Max': max(numbers)
        ]
    }

    static Map<String, Object> addCategorySummary(List<Object> objects, Class<?> type) {
        def freq = frequency(objects)
        def mostFrequent = freq.subset('Frequency', {it == max(freq['Frequency'])})

        return [
            'Type': type.getSimpleName(),
            'Number of unique values': freq.rowCount(),
            'Most frequent': "${mostFrequent[0,0]} occurs ${mostFrequent[0,1]} times (${mostFrequent[0,2]}%)".toString()
        ] as Map<String, Object>
    }

    static BigDecimal sum(List<?> list) {
        BigDecimal s = 0g
        for (value in list) {
            if (value instanceof Number) {
                s += value
            }
        }
        return s
    }


    static BigDecimal sum(List<List<?>> grid, Integer colNum) {
        return sum(grid, [colNum])[0]
    }

    static BigDecimal sum(Grid grid, Integer colNum) {
        return sum(grid.data, colNum)
    }

    static BigDecimal[] sum(Grid grid, List<Integer> colNums) {
        return sum(grid.data, colNums)
    }

    static BigDecimal[] sum(List<List<?>> grid, List<Integer> colNums) {
        def s = [0.0g] * colNums.size()
        def value
        int idx
        for (row in grid) {
            idx = 0
            for (colNum in colNums) {
                value = row[colNum]
                if (value instanceof Number) {
                    //s.set(idx, s.get(colNum) + value)
                    s[idx] = s[idx] + value
                }
                idx++
            }
        }
        return s
    }

    static Matrix countBy(Matrix table, String groupBy) {
        Map<?, Matrix> groups = table.split(groupBy)
        List<List<?>> counts = []
        groups.each {
            counts.add([it.key, it.value.rowCount()])
        }
        return Matrix.create(
                "${table.name} - counts by $groupBy".toString(),
                [groupBy, "${groupBy}_count".toString()],
                counts,
                [table.columnType(groupBy),
                 Integer]
        )
    }

    static Matrix sumBy(Matrix table, String sumColumn, String groupBy) {
        Matrix sums = funBy(table, sumColumn, groupBy, Stat.&sum, BigDecimal)
        sums.setName("${table.name}-sums by $groupBy".toString())
        return sums
    }

    static Matrix meanBy(Matrix table, String meanColumn, String groupBy) {
        Matrix means = funBy(table, meanColumn, groupBy, Stat.&mean, BigDecimal)
        means.setName("${table.name}-means by $groupBy".toString())
        return means
    }

    static Matrix medianBy(Matrix table, String medianColumn, String groupBy) {
        def sorted = table.orderBy(medianColumn)
        Matrix means = funBy(sorted, medianColumn, groupBy, Stat.&median, BigDecimal)
        means.setName("${table.name}-medians by $groupBy".toString())
        return means
    }

    /**
     * Splits the table into separate tables, one for each value in groupBy column,
     * then apply the closure to the columnName column.
     *
     * Here is an example of sumBy using funBy:
     * Matrix sums = funBy(table, sumColumn, groupBy, Stat.&sum, BigDecimal)
     *
     * @param table the Matrix to operate on
     * @param columnName the name of the column containing the values
     * @param groupBy the name of the column to split on
     * @param fun the closure to apply to the column values (List<?>)
     * @param columnType the type of list (class) that the closure function returns (e.g. if the closure returns a
     * List<Double> then the columnType should be Double
     * @return
     */
    static Matrix funBy(Matrix table, String columnName, String groupBy, Closure fun, Class<?> columnType) {
        Map<?, Matrix> groups = table.split(groupBy)
        List<List<?>> calculations = []
        groups.each {
            def val = fun(it.value[columnName])
            calculations.add([ it.key,  val])
        }
        return Matrix.create(
                "${table.name} - by $groupBy".toString(),
                [groupBy, columnName],
                calculations,
                [table.columnType(groupBy), columnType]
        )
    }

    static BigDecimal[] mean(List<List<?>> rows, Integer colNum) {
        return mean(rows, [colNum])
    }

    static BigDecimal[] mean(List<List<?>> matrix, List<Integer> colNums) {
        def sums = [0.0g] * colNums.size()
        def ncols = [0.0g] * colNums.size()
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
        def means = new ArrayList<BigDecimal>(colNums.size())
        for (int i = 0; i < colNums.size(); i++) {
            means[i] = sums[i] / ncols[i]
        }
        return means
    }

    static BigDecimal[] mean(Matrix table, List<String> colNames) {
        return mean(table.rows(), table.columnIndexes(colNames))
    }

    static BigDecimal mean(List<?> list) {
        def sum = 0 as BigDecimal
        def nVals = 0
        for (value in list) {
            if (value != null && value instanceof Number) {
                sum += value
                nVals++
            }
        }
        return sum / nVals
    }

    static BigDecimal mean(Matrix table, String colName) {
        return mean(table.column(colName))
    }

    static BigDecimal[] median(List<List<?>> matrix, Integer colNum) {
        return median(matrix, [colNum])
    }

    static BigDecimal[] median(Matrix table, String colName) {
        return median(table.column(colName) as List<List<?>>, [table.columnNames().indexOf(colName)])
    }

    static BigDecimal[] median(List<List<?>> matrix, List<Integer> colNums) {
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
        def medians = new ArrayList<BigDecimal>(colNums.size())
        def m
        def list
        for (colNum in colNums) {
            list = valueList[String.valueOf(colNum)].sort()
            m = median(list)
            medians.add(m)
        }

        return medians
    }

    static BigDecimal[] median(Matrix table, List<String> colNames) {
        return median(table.rows(), table.columnIndexes(colNames))
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
            return asBigDecimal(valueList[valueList.size()/2 as int])
        }
    }

    /**
     * The quartiles of a ranked set of data values are three points which divide the data into exactly four equal parts,
     * each part comprising of quarter data. As Q2 is the median only Q1 and Q3 is returned here
     * @param values a list of numbers to use
     * @return a list of the 1:st and 3:rd quartile
     */
    static Number[] quartiles(List<? extends Number> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("The list of values are either null or does not contain any data.")
        }

        // Rank order the values
        def v = values.collect() as Number[]
        v.sort()

        int q1 = (int) Math.round((v.size() -1) * 25 / 100)
        int q3 = (int) Math.round((v.size() -1) * 75 / 100)

        return [v[q1], v[q3]]
    }

    static Number min(List<?> list) {
        def minVal = null
        for (value in list) {
            if (value instanceof Number ) {
                if (minVal == null || value < minVal) {
                    minVal = value
                }
            }
        }
        return minVal
    }

    static Number[] min(List<List<?>> matrix, Integer colNum) {
        return min(matrix, [colNum])
    }

    static Number[] min(List<List<?>> matrix, List<Integer> colNums) {
        def value
        def minVal
        def minVals = new ArrayList<Number>(colNums.size())
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

    static Number[] min(Matrix table, List<String> colNames) {
        return min(table.rows(), table.columnIndexes(colNames))
    }

    static Number max(List<?> list) {
        def maxVal = null
        for (value in list) {
            if (value instanceof Number ) {
                if (maxVal == null || value > maxVal) {
                    maxVal = value
                }
            }
        }
        return maxVal
    }

    static Number[] max(List<List<?>> matrix, Integer colNum) {
        return max(matrix, [colNum])
    }

    static Number[] max(List<List<?>> matrix, List<Integer> colNums) {
        def value
        def maxVal
        def maxVals = new ArrayList<Number>(colNums.size())
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

    static Number[] max(Matrix table, List<String> colNames) {
        return max(table.rows(), table.columnIndexes(colNames))
    }


    static BigDecimal[] sd(List<List<?>> matrix, boolean isBiasCorrected = true, Integer colNum) {
        return sd(matrix, isBiasCorrected, [colNum])
    }

    /**
     *
     * @param matrix the matrix containing the column to compute
     * @param colNum the column index for the column
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static BigDecimal[] sd(List<List<?>> matrix, boolean isBiasCorrected = true, List<Integer> colNums) {
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
            std = sd(list, isBiasCorrected)
            stds.add(std)
        }
        return stds
    }

    static BigDecimal[] sd(Matrix table, List<String> columnNames, boolean isBiasCorrected = true) {
        return sd(table.rows(), isBiasCorrected, table.columnIndexes(columnNames))
    }

    static BigDecimal variance(List<?> values, boolean isBiasCorrected = true) {
        if (values == null || values.isEmpty()) {
            return null
        }
        def nullFreeNumbers = new ArrayList<? extends Number>()
        values.each { if (it != null && it instanceof Number) nullFreeNumbers.add(it) }
        def m = mean(nullFreeNumbers)
        def squaredDeviations = []
        nullFreeNumbers.each {
            squaredDeviations.add((it - m) ** 2)
        }
        def sumOfSquares = sum(squaredDeviations)
        def size = isBiasCorrected ? nullFreeNumbers.size() - 1 : nullFreeNumbers.size()
        return sumOfSquares / size
    }

    static BigDecimal sd(Matrix table, String columnName, boolean isBiasCorrected = true) {
        return sd(table.column(columnName), isBiasCorrected)
    }

    static BigDecimal sd(List<?> values, boolean isBiasCorrected = true) {
        if (values == null || values.isEmpty()) {
            return null
        }
        def variance = variance(values, isBiasCorrected)
        return Math.sqrt(variance) as BigDecimal
    }

    static BigDecimal sdSample(List<?> population) {
        return sd(population, true)
    }

    static BigDecimal sdPopulation(List<?> population) {
        return sd(population, false)
    }

    static Matrix frequency(List<?> column) {
        Map<Object, AtomicInteger> freq = new HashMap<>()
        column.forEach(v -> {
            freq.computeIfAbsent(v, k -> new AtomicInteger(0)).incrementAndGet()
        })
        int size = column.size()
        List<List<?>> matrix = []
        def percent
        for (Map.Entry<Object, AtomicInteger> entry : freq.entrySet()) {
            int numOccurrence = entry.getValue().intValue()
            percent = (numOccurrence * 100.0 / size).setScale(2, RoundingMode.HALF_EVEN)
            matrix.add([String.valueOf(entry.getKey()), numOccurrence, percent])
        }
        return Matrix.create(
            ["Value", "Frequency", "Percent"],
            matrix,
            [String, int, BigDecimal]
        )
    }

    static Matrix frequency(Matrix table, String columnName) {
        return frequency(table.column(columnName))
    }

    static Matrix frequency(Matrix table, int columnIndex) {
        return frequency(table.column(columnIndex))
    }
}
