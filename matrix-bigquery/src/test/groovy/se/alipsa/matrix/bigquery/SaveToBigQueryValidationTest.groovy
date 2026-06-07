package se.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

/**
 * These tests live in `se.alipsa.matrix.bigquery` so they can access the
 * `@PackageScope` validation helpers used by `saveToBigQuery`.
 */
@CompileStatic
class SaveToBigQueryValidationTest {

  @Test
  void validateMatrixNameRejectsBlankValues() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Bq.validateMatrixName('   ')
    }

    assertEquals('Matrix matrixName cannot be null or blank when saving to BigQuery', exception.message)
  }

  @Test
  void validateMatrixNameTrimsValidValues() {
    assertEquals('orders', Bq.validateMatrixName(' orders '))
  }

  @Test
  void validateSaveDatasetNameRejectsBlankValues() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Bq.validateSaveDatasetName('\n')
    }

    assertEquals('Dataset name cannot be null or blank when saving to BigQuery', exception.message)
  }

  @Test
  void validateSaveDatasetNameTrimsValidValues() {
    assertEquals('analytics', Bq.validateSaveDatasetName(' analytics '))
  }
}
