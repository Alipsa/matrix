import groovy.sql.Sql
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row
import se.alipsa.groovy.matrix.sql.MatrixSql
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.datasets.Dataset

import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.*

class MatrixSqlTest {


  @Test
  void testH2TableCreation() {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.3.232')
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'h2testdb').getAbsolutePath()
    ci.setUrl("jdbc:h2:file:${tmpDb}")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    Matrix airq = Dataset.airquality()
    try (MatrixSql matrixSql = new MatrixSql(ci)) {

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

    Matrix complexData = new Matrix([
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ],
        [int, String, LocalDate]
    ).withName('complexData')

    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.3.232')
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'testdb').getAbsolutePath()
    ci.setUrl("jdbc:h2:file:${tmpDb}")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    try(MatrixSql matrixSql = new MatrixSql(ci)) {

      String tableName = matrixSql.tableName(complexData)

      if (matrixSql.tableExists(complexData)) {
        matrixSql.dropTable(complexData)
      }

      matrixSql.create(complexData)

      Matrix stored = matrixSql.select("* from $tableName")
      println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"

      stored = stored.convert('start', LocalDate)
      println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
    }
  }

  @Test
  void testPrimaryKey() {
    Matrix pkdata = new Matrix([
        'id': [1,2,3],
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ],
        [int, int, String, LocalDate]
    ).withName('pkdata')

    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.3.232')
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'pktestdb').getAbsolutePath()
    ci.setUrl("jdbc:h2:file:${tmpDb}")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")

    try (MatrixSql matrixSql = new MatrixSql(ci)) {

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
}
