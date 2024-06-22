import groovy.sql.Sql
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.sql.MatrixSql
import se.alipsa.groovy.datasets.Dataset

import static org.junit.jupiter.api.Assertions.*

class MatrixSqlTest {


  @Test
  void testH2TableCreation() {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.2.224')
    ci.setUrl("jdbc:h2:file:${System.getProperty('java.io.tmpdir')}testdb")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    Matrix airq = Dataset.airquality()
    MatrixSql matrixSql = new MatrixSql(ci)
    if (matrixSql.dbTableExists( 'airquality')) {
      matrixSql.dbDropTable("airquality")
    }
    matrixSql.dbCreate(airq)

    try(Sql sql = new Sql(matrixSql.connect(ci))) {
      int i = 0
      sql.query("select * from airquality") { rs ->
        while (rs.next()) {
          assertEquals(ps(airq[i, 0]), ps(rs.getBigDecimal("Ozone")), "ozone on row $i differs")
          assertEquals(ps(airq[i, 1]), ps(rs.getBigDecimal("Solar.R")), "Solar.R on row $i differs")
          assertEquals(ps(airq[i, 2]), ps(rs.getBigDecimal("Wind")), "Wind on row $i differs")
          assertEquals(ps(airq[i, 3]), ps(rs.getBigDecimal("Temp")), "Temp on row $i differs")
          assertEquals(airq[i, 4], rs.getInt("Month"), "Month on row $i differs")
          assertEquals(airq[i, 5], rs.getString("Day"), "Day on row $i differs")
          i++
        }
      }
    }
  }

  static Double ps(BigDecimal bd) {
    bd == null ? null : bd.doubleValue()
  }
}
