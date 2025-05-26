import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.Summary

import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.*
import static se.alipsa.matrix.core.Stat.*
import static se.alipsa.matrix.core.ValueConverter.*

import org.junit.jupiter.api.Test

class StatTest {

  @Test
  void testMean() {
    def matrix = [
        [1, 2, 3],
        [1.1, 1, 0.9],
        [null, 7, "2"]
    ]

    assertEquals(2.0g, mean(matrix[0], 1))
    def m = means(matrix, [1, 2])
    assertEquals(10 / 3, m[0])
    assertEquals(1.95g, m[1])

    def table = Matrix.builder().columnNames(["v0", "v1", "v2"]).rows(matrix).build()
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

    def table = Matrix.builder().columnNames(["v0", "v1", "v2"]).rows(matrix).build()
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

    def table = Matrix.builder().columnNames(["v0", "v1", "v2"]).rows(matrix).build()
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

    def table = Matrix.builder().columnNames(["v0", "v1", "v2"]).rows(matrix).build()
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

    def table = Matrix.builder().columnNames(["v0", "v1", "v2"]).rows(matrix).build()
    def s2 = sd(table, ["v1", "v2"])
    assertIterableEquals(s, s2)
  }

  @Test
  void testFrequency() {
    def values = [12.1, 23, 0, 12.1, 99, 0, 23, 12.1]
    def freq = frequency(values)
    assertEquals(2, freq.get(1, 1), "occurrences of 0")
    assertEquals(12.50, freq.get(3, 2), "occurrences of 99")
    assertEquals(23, freq.get(2, 0) as int, "occurrences of 23")
    assertEquals(37.50, freq.get(0, 2), "occurrences of 12.1")

    def table = Matrix.builder().data([id: 0..7, vals: values]).build()
    def freq2 = frequency(table, "vals")
    assertEquals(2, freq2.get(1, 1), "occurrences of 0 from table")
    assertEquals(12.50, freq2.get(3, 2), "occurrences of 99 from table")
    assertEquals(23, freq2.get(2, 0) as int, "occurrences of 23 from table")
    assertEquals(37.50, freq2.get(0, 2), "occurrences of 12.1 from table")

    def freq3 = frequency([null, 1,2,1,null])
    def row = freq3.findFirstRow(0, 'null')
    assertNotNull(row, freq3.content())
    assertEquals(2, row[FREQUENCY_FREQUENCY], freq3.content())
    assertEquals(2/5 * 100, row[FREQUENCY_PERCENT] as Double, freq3.content())

    def freq4 = frequency([null, 'Foo','Bar','Bar',null])
    Row nullRow = freq4.findFirstRow('Value', 'null')
    assertEquals(2, nullRow[FREQUENCY_FREQUENCY])
    assertEquals(2/5 * 100, nullRow[FREQUENCY_PERCENT] as Double)
  }

  @Test
  void testFrequencyTable() {
    def table = Matrix.builder().data([
        gear: [4, 4, 4, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 4, 4, 4, 3, 3, 3, 3, 3, 4, 5, 5, 5, 5, 5, 4],
        vs  : [0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1]
    ]).types(Number, Number).build()

    def vsByGears = frequency(table, "gear", "vs")
    assertEquals(4, vsByGears.columnCount(), "column count\n" + vsByGears.content())
    assertEquals(2, vsByGears.rowCount(), "row count\n" + vsByGears.content())
    assertEquals(['0', '1'], vsByGears["vs"], vsByGears.content())
    assertEquals([12, 3], vsByGears["3"], vsByGears.content())
    assertEquals([10, 2], vsByGears["4"], vsByGears.content())
    assertEquals([4, 1], vsByGears["5"], vsByGears.content())
  }

  @Test
  void testSummary() {
    def table = Matrix.builder().data([
        v0: [0.3, 2, 3],
        v1: [1.1, 1, 0.9],
        v2: [null, 'Foo', "Foo"]
    ]).types([Number, double, String]).build()
    def sum = summary(table)
    //println summary
    assertEquals(0.3, sum['v0']["Min"])
    assertEquals(1.1, sum['v1']["Max"])
    assertEquals(2, sum['v2']["Number of unique values"])

    Summary colSum = summary(table.v1)
    assertEquals(1.1, colSum.v1["Max"])
    assertEquals(0.9, colSum['v1',"Min"])
  }

  @Test
  void testStr() {
    def table = Matrix.builder()
        .matrixName('Test')
        .data([
            v0: [0.3, 2, 3],
            v1: [1.1, 1, 0.9],
            v2: [null, 'Foo', "Foo"]
        ])
        .types([Number, double, String])
        .build()
    def structure = str(table)
    //println structure
    def rows = structure.toString().split('\n')
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

    data = [9, 9, 8, 9, 10, 9, 3, 5, 6, 8, 9, 10, 11, 12, 13, 11, 10]
    assertIterableEquals([8, 10], quartiles(data))
  }

  @Test
  void testSum() {
    def table = Matrix.builder()
        .matrixName('Test')
        .data(
            [
                v0: [0.3, 2, 3],
                v1: [1.1, 1, 0.9],
                v2: 1..3 as IntRange
            ])
        .types([Number, double, Integer])
        .build()
    assertIterableEquals([5.3, 3.0d, 6i], sum(table))
    assertIterableEquals([3.0d, 6i], sum(table, 'v1', 'v2'))
    assertIterableEquals([3.0d, 6i], sum(table, 1..2))
    assertIterableEquals([5.3, 6i], sum(table, [0, 2]))
  }

  @Test
  void testSumBy() {
    def empData = Matrix.builder()
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            department: ["IT", "OPS", "OPS", "IT", "Infra"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types([int, String, String, Number, LocalDate])
        .build()

    def sums = sumBy(empData, "salary", "department")
    def salaries = empData["salary"]

    assertEquals((salaries[0] + salaries[3]) as BigDecimal, sums.findFirstRow('department', "IT")[1])
    assertEquals((salaries[1] + salaries[2]) as BigDecimal, sums.findFirstRow('department', "OPS")[1])
    assertEquals(843.25g, sums.findFirstRow('department', "Infra")[1])
  }

  @Test
  void testMeanBy() {
    def empData = Matrix.builder()
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            department: ["IT", "OPS", "OPS", "IT", "Infra"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types(int, String, String, Number, LocalDate)
        .build()

    def sums = meanBy(empData, "salary", "department", 2)
    def salaries = empData["salary"]

    assertEquals(((salaries[0] + salaries[3]) / 2).setScale(2, RoundingMode.HALF_UP), sums.findFirstRow('department', "IT")[1])
    assertEquals(((salaries[1] + salaries[2]) / 2).setScale(2, RoundingMode.HALF_UP), sums.findFirstRow('department', "OPS")[1])
    assertEquals(843.25g, sums.findFirstRow('department', "Infra")[1])
  }

  @Test
  void testMedianBy() {
    def empData = Matrix.builder()
        .data(
            emp_id: 1..6,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary", "Stu"],
            department: ["IT", "OPS", "IT", "IT", "Infra", "OPS"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25, 611.0],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27", "2020-01-15"))
        .types(int, String, String, Number, LocalDate)
        .build()

    def sums = medianBy(empData, "salary", "department")
    def salaries = empData["salary"]

    assertEquals(salaries[0] as BigDecimal,
        sums.findFirstRow('department', "IT")[1],
        "median for IT department"
    )
    assertEquals((salaries[1] + salaries[2]) / 2 as BigDecimal,
        sums.findFirstRow('department', "OPS")[1],
        "median for OPS department"
    )
    assertEquals(843.25g, sums.findFirstRow('department', "Infra")[1], "median for Infra department")
  }

  @Test
  void testMinMaxYearMonth() {
    def bp = toYearMonths(["2023-07", "2023-06", "2023-04", "2023-05"])
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
    assertEquals(3.14, min([6, 99g, 3.14d, 7 / 1.23, 5 as Short, (byte) 19, 787987987]))
    assertEquals(787987987, max([6, 99g, 3.14d, 7 / 1.23, 5 as Short, (byte) 19, 787987987]))
  }

  @Test
  void testApply() {
    List x = [0, 1, 2, 3]
    List y = [1, 2, 3, 4]
    def result = apply(x, y) { a, b ->
      a * b
    }
    assertIterableEquals([0, 2, 6, 12], result)
  }

  @Test
  void testMeanRows() {
    Matrix m = Matrix.builder().data(
        foo: [1.0,2.0,3.0],
        bar: [5.0,6.0,7.0],
        baz: [2.0, 3.0, 4.0],
        qux: [3.0, 1.0, 9.0]
    ).types([BigDecimal]*4)
    .build()
    assert [1.5, 2.5, 3.5] == meanRows(m, [0, 2])
    assert [3.0, 4.0, 5.0] == meanRows(m, 0..1)
    assert [3.0, 4.0, 5.0] == meanRows(m, 'foo', 'bar')
    assert [2.75, 3.0, 5.75] == meanRows(m, 0..3)
    assert [2.75, 3.0, 5.75] == meanRows(m)

    m.means = meanRows(m[0..3])
    assert [2.75, 3.0, 5.75] == m['means']
  }

  @Test
  void testSumRows() {
    Matrix m = Matrix.builder().data(
        foo: [1.0,2.0,3.0],
        bar: [5.0,6.0,7.0],
        baz: [2.0, 3.0, 4.0],
        qux: [3.0, 1.0, 9.0]
    ).types([BigDecimal]*4)
        .build()
    def sums = sumRows(m, [0, 2])
    assert [3.0, 5.0, 7.0] == sums
    assert [6.0, 8.0, 10.0] == sumRows(m, 0..1)
    assert [6.0, 8.0, 10.0] == sumRows(m, 'foo', 'bar')
    assert [11.0, 12.0, 23.0] == sumRows(m)

    m.addColumn('total', sumRows(m[0..2]))
    assertIterableEquals([8.0, 11.0, 14.0], m.total)
  }

  @Test
  void testMedianRows() {
    Matrix m = Matrix.builder().data(
        foo: [1.0,2.0,3.0],
        bar: [5.0,6.0,7.0],
        baz: [2.0, 3.0, 4.0],
        qux: [3.0, 1.0, 9.0]
    ).types([BigDecimal]*4)
        .build()

    //println m.content()
    assert [2.0, 2.0, 4.0] == medianRows(m, [0, 2, 3])
    assert [2.0, 3.0, 4.0] == medianRows(m, 0..2)
    assert [2.0, 3.0, 4.0] == medianRows(m, 'foo', 'bar', 'baz')
    assert [2.5, 2.5, 5.5] == medianRows(m, 0..3)
    assert [2.5, 2.5, 5.5] == medianRows(m)

    m['medians'] = medianRows(m[0..2])
    assert [2.0, 3.0, 4.0] == m.medians
  }

  @Test
  void testGroupBy() {
    def empData = Matrix.builder()
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            department: ["IT", "OPS", "OPS", "IT", "IT"],
            emp_type: ["Full-time", "Part-time", "Full-time", "Part-time", "Full-time"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types(int, String, String, String, Number, LocalDate)
        .build()

    def grouped = groupBy(empData, ['department', 'emp_type'])
    assertEquals(1, grouped['OPS_Full-time'].size())
    assertEquals(1, grouped['OPS_Part-time'].size())
    assertEquals(2, grouped['IT_Full-time'].size())
    assertEquals(1, grouped['IT_Part-time'].size())

    def ab = Matrix.builder().data(
        a: [1, 1, 2, 2, 2, 2, 2],
        b: ['A', 'B', 'A', 'B', 'B', 'A', 'B']
    )
        .types(int, String)
        .build()
    def grouped2 = groupBy(ab, 'a', 'b')
    assertEquals(1, grouped2['1_A'].size())
    assertEquals(1, grouped2['1_B'].size())
    assertEquals(2, grouped2['2_A'].size())
    assertEquals(3, grouped2['2_B'].size())
  }
}
