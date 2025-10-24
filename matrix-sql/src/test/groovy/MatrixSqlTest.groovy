import groovy.sql.Sql
import it.AbstractDbTest
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.sql.MatrixSqlFactory

import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*

class MatrixSqlTest {


  @Test
  void testH2TableCreation() {
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'h2testdb')
    Matrix airq = Dataset.airquality()
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(tmpDb, 'sa', '123')) {

      String tableName = matrixSql.tableName(airq)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(airq)

      // For h2 we MUST piggyback on the existing connection as there can only be one
      // for another db we could have done Sql sql = new Sql(matrixSql.dbConnect(ci))
      // But since this is surrounded in a "try with resources", this is actually better
      Sql sql = new Sql(matrixSql.connect())
      int i = 0
      // Explicitly call toString to force interpolation in a closure
      sql.query("select * from $tableName".toString()) { rs ->
        while (rs.next()) {
          assertEquals(ps(airq[i, 0]), ps(rs.getBigDecimal("Ozone")), "ozone on row $i differs")
          assertEquals(ps(airq[i, 1]), ps(rs.getBigDecimal("Solar.R")), "Solar.R on row $i differs")
          assertEquals(ps(airq[i, 2]), ps(rs.getBigDecimal("Wind")), "Wind on row $i differs")
          assertEquals(ps(airq[i, 3]), ps(rs.getBigDecimal("Temp")), "Temp on row $i differs")
          assertEquals(airq[i, 4], rs.getShort("Month"), "Month on row $i differs")
          assertEquals(airq[i, 5], rs.getShort("Day"), "Day on row $i differs")
          i++
        }
      }

      Matrix m2 = matrixSql.select("select * from $tableName")
      airq.eachWithIndex { Row row, int r ->
        row.eachWithIndex { BigDecimal expected, int c ->
          def actual = (m2[r, c] as BigDecimal)
          if (expected != null && actual != null) {
            actual = actual.setScale(expected.scale())
          }
          assertEquals(expected, actual, "Diff detected on row $r, column ${airq.columnName(c)}")
        }
      }
    }
  }

  static Double ps(BigDecimal bd) {
    bd == null ? null : bd.doubleValue()
  }

  @Test
  void testExample() {

    Matrix complexData = Matrix.builder('complexData').data([
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ])
    .types(int, String, LocalDate)
    .build()

    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'testdb')
    try(MatrixSql matrixSql = MatrixSqlFactory.createH2(tmpDb, 'sa', '123')) {

      String tableName = matrixSql.tableName(complexData)

      if (matrixSql.tableExists(complexData)) {
        matrixSql.dropTable(complexData)
      }

      matrixSql.create(complexData)

      Matrix stored = matrixSql.select("select * from $tableName")
      //println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
      assertEquals(java.sql.Date, stored.type('start'))
      assertEquals(ListConverter.toSqlDates('2021-12-01', '2022-07-10', '2023-05-27'), stored.column('start'))

      stored = stored.convert('start', LocalDate)
      assertEquals(LocalDate, stored.type('start'))
      assertEquals(ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'), stored.column('start'))
    }
  }

  @Test
  void testPrimaryKey() {
    Matrix pkdata = Matrix.builder('pkdata').data([
        'id': [1,2,3],
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ])
    .types(int, int, String, LocalDate)
    .build()

    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'pktestdb')

    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(tmpDb, 'sa', '123')) {
      String tableName = matrixSql.tableName(pkdata)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }

      matrixSql.create(pkdata, 'id')

      try (Connection con = matrixSql.connect(); ResultSet rs = con.getMetaData().getPrimaryKeys(null, null, tableName.toUpperCase())) {
        assertTrue(rs.next(), 'No pk results found')
        assertEquals(
            'id',
            rs.getString("COLUMN_NAME"),
            "Expected to find 'id' as the primary key"
        )
      }
    }
  }

  @Test
  void testDdl() {
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'ddltestdb')
    String h2version = "2.4.240"
    Matrix m = AbstractDbTest.getComplexData()
    String props = "MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"

    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency("com.h2database:h2:$h2version")
    ci.setUrl("jdbc:h2:file:${tmpDb};$props")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    String ddl1
    String ddl2
    try (MatrixSql sql = new MatrixSql(ci)) {
      ddl1 = sql.createDdl(m)
      //check that DB detection works properly
      assertTrue(ddl1.contains('"local date time" datetime2'), "Expected to find datetime2 but was $ddl1")
    }

    // Check that the factory creates the same MatrixSql as the explicit creation does
    try (MatrixSql h2 = MatrixSqlFactory.createH2(tmpDb, 'sa', '123', props)) {
      //println "using $h2.connectionInfo.dependency with url ${h2.connectionInfo.url}"
      ddl2 = h2.createDdl(m)
      String latestVersion = h2.connectionInfo.dependencyVersion
      if (latestVersion != h2version) {
        System.err.println("We are using version $h2version when explicitly specifying it but latest version is $latestVersion")
        System.err.println("Consider updating it!")
      }
      //check that DB detection works properly
      assertTrue(ddl2.contains('"local date time" datetime2'), "Expected to find datetime2 but was $ddl2")
      assertEquals(ci.url, h2.connectionInfo.url, "Urls does not match")
    }
    assertEquals(ddl1, ddl2)
  }
}
