package test.alipsa.matrix.api

import groovy.sql.Sql
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixSql

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

/** Exercises MatrixSql create, read, update, and drop operations against H2. */
@Tag('sql')
class SqlApiIT implements ApiItSupport {

  @Test
  void createsReadsUpdatesAndDropsTables() {
    ConnectionInfo connectionInfo = new ConnectionInfo(
        dependency: 'com.h2database:h2:2.4.240',
        url: 'jdbc:h2:mem:matrixApiIt', user: 'sa', password: '', driver: 'org.h2.Driver')
    Matrix data = Matrix.builder([id: [1, 2], text: ['a', 'b']], [Integer, String], 'sql_data').build()
    try (MatrixSql sql = new MatrixSql(connectionInfo)) {
      String table = sql.tableName(data)
      if (sql.tableExists(table)) sql.dropTable(table)
      sql.create(data)
      assertTrue(sql.tableExists(table))
      assertEquals(2, sql.select("select * from $table").rowCount())
      sql.update("update $table set \"text\"='z' where \"id\"=1")
      assertEquals('z', sql.select("select * from $table where \"id\"=1")[0, 'text'])
      sql.dropTable(table)
      assertFalse(sql.tableExists(table))
    }
  }
}
