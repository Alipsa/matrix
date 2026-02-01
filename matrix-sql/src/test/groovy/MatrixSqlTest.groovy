import groovy.sql.Sql
import it.AbstractDbTest
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.SqlUtil
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

  private static String h2MemUrl(String name, String additionalProperties = null) {
    String uniqueName = "${name}_${System.nanoTime()}"
    String url = "jdbc:h2:mem:${uniqueName};DB_CLOSE_DELAY=-1"
    if (additionalProperties != null && !additionalProperties.isBlank()) {
      url += ";${additionalProperties}"
    }
    return url
  }


  @Test
  void testH2TableCreation() {
    String url = h2MemUrl('h2testdb')
    Matrix airq = Dataset.airquality()
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {

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

    String url = h2MemUrl('testdb', 'MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE')
    // Test that deriving the dependency and driver from the url works
    try(MatrixSql matrixSql = MatrixSqlFactory.create(url, 'sa', '123')) {

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
  void testFactorySetsDriverWhenMissing() {
    String url = h2MemUrl('driver_testdb')
    String expectedDriver = SqlUtil.getDriverClassName(url)
    assertNotNull(expectedDriver, "Expected SqlUtil to resolve a driver for $url")
    try (MatrixSql matrixSql = MatrixSqlFactory.create(url, 'sa', '123', '2.4.240')) {
      assertEquals(expectedDriver, matrixSql.connectionInfo.driver)
    }
  }

  @Test
  void testReconnectAfterClose() {
    String url = h2MemUrl('reconnect_testdb')
    MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')
    try {
      Connection first = matrixSql.connect()
      assertFalse(first.isClosed(), 'Expected initial connection to be open')
      matrixSql.close()
      assertTrue(first.isClosed(), 'Expected initial connection to be closed after MatrixSql.close()')
      Connection second = matrixSql.connect()
      assertFalse(second.isClosed(), 'Expected new connection after close')
      assertNotSame(first, second, 'Expected a new connection instance after close')
    } finally {
      matrixSql.close()
    }
  }

  @Test
  void testUpdateUsesPreparedStatement() {
    Matrix data = Matrix.builder('people').data([
        id: [1],
        name: ["O'Neil"]
    ])
    .types(int, String)
    .build()

    String props = "DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
    String url = h2MemUrl('update_testdb', props)
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(data)

      Row row = data.row(0)
      row['name'] = "D'Arcy"
      assertEquals(1, matrixSql.update(tableName, row, 'id'))

      Matrix stored = matrixSql.select("select * from $tableName")
      assertEquals("D'Arcy", stored[0, 'name'])
    }
  }

  @Test
  void testUpdateRequiresMatchColumn() {
    Matrix data = Matrix.builder('people2').data([
        id: [1],
        name: ['Alice']
    ])
    .types(int, String)
    .build()

    String url = h2MemUrl('update_match_testdb')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(data)

      Row row = data.row(0)
      row['name'] = 'Bob'
      assertThrows(IllegalArgumentException) { matrixSql.update(tableName, row) }
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

    String url = h2MemUrl('pktestdb')

    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
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
    String h2version = "2.4.240"
    Matrix m = AbstractDbTest.getComplexData()
    String props = "MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
    String url = h2MemUrl('ddltestdb', props)

    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency("com.h2database:h2:$h2version")
    ci.setUrl(url)
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
    try (MatrixSql h2 = MatrixSqlFactory.createH2(url, 'sa', '123', null, h2version)) {
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
