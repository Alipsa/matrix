package db

import org.junit.jupiter.api.Test
import se.alipsa.matrix.sql.MatrixSqlFactory

class DerbyDbTest {

  @Test
  void testCreateTable() {
    try (def derby = MatrixSqlFactory.createDerby("derbyTest")) {
      def result
      if (derby.tableExists('test')) {
        result = derby.executeQuery("drop table test")
        println "drop table result = $result"
      }

      result = derby.executeQuery("""
        create table test (
          "place" INTEGER,
          "firstname" VARCHAR(8),
          "start" DATE,
          "bin" VARCHAR(32672) FOR BIT DATA,
          "theTime" TIME,
          "local date time" TIMESTAMP
        )
        """)
      println "create table result = $result"

      result = derby.executeQuery("""
      insert into test values (1, 'Per', '2025-02-27', X'DE', '11:24', '2025-01-12 12:14:32')
      """)
      println "insert result = $result"

      result = derby.executeQuery("select * from test").withMatrixName("test")
      println "select result = $result.content()"

      result = derby.executeQuery("drop table test")
      println "drop table result = $result"
    }
  }
}
