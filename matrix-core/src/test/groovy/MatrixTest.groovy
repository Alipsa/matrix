import se.alipsa.matrix.core.Converter
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.core.Matrix

import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAccessor

import static se.alipsa.matrix.core.ListConverter.*
import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.*

import static se.alipsa.matrix.core.ValueConverter.asLocalDate
import static se.alipsa.matrix.core.ValueConverter.asYearMonth


class MatrixTest {

  @Test
  void testMatrixConstructors() {
    def empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        )
        .types([int, String, Number, LocalDate])
        .build()
    assertEquals('empData', empData.getMatrixName())
    assertEquals(1, empData[0, 0])
    assertEquals("Dan", empData[1, 1])
    assertEquals(611.0, empData[2, 2])
    assertEquals(LocalDate.of(2015, 3, 27), empData[4, 3])
    assertIterableEquals([Integer, String, Number, LocalDate], empData.types())

    def dims = empData.dimensions()
    assertEquals(5, dims.observations)
    assertEquals(4, dims.variables)

    def ed = Matrix.builder().
        matrixName("ed")
        .columnNames(['id', 'name', 'salary', 'start'])
        .columns(
            1..5,
            ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            [623.3, 515.2, 611.0, 729.0, 843.25],
            toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        ).build()
    assertEquals('ed', ed.getMatrixName())
    assertEquals(1, ed[0, 0])
    assertEquals("Dan", ed[1, 1])
    assertEquals(611.0, ed[2, 2])
    assertEquals(LocalDate.of(2015, 3, 27), ed[4, 3])
    assertIterableEquals([Object] * 4, ed.types())

    def e = Matrix.builder().columns([
        1..5,
        ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        [623.3, 515.2, 611.0, 729.0, 843.25],
        toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ]).build()
    assertNull(e.getMatrixName())
    assertEquals(1, e[0, 0])
    assertEquals("Dan", e[1, 1])
    assertEquals(611.0, e[2, 2])
    assertEquals(LocalDate.of(2015, 3, 27), e[4, 3])
    assertIterableEquals([Object] * 4, ed.types())
  }

  @Test
  void testAddRow() {
    Matrix m = Matrix.builder()
        .matrixName("years")
        .columnNames((1..5).collect { "Y" + it })
        .build()
    m.addRow([1, 2, 3, 4, 5])
    m = m.plus([10, 20, 30, 40, 50])
    m.addRow(0, m.columnNames())
    assertIterableEquals(['Y1', 'Y2', 'Y3', 'Y4', 'Y5'], m.columnNames())
    assertIterableEquals(['Y1', 'Y2', 'Y3', 'Y4', 'Y5'], m.row(0))
    assertIterableEquals([1, 2, 3, 4, 5], m.row(1))
    assertIterableEquals([10, 20, 30, 40, 50], m.row(2))

    def m2 = m + ['Foo', 3, 4, 5, 6]
    assertIterableEquals(['Foo', 3, 4, 5, 6], m2.row(m2.lastRowIndex()))

    // test add row mutable
    m & [3, 4, 5, 6, 7]
    assertIterableEquals([3, 4, 5, 6, 7], m.row(m.lastRowIndex()))

    Matrix n = Matrix.builder().columns(
        Y1: [10, 11],
        Y2: [12, 13],
        Y3: [14, 15],
        Y4: [16, 17],
        Y5: [18, 19]
    ).build()
    m & n
    assertIterableEquals([11, 13, 15, 17, 19], m.row(m.lastRowIndex()))
  }

  @Test
  void testTransposing() {
    def report = [
        "Year"            : [1, 2, 3, 4],
        "Full Funding"    : [4563.153, 380.263, 4.938, 101.1],
        "Baseline Funding": [3385.593, 282.133, 3.664, 123.123],
        "Current Funding" : [2700, 225, 2.922, 1010.12]
    ]
    def table = Matrix.builder().data(report).build()
    def tr = table.transpose(['y1', 'y2', 'y3', 'y4'])
    assertEquals(["y1", "y2", "y3", "y4"], tr.columnNames())
    assertEquals([
        [1, 2, 3, 4],
        [4563.153, 380.263, 4.938, 101.1],
        [3385.593, 282.133, 3.664, 123.123],
        [2700, 225, 2.922, 1010.12]
    ], tr.rows(), table.content())
    assertEquals(4, tr.types().size(), "Column types")

    assertEquals([
        [1, 2, 3, 4],
        [4563.153, 380.263, 4.938, 101.1],
        [3385.593, 282.133, 3.664, 123.123],
        [2700, 225, 2.922, 1010.12]
    ], tr.rowList(), table.content())

    def tr2 = table.transpose(true)
    assertEquals([
        ["Year", 1, 2, 3, 4],
        ["Full Funding", 4563.153, 380.263, 4.938, 101.1],
        ["Baseline Funding", 3385.593, 282.133, 3.664, 123.123],
        ["Current Funding", 2700, 225, 2.922, 1010.12]
    ], tr2.rows())
    assertEquals(5, tr2.types().size(), tr2.content() + "\nColumn types: " + tr2.typeNames())

    def t3 = table.transpose('Year', true)
    assertEquals([
        ["Year", 1, 2, 3, 4],
        ["Full Funding", 4563.153, 380.263, 4.938, 101.1],
        ["Baseline Funding", 3385.593, 282.133, 3.664, 123.123],
        ["Current Funding", 2700, 225, 2.922, 1010.12]
    ], t3.rows(), t3.content())
    assertEquals(['', '1', '2', '3', '4'], t3.columnNames())
    assertEquals(5, t3.types().size(), t3.content() + "\nColumn types: " + t3.typeNames())

    def t4 = table.transpose('Year', [String, Number, Number, Number, Number], true)
    assertEquals([
        ["Year", 1, 2, 3, 4],
        ["Full Funding", 4563.153, 380.263, 4.938, 101.1],
        ["Baseline Funding", 3385.593, 282.133, 3.664, 123.123],
        ["Current Funding", 2700, 225, 2.922, 1010.12]
    ], t4.rows(), t4.content())
    assertEquals(['', '1', '2', '3', '4'], t4.columnNames())
    assertEquals(5, t4.types().size(), t4.content() + "\nColumn types: " + t4.typeNames())
  }

  @Test
  void testStr() {
    def empData = Matrix.builder().data(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ).types(int, String, Number, LocalDate)
        .build()

    def struct = Stat.str(empData)
    assertEquals(['5 observations of 4 variables'], struct['Matrix'])
    assertIterableEquals(['Integer', '1', '2', '3', '4'], struct['emp_id'])
    assertIterableEquals(['LocalDate', '2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11'], struct['start_date'])
  }

  @Test
  void testConvert() {
    def data = [
        'place'    : ['1', '2', '3', ','],
        'firstname': ['Lorena', 'Marianne', 'Lotte', 'Chris'],
        'start'    : ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10'],
        'end'      : ['2022-12-01 10:00:00', '2023-07-10 00:01:00', '2024-05-27 00:00:30', '2042-01-10 00:00:00']
    ]
    def table = Matrix.builder().data(data).types([String] * 4).build()

    def table2 = table.clone().convert(place: Integer, start: LocalDate)
    table2.convert(end: LocalDateTime,
        'yyyy-MM-dd HH:mm:ss')
    assertEquals(Integer, table2.type('place'))
    assertEquals(Integer, table2[0, 0].class)

    assertEquals(LocalDate, table2.type('start'))
    assertEquals(LocalDate, table2[0, 2].class)
    assertEquals(LocalDateTime.parse('2022-12-01T10:00:00.000'), table2['end'][0])

    def table3 = table.clone().convert('place', Integer, { Object it ->
      String val = String.valueOf(it).trim()
      if (val == 'null' || val == ',' || val.isBlank()) return null
      return Integer.valueOf(val)
    })
    assertEquals(Integer, table3.type('place'))
    assertEquals(3, table3['place'][2])

    def table4 = table.clone().convert([
        new Converter('place', Integer, { try { Integer.parseInt(it) } catch (NumberFormatException ignore) { null } }),
        new Converter('start', LocalDate, { LocalDate.parse(it) })
    ] as Converter[])

    //println table.content()
    //println table4.content()
    assertEquals(Integer, table4.type('place'))
    assertEquals(Integer, table4[0, 0].class)
    assertEquals(3, table4[2, 0])

    assertEquals(LocalDate, table4.type('start'))
    assertEquals(LocalDate, table4[0, 2].class)
    assertEquals(LocalDate.of(2023, 5, 27), table4[2, 2])

    def table5 = table.clone().convert(
        [Integer, String, LocalDate, String]
    )
    assertEquals(table4, table5, table4.diff(table5))

    table2[3, 0] = ',' // We lost the comma when converted to Integer, restore so we can compare
    def table6 = table2
        .convert(0..2, String, 'yyyy-MM-dd')
        .convert(3, String, 'yyyy-MM-dd HH:mm:ss')
    assertEquals(table, table6, table.diff(table6))
  }

  @Test
  void testGetRowsForCriteria() {
    def data = [
        'place'    : [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start'    : ['2021-12-01', '2022-07-10', '2023-05-27']
    ]
    Matrix table = Matrix.builder().data(data).types([int, String, String]).build()
    List<Row> rows = table.rows(table['place'].findIndexValues { it > 1 })
    assertEquals(2, rows.size())

    // Same thing using subset
    def subSet = table.subset({ it.place > 1 })
    assertIterableEquals(table.rows(1..2), subSet.rows(), subSet.content())

    def subSet2 = table.subset { it[0] > 1 }
    assertIterableEquals(table.rows(1..2), subSet2.rows())

    def subSet3 = table.subset {
      !it.firstname.startsWith('Ma')
          && asLocalDate(it.start).isBefore(LocalDate.of(2022, 10, 1))
    }
    assertEquals(table[0, 1], subSet3[0, 1])

    def subset4 = table.subset(0..1)
    assertEquals(2, subset4.rowCount())
    assertIterableEquals([1, 'Lorena', '2021-12-01'], subset4.row(0))
    assertIterableEquals([2, 'Marianne', '2022-07-10'], subset4.row(1))
  }

  @Test
  void testRowDetach() {
    Matrix m = Matrix.builder().rows([
        [1, 'Abc'],
        [2, 'Def']
    ]).build()
    def firstRow = m.row(0)
    firstRow[1] = 'Ghi'
    assertEquals(m[0, 1], 'Ghi')
    firstRow.detach()
    firstRow[1] = 'Jkl'
    assertEquals(m[0, 1], 'Ghi', 'Matrix should be unchanged')
    assertEquals('Jkl', firstRow[1])
  }

  @Test
  void testHeadAndTail() {
    def table = Matrix.builder()
        .data([
            'place'    : [1, 20, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start'    : ['2021-12-01', '2022-07-10', '2023-05-27']
        ])
        .types([int, String, String])
        .build()

    def head = table.head(1, false)
    assertEquals(' 1\tLorena  \t2021-12-01\n', head, head)
    def tail = table.tail(2, false)
    assertEquals('20\tMarianne\t2022-07-10\n 3\tLotte   \t2023-05-27\n', tail, tail)
    String[] content = table.content(includeHeader: false, includeTitle: false, maxColumnLength: 7).split('\n')
    assertEquals(' 1\tLorena \t2021-12', content[0])

  }

  @Test
  void testSelectRows() {
    def data = [
        'place'    : [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ]
    def table = Matrix.builder().data(data).types([int, String, LocalDate]).build()
    def selection = table.rowIndices {
      return it[2, LocalDate].isAfter(LocalDate.of(2022, 1, 1))
    }
    assertIterableEquals([1, 2], selection)

    def rows = table.rows {
      return it[2].isAfter(LocalDate.of(2022, 10, 1))
    }
    assertIterableEquals([[3, 'Lotte', LocalDate.of(2023, 5, 27)]], rows)
    // We can refer to a row column as a property
    def row = rows[0]
    assertEquals(3, row.place)
    assertEquals('Lotte', row.firstname)
    assertEquals(LocalDate.of(2023, 5, 27), row.start)
  }

  @Test
  void testApply() {
    def data = [
        'place'     : ['1', '2', '3', ','],
        'firstname' : ['Lorena', 'Marianne', 'Lotte', 'Chris'],
        'start'     : ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10'],
        'bloodsugar': [5.23, 5.64, 4.22, 3.91]
    ]
    def table = Matrix.builder().columns(data).build()
        .convert(place: int, start: LocalDate)
    table.apply("start") { startDate ->
      startDate.plusDays(10)
    }
    assertEquals(LocalDate.of(2021, 12, 11), table["start"][0])
    assertEquals(LocalDate.of(2022, 7, 20), table["start"][1])
    assertEquals(LocalDate.of(2023, 6, 6), table["start"][2])
    assertEquals(LocalDate.of(2023, 1, 20), table["start"][3])
    assertEquals(LocalDate, table.type("start"))

    table.apply('bloodsugar') { BigDecimal v ->
      v.setScale(1, RoundingMode.HALF_UP)
    }
    assertIterableEquals([5.2, 5.6, 4.2, 3.9], table.bloodsugar, table.content())
  }

  @Test
  void testApplyChangeType() {
    def data = [
        'foo'      : [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ]

    def table = Matrix.builder().columns(data).types([int, String, LocalDate]).build()

    def foo = table.apply("start", { asYearMonth(it) })
    assertEquals(YearMonth.of(2021, 12), foo[0, 2])
    assertEquals(YearMonth, foo.type("start"))
  }

  @Test
  void testSelectRowsAndApply() {
    def data = [
        'place'    : [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ]
    def table = Matrix.builder()
        .columns(data)
        .types(int, String, LocalDate)
        .build()
    assertEquals(Integer, table.type(0), "place column type")
    def selection = table.rowIndices {
      def date = it[2] as LocalDate
      return date.isAfter(LocalDate.of(2022, 1, 1))
    }
    assertIterableEquals([1, 2], selection)
    def foo = table.clone().apply("place", selection, { it * 2 })
    assertEquals(3, foo.rowCount(), 'Number of rows should be unaffected by apply')
    assertEquals(3, foo.columnCount(), 'Number of columns should be unaffected by apply')
    assertEquals(4, foo[1, 0], foo.content())
    assertEquals(6, foo[2, 0])
    assertEquals(LocalDate, foo.type(2))
    assertEquals(Integer, foo.type(0), "place column type")

    def bar = table.clone().apply("place", {
      def date = it[2] as LocalDate
      return date.isAfter(LocalDate.of(2022, 1, 1))
    }, {
      it + 2
    })
    //println(bar.types())
    //println(bar.content())
    assertEquals(1, bar[0, 0])
    assertEquals(4, bar[1, 0])
    assertEquals(5, bar[2, 0])

    assertEquals(asLocalDate('2022-07-10'), bar[1, 'start'])
    assertEquals(LocalDate, bar.type(2), "start column type")
    assertEquals(String, bar.firstname.type, "firstname column type")
    assertEquals(Integer, bar.type(0), "place column type")

    def r = table.rows { row ->
      row['place'] == 2
    }
    assertEquals([2, 'Marianne', LocalDate.parse('2022-07-10')], r[0])

    // An item in a Row can also be referenced by the column name
    Row r2 = table.rows().find { row ->
      row['place'] == 3
    }
    assertIterableEquals([3, 'Lotte', LocalDate.parse('2023-05-27')], r2, String.valueOf(r2))
  }

  @Test
  void testApplyRows() {
    def data = [
        'place'    : [1, 2, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ]
    def table = Matrix.builder().columns(data).types(int, String, LocalDate).build()
    table.applyRows('place') { Row row ->
      if (row.start.isBefore(LocalDate.of(2022, 8, 1))) {
        row.place + row.firstname.length()
      } else {
        row.place
      }
    }
    assert [7, 10, 3] == table.place
  }

  @Test
  void testAppendColumnValue() {
    String name = 'numbers'
    List<String> header = ['foo', 'bar']
    def table = Matrix.builder().matrixName(name).columnNames(header).build()
    table['foo'] << 1
    table[0] << 2
    assertEquals([1, 2], table.foo)
  }

  @Test
  void testAddColumn() {
    def empData = Matrix.builder().columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types([int, String, Number, LocalDate]).build()
    def table = empData.clone().addColumn("yearMonth", YearMonth, toYearMonths(empData["start_date"]))
    assertEquals(5, table.columnCount())
    assertEquals("yearMonth", table.columnNames()[table.columnCount() - 1])
    assertEquals(YearMonth, table.type("yearMonth"))
    assertEquals(YearMonth.of(2012, 1), table[0, 4])
    assertEquals(YearMonth.of(2015, 3), table[4, 4])
    assertIterableEquals([Integer, String, Number, LocalDate, YearMonth], table.types())

    // Append a new column to the end
    Matrix table2 = empData.clone()
    assertIterableEquals([Integer, String, Number, LocalDate], table2.types())
    table2["yearMonth", YearMonth] = toYearMonths(table2["start_date"])
    assertEquals(empData.columnCount() + 1, table2.columnCount())
    assertEquals("yearMonth", table2.columnNames()[table2.columnCount() - 1])
    assertEquals(YearMonth, table2.type("yearMonth"))
    assertEquals(YearMonth.of(2012, 1), table2[0, 4])
    assertEquals(YearMonth.of(2015, 3), table2[4, 4])

    // Insert a new column first
    Matrix table3 = empData.clone()
    table3["yearMonth", YearMonth, 0] = toYearMonths(table3["start_date"])
    assertEquals(empData.columnCount() + 1, table3.columnCount())
    assertEquals("yearMonth", table3.columnNames()[0])
    assertEquals(YearMonth, table3.type("yearMonth"))
    assertEquals(YearMonth.of(2012, 1), table3[0, 0])
    assertEquals(YearMonth.of(2015, 3), table3[4, 0])

    // Use property notation
    Matrix table4 = empData.clone()
    table4.yearMonth = toYearMonths(table4["start_date"])
    assertEquals(empData.columnCount() + 1, table3.columnCount())
    assertEquals("yearMonth", table3.columnNames()[0])
    assertEquals(YearMonth, table3.type("yearMonth"))
    assertEquals(YearMonth.of(2012, 1), table3[0, 0])
    assertEquals(YearMonth.of(2015, 3), table3[4, 0])

    Matrix table5 = empData.clone()
    table5 << [vacation: [10, 15, 12, 20, 30]]
    assertIterableEquals([10, 15, 12, 20, 30], table5.vacation)

    //table5.leftShift(table5['vacation'], 2)
    //table5 << (table5['vacation'], 2)

    def t5 = Matrix.builder()
        .columnNames('rv')
        .types(Integer)
        .columns([[5, 5, 1, 2, 22]])
        .build()
    table5 << t5
    assertIterableEquals([5, 5, 1, 2, 22], table5.rv)
  }

  @Test
  void testAddColumns() {
    def empData = Matrix.builder().columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2019-01-01", "2019-01-23", "2019-05-15", "2019-05-11", "2019-03-27"))
        .types([int, String, Number, LocalDate]).build()

    def empData2 = Matrix.builder().columns(
        foo: [0, 1, 0, 0, 2],
        bar: ["Truman", "Schwartz", "Bowman", "Lawson", "Carlson"])
        .types([int, String]).build()

    def empData3 = Matrix.builder().columns(
        baz: [8, 9, 12.1, 3, 4],
        qui: [1.0, 1.5, 2.0, 2.5, 3.0])
        .types(Number, Number).build()

    def empd = empData.clone()
    empd.addColumns(empData2)
    empd.addColumns(empData3)

    //println empd.content()

    empData.addColumn("yearMonth", YearMonth, toYearMonths(empData["start_date"]))
    assertEquals(YearMonth, empData[0, 4].class, "type of the added column")
    assertEquals(YearMonth, empData.type("yearMonth"), "claimed type of the added column")

    def counts = Stat.countBy(empData, "yearMonth").orderBy('yearMonth')
    assertEquals(YearMonth, counts[0, 0].class, "type of the count column")
    assertEquals(YearMonth, counts.type("yearMonth"), "claimed type of the count column")

    assertEquals(2, counts.subset('yearMonth', { it == YearMonth.of(2019, 5) })[0, 1])
    assertEquals(1, counts.subset('yearMonth', { it == YearMonth.of(2019, 3) })['yearMonth_count'][0])
    assertEquals(2, counts[0, 'yearMonth_count'])

    def sums = Stat.sumBy(empData, "salary", "yearMonth")
        .orderBy("yearMonth", true)
    assertEquals(YearMonth, sums[0, 0].class, "type of the sums column")
    assertEquals(YearMonth, sums.type("yearMonth"), "claimed type of the sums column")
    assertEquals(611.0 + 729.0, sums[0, 1], sums.content())
    assertEquals(843.25, sums[1, 1], sums.content())
    assertEquals(623.3 + 515.2, sums[2, 1], sums.content())

    def salaryPerYearMonth = counts
        .orderBy("yearMonth", true)
        << sums.salary

    assertEquals(asYearMonth("2019-05"), salaryPerYearMonth[0, 0], salaryPerYearMonth.content())
    assertEquals(611.0 + 729.0, salaryPerYearMonth[0, 2], salaryPerYearMonth.content())
    assertEquals(2, salaryPerYearMonth[0, 1], salaryPerYearMonth.content())
    assertEquals(843.25, salaryPerYearMonth[1, 2], salaryPerYearMonth.content())
    assertEquals(1, salaryPerYearMonth[1, 1], salaryPerYearMonth.content())

    Matrix m = Matrix.builder().data(id: [1, 2, 3]).build()
    m << [salary: [10000, 9979, 8980]]
    assertIterableEquals([10000, 9979, 8980], m.salary)

    Matrix n = Matrix.builder().data(years: [2, 4, 3], vacation: [12, 20, 30]).build()
    m << n
    assertEquals(4, m.columnCount())
    assertIterableEquals(['id', 'salary', 'years', 'vacation'], m.columnNames())

    m.years = [3, 4, 5]
    assertEquals(3, m[0, 'years'])

    m.foo = ['jabba', 'dabba', 'doo']
    assertEquals(['jabba', 'dabba', 'doo'], m.column('foo'))

    def e = empData.clone()
    e << empData2[0..1]
    e.addColumns(empData3, 0..1)
    def expected = empData.columnNames() + empData2.columnNames()[0..1] + empData3.columnNames()[0..1]
    assertIterableEquals(expected, e.columnNames())
    assertIterableEquals(empData3.qui, e.qui)
    assertIterableEquals(empData2.bar, e.bar)

    def a = empData.clone()
    a.addColumns(empData2, 'foo', 'bar')
    assertIterableEquals(empData.columnNames() + ['foo', 'bar'], a.columnNames())
    assertIterableEquals(empData2.foo, a.foo)
    assertIterableEquals(empData2.bar, a.bar)
  }

  @Test
  void testSort() {
    def empData = Matrix.builder().columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-01-01", "2014-11-15", "2014-05-11"))
        .types([Integer, String, Number, LocalDate])
        .build()
    def dateSorted = empData.orderBy("start_date")
    assertEquals(4, dateSorted[4, 0], "Last row should be the Ryan row: \n${dateSorted.content()}")
    assertEquals(asLocalDate("2012-03-27"), dateSorted[0, 3], "First row should be the Dan Row")

    //println(empData.content())
    def salarySorted = empData.orderBy(["salary": Matrix.DESC])
    assertEquals(843.25, salarySorted["salary"][0], "Highest salary: ${salarySorted.content()}")
    assertEquals(515.2, salarySorted["salary"][4], "Lowest salary: ${salarySorted.content()}")

    def dateSalarySorted = empData.orderBy(["start_date", "salary"])
    assertIterableEquals([2, 3, 1, 5, 4], dateSalarySorted['emp_id'], dateSalarySorted.content())
  }

  @Test
  void testDropColumns() {
    def empData = Matrix.builder().columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11"))
        .types(int, String, Number, LocalDate)
        .build()
    def noId = empData.clone().dropColumns('emp_id')
    assertEquals(3, noId.columnCount())
    assertIterableEquals(['emp_name', 'salary', 'start_date'], noId.columnNames())
    assertIterableEquals([String, Number, LocalDate], noId.types())

    def empList = empData.clone().dropColumns("salary", "start_date")
    //println(empList.content())
    assertEquals(2, empList.columnCount(), "Number of columns after drop")
    assertEquals(5, empList.rowCount(), "Number of rows after drop")
    assertIterableEquals(["emp_id", "emp_name"], empList.columnNames(), "column names after drop")
    assertIterableEquals([Integer, String], empList.types(), "Column types after drop")

    def empRange = empData.clone().dropColumns(2..3)
    assertEquals(empList, empRange, empList.diff(empRange))
  }

  @Test
  void testDropColumnsExcept() {
    def empData = Matrix.builder()
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11")
        )
        .types(int, String, Number, LocalDate)
        .build()
    def empList = empData.dropColumnsExcept("emp_id", "start_date")
    //println(empList.content())
    assertEquals(2, empList.columnCount(), "Number of columns after drop")
    assertEquals(5, empList.rowCount(), "Number of rows after drop")
    assertIterableEquals(["emp_id", "start_date"], empList.columnNames(), "column names after drop")
    assertIterableEquals([Integer, LocalDate], empList.types(), "Column types after drop")
  }

  @Test
  void testIteration() {
    def empData = Matrix.builder()
        .columns(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11"))
        .types([int, String, Number, LocalDate])
        .build()

    int i = 1
    empData.each { row ->
      assertEquals(i, row[0], String.valueOf(row))
      assertEquals(empData[i - 1, 'emp_name'], row[1], String.valueOf(row))
      i++
    }

    for (row in empData) {
      if (row[2] > 600) {
        row[2] = row[2] - 600
      }
    }

    assertEquals(23.3, empData[0, 2], empData.toMarkdown())
    assertEquals(515.2, empData[1, 2], empData.toMarkdown())
    assertEquals(11.0, empData[2, 2], empData.toMarkdown())
    assertEquals(129.0, empData[3, 2], empData.toMarkdown())
    assertEquals(243.25, empData[4, 2], empData.toMarkdown())
  }

  @Test
  void testMatrixToGrid() {
    def report = [
        "Full Funding"    : [4563.153, 380.263, 4.938, 12.23],
        "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
        "Current Funding" : [2700, 225, 2.922, 1.871]
    ]
    Matrix table = Matrix.builder().data(report).types([BigDecimal] * 3).build()

    Grid grid = table.grid()
    assertEquals(3.664, grid[2, 1] as BigDecimal)
    Grid<BigDecimal> typedGrid = table.grid(BigDecimal)
    assertEquals(new BigDecimal("380.263"), typedGrid[1, 0])

    var report2 = [
        "Full Funding"    : toDoubles(4563.153, 380.263, 4.938, 12.23),
        "Baseline Funding": ['3385.593', '282.133', '3.664', '2.654'],
        "Current Funding" : [2700, 225, 2.922, 1.871]
    ]
    table = Matrix.builder().data(report2).types([Double, String, BigDecimal]).build()
    Grid<BigDecimal> tg2 = table.grid(BigDecimal)
    assertEquals(new BigDecimal("12.23"), tg2[3, 0])
    // no explicit conversion so this should fail since BigDecimal cannot equal a String
    assertNotEquals(new BigDecimal("3.664"), tg2[2, 1])
    assertEquals(new BigDecimal("380.263"), tg2[1, 0])
    // explicit conversion, everything should check out
    Grid<BigDecimal> tg3 = table.grid(BigDecimal, true)
    assertEquals(new BigDecimal("12.23"), tg3[3, 0])
    assertEquals(new BigDecimal("3.664"), tg3[2, 1])
    assertEquals(new BigDecimal("380.263"), tg3[1, 0])
  }

  @Test
  void testSelectColumns() {
    def report = [
        "Full Funding"   : [4563.153, 380.263, 4.938, 12.23],
        "Base Funding"   : [3385.593, 282.133, 3.664, 2.654],
        "Current Funding": [2700, 225, 2.922, 1.871]
    ]
    Matrix table = Matrix.builder().data(report).types([BigDecimal] * 3).build()

    Matrix m = table.selectColumns("Base Funding", "Full Funding")

    assertEquals(3385.593, m[0, 0])
    assertEquals(12.23, m[3, 1])

    m.moveColumn("Base Funding", 1)
    Matrix m2 = table.selectColumns(0..1)
    assertEquals(m, m2, m.diff(m2))
  }

  @Test
  void testRenameColumns() {
    def empData = Matrix.builder().data(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11"))
        .types([int, String, Number, LocalDate])
        .build()

    empData.renameColumn('emp_id', 'id')
    empData.renameColumn(1, 'name')

    assertEquals('id', empData.columnNames()[0])
    assertEquals('name', empData.columnNames()[1])

  }

  @Test
  void testToMarkdown() {
    def report = [
        "YearMonth"       : toYearMonths(['2023-01', '2023-02', '2023-03', '2023-04']),
        "Full Funding"    : [4563.153, 380.263, 4.938, 12.23],
        "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
        "Current Funding" : [2700, 225, 2.922, 1.871]
    ]
    Matrix table = Matrix.builder().data(report).types(YearMonth, BigDecimal, BigDecimal, BigDecimal).build()

    def md = table.toMarkdown()
    def rows = md.split('\n')
    assertEquals(6, rows.length)
    assertEquals('| YearMonth | Full Funding | Baseline Funding | Current Funding |', rows[0])
    assertEquals('| --- | ---: | ---: | ---: |', rows[1])
    assertEquals('| 2023-01 | 4563.153 | 3385.593 | 2700 |', rows[2])
    assertEquals('| 2023-04 | 12.23 | 2.654 | 1.871 |', rows[5])

    md = table.toMarkdown(Map.of("class", "table"))
    rows = md.split('\n')
    assertEquals(7, rows.length)
    assertEquals('{class="table" }', rows[6])

    md = table.toMarkdown(2)
    rows = md.split('\n')
    assertEquals(4, rows.length)
    assertEquals('| YearMonth | Full Funding | Baseline Funding | Current Funding |', rows[0])
    assertEquals('| --- | ---: | ---: | ---: |', rows[1])
    assertEquals('| 2023-01 | 4563.153 | 3385.593 | 2700 |', rows[2])
    assertEquals('| 2023-02 | 380.263 | 282.133 | 225 |', rows[3])

    md = table.toMarkdown(2, false)
    rows = md.split('\n')
    assertEquals(4, rows.length)
    assertEquals(4, rows.length)
    assertEquals('| YearMonth | Full Funding | Baseline Funding | Current Funding |', rows[0])
    assertEquals('| --- | ---: | ---: | ---: |', rows[1])
    assertEquals('| 2023-03 | 4.938 | 3.664 | 2.922 |', rows[2])
    assertEquals('| 2023-04 | 12.23 | 2.654 | 1.871 |', rows[3])

    assert table.subset(0..1).toMarkdown() == table.toMarkdown(2)
    assert table.subset(2..3).toMarkdown() == table.toMarkdown(2, false)
  }

  @Test
  void testToHtml() {
    def empData = Matrix.builder()
        .columns(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11"))
        .types([int, String, Number, LocalDate])
        .build()
    String html = empData.toHtml(id: 'mytable', class: 'table', align: 'emp_id: left, salary: right, start_date: center')
    assertTrue(html.contains(' id="mytable"'), 'table should have the id attribute set')
    assertTrue(html.contains("<th class='salary Number' style='text-align: right'>salary</th>"), 'table header for salary should be right aligned but was ' + html)
    assertTrue(html.contains("<td class='salary Number' style='text-align: right'>623.3</td>"), 'table row for salary should be right aligned but was ' + html)

    // no alignment so it should be auto aligned
    html = empData.toHtml(["class": "table"])
    assertTrue(html.contains(' class="table"'), 'table should have the class attribute set')
    assertTrue(html.contains("<th class='salary Number' style='text-align: right'>salary</th>"), 'table header for salary should be right aligned but was ' + html)
    assertTrue(html.contains("<td class='salary Number' style='text-align: right'>623.3</td>"), 'table row for salary should be right aligned but was ' + html)

  }

  @Test
  void testEquals() {
    def empData = Matrix.builder()
        .data(
            emp_id: [1, 2],
            emp_name: ["Rick", "Dan"],
            salary: [623.3, 515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types(int, String, Number, LocalDate)
        .build()

    assertEquals(empData, Matrix.builder()
        .data(
            emp_id: [1, 2],
            emp_name: ["Rick", "Dan"],
            salary: [623.3, 515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types(int, String, Number, LocalDate)
        .build())

    assertNotEquals(empData, Matrix.builder().columns(
        emp_id: [1, 2],
        emp_name: ["Rick", "Dan"],
        salary: [623.3, 515.1],
        start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types([int, String, Number, LocalDate]).build()
    )

    Matrix differentTypes = Matrix.builder()
        .columns(
            emp_id: [1, 2],
            emp_name: ["Rick", "Dan"],
            salary: [623.3, 515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types([Object] * 4)
        .build()
    assertTrue(empData.equals(differentTypes, false, true, true), empData.diff(differentTypes))
    assertNotEquals(empData, differentTypes.withMatrixName("differentTypes"), empData.diff(differentTypes))
  }

  @Test
  void testDiff() {
    def empData = Matrix.builder().data(
        emp_id: [1, 2],
        emp_name: ["Rick", "Dan"],
        salary: [623.3, 515.2],
        start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types(int, String, Number, LocalDate).build()

    def d1 = Matrix.builder().data(
        emp_id: [1, 2],
        emp_name: ["Rick", "Dan"],
        salary: [623.3, 515.1],
        start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types([int, String, Number, LocalDate]).build()
    assertEquals('Row 1 differs: \n\tthis: 2, Dan, 515.2, 2012-03-27 \n\tthat: 2, Dan, 515.1, 2012-03-27',
        empData.diff(d1).trim())

    def d2 = Matrix.builder().data(
        emp_id: [1, 2],
        emp_name: ["Rick", "Dan"],
        salary: [623.3, 515.2],
        start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types([Object] * 4).build()
    assertEquals('Column types differ: \n\tthis: Integer, String, Number, LocalDate \n\tthat: Object, Object, Object, Object',
        empData.diff(d2))
  }

  @Test
  void removeRows() {
    def report = [
        "YearMonth"       : toYearMonths(['2023-01', '2023-02', '2023-03', '2023-04']),
        "Full Funding"    : [4563.153, 380.263, 4.938, 12.23],
        "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
        "Current Funding" : [2700, 225, 2.922, 1.871]
    ]
    Matrix table = Matrix.builder()
        .data(report)
        .types(YearMonth, BigDecimal, BigDecimal, BigDecimal)
        .build()
    table.removeRows([0, 2])
    assertEquals(2, table.rowCount())
    assertIterableEquals([asYearMonth('2023-02'), 380.263, 282.133, 225], table.row(0))
    assertIterableEquals([asYearMonth('2023-04'), 12.23, 2.654, 1.871], table.row(1))
  }

  @Test
  void testRemoveEmptyRows() {
    def empData = Matrix.builder()
        .data(
            emp_id: [1, 2],
            emp_name: ["Rick", "Dan"],
            salary: [623.3, 515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"))
        .types([int, String, Number, LocalDate])
        .build()

    def d0 = Matrix.builder()
        .data(
            emp_id: [1, null, 2, null],
            emp_name: ["Rick", "", "Dan", " "],
            salary: [623.3, null, 515.2, null],
            start_date: toLocalDates("2013-01-01", null, "2012-03-27", null))
        .types([int, String, Number, LocalDate])
        .build()

    def row = d0.row(1)
    assertEquals(0, row['salary', Double, 0])
    def d0r = d0.removeEmptyRows()
    assertEquals(empData, d0r, empData.diff(d0r, true))

    def m = Matrix.builder().rows([
        [null, 'foo', 'bar', null],
        [null, 1, 2, null],
        [null, null, null, null],
        [0, 1, 2, null]
    ]).build()
    m.removeEmptyRows()
    assertEquals(3, m.rowCount())
    assertNull(m[0, 0])
    assertEquals('foo', m[0, 1])
    assertEquals('bar', m[0, 2])
    assertNull(m[0, 3])
    assertIterableEquals([0, 1, 2], m.row(2)[0..2])
    assertNull(m[2, 3])
    m.removeEmptyColumns()
    assertEquals(3, m.rowCount())
    assertEquals(3, m.columnCount())
  }

  @Test
  void testRemoveEmptyColumns() {
    def empData = Matrix.builder()
        .columns(
            emp_id: [1, 2],
            emp_name: [null, null],
            salary: [623.3, 515.2],
            start_date: [null, null],
            other: [null, null])
        .types(int, String, Number, LocalDate, String)
        .build()
    assertIterableEquals(['emp_id', 'emp_name', 'salary', 'start_date', 'other'], empData.columnNames())
    empData.removeEmptyColumns()
    assertEquals(2, empData.columnCount())
    assertIterableEquals(['emp_id', 'salary'], empData.columnNames())
  }

  boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles()
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file)
      }
    }
    return directoryToBeDeleted.delete()
  }

  @Test
  void testWithColumns() {
    def table = Matrix.builder().data([
        a: [1, 2, 3, 4, 5],
        b: [1.2, 2.3, 0.7, 1.3, 1.9]
    ]).types([Integer, BigDecimal]).build()

    def aPlus = table.withColumn('a') { a ->
      a + 1
    }
    assertEquals([2, 3, 4, 5, 6], aPlus)

    def firstPlus = table.withColumn(0) { a ->
      a + 1
    }
    assertEquals([2, 3, 4, 5, 6], firstPlus)

    def m = table.withColumns(['a', 'b']) { x, y -> x - y }
    assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], m)

    def n = table.withColumns([0, 1] as Integer[]) { x, y -> x - y }
    assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], n)

    def o = table.withColumns(0..1) { x, y -> x - y }
    assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], o)
  }

  @Test
  void testPivot() {
    def m = Matrix.builder('deposits').data(
        customerId: [1, 1, 2, 2, 2, 3],
        amount: [100, 110, 100, 110, 120, 100],
        currency: ['SEK', 'DKK', 'SEK', 'USD', 'EUR', 'SEK'],
        name: ['Per', 'Per', 'Ian', 'Ian', 'Ian', 'John']
    ).types(int, int, String, String)
        .build()

    def pm = m.pivot('customerId', 'currency', 'amount')
    /*
     customerId	name	SEK	 DKK	 USD	 EUR
             1	Per 	100	 110	null	null
             2	Ian 	100	null	 110	 120
             3	John	100	null	null	null
     */
    assertEquals(3, pm.rowCount())
    assertEquals(6, pm.columnCount())
    assertEquals(100, pm[0, 'SEK'])
    assertEquals(110, pm[0, 'DKK'])
    assertNull(pm[0, 'USD'])
    assertNull(pm[0, 'EUR'])
    assertEquals(100, pm[1, 'SEK'])
    assertEquals(110, pm[1, 'USD'])
    assertEquals(120, pm[1, 'EUR'])
    assertNull(pm[1, 'DKK'])
    assertEquals(100, pm[2, 'SEK'])
    assertNull(pm[2, 'USD'])
    assertNull(pm[2, 'EUR'])
    assertNull(pm[2, 'DKK'])
  }

  @Test
  void testUnPivot() {
    Matrix orgMatrix = Matrix.builder('deposits').data(
        customerId: [1, 2, 3],
        name: ['Per', 'Ian', 'John'],
        SEK: [100, 100, 100],
        DKK: [110, null, null],
        USD: [null, 110, null],
        EUR: [null, 120, null]
    )
        .types(int, String, int, int, int, int)
        .build()
    Matrix unPivotedMatrix = orgMatrix.unPivot('amount', 'currency', ['SEK', 'DKK', 'USD', 'EUR'])
    //println unPivotedMatrix.content()
    /* customerId	name	amount	currency
              1	  Per	     100	SEK
              1	  Per	     110	DKK
              2	  Ian	     100	SEK
              2	  Ian	     110	USD
              2	  Ian	     120	EUR
              3	  John	   100	SEK
     */
    assertEquals(6, unPivotedMatrix.rowCount())
    assertEquals(4, unPivotedMatrix.columnCount())
    assertIterableEquals([1, 'Per', 100, 'SEK'], unPivotedMatrix.row(0))
    assertIterableEquals([1, 'Per', 110, 'DKK'], unPivotedMatrix.row(1))
    assertIterableEquals([2, 'Ian', 100, 'SEK'], unPivotedMatrix.row(2))
    assertIterableEquals([2, 'Ian', 110, 'USD'], unPivotedMatrix.row(3))
    assertIterableEquals([2, 'Ian', 120, 'EUR'], unPivotedMatrix.row(4))
    assertIterableEquals([3, 'John', 100, 'SEK'], unPivotedMatrix.row(5))
  }

  @Test
  void testPopulateColumn() {
    Matrix components = Matrix.builder().data([
        id  : [1, 2, 3, 4, 5],
        size: [1.2, 2.3, 0.7, 1.3, 1.9]
    ]).types([Integer, BigDecimal]).build()
    components['id'] = [10, 11, 12, 13, 14]
    assertEquals(10, components[0, 'id'])
    assertEquals(13, components[3, 'id'])
    assertEquals(14, components[4, 'id'])
    components.id = [10, 11, 12, 13, 14]
    assertEquals(10, components[0, 'id'])
    assertEquals(13, components[3, 'id'])
    assertEquals(14, components[4, 'id'])
    components.identity = [10, 11, 12, 13, 14]
    assertEquals(components.id[0], components[0, 'identity'])
    assertEquals(components.identity[3], components[3, 'id'])
    assertEquals(components.id[4], components.identity[4])
  }

  @Test
  void testMoveRow() {
    def table = Matrix.builder()
        .columns([
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo'      : [1, 2, 3]
        ])
        .types(String, LocalDate, int)
        .build()

    table.moveRow(2, 0)
    assertIterableEquals(['Lotte', asLocalDate('2023-05-27'), 3], table.row(0))
    assertIterableEquals(['Lorena', asLocalDate('2021-12-01'), 1], table.row(1))
    assertIterableEquals(['Marianne', asLocalDate('2022-07-10'), 2], table.row(2))
  }

  @Test
  void testMoveColumn() {
    def table = Matrix.builder()
        .columns([
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo'      : [1, 2, 3]
        ])
        .types(String, LocalDate, int)
        .build()

    table.moveColumn('foo', 0)
    assertIterableEquals(['foo', 'firstname', 'start'], table.columnNames())
    assertIterableEquals([1, 2, 3], table[0])
    assertIterableEquals([Integer, String, LocalDate], table.types())
  }

  @Test
  void testPutAt() {
    // putAt(String columnName, Class<?> type, Integer index = null, List<?> column)
    def table = Matrix.builder()
        .data([
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo'      : [1, 2, 3]
        ])
        .types(String, LocalDate, int)
        .build()
    table["yearMonth", YearMonth, 0] = toYearMonths(table["start"])
    assertEquals(4, table.columnCount())
    assertIterableEquals(['yearMonth', 'firstname', 'start', 'foo'], table.columnNames())
    assertIterableEquals(toYearMonths(['2021-12', '2022-07', '2023-05']), table[0])

    // putAt(List where, List<?> column)
    table = Matrix.builder()
        .data([
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start'    : toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo'      : [1, 2, 3]
        ])
        .types(String, LocalDate, int)
        .build()
    table["start"] = table["start"].collect { it.plusDays(10) }
    assertEquals(3, table.columnCount())
    assertIterableEquals(['firstname', 'start', 'foo'], table.columnNames())
    // getAt and putAt should have the same semantics when referring to columns:
    assertIterableEquals(toLocalDates(['2021-12-11', '2022-07-20', '2023-06-06']), table[1])
    assertIterableEquals(table.column(2), table[2])
    assertIterableEquals(table.column("foo"), table["foo"])

    Matrix m = Matrix.builder()
        .columnNames(table.columnNames())
        .types(table.types())
        .build()

    m[0..1] = table[0..1]
    m[2] = table[2]
    assertEquals(table, m, table.diff(m))

    m[2, 2] = 4
    m[0, 'firstname'] = 'Larry'
    m["yearMonth", YearMonth] = toYearMonths(m["start"])

    // Verify that we can also assign a value to the next element in the column
    m[3, 0] = 'Sven'
    m[3, 'start'] = asLocalDate('2024-01-10')
    m[3, 2] = 5
    m[3, 'yearMonth'] = asYearMonth('2024-01')
    assertIterableEquals(['firstname', 'start', 'foo', 'yearMonth'], m.columnNames())
    assertIterableEquals(['Larry', asLocalDate('2021-12-11'), 1, asYearMonth('2021-12')], m.row(0))
    assertIterableEquals(['Marianne', asLocalDate('2022-07-20'), 2, asYearMonth('2022-07')], m.row(1))
    assertIterableEquals(['Lotte', asLocalDate('2023-06-06'), 4, asYearMonth('2023-06')], m.row(2))
    assertIterableEquals(['Sven', asLocalDate('2024-01-10'), 5, asYearMonth('2024-01')], m.row(3))

    // verify that we can assign a null value
    m[1, 2] = null
    assertNull(m[1, 2], m.content())
    m[1, 'firstname'] = null
    assertNull(m[1, 'firstname'], m.content())
  }

  @Test
  void testGetAt() {
    def table = Matrix.builder()
        .columns(
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo': [1, 2, 3])
        .types(String, LocalDate, int)
        .build()

    assertEquals(Integer, table.getAt(2, 2).class)
    assertEquals(3, table.getAt(2, 2))

    assertEquals(Integer, table[2, 2].class)
    assertEquals(3, table[2, 2])
    assertEquals(3, table.foo[2], "Get column property did not work")


    assertEquals(LocalDate, table[2, 'start'].class)
    assertEquals(LocalDate, table.start[2].class)
    assertEquals(asLocalDate('2023-05-27'), table[2, 'start'])
    assertEquals(asLocalDate('2023-05-27'), table.start[2])

    assertEquals(asLocalDate('2023-05-27'), table.getAt(2, 'start'))
    assertEquals(LocalDate, table.getAt(2, 'start').class)


    Row row = table.row(1)
    assertEquals(LocalDate, row.getAt('start').class)
    assertEquals(LocalDate, row[1].class)
    assertEquals(LocalDate, row['start'].class)
    assertEquals(LocalDate, row.start.class)
    assertEquals(LocalDate.parse('2022-07-10'), row[1])
    assertEquals(LocalDate.parse('2022-07-10'), row['start'])
    assertEquals(LocalDate.parse('2022-07-10'), row.start)

    assertEquals(String, table[0, 1, String].class)
    assertEquals('2021-12-01', table[0, 1, String])

    assertEquals(String, table[0, 'foo', String].class)
    assertEquals('3', table[2, 'foo', String])

    assertEquals(2 as BigDecimal, row['foo', BigDecimal])
    assertEquals(2 as BigDecimal, row.getAt('foo', BigDecimal))
    assertEquals('2', row['foo', String])
    assertEquals(2 as BigDecimal, row[2, BigDecimal])
    assertEquals('2', row[2, String])
    assertEquals('2', row.getAt(2, String))

    assertIterableEquals([asLocalDate('2021-12-01'), 1], table[0, 1..2], "column intRange")
    assertIterableEquals(['Marianne', 'Lotte'], table[1..2, 0], "row intRange")
    table[3, 2] = Stat.sum(table[0..2, 2])
    assertEquals(6, table[3, 2])
    assertIterableEquals([1, 2, 3], table.getAt(0..2, 2))
  }

  @Test
  void testColumnArithmetics() {
    Matrix m = Matrix.builder().columns(
        [id     : [1, 2, 3, 4],
         balance: [10000, 1000.23, 20122, 12.1]
        ]
    ).types(int, double)
        .build()

    Matrix r = Matrix.builder().columns(
        [id: [1, 2, 3, 4],
         ir: [0.041, 0.020, 0.035, 0.5]
        ])
        .types(int, double)
        .build()

    // verify individual parts
    def interest = [10000 * 0.041, 1000.23 * 0.020, 20122 * 0.035, 12.1 * 0.5]
    assert interest == m.balance * r.ir

    def balanceSum = 10000 + 1000.23 + 20122 + 12.1
    assert balanceSum == m.balance.sum()

    def weighted = interest.sum() / balanceSum
    assert weighted == 0.0366259560

    // now we arrive at the nice, compound syntax:
    assert weighted == (m.balance * r.ir).sum() / m.balance.sum()

    // verify that single number multiplication works
    assert [10000 * 0.05, 1000.23 * 0.05, 20122 * 0.05, 12.1 * 0.05] == m.balance * 0.05
  }

  @Test
  void testNamesForType() {
    def empData = Matrix.builder().data(
        emp_id: 1..3,
        name: ["Rick", "Dan", "Michelle"],
        salary: [623.3, 515.2, 611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
        .types([int, String, Number, LocalDate])
        .build()
    assertIterableEquals(['start_date'], empData.columnNames(TemporalAccessor))
    assertIterableEquals(['emp_id', 'salary'], empData.columnNames(Number))
    assertIterableEquals([], empData.columnNames(float))
  }

  @Test
  void testColumnIndexFuzzy() {
    def empData = Matrix.builder().data(
        emp_id: 1..3,
        name: ["Rick", "Dan", "Michelle"],
        salary: [623.3, 515.2, 611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
        .types([int, String, Number, LocalDate])
        .build()

    assertEquals(0, empData.columnIndexFuzzy('id'))
    assertEquals(3, empData.columnIndexFuzzy('start'))
    assertEquals(1, empData.columnIndexFuzzy('nam'))
    assertEquals(-1, empData.columnIndexFuzzy('am'))
  }

  @Test
  void testCast() {
    def empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        )
        .types([int, String, Number, LocalDate])
        .build()
    List list = empData as List
    assertEquals(5, list.size())
    assertEquals([1, "Rick", 623.3, asLocalDate("2012-01-01")], list[0])
    Map map = empData as Map
    assertEquals(4, map.size())
    assertEquals([623.3, 515.2, 611.0, 729.0, 843.25], map.salary)
    Grid grid = empData as Grid
    assertEquals(5, grid.dimensions().observations)
    assertEquals(4, grid.dimensions().variables)
  }
}
