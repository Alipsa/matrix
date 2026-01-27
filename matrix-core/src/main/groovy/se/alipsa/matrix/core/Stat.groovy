package se.alipsa.matrix.core

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.Logger
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicInteger

import static ValueConverter.asBigDecimal

/**
 * Basic functions for simple statistical calculations such as sum, mean, median, variance, standard deviation etc.
 * as well as some basic "overview" functions such as structure (str), summary, frequency.
 *
 * For statistical test functions, see the matrix-stat-tests library
 *
 */
@CompileStatic
class Stat {

    private static final Logger log = Logger.getLogger(Stat)
    private static final List<String> primitives = ['double', 'float', 'int', 'long', 'short', 'byte']
    static final String FREQUENCY_VALUE = "Value"
    static final String FREQUENCY_FREQUENCY = "Frequency"
    static final String FREQUENCY_PERCENT = "Percent"

    @CompileDynamic
    static Structure str(Matrix table) {
        Structure map = new Structure()
        def name = table.matrixName == null ? '' : table.matrixName + ', '
        map["Matrix"] = ["${name}${table.rowCount()} observations of ${table.columnCount()} variables".toString()]
        for (colName in table.columnNames()) {
            Class type = table.type(colName)
            if (type == null) {
                type = scanForType(table[colName])
            }
            def vals = [type?.getSimpleName()]
            def endRowIndex = Math.min(3, table.lastRowIndex())
            if (endRowIndex > 0) {
                def samples = ListConverter.convert(table[colName][0..endRowIndex], String.class)
                vals.addAll(samples)
            }
            map[colName] = vals
        }
        return map
    }

    static Summary summary(Matrix table) {
        def map = new Summary()
        for (colName in table.columnNames()) {
            def column = table.column(colName)
            def type = table.type(colName)
            if (Number.isAssignableFrom(type) || primitives.contains(type.getTypeName())) {
                def summary = addNumericSummary(column, type)
                if (summary != null) {
                    map[colName] = summary
                }
            } else {
                map[colName] = addCategorySummary(column, type)
            }
        }
        return map
    }

    static Summary summary(Column column) {
        def sum = new Summary()
        def colName = column.name
        def type = column.type
        if (Number.isAssignableFrom(type) || primitives.contains(type.getTypeName())) {
            def summary = addNumericSummary(column, type)
            if (summary != null) {
                sum[colName] = summary
            }
        } else {
            sum[colName] = addCategorySummary(column, type)
        }
        sum
    }

    static Map<String, Object> addNumericSummary(List<Object> objects, Class<?> type) {
        if (objects == null) {
            log.error("The list of objects for addNumericSummary is null")
            return null
        }
        List<Number> numbers = objects as List<Number>
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

    static <T> T sum(List<?> list, Class<T> type) {
        BigDecimal s = 0.0g
        for (value in list) {
            if (value instanceof Number) {
                s += (value as BigDecimal)
            }
        }
        return s.asType(type)
    }

    @CompileDynamic
    static List<Number> sum(Matrix matrix, String... columnNames) {
        List<Number> sums = []
        List<String> columns
        if (columnNames.length == 0) {
            columns = matrix.columnNames()
        } else {
            columns = columnNames as List<String>
        }
        for (columnName in columns) {
            Column column = matrix.column(columnName)
            Class type = matrix.type(columnName)
            if (Number.isAssignableFrom(type)) {
                sums.add(sum(column, type as Class<Object>) as Number)
            } else {
                println("${matrix.matrixName}: $columnName is not a Numeric column but of type ${type.simpleName}")
                sums.add(null)
            }
        }
        return sums
    }

    @CompileDynamic
    static List<Number> sum(Matrix matrix, IntRange columnIndices) {
        return sum(matrix, columnIndices as List)
    }

    @CompileDynamic
    static List<Number> sum(Matrix matrix, List<Integer> columnIndices) {
        List<? extends Number> sums = []
        columnIndices.each { colIdx ->
            def column = matrix.column(colIdx)
            def type = matrix.type(colIdx)
            if (Number.isAssignableFrom(type)) {
                sums.add(sum(column, type as Class<Object>) as Number)
            } else {
                sums << null
            }

        }
        return sums
    }


    @CompileDynamic
    static <T extends Number> T sum(List<List<T>> grid, Integer colNum) {
        return sum(grid, [colNum])[0]
    }

    @CompileDynamic
    static <T extends Number> T sum(Grid grid, Integer colNum) {
        return sum(grid.data, colNum)
    }

    @CompileDynamic
    static <T extends Number> List<T> sum(Grid grid, List<Integer> colNums) {
        return sum(grid.data, colNums)
    }

    @CompileDynamic
    static <T extends Number> List<T> sum(List<List<T>> grid, List<Integer> colNums) {
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

    /**
     * summarises each row and return a list of the result.
     *
     * @param m the matrix to summarise
     * @param colNums either a List of numbers or an IntRange
     * @return a list (a column) with the sum of the rows for the columns specified
     */
    static <T extends Number> List<T> sumRows(Matrix m, List<Integer> colNums) {
        List<T> means = []
        m.each { Row row ->
            means << (sum(row[colNums]) as T)
        }
        means
    }

    @CompileDynamic
    static <T extends Number> List<T> sumRows(Matrix m, String... colNames) {
        List<T> means = []
        if (colNames.length == 0) {
            colNames = m.columnNames()
        }
        m.each { row ->
            means << (sum(row[colNames]) as T)
        }
        means
    }

    /**
     * Enable expressions such as <code>table.addColumn('total', sumRows(table[1..23]))</code>
     * @param columns a list of columns to summarize by row
     * @return a list of numbers with sums for each row
     */
    static <T extends Number> List<T> sumRows(List<List<?>> columns) {
        def sums = []
        columns.transpose().each {
            sums << sum(it as List<?>)
        }
        sums
    }

    static Matrix countBy(Matrix table, String groupBy) {
        Map<?, Matrix> groups = table.split(groupBy)
        List<List<?>> counts = []
        groups.each {
            counts.add([it.key, it.value.rowCount()])
        }
        Matrix.builder()
            .matrixName("${table.matrixName} - counts by $groupBy".toString())
            .columnNames([groupBy, "${groupBy}_count".toString()])
            .rows(counts)
            .types([table.type(groupBy), Integer])
            .build()
    }

    static Matrix sumBy(Matrix table, String sumColumn, String groupBy) {
        Matrix sums = funBy(table, sumColumn, groupBy, Stat.&sum, BigDecimal)
        sums.setMatrixName("${table.matrixName}-sums by $groupBy".toString())
        return sums
    }

    @CompileDynamic
    static Matrix meanBy(Matrix table, String meanColumn, String groupBy, int scale = 9) {
        Matrix means = funBy(table, meanColumn, groupBy, Stat.&mean, BigDecimal)
        means = means.apply(meanColumn) {
            it.setScale(scale, RoundingMode.HALF_UP)
        }
        means.setMatrixName("${table.matrixName}-means by $groupBy".toString())
        return means
    }

    static Matrix medianBy(Matrix table, String medianColumn, String groupBy) {
        def sorted = table.clone().orderBy(medianColumn)
        Matrix medians = funBy(sorted, medianColumn, groupBy, Stat.&median, BigDecimal)
        medians.setMatrixName("${table.matrixName ?: ''}-medians by $groupBy".toString())
        return medians
    }

    /**
     * Splits the table into separate tables, one for each value in the named columns.
     * The key values are a compound of the grouped values in the order specified in columnNames
     * separated by underscore.
     * Example usage:
     * <pre><code>
     * def ab = Matrix.builder().data(
     *   a: [1, 1, 2, 2, 2, 2, 2],
     *   b: ['A', 'B', 'A', 'B', 'B', 'A', 'B']
     *   )
     *   .types(int, String)
     *   .build()
     * def groups = Stat.groupBy(ab, 'a', 'b')
     * assert 1 == groups['1_A'].size()
     * assert 1 == groups['1_B'].size()
     * assert 2 == groups['2_A'].size()
     * assert 3 == groups['2_B'].size()
     * </code></pre>
     *
     * @param table the Matrix to operate on
     * @param columnName the name(s) of the column(s) containing the values
     * @return a Map<String, Matrix> where the key is a compound of the grouped values separated by underscore.
     */
    @CompileDynamic
    static Map<String, Matrix> groupBy(Matrix table, String... columnNames) {
        if (columnNames.length == 0) {
            throw new IllegalArgumentException("At least one column name must be specified to groupBy")
        }

        Map<String, Matrix> result = [:]

        def recursiveGroup
        recursiveGroup = { Matrix currentTable, int index, String prefix ->
            Map<?, Matrix> splitMap = currentTable.split(columnNames[index])
            splitMap.each { entry ->
                String key = prefix ? "${prefix}_${entry.key}" : "${entry.key}"
                if (index == columnNames.length - 1) {
                    result[key] = entry.value
                } else {
                    recursiveGroup(entry.value, index + 1, key)
                }
            }
        }

        recursiveGroup(table, 0, "")
        return result
    }

    static Map<String, Matrix> groupBy(Matrix table, List<String> columnNames) {
        groupBy(table, columnNames as String[])
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
        Matrix.builder()
            .matrixName("${table.matrixName} - by $groupBy".toString())
            .columnNames([groupBy, columnName])
            .rows(calculations)
            .types([table.type(groupBy), columnType])
            .build()
    }

    static List<BigDecimal> means(Grid grid, int scale = 9) {
        return means(grid.getRowList(), scale)
    }

    static List<BigDecimal> means(List<List<?>> rowList, int scale = 9) {
        List<List<?>> columns = Grid.transpose(rowList)
        List<BigDecimal> results = []
        columns.each {
            results << mean(it, scale)
        }
        return results
    }

    static List<BigDecimal> means(List<List<?>> matrix, IntRange colNums) {
        means(matrix, colNums as List<Integer>)
    }

    static List<BigDecimal> means(List<List<?>> matrix, List<Integer> colNums) {
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

    static List<BigDecimal> means(Matrix table, List<String> colNames) {
        // Optimized: use columnar access instead of row iteration
        List<BigDecimal> results = []
        colNames.each { colName ->
            List<?> columnData = table.column(colName)
            def sum = 0.0g
            def count = 0.0g
            columnData.each { value ->
                if (value != null && value instanceof Number) {
                    sum += value
                    count += 1
                }
            }
            // Handle division by zero for empty columns or columns with no numeric values
            results << (count == 0 ? null : sum / count)
        }
        return results
    }

    /**
     * calculates the mean for each row and return a list of the result.
     *
     * @param m the matrix to use
     * @param colNums either a List of numbers or an IntRange to include
     * @return a list (a column) with the mean of the rows for the columns specified
     */
    @CompileDynamic
    static List<BigDecimal> meanRows(Matrix table, List<Integer> columns) {
        List<BigDecimal> means = []
        table.each { row ->
            means << mean(row[columns])
        }
        means
    }

    @CompileDynamic
    static List<BigDecimal> meanRows(Matrix m, String... colNames) {
        List<BigDecimal> means = []
        if (colNames.length == 0) {
            colNames = m.columnNames()
        }
        m.each { row ->
            means << mean(row[colNames])
        }
        means
    }

    /**
     * Enable expressions such as <code>table.addColumn('means', meanRows(table[1..23]))</code>
     * @param columns a list of columns to summarize by row
     * @return a list of numbers with sums for each row
     */
    static <T extends Number> List<T> meanRows(List<List<?>> columns) {
        def means = []
        columns.transpose().each {
            means << mean(it as List<?>)
        }
        means
    }

    static BigDecimal mean(List<?> list, int scale = 9) {
        if (list == null || list.isEmpty()) {
            return null
        }
        BigDecimal sum = BigDecimal.ZERO //.setScale(scale, RoundingMode.HALF_UP)
        int nVals = 0
        for (value in list) {
            if (value != null && value instanceof Number) {
                sum += (value as BigDecimal) //.setScale(scale, RoundingMode.HALF_UP)
                nVals++
            }
        }
        if (sum == 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP)
        }
        return sum.divide((nVals as BigDecimal), scale, RoundingMode.HALF_UP)
    }

    static BigDecimal mean(Matrix table, String colName) {
        return mean(table.column(colName))
    }

    @CompileDynamic
    static List<BigDecimal> medians(List<List<?>> matrix, Integer colNum) {
        return medians(matrix, [colNum])
    }

    @CompileDynamic
    static List<BigDecimal> medians(Matrix table, String colName) {
        return medians(table.column(colName) as List<List<?>>, [table.columnNames().indexOf(colName)])
    }

    @CompileDynamic
    static List<BigDecimal> medians(List<List<?>> matrix, List<Integer> colNums) {
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
            m = median(list as List<? extends Number>)
            medians.add(m)
        }

        return medians
    }

    static List<BigDecimal> medians(Matrix table, List<String> colNames) {
        // Optimized: use columnar access instead of row iteration
        List<BigDecimal> results = []
        colNames.each { colName ->
            List<?> columnData = table.column(colName)
            List<Number> numericValues = columnData.findAll { it != null && it instanceof Number } as List<Number>
            // median() handles sorting internally, no need to pre-sort
            results << median(numericValues)
        }
        return results
    }

    /**
     * calculates the mean for each row and return a list of the result.
     *
     * @param m the matrix to use
     * @param colNums either a List of numbers or an IntRange to include
     * @return a list (a column) with the mean of the rows for the columns specified
     */
    @CompileDynamic
    static List<BigDecimal> medianRows(Matrix table, List<Integer> columns) {
        List<BigDecimal> means = []
        table.each { row ->
            means << median(row[columns])
        }
        means
    }

    /**
     *
     * @param m the matrix containing the data
     * @param colNames the names of the columns to include, if omitted, all columns will be included
     * @return a list of the row sums
     */
    @CompileDynamic
    static List<BigDecimal> medianRows(Matrix m, String... colNames) {
        List<BigDecimal> means = []
        if (colNames.length == 0) {
            colNames = m.columnNames()
        }
        m.each { row ->
            means << median(row[colNames])
        }
        means
    }

    /**
     * Enable expressions such as <code>table.addColumn('medians', medianRows(table[1..23]))</code>
     * @param columns a list of columns to summarize by row
     * @return a list of numbers with sums for each row
     */
    @CompileDynamic
    static <T extends Number> List<T> medianRows(List<List<?>> columns) {
        def medians = []
        columns.transpose().each {
            medians << median(it as List<?>)
        }
        medians
    }

    static BigDecimal median(List<? extends Number> valueList) {
        if (valueList == null || valueList.size() == 0) {
            return null
        }
        if (valueList.size() == 1) {
            return valueList[0]
        }
        List<? extends Number> vals = new ArrayList(valueList)
        vals.sort()
        if (vals.size() % 2 == 0) {
            def index = vals.size()/2 as int
            def val1 = vals[index -1] as Number
            def val2 = vals[index] as Number
            BigDecimal median = (val1 + val2) / 2
            return median
        } else {
            return asBigDecimal(vals[vals.size()/2 as int])
        }
    }

    /**
     * The quartiles of a ranked set of data values are three points which divide the data into exactly four equal parts,
     * each part comprising of quarter data. As Q2 is the median only Q1 and Q3 is returned here
     * @param values a list of numbers to use
     * @return a list of the 1:st and 3:rd quartile
     */
    @CompileDynamic
    static <T extends Number> List<T> quartiles(List<T> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("The list of values are either null or does not contain any data.")
        }

        // Rank order the values
        def v = values.collect() as Number[]
        v.sort()

        int q1 = ((v.size() -1) * 25 / 100).round(0).intValue()
        int q3 = ((v.size() -1) * 75 / 100).round(0).intValue()

        return [v[q1], v[q3]] as List<T>
    }

    /**
     * IQR (inter quartile range) is a measure of the spread of the data
     * (3rd quartile - 1st quartile).
     *
     * @param values a list of numbers to use
     * @return the difference between the 3rd and 1st quartile (always positive)
     */
    static <T extends Number> T iqr(List<T> values) {
        def q = quartiles(values)
        (q[1] - q[0]) as T
    }

    static <T> T min(List<T> list, boolean ignoreNonNumerics = false) {
        def minVal = null
        for (value in list) {
            boolean skip = false
            if (ignoreNonNumerics && !(value instanceof Number)) {
                skip = true
            }
            if (value instanceof Comparable && !skip) {
                if (minVal == null || value < minVal) {
                    minVal = value
                }
            }
        }
        return minVal
    }

    static <T extends Comparable> List<T> min(List<List<T>> matrix, Integer colNum, boolean ignoreNonNumerics = false) {
        return min(matrix, [colNum], ignoreNonNumerics)
    }

    static <T extends Comparable> List<T> min(List<List<T>> matrix, List<Integer> colNums, boolean ignoreNonNumerics = false) {
        def value
        def minVal
        List<T> minVals = new ArrayList<T>(colNums.size())
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

    static <T extends Comparable> List<T> min(Matrix table, List<String> colNames, boolean ignoreNonNumerics = false) {
        return min(table.rows() as List<List<T>>, table.columnIndices(colNames), ignoreNonNumerics)
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

    static <T extends Comparable> T max(List<List<T>> matrix, Integer colNum, boolean ignoreNonNumerics = false) {
        return max(matrix, [colNum], ignoreNonNumerics)[0]
    }

    static <T extends Comparable> List<T> max(List<List<T>> matrix, List<Integer> colNums, boolean ignoreNonNumerics = false) {
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

    static <T extends Comparable> List<T> max(Matrix table, List<String> colNames, boolean ignoreNonNumerics = false) {
        return max(table.rows() as List<List<T>>, table.columnIndices(colNames), ignoreNonNumerics)
    }

    static <T extends Comparable> T max(Matrix table, String colName) {
        max(table.rows() as List<List<T>>, table.columnIndex(colName))
    }


    @CompileDynamic
    static <T extends Number> List<T> sd(List<List<T>> matrix, boolean isBiasCorrected = true, Integer colNum) {
        return sd(matrix, isBiasCorrected, [colNum])
    }

    /**
     *
     * @param matrix the matrix containing the column to compute
     * @param colNum the column index for the column
     * @param isBiasCorrected - whether or not the variance computation will use the bias-corrected formula
     * @return the standard deviation
     */
    @CompileDynamic
    static <T extends Number> List<T> sd(List<List<T>> matrix, boolean isBiasCorrected = true, List<Integer> colNums) {
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
        List<T> stds = []
        for (colNum in colNums) {
            list = numberMap[String.valueOf(colNum)]
            std = (T)sd(list, isBiasCorrected)
            stds.add(std)
        }
        return stds
    }

    static <T extends Number> List<T> sd(Matrix table, List<String> columnNames, boolean isBiasCorrected = true) {
        // Optimized: use columnar access instead of row iteration
        List<T> results = []
        columnNames.each { colName ->
            List<?> columnData = table.column(colName)
            List<Double> numericValues = []
            columnData.each { value ->
                if (value instanceof Number) {
                    numericValues.add(value.doubleValue())
                }
            }
            results << (T)sd(numericValues, isBiasCorrected)
        }
        return results
    }

    @CompileDynamic
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
        if (size <= 0) {
            return null
        }
        return (sumOfSquares / size) as T
    }

    static <T extends Number> T sd(Matrix table, String columnName, boolean isBiasCorrected = true) {
        return sd(table.column(columnName) as List<T>, isBiasCorrected)
    }

    @CompileDynamic
    static <T extends Number> T sd(List<T> values, boolean isBiasCorrected = true) {
        if (values == null || values.isEmpty()) {
            return null
        }
        def variance = variance(values, isBiasCorrected)
        if (variance == null) {
            return null
        }
        return Math.sqrt(variance as double) as T
    }

    static <T extends Number> T sdSample(List<T> population) {
        return sd(population, true)
    }

    static <T extends Number> T sdPopulation(List<T> population) {
        return sd(population, false)
    }

    static Matrix frequency(List<?> column) {
        Map<String, AtomicInteger> freq = new HashMap<>()
        column.forEach(v -> {
            freq.computeIfAbsent(String.valueOf(v), k -> new AtomicInteger(0)).incrementAndGet()
        })
        int size = column.size()
        List<List<?>> matrix = []
        def percent
        for (Map.Entry<Object, AtomicInteger> entry : freq.entrySet()) {
            int numOccurrence = entry.getValue().intValue()
            percent = (numOccurrence * 100.0 / size).setScale(2, RoundingMode.HALF_EVEN)
            matrix.add([String.valueOf(entry.getKey()), numOccurrence, percent])
        }
        Matrix.builder()
            .columnNames([FREQUENCY_VALUE, FREQUENCY_FREQUENCY, FREQUENCY_PERCENT])
            .rows(matrix)
            .types([String, int, BigDecimal])
            .build()
        .orderBy((FREQUENCY_FREQUENCY): true, (FREQUENCY_VALUE): false)
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
     * @param includeColumnNameCategory add the values for the columnName in a separate column
     * @return a table with one column for the frequency value and each group value as a column
     * with the frequency counts as rows
     */
    static Matrix frequency(Matrix table, String groupName, String columnName, boolean includeColumnNameCategory = true) {
        def groups = table.split(groupName)
        def tbl = new LinkedHashMap<String, List<?>>()
        groups.each {
            def freqTbl = frequency(it.value, columnName)
            if (includeColumnNameCategory) {
                tbl[columnName] = freqTbl.column(FREQUENCY_VALUE)
            }
            tbl[String.valueOf(it.key)] = freqTbl.column(FREQUENCY_FREQUENCY)
        }
        def nam = (table.getMatrixName() == null || table.getMatrixName().isBlank()) ? groupName : table.getMatrixName() + '_' + groupName
        return Matrix.builder().data(tbl).matrixName(nam).build()
    }

    /**
     * Allows for various column operations such as multiplying, adding values together etc.
     * Example:
     * <code><pre>
     * List a = [0,1,2,3]
     * List b = [1,2,3,4]
     * def result = Stat.apply(a, b) { x, y ->
     *  x * y
     * }
     * assertIterableEquals([0, 2, 6, 12], result)
     * </pre></code>
     *
     * @param x the first list of values
     * @param y the second list of values
     * @param operation what each element should to with the corresponding element in the other list
     * @return a list containing the result of the operation
     */
    static List apply(List x, List y, Closure operation) {
        int i = 0
        x.collect {
            operation.call(it, y[i++])
        }
    }

    static Class scanForType(Column column) {
        for (def val in column) {
            if (val != null) {
                return val.getClass()
            }
        }
        return null
    }
}
