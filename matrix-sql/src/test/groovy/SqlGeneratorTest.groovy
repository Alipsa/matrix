import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.sql.SqlGenerator

class SqlGeneratorTest {

  @Test
  void testDeprecatedPreparedUpdateSqlOverloadPreservesQuoteChoice() {
    assertEquals(
        'update people set "name" = ? where "id" = ?',
        SqlGenerator.createPreparedUpdateSql('people', ['name'], ['id'], true)
    )
    assertEquals(
        'update people set name = ? where id = ?',
        SqlGenerator.createPreparedUpdateSql('people', ['name'], ['id'], false)
    )
  }

  @Test
  void testStoredColumnMapReportsMissingRowColumn() {
    Row row = Matrix.builder('row').data([id: [1], name: ['Alice']]).types(int, String).build().row(0)

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      SqlGenerator.createPreparedUpdate('people', row, ['id'] as String[], [id: 'ID'])
    }
    assertEquals('No stored column name mapping for row column: name', exception.message)
  }

  @Test
  void testPreparedUpdateReportsMatchColumnMissingFromRow() {
    Row row = Matrix.builder('row').data([id: [1], name: ['Alice']]).types(int, String).build().row(0)

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      SqlGenerator.createPreparedUpdate('people', row, ['nosuchcol'] as String[])
    }
    assertEquals('No stored column name mapping for row column: nosuchcol', exception.message)
  }
}
