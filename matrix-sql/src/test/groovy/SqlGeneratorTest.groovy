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
    assertEquals('No row column found for match column: nosuchcol', exception.message)
  }

  @Test
  void testPreparedUpdateCarriesItsParameterColumnOrder() {
    Row row = Matrix.builder('row').data([name: ['Alice'], id: [1], status: ['active']])
        .types(String, int, String)
        .build()
        .row(0)

    SqlGenerator.PreparedUpdate prepared = SqlGenerator.createPreparedUpdate(
        'people', row, ['id'] as String[]
    )

    assertEquals(['name', 'status'], prepared.updateColumns)
    assertEquals(['id'], prepared.matchColumns)
    assertEquals(['Alice', 'active', 1], prepared.values)
  }

  @Test
  void testPreparedUpdateResolvesStoredMatchColumnSpelling() {
    Row row = Matrix.builder('row').data([id: [1], name: ['Alice']]).types(int, String).build().row(0)

    SqlGenerator.PreparedUpdate prepared = SqlGenerator.createPreparedUpdate(
        'PEOPLE', row, ['ID'] as String[], [id: 'ID', name: 'NAME']
    )

    assertEquals('update "PEOPLE" set "NAME" = ? where "ID" = ?', prepared.sql)
    assertEquals(['name'], prepared.updateColumns)
    assertEquals(['id'], prepared.matchColumns)
    assertEquals(['Alice', 1], prepared.values)
  }
}
