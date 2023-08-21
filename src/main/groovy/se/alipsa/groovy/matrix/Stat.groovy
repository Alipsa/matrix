package se.alipsa.groovy.matrix

import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicInteger

import static ValueConverter.asBigDecimal

/**
 * Basic functions for simple statistical calculations such as sum, mean, median, variance, standard deviation etc.
 * as well as some basic "overview" functions such as structure (str), summary, frequency.
 *
 * For statistical test functions, see the matrix-stat-tests library
 */
class Stat {

    private static final primitives = ['double', 'float', 'int', 'long', 'short', 'byte']
    static final String FREQUENCY_VALUE = "Value"
    static final String FREQUENCY_FREQUENCY = "Frequency"
    static final String FREQUENCY_PERCENT = "Percent"

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
        def mostFrequent = freq.subset(FREQUENCY_FREQUENCY, {it == max(freq[FREQUENCY_FREQUENCY])})

        return [
            'Type': type.getSimpleName(),
            'Number of unique values': freq.rowCount(),
            'Most frequent': "${mostFrequent[0,0]} occurs ${mostFrequent[0,1]} times (${mostFrequent[0,2]}%)".toString()
        ] as Map<String, Object>
    }

    static Number sum(List<?> list) {
        def s = 0 as BigDecimal
        for (value in list) {
            if (value instanceof Number) {
                s += value
            }
        }
        return s
    }


    static <T extends Number> T sum(List<List<T>> grid, Integer colNum) {
        return sum(grid, [colNum])[0]
    }

    static <T extends Number> T sum(Grid grid, Integer colNum) {
        return sum(grid.data, colNum)
    }

    static <T extends Number> T[] sum(Grid grid, List<Integer> colNums) {
        return sum(grid.data, colNums)
    }

    static <T extends Number> T[] sum(List<List<T>> grid, List<Integer> colNums) {
        def s = [0 as T] * colNums.size()
        def value
        int idx
        for (row in grid) {
            idx = 0
            for (colNum in colNums) {
                value = row[colNum]
                if (value instanceof Number) {
                    //s.set(idx, s.get(colNum) + value)
                    s[idx] = s[idx] + value as T
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
    static <T extends Number> T[] quartiles(List<T> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("The list of values are either null or does not contain any data.")
        }

        // Rank order the values
        def v = values.collect() as Number[]
        v.sort()

        int q1 = (int) Math.round((v.size() -1) * 25 / 100)
        int q3 = (int) Math.round((v.size() -1) * 75 / 100)

        return [v[q1], v[q3]] as T[]
    }

    static <T extends Comparable> T min(List<T> list, boolean ignoreNonNumerics = false) {
        def minVal = null
        for (value in list) {
            if (value instanceof Comparable ) {
                boolean skip = false
                if (ignoreNonNumerics && !(value instanceof Number)) {
                    skip = true
                }
                if (!skip && (minVal == null || value < minVal)) {
                    minVal = value
                }
            }
        }
        return minVal
    }

    static <T extends Comparable> T[] min(List<List<T>> matrix, Integer colNum, boolean ignoreNonNumerics = false) {
        return min(matrix, [colNum], ignoreNonNumerics)
    }

    static <T extends Comparable> T[] min(List<List<T>> matrix, List<Integer> colNums, boolean ignoreNonNumerics = false) {
        def value
        def minVal
        def minVals = new ArrayList<T>(colNums.size())
        def idx
        for (row in matrix) {
            idx = 0
            for (colNum in colNums) {
                boolean skip = false
                value = row[colNum]
                if (ignoreNonNumerics && !(value instanceof Number)) {
                    skip = true
                }
                if (value instanceof Number && !skip) {
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

    static <T extends Comparable> T[] min(Matrix table, List<String> colNames, boolean ignoreNonNumerics = false) {
        return min(table.rows(), table.columnIndexes(colNames), ignoreNonNumerics)
    }

    static <T> T max(List<T> list, boolean ignoreNonNumerics = false) {
        def maxVal = null
        for (value in list) {
            boolean skip = false
            if (ignoreNonNumerics && !(value instanceof Number)) {
                skip = true
            }
            if (value instanceof Comparable && !skip) {
                if (maxVal == null || value > maxVal) {
                    maxVal = value
                }
            }
        }
        return maxVal
    }

    static <T extends Comparable> T[] max(List<List<T>> matrix, Integer colNum, boolean ignoreNonNumerics = false) {
        return max(matrix, [colNum], ignoreNonNumerics)
    }

    static <T extends Comparable> T[] max(List<List<T>> matrix, List<Integer> colNums, boolean ignoreNonNumerics = false) {
        def value
        def maxVal
        def maxVals = new ArrayList<T>(colNums.size())
        def idx
        for (row in matrix) {
            idx = 0
            for (colNum in colNums) {
                boolean skip = false
                value = row[colNum]
                if (ignoreNonNumerics && !(value instanceof Number)) {
                    skip = true
                }
                if (value instanceof Comparable && !skip) {
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

    static <T extends Comparable> T[] max(Matrix table, List<String> colNames, boolean ignoreNonNumerics = false) {
        return max(table.rows(), table.columnIndexes(colNames), ignoreNonNumerics)
    }


    static <T extends Number> T[] sd(List<List<T>> matrix, boolean isBiasCorrected = true, Integer colNum) {
        return sd(matrix, isBiasCorrected, [colNum])
    }

    /**
     *
     * @param matrix the matrix containing the column to compute
     * @param colNum the column index for the column
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    static <T extends Number> T[] sd(List<List<T>> matrix, boolean isBiasCorrected = true, List<Integer> colNums) {
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

    static <T extends Number> T[] sd(Matrix table, List<String> columnNames, boolean isBiasCorrected = true) {
        return sd(table.rows(), isBiasCorrected, table.columnIndexes(columnNames))
    }

    static <T extends Number> T variance(List<T> values, boolean isBiasCorrected = true) {
        if (values == null || values.isEmpty()) {
            return null
        }
        def nullFreeNumbers = new ArrayList<T>()
        values.each { if (it != null && it instanceof Number) nullFreeNumbers.add(it) }
        def m = mean(nullFreeNumbers)
        def squaredDeviations = []
        nullFreeNumbers.each {
            squaredDeviations.add((it - m) ** 2)
        }
        def sumOfSquares = sum(squaredDeviations)
        def size = isBiasCorrected ? nullFreeNumbers.size() - 1 : nullFreeNumbers.size()
        return (sumOfSquares / size) as T
    }

    static <T extends Number> T sd(Matrix table, String columnName, boolean isBiasCorrected = true) {
        return sd(table.column(columnName) as List<T>, isBiasCorrected)
    }

    static <T extends Number> T sd(List<T> values, boolean isBiasCorrected = true) {
        if (values == null || values.isEmpty()) {
            return null
        }
        def variance = variance(values, isBiasCorrected)
        return Math.sqrt(variance as double) as T
    }

    static <T extends Number> T sdSample(List<T> population) {
        return sd(population, true)
    }

    static <T extends Number> T sdPopulation(List<T> population) {
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
            [FREQUENCY_VALUE, FREQUENCY_FREQUENCY, FREQUENCY_PERCENT],
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

    /**
     * This is similar to the table function in R:
     * table(mtcars$vs, mtcars$gear)
     * would be expressed here as
     * table(mtcars, "gear", "vs")
     * i.e. yield:
     * <table>
     *   <thead><tr>
     *      <th align="right">vs</th> <th align="right">3</th>	<th align="right">4</th>	<th align="right">5</th>
     *   </tr></thead>
     *   <tbody align="right">
     *     <tr><td align="right">0</td><td align="right">12</td><td align="right">2</td><td align="right">4</td></tr>
     *     <tr><td align="right">1</td><td align="right">3</td><td align="right">10</td><td align="right">1</td></tr>
     *   </tbody>
     * </table>
     *
     * @param table the Matrix to use
     * @param groupName the name to group (split) the matrix on
     * @param columnName the column name of the column to do a frequency count on
     * @return a table with one column for the frequency value and each group value as a column
     * with the frequency counts as rows
     */
    static Matrix frequency(Matrix table, String groupName, String columnName) {
        def groups = table.split(groupName)
        def tbl = new LinkedHashMap<String, List<?>>()
        groups.each {
            def freqTbl = frequency(it.value, columnName)
            tbl[columnName] = freqTbl.column(FREQUENCY_VALUE)
            tbl[String.valueOf(it.key)] = freqTbl.column(FREQUENCY_FREQUENCY)
        }
        def name = (table.getName() == null || table.getName().isBlank()) ? groupName : table.getName() + '_' + groupName
        return Matrix.create(tbl).withName(name)
    }
}
