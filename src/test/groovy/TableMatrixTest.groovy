import se.alipsa.groovy.datautil.SqlUtil
import se.alipsa.groovy.matrix.Stat
import se.alipsa.groovy.matrix.TableMatrix

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static se.alipsa.groovy.matrix.ListConverter.*
import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.*

import static se.alipsa.groovy.matrix.ValueConverter.toLocalDate


class TableMatrixTest {

    @Test
    void testTableCreationFromMatrix() {
        def employees = []
        employees.add(['John Doe', 21000, toLocalDate('2013-11-01'), toLocalDate('2020-01-10')])
        employees.add(['Peter Smith', 23400,	'2018-03-25',	'2020-04-12'])
        employees.add(['Jane Doe',	26800, toLocalDate('2017-03-14'), toLocalDate('2020-10-02')])

        def table = TableMatrix.create(employees)
        assertEquals('John Doe', table[0,0])
        assertEquals(23400, table[1,1] as Integer)
        assertEquals(LocalDate.of(2017, 3, 14), table[2,2])
    }

    @Test
    void testTransposing() {
        def report = [
                "Full Funding": [4563.153, 380.263, 4.938, 12.23],
                "Baseline Funding": [3385.593, 282.133, 3.664, 2.654],
                "Current Funding": [2700, 225, 2.922, 1.871]
        ]
        def tr = TableMatrix.create(report).transpose()
        assertEquals(["Full Funding", "Baseline Funding", "Current Funding"], tr.columnNames())
        assertEquals([
           [4563.153, 380.263, 4.938, 12.23],
           [3385.593, 282.133, 3.664, 2.654],
           [2700, 225, 2.922, 1.871]
        ], tr.matrix())
    }

    @Test
    void testStr() {
        def empData = TableMatrix.create(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
            [int, String, Number, LocalDate]
        )
        def struct = Stat.str(empData)
        assertEquals(['5 observations of 4 variables'], struct['TableMatrix'])
        assertArrayEquals(['int', '1', '2', '3', '4'].toArray(), struct['emp_id'].toArray())
        assertArrayEquals(['LocalDate', '2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11'].toArray(), struct['start_date'].toArray())
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

        def table = TableMatrix.create(file)
        assertArrayEquals(data[0] as String[], table.columnNames() as String[])
        assertEquals(data[1][1], table[0, 1] as String)
        assertEquals('Team SD Worx', table[2, 3])
    }

    @Test
    void testConvert() {
        def data = [
            'place': ['1', '2', '3', ','],
            'firstname': ['Lorena', 'Marianne', 'Lotte', 'Chris'],
            'start': ['2021-12-01', '2022-07-10', '2023-05-27', '2023-01-10'],
            'end': ['2022-12-01 10:00:00', '2023-07-10 00:01:00', '2024-05-27 00:00:30', '2042-01-10 00:00:00']
        ]
        def table = TableMatrix.create(data, [String]*4)

        def table2 = table.convert(place: int, start: LocalDate)
        table2 = table2.convert([end: LocalDateTime],
                DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
        assertEquals(int, table2.columnType('place'))
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

    }

    @Test
    void testGetRowsForCriteria() {
        def data = [
            'place': [1, 2, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': ['2021-12-01', '2022-07-10', '2023-05-27']
        ]
        def table = TableMatrix.create(data, [int, String, String])
        def rows = table.rows(table['place'].findIndexValues { it > 1 })
        assertEquals(2, rows.size())

        // Same thing using subset
        def subSet = table.subset('place', { it > 1 })
        assertArrayEquals(table.rows(1..2).toArray(), subSet.matrix().toArray())
    }

    @Test
    void testHeadAndTail() {
        def table = TableMatrix.create([
                'place': [1, 2, 3],
                'firstname': ['Lorena', 'Marianne', 'Lotte'],
                'start': ['2021-12-01', '2022-07-10', '2023-05-27']
            ],
            [int, String, String]
        )
        assertEquals('1\tLorena\t2021-12-01\n', table.head(1, false))
        assertEquals('3\tLotte\t2023-05-27\n', table.tail(1, false))
    }

    @SuppressWarnings('SqlNoDataSourceInspection')
    @Test
    void testCreateFromDb() {
        def dbDriver = "org.h2.Driver"
        def dbUrl = "jdbc:h2:file:" + System.getProperty("java.io.tmpdir") + "/testdb"
        def dbUser = "sa"
        def dbPasswd = "123"

        SqlUtil.withInstance(dbUrl, dbUser, dbPasswd, dbDriver, this) { sql ->
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
        SqlUtil.withInstance(dbUrl, dbUser, dbPasswd, dbDriver, this) { sql ->
            sql.query('SELECT * FROM PROJECT') { rs -> project = TableMatrix.create(rs) }
        }

        assertEquals(2, project.rowCount())
    }
}
