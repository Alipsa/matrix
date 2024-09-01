import se.alipsa.groovy.matrix.Converter
import se.alipsa.groovy.matrix.Grid
import se.alipsa.groovy.matrix.Row
import se.alipsa.groovy.matrix.Stat
import se.alipsa.groovy.matrix.Matrix

import groovy.sql.Sql

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

import static se.alipsa.groovy.matrix.ListConverter.*
import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.*

import static se.alipsa.groovy.matrix.ValueConverter.asLocalDate
import static se.alipsa.groovy.matrix.ValueConverter.asYearMonth


class MatrixTest {

    @Test
    void testMatrixConstructors() {
        def empData = new Matrix('empData',
                [
                emp_id: 1..5,
                emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
                salary: [623.3,515.2,611.0,729.0,843.25],
                start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
                ],
                [int, String, Number, LocalDate]
        )
        assertEquals('empData', empData.getName())
        assertEquals(1, empData[0,0])
        assertEquals("Dan", empData[1,1])
        assertEquals(611.0, empData[2,2])
        assertEquals(LocalDate.of(2015, 3, 27), empData[4,3])
        assertIterableEquals([Integer, String, Number, LocalDate], empData.columnTypes())

        def dims = empData.dimensions()
        assertEquals(5, dims.observations)
        assertEquals(4, dims.variables)

        def ed = new Matrix("ed",
                ['id', 'name', 'salary', 'start'], [
                    1..5,
                    ["Rick","Dan","Michelle","Ryan","Gary"],
                    [623.3,515.2,611.0,729.0,843.25],
                    toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
                ]
        )
        assertEquals('ed', ed.getName())
        assertEquals(1, ed[0,0])
        assertEquals("Dan", ed[1,1])
        assertEquals(611.0, ed[2,2])
        assertEquals(LocalDate.of(2015, 3, 27), ed[4,3])
        assertIterableEquals([Object] * 4, ed.columnTypes())

        def e = new Matrix([
                1..5,
                ["Rick","Dan","Michelle","Ryan","Gary"],
                [623.3,515.2,611.0,729.0,843.25],
                toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
            ])
        assertNull(e.getName())
        assertEquals(1, e[0,0])
        assertEquals("Dan", e[1,1])
        assertEquals(611.0, e[2,2])
        assertEquals(LocalDate.of(2015, 3, 27), e[4,3])
        assertIterableEquals([Object] *4 , ed.columnTypes())
    }

    @Test
    void testTableCreationFromMatrix() {
        def employees = []
        employees << ['John Doe', 21000, asLocalDate('2013-11-01'), asLocalDate('2020-01-10')]
        employees << ['Peter Smith', 23400,	'2018-03-25',	'2020-04-12']
        employees << ['Jane Doe', 26800, asLocalDate('2017-03-14'), asLocalDate('2020-10-02')]

        def table = Matrix.create(employees)
        assertEquals('John Doe', table[0,0])
        assertEquals(23400, table[1,1] as Integer)
        assertEquals(LocalDate.of(2017, 3, 14), table[2,2])
    }

    @Test
    void testAddRow() {
        Matrix m = new Matrix("years", (1..5).collect{"Y" + it})
        m.addRow([1,2,3,4,5])
        m << [10,20,30,40,50]
        m.addRow(0, m.columnNames())
        assertIterableEquals(['Y1',	'Y2',	'Y3',	'Y4',	'Y5'], m.columnNames())
        assertIterableEquals(['Y1',	'Y2',	'Y3',	'Y4',	'Y5'], m.row(0))
        assertIterableEquals([1,2,3,4,5], m.row(1))
        assertIterableEquals([10,20,30,40,50], m.row(2))
    }

    @Test
    void testTransposing() {
        def report = [
                "Year": [1, 2, 3, 4],
                "Full Funding": [4563.153, 380.263, 4.938, 101.1],
                "Baseline Funding": [3385.593, 282.133, 3.664, 123.123],
                "Current Funding": [2700, 225, 2.922, 1010.12]
        ]
        def table = new Matrix(report)
        def tr = table.transpose(['y1', 'y2', 'y3', 'y4'])
        assertEquals(["y1", "y2", "y3", "y4"], tr.columnNames())
        assertEquals([
            [1, 2, 3, 4],
            [4563.153, 380.263, 4.938, 101.1],
            [3385.593, 282.133, 3.664, 123.123],
            [2700, 225, 2.922, 1010.12]
        ], tr.rows(), table.content())
        assertEquals(4, tr.columnTypes().size(), "Column types")

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
        assertEquals(5, tr2.columnTypes().size(), tr2.content() + "\nColumn types: " + tr2.columnTypeNames())

        def t3 = table.transpose('Year', true)
        assertEquals([
            ["Year", 1, 2, 3, 4],
            ["Full Funding", 4563.153, 380.263, 4.938, 101.1],
            ["Baseline Funding", 3385.593, 282.133, 3.664, 123.123],
            ["Current Funding", 2700, 225, 2.922, 1010.12]
        ], t3.rows(), t3.content())
        assertEquals(['', '1', '2', '3', '4'], t3.columnNames())
        assertEquals(5, t3.columnTypes().size(), t3.content() + "\nColumn types: " + t3.columnTypeNames())

        def t4 = table.transpose('Year', [String, Number, Number, Number, Number], true)
        assertEquals([
                ["Year", 1, 2, 3, 4],
                ["Full Funding", 4563.153, 380.263, 4.938, 101.1],
                ["Baseline Funding", 3385.593, 282.133, 3.664, 123.123],
                ["Current Funding", 2700, 225, 2.922, 1010.12]
        ], t4.rows(), t4.content())
        assertEquals(['', '1', '2', '3', '4'], t4.columnNames())
        assertEquals(5, t4.columnTypes().size(), t4.content() + "\nColumn types: " + t4.columnTypeNames())

    }

    @Test
    void testStr() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
            [int, String, Number, LocalDate]
        )
        def struct = Stat.str(empData)
        assertEquals(['5 observations of 4 variables'], struct['Matrix'])
        assertIterableEquals(['Integer', '1', '2', '3', '4'], struct['emp_id'])
        assertIterableEquals(['LocalDate', '2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11'], struct['start_date'])
    }

    @Test
    void testCsv() {
        def data = [
            ['place', 'firstname', 'lastname', 'team'],
            ['1', 'Lorena', 'Wiebes', 'Team DSM'],
            ['2', 'Marianne', 'Vos', 'Team Jumbo Visma'],
            ['3', 'Lotte', 'Kopecky', 'Team SD Worx']
        ]
        def file = File.createTempFile('FemmesStage1Podium', '.csv')
        file.text = data*.join(',').join('\n')

        def table = Matrix.create(file)
        assertIterableEquals(data[0], table.columnNames())
        assertEquals(data[1][1], table[0, 1] as String)
        assertEquals('Team SD Worx', table[2, 3])

        def plantGrowth = Matrix.create(
                getClass().getResource('/PlantGrowth.csv'),
                ',',
                '"',
        )
        assertEquals('PlantGrowth', plantGrowth.name)
        assertIterableEquals(['id', 'weight','group'], plantGrowth.columnNames())
        def row30 = plantGrowth.findFirstRow('id', '30')
        assertEquals('5.26', row30[1])
        assertEquals('trt2', row30[2])
    }

    @Test
    void testConvert() {
        def data = [
            'place': ['1', '2', '3', ','],
            'firstname': ['Lorena', 'Marianne', 'Lotte', 'Chris'],
            'start': ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10'],
            'end': ['2022-12-01 10:00:00', '2023-07-10 00:01:00', '2024-05-27 00:00:30', '2042-01-10 00:00:00']
        ]
        def table = new Matrix(data, [String]*4)

        def table2 = table.convert(place: Integer, start: LocalDate)
        table2 = table2.convert([end: LocalDateTime],
                DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
        assertEquals(Integer, table2.columnType('place'))
        assertEquals(Integer, table2[0, 0].class)

        assertEquals(LocalDate, table2.columnType('start'))
        assertEquals(LocalDate, table2[0, 2].class)
        assertEquals(LocalDateTime.parse('2022-12-01T10:00:00.000'), table2['end'][0])

        def table3 = table.convert('place', Integer, {
            String val = String.valueOf(it).trim()
            if (val == 'null' || val == ',' || val.isBlank()) return null
            return Integer.valueOf(val)
        })
        assertEquals(Integer, table3.columnType('place'))
        assertEquals(3, table3['place'][2])

        def table4 = table.convert([
            new Converter('place', Integer, {try {Integer.parseInt(it)} catch (NumberFormatException e) {null}}),
            new Converter('start', LocalDate, {LocalDate.parse(it)})
        ] as Converter[])

        //println table.content()
        //println table4.content()
        assertEquals(Integer, table4.columnType('place'))
        assertEquals(Integer, table4[0, 0].class)
        assertEquals(3, table4[2, 0])

        assertEquals(LocalDate, table4.columnType('start'))
        assertEquals(LocalDate, table4[0, 2].class)
        assertEquals(LocalDate.of(2023, 5, 27), table4[2, 2])

        def table5 = table.convert(
            [Integer, String, LocalDate, String]
        )
        assertEquals(table4, table5, table4.diff(table5))
    }

    @Test
    void testGetRowsForCriteria() {
        def data = [
            'place': [1, 2, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': ['2021-12-01', '2022-07-10', '2023-05-27']
        ]
        def table = new Matrix(data, [int, String, String])
        def rows = table.rows(table['place'].findIndexValues { it > 1 })
        assertEquals(2, rows.size())

        // Same thing using subset
        def subSet = table.subset('place', { it > 1 })
        assertIterableEquals(table.rows(1..2), subSet.rows())

        def subSet2 = table.subset {it[0] > 1}
        assertIterableEquals(table.rows(1..2), subSet2.rows())

        def subSet3 = table.subset {
            String name = it[1]
            !name.startsWith('Ma')
                && asLocalDate(it[2]).isBefore(LocalDate.of(2022,10,1))
        }
        assertEquals(table[0, 1], subSet3[0,1])
    }

    @Test
    void testHeadAndTail() {
        def table = new Matrix([
                'place': [1, 20, 3],
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': ['2021-12-01', '2022-07-10', '2023-05-27']
            ],
            [int, String, String]
        )
        def head = table.head(1, false)
        assertEquals(' 1\tLorena  \t2021-12-01\n', head, head)
        def tail = table.tail(2, false)
        assertEquals('20\tMarianne\t2022-07-10\n 3\tLotte   \t2023-05-27\n', tail, tail)
        String[] content = table.content(includeHeader: false, maxColumnLength:7).split('\n')
        assertEquals(' 1\tLorena \t2021-12', content[0])

    }

    @SuppressWarnings('SqlNoDataSourceInspection')
    @Test
    void testCreateFromDb() {
        def dbDriver = "org.h2.Driver"
        def dbFileName = System.getProperty("java.io.tmpdir") + "/testdb"
        def dbUrl = "jdbc:h2:file:" + dbFileName
        def dbUser = "sa"
        def dbPasswd = "123"

        File dbFile = new File(dbFileName + ".mv.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
        File dbTraceFile = new File(dbFileName + "trace.db")
        if (dbTraceFile.exists()) {
            dbTraceFile.delete()
        }

        Sql.withInstance(dbUrl, dbUser, dbPasswd, dbDriver) { sql ->
            sql.execute '''
                create table IF NOT EXISTS PROJECT  (
                    id integer not null primary key,
                    name varchar(50),
                    url varchar(100)
                )
            '''
            sql.execute('delete from PROJECT')
            sql.execute 'insert into PROJECT (id, name, url) values (?, ?, ?)', [10, 'Groovy', 'http://groovy.codehaus.org']
            sql.execute 'insert into PROJECT (id, name, url) values (?, ?, ?)', [20, 'Alipsa', 'http://www.alipsa.se']
        }

        def project = null
        Sql.withInstance(dbUrl, dbUser, dbPasswd, dbDriver) { sql ->
            sql.query('SELECT * FROM PROJECT') { rs -> project = Matrix.create(rs) }
        }

        assertEquals(2, project.rowCount())
    }

    @Test
    void testSelectRows() {
        def data = [
            'place': [1, 2, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
        ]
        def table = new Matrix(data, [int, String, LocalDate])
        def selection = table.selectRowIndices {
            return it[2, LocalDate].isAfter(LocalDate.of(2022,1, 1))
        }
        assertIterableEquals([1,2], selection)

        def rows = table.rows {
            return it[2].isAfter(LocalDate.of(2022,10, 1))
        }
        assertIterableEquals([[3, 'Lotte', LocalDate.of(2023, 5, 27)]], rows)
    }

    @Test
    void testApply() {
        def data = [
            'place': ['1', '2', '3', ','],
            'firstname': ['Lorena', 'Marianne', 'Lotte', 'Chris'],
            'start': ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10'],
        ]
        def table = new Matrix(data)
            .convert(place: int, start: LocalDate)
        def table2 = table.apply("start", { startDate ->
            startDate.plusDays(10)
        })
        assertEquals(LocalDate.of(2021, 12, 11), table2["start"][0])
        assertEquals(LocalDate.of(2022, 7, 20), table2["start"][1])
        assertEquals(LocalDate.of(2023, 6, 6), table2["start"][2])
        assertEquals(LocalDate.of(2023, 1, 20), table2["start"][3])
        assertEquals(LocalDate, table2.columnType("start"))
    }

    @Test
    void testApplyChangeType() {
        def data = [
            'foo': [1, 2, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
        ]

        def table = new Matrix(data, [int, String, LocalDate])

        def foo = table.apply("start", { asYearMonth(it)})
        assertEquals(YearMonth.of(2021,12), foo[0, 2])
        assertEquals(YearMonth, foo.columnType("start"))
    }

    @Test
    void testSelectRowsAndApply() {
        def data = [
            'place': [1, 2, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
        ]
        def table = new Matrix(data, [int, String, LocalDate])
        assertEquals(Integer, table.columnType(0), "place column type")
        def selection = table.selectRowIndices {
            def date = it[2] as LocalDate
            return date.isAfter(LocalDate.of(2022,1, 1))
        }
        assertIterableEquals([1,2], selection)
        def foo = table.apply("place", selection, { it * 2})
        //println(foo.content())
        assertEquals(4, foo[1, 0])
        assertEquals(6, foo[2, 0])
        assertEquals(LocalDate, foo.columnType(2))
        assertEquals(Integer, foo.columnType(0), "place column type")

        def bar = table.apply("place", {
            def date = it[2] as LocalDate
            return date.isAfter(LocalDate.of(2022,1, 1))
        }, {
            it * 2
        })
        //println(bar.content())
        assertEquals(4, bar[1, 0])
        assertEquals(6, bar[2, 0])
        assertEquals(LocalDate, bar.columnType(2), "start column type")
        assertEquals(Integer, bar.columnType(0), "place column type")

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
    void testAddColumn() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
            [int, String, Number, LocalDate]
        )
        def table = empData.clone().addColumn("yearMonth", YearMonth, toYearMonths(empData["start_date"]))
        assertEquals(5, table.columnCount())
        assertEquals("yearMonth", table.columnNames()[table.columnCount()-1])
        assertEquals(YearMonth, table.columnType("yearMonth"))
        assertEquals(YearMonth.of(2012, 1), table[0,4])
        assertEquals(YearMonth.of(2015, 3), table[4,4])
        assertIterableEquals([Integer, String, Number, LocalDate, YearMonth], table.columnTypes())

        // Append a new column to the end
        Matrix table2 = empData.clone()
        assertIterableEquals([Integer, String, Number, LocalDate], table2.columnTypes())
        table2["yearMonth", YearMonth] = toYearMonths(table2["start_date"])
        assertEquals(empData.columnCount() + 1, table2.columnCount())
        assertEquals("yearMonth", table2.columnNames()[table2.columnCount()-1])
        assertEquals(YearMonth, table2.columnType("yearMonth"))
        assertEquals(YearMonth.of(2012, 1), table2[0,4])
        assertEquals(YearMonth.of(2015, 3), table2[4,4])

        // Insert a new column first
        Matrix table3 = empData.clone()
        table3["yearMonth", YearMonth, 0] = toYearMonths(table3["start_date"])
        assertEquals(empData.columnCount() + 1, table3.columnCount())
        assertEquals("yearMonth", table3.columnNames()[0])
        assertEquals(YearMonth, table3.columnType("yearMonth"))
        assertEquals(YearMonth.of(2012, 1), table3[0,0])
        assertEquals(YearMonth.of(2015, 3), table3[4,0])
    }

    @Test
    void testAddColumns() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2019-01-01", "2019-01-23", "2019-05-15", "2019-05-11", "2019-03-27"),
            [int, String, Number, LocalDate]
        )

        empData = empData.addColumn("yearMonth", YearMonth, toYearMonths(empData["start_date"]))
        assertEquals(YearMonth, empData[0,4].class, "type of the added column")
        assertEquals(YearMonth, empData.columnType("yearMonth"), "claimed type of the added column")

        def counts = Stat.countBy(empData, "yearMonth").orderBy('yearMonth')
        assertEquals(YearMonth, counts[0,0].class, "type of the count column")
        assertEquals(YearMonth, counts.columnType("yearMonth"), "claimed type of the count column")

        assertEquals(2, counts.subset('yearMonth', {it == YearMonth.of(2019,5)})[0,1])
        assertEquals(1, counts.subset('yearMonth', {it == YearMonth.of(2019,3)})['yearMonth_count'][0])
        assertEquals(2, counts[0, 'yearMonth_count'])

        def sums = Stat.sumBy(empData, "salary", "yearMonth").orderBy("yearMonth", true)
        assertEquals(YearMonth, sums[0,0].class, "type of the sums column")
        assertEquals(YearMonth, sums.columnType("yearMonth"), "claimed type of the sums column")
        assertEquals(611.0 + 729.0, sums[0, 1], sums.content())
        assertEquals(843.25, sums[1, 1], sums.content())
        assertEquals(623.3 + 515.2, sums[2, 1], sums.content())

        def salaryPerYearMonth = counts
                .orderBy("yearMonth", true)
                .addColumns(sums, "salary")

        assertEquals(asYearMonth("2019-05"), salaryPerYearMonth[0, 0], salaryPerYearMonth.content())
        assertEquals(611.0 + 729.0, salaryPerYearMonth[0, 2], salaryPerYearMonth.content())
        assertEquals(2, salaryPerYearMonth[0, 1], salaryPerYearMonth.content())
        assertEquals(843.25, salaryPerYearMonth[1, 2], salaryPerYearMonth.content())
        assertEquals(1, salaryPerYearMonth[1, 1], salaryPerYearMonth.content())
    }

    @Test
    void testSort() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [Integer, String, Number, LocalDate]
        )
        def dateSorted = empData.orderBy("start_date")
        assertEquals(4, dateSorted[4, 0], "Last row should be the Ryan row: \n${dateSorted.content()}")
        assertEquals(asLocalDate("2012-03-27"), dateSorted[0, 3], "First row should be the Dan Row")

        //println(empData.content())
        def salarySorted = empData.orderBy(["salary": Matrix.DESC])
        assertEquals(843.25, salarySorted["salary"][0], "Highest salary: ${salarySorted.content()}")
        assertEquals(515.2, salarySorted["salary"][4], "Lowest salary: ${salarySorted.content()}")
    }

    @Test
    void testDropColumns() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )
        def noId = empData.clone().dropColumns('emp_id')
        assertEquals(3, noId.columnCount())
        assertIterableEquals(['emp_name', 'salary', 'start_date'], noId.columnNames())
        assertIterableEquals([String, Number, LocalDate], noId.columnTypes())

        def empList = empData.dropColumns("salary", "start_date")
        //println(empList.content())
        assertEquals(2, empList.columnCount(), "Number of columns after drop")
        assertEquals(5, empList.rowCount(), "Number of rows after drop")
        assertIterableEquals(["emp_id",	"emp_name"], empList.columnNames(), "column names after drop")
        assertIterableEquals([Integer, String], empList.columnTypes(), "Column types after drop")
    }

    @Test
    void testDropColumnsExcept() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )
        def empList = empData.dropColumnsExcept("emp_id", "start_date")
        //println(empList.content())
        assertEquals(2, empList.columnCount(), "Number of columns after drop")
        assertEquals(5, empList.rowCount(), "Number of rows after drop")
        assertIterableEquals(["emp_id", "start_date"], empList.columnNames(), "column names after drop")
        assertIterableEquals([Integer, LocalDate], empList.columnTypes(), "Column types after drop")
    }

    @Test
    void testIteration() {
        def empData = new Matrix(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )

        int i = 1
        empData.each { row ->
            assertEquals(i, row[0], String.valueOf(row))
            assertEquals(empData[i-1, 'emp_name'], row[1], String.valueOf(row))
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
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [BigDecimal]*3)

        Grid grid = table.grid()
        assertEquals(3.664, grid[2,1] as BigDecimal)
    }

    @Test
    void testSelectColumns() {
        def report = [
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Base Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [BigDecimal]*3)
            .selectColumns("Base Funding", "Full Funding")

        assertEquals(3385.593, table[0,0])
        assertEquals(12.23, table[3,1])

    }

    @Test
    void testRenameColumns() {
        def empData = new Matrix (
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2013-01-01", "2012-03-27", "2013-09-23", "2014-11-15", "2014-05-11" ),
            [int, String, Number, LocalDate]
        )

        empData.renameColumn('emp_id', 'id')
        empData.renameColumn(1, 'name')

        assertEquals('id', empData.columnNames()[0])
        assertEquals('name', empData.columnNames()[1])

    }

    @Test
    void testToMarkdown() {
        def report = [
            "YearMonth": toYearMonths(['2023-01', '2023-02', '2023-03', '2023-04']),
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [YearMonth, BigDecimal, BigDecimal, BigDecimal])

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
    }

    @Test
    void testEquals() {
        def empData = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        )

        assertEquals(empData, new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        ))

        assertNotEquals(empData, new Matrix (
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.1],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        ))

        Matrix differentTypes = new Matrix (
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [Object, Object, Object, Object]
        )
        assertEquals(empData,differentTypes , empData.diff(differentTypes))
        assertNotEquals(empData,differentTypes.withName("differentTypes") , empData.diff(differentTypes))
    }

    @Test
    void testDiff() {
        def empData = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        )
        def d1 = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.1],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [int, String, Number, LocalDate]
        )
        assertEquals('Row 1 differs: this: 2, Dan, 515.2, 2012-03-27; that: 2, Dan, 515.1, 2012-03-27',
                    empData.diff(d1).trim())

        def d2 = new Matrix(
            emp_id: [1,2],
            emp_name: ["Rick","Dan"],
            salary: [623.3,515.2],
            start_date: toLocalDates("2013-01-01", "2012-03-27"),
            [Object, Object, Object, Object]
        )
        assertEquals('Column types differ: this: Integer, String, Number, LocalDate; that: Object, Object, Object, Object',
            empData.diff(d2))
    }

    @Test
    void removeRows() {
        def report = [
            "YearMonth": toYearMonths(['2023-01', '2023-02', '2023-03', '2023-04']),
            "Full Funding": [4563.153, 380.263, 4.938, 12.23],
            "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
            "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        Matrix table = new Matrix(report, [YearMonth, BigDecimal, BigDecimal, BigDecimal])
        table.removeRows(0, 2)
        assertEquals(2, table.rowCount())
        assertIterableEquals([asYearMonth('2023-02'), 380.263, 282.133, 225], table.row(0))
        assertIterableEquals([asYearMonth('2023-04'), 12.23, 2.654, 1.871], table.row(1))
    }

    @Test
    void testRemoveEmptyRows() {
        def empData = new Matrix(
                emp_id: [1,2],
                emp_name: ["Rick","Dan"],
                salary: [623.3,515.2],
                start_date: toLocalDates("2013-01-01", "2012-03-27"),
                [int, String, Number, LocalDate]
        )

        def d0 = new Matrix(
                emp_id: [1,null, 2, null],
                emp_name: ["Rick", "", "Dan", " "],
                salary: [623.3, null, 515.2, null],
                start_date: toLocalDates("2013-01-01", null, "2012-03-27", null),
                [int, String, Number, LocalDate]
        )
        def d0r = d0.removeEmptyRows()
        assertEquals(empData, d0r, empData.diff(d0r, true))
    }

    @Test
    void testRemoveEmptyColumns() {
        def empData = new Matrix(
            emp_id: [1,2],
            emp_name: [null, null],
            salary: [623.3,515.2],
            start_date: [null, null],
            other: [null, null],
            [int, String, Number, LocalDate, String]
        )
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
        def table = new Matrix([
            a: [1,2,3,4,5],
            b: [1.2,2.3,0.7,1.3,1.9]
        ], [Integer, BigDecimal])

        def m = table.withColumns(['a', 'b']) { x, y -> x - y }
        assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], m)

        def n = table.withColumns([0,1] as Integer[]) { x, y -> x - y }
        assertEquals([-0.2, -0.3, 2.3, 2.7, 3.1], n)
    }

    @Test
    void testPopulateColumn() {
        Matrix components = new Matrix([
            id: [1,2,3,4,5],
            size: [1.2,2.3,0.7,1.3,1.9]
        ], [Integer, BigDecimal])
        components['id'] = [10, 11, 12, 13, 14]
        assertEquals(10, components[0, 'id'])
        assertEquals(13, components[3, 'id'])
        assertEquals(14, components[4, 'id'])
    }

    @Test
    void testMoveColumn() {
        def table = new Matrix([
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
                'foo': [1, 2, 3]
        ], [String, LocalDate, int])

        table.moveColumn('foo', 0)
        assertIterableEquals(['foo', 'firstname', 'start'], table.columnNames())
        assertIterableEquals([1, 2, 3], table[0])
        assertIterableEquals([Integer, String, LocalDate], table.columnTypes())
    }

    @Test
    void testPutAt() {
        // putAt(String columnName, Class<?> type, Integer index = null, List<?> column)
        def table = new Matrix([
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
            'foo': [1, 2, 3]
        ], [String, LocalDate, int])
        table["yearMonth", YearMonth, 0] = toYearMonths(table["start"])
        assertEquals(4, table.columnCount())
        assertIterableEquals(['yearMonth', 'firstname', 'start', 'foo'], table.columnNames())
        assertIterableEquals(toYearMonths(['2021-12', '2022-07', '2023-05']), table[0])

        // putAt(List where, List<?> column)
        table = new Matrix([
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
                'foo': [1, 2, 3]
        ], [String, LocalDate, int])
        table["start"] = table["start"].collect {it.plusDays(10)}
        assertEquals(3, table.columnCount())
        assertIterableEquals(['firstname', 'start', 'foo'], table.columnNames())
        // getAt and putAt should have the same semantics i refer to columns:
        assertIterableEquals(toLocalDates(['2021-12-11', '2022-07-20', '2023-06-06']), table[1])
        assertIterableEquals(table.column(2), table[2])
        assertIterableEquals(table.column("foo"), table["foo"])
    }

    @Test
    void testGetAt() {
        def table = new Matrix([
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
                'foo': [1, 2, 3]
        ], [String, LocalDate, int])

        assertEquals(Integer, table.getAt(2, 2).class)
        assertEquals(3, table.getAt(2, 2))

        assertEquals(Integer, table[2, 2].class)
        assertEquals(3, table[2, 2])

        assertEquals(LocalDate, table[2, 'start'].class)
        assertEquals(asLocalDate('2023-05-27'), table[2, 'start'])

        assertEquals(asLocalDate('2023-05-27'), table.getAt(2, 'start'))
        assertEquals(LocalDate, table.getAt(2, 'start').class)


        Row row = table.row(1)
        assertEquals(LocalDate, row.getAt('start').class)
        assertEquals(LocalDate, row[1].class)
        assertEquals(LocalDate, row['start'].class)
        assertEquals(LocalDate.parse('2022-07-10'), row[1])
        assertEquals(LocalDate.parse('2022-07-10'), row['start'])

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
    }
}
