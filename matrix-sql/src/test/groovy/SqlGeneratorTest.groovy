import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

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
}
