package test.alipsa.matrix.bigquery

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.datasets.Dataset

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

@Testcontainers
@Tag("flaky")  // BigQuery emulator testcontainer has threading issues - see issue #XXX
class BqTestContainerTest {

  // https://github.com/goccy/bigquery-emulator/pkgs/container/bigquery-emulator
  @SuppressWarnings('FieldName')
  @Container
  private static final BigQueryEmulatorContainer container = new BigQueryEmulatorContainer(
      "ghcr.io/goccy/bigquery-emulator:0.6.6"
  )

  @Test
  void testCreateAndRetrieve() {
    String fullUrl = container.getEmulatorHttpEndpoint()
    assertNotNull(fullUrl)

    URI emulatorUri = new URI(fullUrl)
    String hostAndPort = emulatorUri.getAuthority()

    String projectId = container.getProjectId()

    System.setProperty("bigquery.host", hostAndPort)
    System.setProperty("bigquery.enable_write_api", "false")
    System.setProperty("google.cloud.project.id", projectId)

    try {
      BigQueryOptions options = BigQueryOptions
          .newBuilder()
          .setProjectId(projectId)
          .setHost(fullUrl)
          .setLocation(fullUrl)
          .setCredentials(NoCredentials.getInstance())
          .build()

      Bq bq = new Bq(options)
      String dsName = "BigQueryModuleTest"

      bq.createDataset(dsName)

      // Add a delay to ensure the emulator service is fully ready for streaming inserts
      // Testcontainers can be slow to fully initialize, especially on first run
      Thread.sleep(2000)

      Matrix airq = Dataset.airquality().rename('Solar.R', 'Solar_r')
      assertTrue(bq.saveToBigQuery(airq, dsName), "Failed to save matrix to big query")

     Matrix m = bq.query("select * from `${projectId}.${dsName}.${airq.matrixName}`")
          .withMatrixName(airq.matrixName)
      MatrixAssertions.assertContentMatches(airq, m, airq.diff(m))
      for (String columnName in airq.columnNames()) {
        Class expected = airq.type(columnName)
        Class actual = m.type(columnName)
        if (expected == Short.class || expected == Integer.class) {
          // If the expected type was Short/Integer, the actual type MUST be Long (INT64)
          assertEquals(Long.class, actual, "BigQuery should promote small integers to Long")
        }
        // Check if the expected type is any other numeric type (e.g., BigDecimal)
        else if (Number.class.isAssignableFrom(expected)) {
          assertEquals(expected, actual, "Other numeric types should match exactly")
        }
        // Handle non-numeric types (like String, Date, etc.)
        else {
          assertEquals(expected, actual, "Non-numeric types should match exactly")
        }
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt()
      throw new RuntimeException("Test interrupted during sleep", e)
    } finally {
      // Always clean up the system properties after the test.
      System.clearProperty("bigquery.host")
      System.clearProperty("bigquery.enable_write_api")
      System.clearProperty("google.cloud.project.id") // Cleanup new property
    }
  }

  /**
   * Verifies that append=false overwrites existing rows when InsertAll is forced directly.
   */
  @Test
  void testInsertAllOverwriteWhenWriteApiDisabled() {
    assertSaveBehavior(true, false, [
        [3L, 'Charlie'],
        [4L, 'Delta']
    ])
  }

  /**
   * Verifies that append=true preserves existing rows when InsertAll is forced directly.
   */
  @Test
  void testInsertAllAppendWhenWriteApiDisabled() {
    assertSaveBehavior(true, true, [
        [1L, 'Alice'],
        [2L, 'Bob'],
        [3L, 'Charlie'],
        [4L, 'Delta']
    ])
  }

  /**
   * Verifies that append=false is still honored when write channel falls back to InsertAll.
   */
  @Test
  void testFallbackOverwriteToInsertAll() {
    assertSaveBehavior(false, false, [
        [3L, 'Charlie'],
        [4L, 'Delta']
    ])
  }

  /**
   * Verifies that append=true is preserved when write channel falls back to InsertAll.
   */
  @Test
  void testFallbackAppendToInsertAll() {
    assertSaveBehavior(false, true, [
        [1L, 'Alice'],
        [2L, 'Bob'],
        [3L, 'Charlie'],
        [4L, 'Delta']
    ])
  }

  @Test
  void testInsertAllPreservesTabsAndNewlines() {
    String fullUrl = container.getEmulatorHttpEndpoint()
    assertNotNull(fullUrl)

    URI emulatorUri = new URI(fullUrl)
    String hostAndPort = emulatorUri.getAuthority()
    String projectId = container.getProjectId()

    System.setProperty("bigquery.host", hostAndPort)
    System.setProperty("bigquery.enable_write_api", "false")
    System.setProperty("google.cloud.project.id", projectId)

    try {
      BigQueryOptions options = BigQueryOptions
          .newBuilder()
          .setProjectId(projectId)
          .setHost(fullUrl)
          .setLocation(fullUrl)
          .setCredentials(NoCredentials.getInstance())
          .build()

      Bq bq = new Bq(options)
      String dsName = "InsertAllStringRoundTrip"
      Matrix textData = Matrix.builder()
          .columnNames(['id', 'notes'])
          .rows([
              [1, "Hello\tWorld\nAgain"],
              [2, "Line1\r\nLine2\tTabbed"]
          ])
          .types([Integer, String])
          .matrixName('string_round_trip')
          .build()

      bq.createDataset(dsName)
      Thread.sleep(2000)

      assertTrue(bq.saveToBigQuery(textData, dsName), "Failed to save string round-trip matrix")

      Matrix retrieved = bq.query("select * from `${projectId}.${dsName}.${textData.matrixName}` order by id")
          .withMatrixName(textData.matrixName)

      assertEquals(textData.rowCount(), retrieved.rowCount())
      assertEquals(textData[0, 'notes'], retrieved[0, 'notes'])
      assertEquals(textData[1, 'notes'], retrieved[1, 'notes'])

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt()
      throw new RuntimeException("Test interrupted during string round-trip", e)
    } finally {
      System.clearProperty("bigquery.host")
      System.clearProperty("bigquery.enable_write_api")
      System.clearProperty("google.cloud.project.id")
    }
  }

  private static void assertSaveBehavior(boolean disableWriteApi, boolean append, List<List<Object>> expectedRows) {
    String fullUrl = container.getEmulatorHttpEndpoint()
    assertNotNull(fullUrl)

    URI emulatorUri = new URI(fullUrl)
    String hostAndPort = emulatorUri.getAuthority()
    String projectId = container.getProjectId()

    System.setProperty("bigquery.host", hostAndPort)
    if (disableWriteApi) {
      System.setProperty("bigquery.enable_write_api", "false")
    } else {
      System.clearProperty("bigquery.enable_write_api")
    }
    System.setProperty("google.cloud.project.id", projectId)

    try {
      BigQueryOptions options = BigQueryOptions
          .newBuilder()
          .setProjectId(projectId)
          .setHost(fullUrl)
          .setLocation(fullUrl)
          .setCredentials(NoCredentials.getInstance())
          .build()

      Bq bq = new Bq(options)
      String mode = disableWriteApi ? 'disabled' : 'fallback'
      String action = append ? 'append' : 'overwrite'
      String dsName = "InsertAll${mode.capitalize()}${action.capitalize()}"

      bq.createDataset(dsName)

      // Add a delay to ensure the emulator service is fully ready
      Thread.sleep(2000)

      Matrix initialData = matrix('append_mode_test', [
          [1, 'Alice'],
          [2, 'Bob']
      ])
      Matrix replacementData = matrix('append_mode_test', [
          [3, 'Charlie'],
          [4, 'Delta']
      ])

      assertTrue(bq.saveToBigQuery(initialData, dsName), "Failed to save initial matrix")
      assertTrue(bq.saveToBigQuery(replacementData, dsName, append), "Failed to save replacement matrix")

      Matrix retrieved = bq.query("select * from `${projectId}.${dsName}.${replacementData.matrixName}` order by id")
          .withMatrixName(replacementData.matrixName)

      assertEquals(expectedRows.size(), retrieved.rowCount(), "Unexpected row count for ${mode}/${action}")
      expectedRows.eachWithIndex { List<Object> expectedRow, int idx ->
        assertEquals(expectedRow[0], retrieved[idx, 'id'], "Unexpected id at row ${idx}")
        assertEquals(expectedRow[1], retrieved[idx, 'name'], "Unexpected name at row ${idx}")
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt()
      throw new RuntimeException("Test interrupted during sleep", e)
    } finally {
      System.clearProperty("bigquery.host")
      System.clearProperty("bigquery.enable_write_api")
      System.clearProperty("google.cloud.project.id")
    }
  }

  private static Matrix matrix(String matrixName, List<List<Object>> rows) {
    Matrix.builder()
        .columnNames(['id', 'name'])
        .rows(rows)
        .types([Integer, String])
        .matrixName(matrixName)
        .build()
  }
}
