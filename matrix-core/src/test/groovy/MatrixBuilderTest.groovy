import groovy.sql.Sql
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static se.alipsa.groovy.matrix.ValueConverter.asLocalDate

/**
 * Note the the calls to content() is a decent way to check the integrity of the matrix and is there to ensure
 * there is no exception from doing that
 */
class MatrixBuilderTest {


  @Test
  void testEmpty() {
    Matrix m1 = Matrix.builder().build()
    m1.content()
    assertEquals(null, m1.name)
    assertEquals(0, m1.rowCount())
    assertEquals(0, m1.columnCount())
    assertEquals(0, m1.columnTypes().size())
  }

  @Test
  void testNameOnly() {
    Matrix m1 = Matrix.builder()
        .name("empData")
        .build()
    m1.content()
    assertEquals('empData', m1.name)
    assertEquals(0, m1.rowCount())
    assertEquals(0, m1.columnCount())
    assertEquals(0, m1.columnTypes().size())
  }

  @Test
  void testColumns() {
    Matrix m2 = Matrix.builder()
        .name('m2')
        .columnNames(['id', 'name', 'salary', 'start']).build()
    m2['id'].addAll([1,2,3])
    assertEquals('m2', m2.name)
    assertEquals(3, m2.rowCount())
    assertEquals(4, m2.columnCount())
    assertEquals(4, m2.columnTypes().size())
    assertIterableEquals([Object]*4, m2.columnTypes())
    assertEquals(m2[1,0], 2)
    m2.content()

    Matrix m3 = Matrix.builder()
        .columns([
            [1,2,3],
            ['foo', 'bar', 'baz']
        ]).build()
    m3[1] = [1,2,3]
    assertEquals(null, m3.name)
    assertEquals(3, m3.rowCount())
    assertEquals(2, m3.columnCount())
    assertEquals(2, m3.columnTypes().size())
    assertIterableEquals([Object]*2, m3.columnTypes())
    assertEquals(m3[1,0], 2)
    m3.content()

    // Same this but from rows
    Matrix r3 = Matrix.builder()
        .rows([
            [1, 'foo'],
            [2, 'bar'],
            [3, 'baz']
        ]).build()
    r3[1] = [1,2,3]
    r3.content()
    assertEquals(m3, r3, m3.diff(r3))

    // add individual data points to a column
    Matrix m5 = Matrix.builder()
        .name("m5")
        .columnNames(['id', 'name', 'salary', 'start'])
        .dataTypes([int, String, Number, LocalDate]).build()
    m5['id'] = [1,2,3]
    m5[1] << 'Rick'
    m5['name'] << 'Dan'
    m5.column(1).add('Michelle')
    assertEquals(2, m5[1, 'id'])
    assertEquals('Rick', m5[0, 1])
    m5.content()
  }

  @Test
  void testDataTypesOnly() {
    Matrix m4 = Matrix.builder()
        .dataTypes([int, String, Number, LocalDate]).build()
    assertEquals([Integer, String, Number, LocalDate], m4.columnTypes())
    assertEquals(null, m4.name)
    assertEquals(0, m4.rowCount())
    assertEquals(4, m4.columnCount())
    m4.content()
  }

  @Test
  void testNameColumnTypes() {
    Matrix m6 = Matrix.builder()
        .name("m6")
        .columns([
            [1,2,3],
            ['foo', 'bar', 'baz']
        ])
        .dataTypes([int, String]).build()

    m6.content()

    Matrix m7 = Matrix.builder()
        .name("m7")
        .columns([
            [1,2,3],
            ['foo', 'bar', 'baz']
        ])
        .dataTypes([int, String, Number]).build()
    m7.content()

    Matrix r6 = Matrix.builder()
        .name("m6")
        .rows([
            [1, 'foo'],
            [2, 'bar'],
            [3, 'baz']
        ])
        .dataTypes([int, String]).build()
    r6.addRow([4, 'qux'])
    r6.removeRows(r6.size() -1)
    r6.content()
    assertEquals(m6, r6)

    Matrix r7 = Matrix.builder()
        .name("m7")
        .rows([
            [1, 'foo'],
            [2, 'bar'],
            [3, 'baz']
        ])
        .dataTypes([int, String, Number]).build()
    r7.addColumn('c4', int, [7,8,9])
    r7.content()
    assertNotEquals(m7, r7)
    r7.dropColumns('c4')
    assertEquals(m7, r7, m7.diff(r7))
  }

  @Test
  void testCreationFromRows() {
    def employees = []
    employees << ['John Doe', 21000, asLocalDate('2013-11-01'), asLocalDate('2020-01-10')]
    employees << ['Peter Smith', 23400,	'2018-03-25',	'2020-04-12']
    employees << ['Jane Doe', 26800, asLocalDate('2017-03-14'), asLocalDate('2020-10-02')]

    def table = Matrix.builder().rows(employees).build()
    assertEquals('John Doe', table[0,0])
    assertEquals(23400, table[1,1] as Integer)
    assertEquals(LocalDate.of(2017, 3, 14), table[2,2])
  }

  @Test
  void testCreationFromMap() {
    def m = Matrix.builder()
        .data([
            'place': [1, 20, 3],
            'firstname': ['Lorena', 'Marianne', 'Lotte'],
            'start': ['2021-12-01', '2022-07-10', '2023-05-27']
        ])
        .dataTypes([int, String, String])
        .build()
    assertIterableEquals([1, 20, 3], m['place'])
    assertIterableEquals([3, 'Lotte', '2023-05-27'], m.row(2))
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
      sql.query('SELECT * FROM PROJECT') { rs -> project = Matrix.builder().data(rs).build() }
    }

    assertEquals(2, project.rowCount())
    assertIterableEquals(['ID', 'NAME', 'URL'], project.columnNames())
    assertIterableEquals([Integer, String, String], project.columnTypes())
    assertEquals(project[1,1], 'Alipsa')
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

    def table = Matrix.builder().data(file).build()
    assertIterableEquals(data[0], table.columnNames())
    assertEquals(data[1][1], table[0, 1] as String)
    assertEquals('Team SD Worx', table[2, 3])

    def plantGrowth = Matrix.builder().data(
        getClass().getResource('/PlantGrowth.csv'),
        ',',
        '"',
    ).build()
    assertEquals('PlantGrowth', plantGrowth.name)
    assertIterableEquals(['id', 'weight','group'], plantGrowth.columnNames())
    def row30 = plantGrowth.findFirstRow('id', '30')
    assertEquals('5.26', row30[1])
    assertEquals('trt2', row30[2])
  }

  @Test
  void testCreationFromComplexCsvFile() {
    File complex = new File(getClass().getResource('/complex.csv').toURI())
    def matrix = Matrix.builder().data(complex, ';', '"', false).build()
    assertEquals('1234567', matrix[0,8])
    assertEquals('', matrix[1,8])

    assertIterableEquals(['324269',"77464400",'APPLICATION','SIGNED','1211121202332555','2','2023-10-09 16:35:05.644','2023-10-09 16:38:00.341','1234567'],
    matrix.row(0))
    assertIterableEquals(
        ["324471","","APPLICATION","SIGNED","1211121202339617","1","2021-11-09 17:47:30.604","2023-10-09 17:55:00.370","1234573"],
        matrix.row(7)
    )
    matrix.content()
  }
}
