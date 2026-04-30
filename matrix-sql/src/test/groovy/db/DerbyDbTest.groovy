package db

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertInstanceOf

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixSqlFactory

class DerbyDbTest {

  @Test
  void testCreateTable() {
    File dbFile = new File('build/derbyTest.db')
    try (def derby = MatrixSqlFactory.createDerby(dbFile)) {
      def result
      if (derby.tableExists('test')) {
        result = derby.executeQuery("drop table test")
        assertEquals(0, result)
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
      assertEquals(0, result)

      result = derby.executeQuery("""
      insert into test values (1, 'Per', '2025-02-27', X'DE', '11:24', '2025-01-12 12:14:32')
      """)
      assertEquals(1, result)

      result = derby.executeQuery("select * from test").withMatrixName("test")
      assertInstanceOf(Matrix, result)
      Matrix selected = result as Matrix
      assertEquals('test', selected.matrixName)
      assertEquals(1, selected.rowCount())
      assertEquals(1, selected[0, 'place'])
      assertEquals('Per', selected[0, 'firstname'])

      result = derby.executeQuery("drop table test")
      assertEquals(0, result)
      assertFalse(derby.tableExists('test'))
    }
  }
}
