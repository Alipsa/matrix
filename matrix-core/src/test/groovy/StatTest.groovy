import se.alipsa.groovy.matrix.Grid
import se.alipsa.groovy.matrix.Matrix

import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.groovy.matrix.ListConverter.*
import static se.alipsa.groovy.matrix.Stat.*
import static se.alipsa.groovy.matrix.ValueConverter.*

import org.junit.jupiter.api.Test

class StatTest {

    @Test
    void testMean() {
        def matrix = [
                [1, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(2.0g, mean(matrix[0],1))
        def m = means(matrix, [1, 2])
        assertEquals(10/3, m[0])
        assertEquals(1.95g, m[1])

        def table = Matrix.create(["v0", "v1", "v2"], matrix)
        def m2 = means(table, ["v1", "v2"])
        assertIterableEquals(m, m2)

        Grid<Number> bar = new Grid<>([
            [12.0, 3.0, Math.PI],
            [1.9, 2, 3],
            [4.3, 2, 3]
        ])

        assertIterableEquals([6.07, 2.33, 3.05], means(bar, 2))
    }

    @Test
    void testMedian() {
        def matrix = [
                [1, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(2 as BigDecimal, median(matrix[0]))
        def m = medians(matrix, [1, 2])
        assertEquals(2 as BigDecimal, m[0])
        assertEquals(1.95g, m[1])

        def table = Matrix.create(["v0", "v1", "v2"], matrix)
        def m2 = medians(table, ["v1", "v2"])
        assertIterableEquals(m, m2)
    }

    @Test
    void testMin() {
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(0.3, min(matrix[0]))
        def m = min(matrix, 1..2)
        assertEquals(1, m[0])
        assertEquals(0.9, m[1])

        def table = Matrix.create(["v0", "v1", "v2"], matrix)
        def m2 = min(table, ["v1", "v2"])
        assertIterableEquals(m, m2)
    }

    @Test
    void testMax() {
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(3, max(matrix[0]))
        def m = max(matrix, 1..2, true)
        assertEquals(7, m[0])
        assertEquals(3, m[1])

        def table = Matrix.create(["v0", "v1", "v2"], matrix)
        def m2 = max(table, ["v1", "v2"], true)
        assertIterableEquals(m, m2)
    }

    @Test
    void testSd() {
        //
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(1.3650396819628847, sd(matrix[0]))
        assertEquals(1.3650396819628847, sdSample([0.3, 2, 3]))
        def s = sd(matrix, [1, 2])
        assertEquals(3.214550253664318, s[0])
        assertEquals(1.4849242404917498, s[1])

        def table = Matrix.create(["v0", "v1", "v2"], matrix)
        def s2 = sd(table, ["v1", "v2"])
        assertIterableEquals(s, s2)
    }

    @Test
    void testFrequency() {
        def values = [12.1, 23, 0, 12.1, 99, 0, 23, 12.1]
        def freq = frequency(values)
        assertEquals(2, freq.get(0, 1), "occurrences of 0")
        assertEquals(12.50, freq.get(1, 2), "occurrences of 99")
        assertEquals(23, freq.get(2, 0) as int, "occurrences of 23")
        assertEquals(37.50, freq.get(3, 2), "occurrences of 12.1")

        def table = new Matrix([id: 0..7, vals: values])
        def freq2 = frequency(table, "vals")
        assertEquals(2, freq2.get(0, 1), "occurrences of 0 from table")
        assertEquals(12.50, freq2.get(1, 2), "occurrences of 99 from table")
        assertEquals(23, freq2.get(2, 0) as int, "occurrences of 23 from table")
        assertEquals(37.50, freq2.get(3, 2), "occurrences of 12.1 from table")
    }

    @Test
    void testFrequencyTable() {
        def table = new Matrix([
            gear: [4, 4, 4, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 4, 4, 4, 3, 3, 3, 3, 3, 4, 5, 5, 5, 5, 5, 4],
            vs: [0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1]
        ], [Number, Number])

        def vsByGears = frequency(table, "gear", "vs")
        assertEquals(4, vsByGears.columnCount(), "column count\n" + vsByGears.content())
        assertEquals(2, vsByGears.rowCount(), "row count\n" + vsByGears.content())
        assertEquals(['0','1'], vsByGears["vs"], vsByGears.content())
        assertEquals([12,3], vsByGears["3"], vsByGears.content())
        assertEquals([2,10], vsByGears["4"], vsByGears.content())
        assertEquals([4,1], vsByGears["5"], vsByGears.content())
    }

    @Test
    void testSummary() {
        def table = new Matrix([
            v0: [0.3, 2, 3],
            v1: [1.1, 1, 0.9],
            v2: [null, 'Foo', "Foo"]
        ], [Number, double, String])
        def summary = summary(table)
        assertEquals(0.3, summary['v0']["Min"])
        assertEquals(1.1, summary['v1']["Max"])
        assertEquals(2, summary['v2']["Number of unique values"])
    }

    @Test
    void testStr() {
        def table = new Matrix('Test',
                [
                v0: [0.3, 2, 3],
                v1: [1.1, 1, 0.9],
                v2: [null, 'Foo', "Foo"]
        ], [Number, double, String])
        def structure =  str(table)
        //println structure
        def rows= structure.toString().split('\n')
        assertEquals(5, rows.length)
        assertTrue(rows[0].startsWith('Matrix (Test'), 'First row')
        assertTrue(rows[1].startsWith('----'), 'second row')
        assertEquals(rows[0].length(), rows[1].length(), 'underline length')
        assertTrue(rows[4].startsWith('v2:'), 'Last row')
    }

    @Test
    void testQuartiles() {
        def data = [3, 6, 7, 8, 8, 10, 13, 15, 16, 20]
        def q = quartiles(data)
        assertIterableEquals([7, 15], q)

        data = [9,9,8,9,10,9,3,5,6,8,9,10,11,12,13,11,10]
        assertIterableEquals([8, 10], quartiles(data))
    }

    @Test
    void testSum() {
        def table = new Matrix('Test',
            [
                v0: [0.3, 2, 3],
                v1: [1.1, 1, 0.9],
                v2: 1..3 as IntRange
            ], [Number, double, Integer])
        assertIterableEquals([5.3, 3.0d, 6i], sum(table))
        assertIterableEquals([3.0d, 6i], sum(table, 'v1', 'v2'))
        assertIterableEquals([3.0d, 6i], sum(table, 1..2))
        assertIterableEquals([5.3, 6i], sum(table, [0,2]))
    }

    @Test
    void testSumBy() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            department: ["IT", "OPS", "OPS", "IT", "Infra"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
            [int, String, String, Number, LocalDate]
        )

        def sums = sumBy(empData, "salary", "department")
        def salaries = empData["salary"]

        assertEquals((salaries[0] + salaries[3]) as BigDecimal, sums.findFirstRow('department', "IT")[1])
        assertEquals((salaries[1] + salaries[2]) as BigDecimal, sums.findFirstRow('department', "OPS")[1])
        assertEquals(843.25g, sums.findFirstRow('department', "Infra")[1])
    }

    @Test
    void testMeanBy() {
        def empData = new Matrix(
                emp_id: 1..5,
                emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
                department: ["IT", "OPS", "OPS", "IT", "Infra"],
                salary: [623.3,515.2,611.0,729.0,843.25],
                start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
                [int, String, String, Number, LocalDate]
        )

        def sums = meanBy(empData, "salary", "department", 2)
        def salaries = empData["salary"]

        assertEquals(((salaries[0] + salaries[3])/2).setScale(2, RoundingMode.HALF_UP), sums.findFirstRow('department', "IT")[1])
        assertEquals(((salaries[1] + salaries[2])/2).setScale(2, RoundingMode.HALF_UP), sums.findFirstRow('department', "OPS")[1])
        assertEquals(843.25g, sums.findFirstRow('department', "Infra")[1])
    }

    @Test
    void testMedianBy() {
        def empData = new Matrix(
                emp_id: 1..6,
                emp_name: ["Rick","Dan","Michelle","Ryan","Gary", "Stu"],
                department: ["IT", "OPS", "IT", "IT", "Infra", "OPS"],
                salary: [623.3, 515.2, 611.0, 729.0, 843.25, 611.0],
                start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27", "2020-01-15"),
                [int, String, String, Number, LocalDate]
        )

        def sums = medianBy(empData, "salary", "department")
        def salaries = empData["salary"]

        assertEquals(salaries[0] as BigDecimal,
                sums.findFirstRow('department', "IT")[1],
                "median for IT department"
        )
        assertEquals((salaries[1] + salaries[2])/2 as BigDecimal,
                sums.findFirstRow('department', "OPS")[1],
                "median for OPS department"
        )
        assertEquals(843.25g, sums.findFirstRow('department', "Infra")[1], "median for Infra department")
    }

    @Test
    void testMinMaxYearMonth() {
        def bp = toYearMonth(["2023-07", "2023-06", "2023-04", "2023-05"])
        assertEquals(bp.min(), min(bp))
        assertEquals(YearMonth.of(2023, 4), min(bp))
        assertEquals(bp.max(), max(bp))
        assertEquals(YearMonth.of(2023, 7), max(bp))
    }

    @Test
    void testMinMaxDates() {
        def expMin = asLocalDate("2023-04-10")
        def expMax = asLocalDate("2023-07-12")
        def bp = [
                expMax,
                asLocalDate("2023-06-01"),
                expMin,
                LocalDate.of(2023, 5, 1)
        ]
        assertEquals(bp.min(), min(bp))
        assertEquals(expMin, min(bp))
        assertEquals(bp.max(), max(bp))
        assertEquals(expMax, max(bp))
    }

    @Test
    void testMixedNumbers() {
        assertEquals(3.14, min([6, 99g, 3.14d, 7/1.23, 5 as Short, (byte)19, 787987987]))
        assertEquals(787987987, max([6, 99g, 3.14d, 7/1.23, 5 as Short, (byte)19, 787987987]))
    }
}
