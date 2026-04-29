import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.sql.SqlIdentifier

class SqlIdentifierTest {

  @Test
  void testQuoteEscapesEmbeddedQuotes() {
    assertEquals('"quote "" col"', SqlIdentifier.quote('quote " col'))
  }

  @Test
  void testQuoteRequiresIdentifier() {
    assertThrows(IllegalArgumentException) {
      SqlIdentifier.quote(null)
    }
    assertThrows(IllegalArgumentException) {
      SqlIdentifier.quote('')
    }
    assertThrows(IllegalArgumentException) {
      SqlIdentifier.quote('  ')
    }
  }

  @Test
  void testConstraintNameNormalizesSpecialCharacters() {
    assertEquals('"pk_odd_table_name"', SqlIdentifier.constraintName('pk', 'odd table-name*'))
  }

  @Test
  void testRenderTableQuotesReservedWords() {
    [
        'SELECT',
        'user',
        'key',
        'constraint',
        'timestamp',
        'value'
    ].each { String keyword ->
      assertEquals("\"$keyword\"", SqlIdentifier.renderTable(keyword))
    }
  }

  @Test
  void testRenderTableKeepsSimpleNamesUnquotedForCompatibility() {
    assertEquals('plain_table', SqlIdentifier.renderTable('plain_table'))
  }

  @Test
  void testRenderTableQuotesNamesThatNeedQuoting() {
    assertEquals('"my table"', SqlIdentifier.renderTable('my table'))
    assertEquals('"quote "" table"', SqlIdentifier.renderTable('quote " table'))
  }

  @Test
  void testRenderTableCanBeForcedUnquoted() {
    assertEquals('select', SqlIdentifier.renderTable('select', false))
  }
}
