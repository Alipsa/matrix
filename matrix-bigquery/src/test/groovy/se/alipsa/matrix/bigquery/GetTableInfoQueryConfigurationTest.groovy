package se.alipsa.matrix.bigquery

import groovy.transform.CompileStatic

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Unit tests for `getTableInfo` query construction and identifier validation.
 *
 * <p>This test intentionally lives in `se.alipsa.matrix.bigquery` rather than the
 * usual `test.alipsa.matrix.bigquery` package so it can access the `@PackageScope`
 * helpers added for query configuration verification.</p>
 */
@CompileStatic
class GetTableInfoQueryConfigurationTest {

  @Test
  void createTableInfoQueryConfigurationUsesNamedParameterForTableName() {
    QueryJobConfiguration queryConfig = Bq.createTableInfoQueryConfiguration('analytics_data', "orders'2024")

    assertTrue(queryConfig.query.contains('`analytics_data.INFORMATION_SCHEMA.COLUMNS`'))
    assertTrue(queryConfig.query.contains('@tableName'))
    assertFalse(queryConfig.query.contains("orders'2024"))
    assertEquals(false, queryConfig.useLegacySql())

    Map<String, QueryParameterValue> namedParameters = queryConfig.namedParameters
    assertEquals(1, namedParameters.size())
    assertEquals("orders'2024", namedParameters.get('tableName').value)
  }

  @Test
  void validateDatasetIdentifierRejectsInvalidCharacters() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Bq.validateDatasetIdentifier('sales-data')
    }

    assertTrue(exception.message.contains('invalid characters'))
  }

  @Test
  void validateDatasetIdentifierRejectsBlankValues() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Bq.validateDatasetIdentifier('   ')
    }

    assertEquals('Dataset name cannot be null or blank', exception.message)
  }

  @Test
  void validateDatasetIdentifierAcceptsLettersDigitsAndUnderscores() {
    assertEquals('Sales_2024', Bq.validateDatasetIdentifier('Sales_2024'))
  }

  @Test
  void validateTableNameRejectsBlankValues() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Bq.validateTableName('\n')
    }

    assertEquals('Table name cannot be null or blank', exception.message)
  }
}
