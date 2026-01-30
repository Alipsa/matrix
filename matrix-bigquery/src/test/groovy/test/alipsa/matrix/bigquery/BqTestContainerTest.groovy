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
import static org.junit.jupiter.api.Assertions.fail

@Testcontainers
@Tag("flaky")  // BigQuery emulator testcontainer has threading issues - see issue #XXX
class BqTestContainerTest {

  // https://github.com/goccy/bigquery-emulator/pkgs/container/bigquery-emulator
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
   * Tests that when the write channel API fails (not supported by emulator),
   * the fallback to InsertAll API is triggered and succeeds.
   *
   * <p>This test deliberately does NOT set bigquery.enable_write_api=false,
   * so the write channel is attempted first, fails with a connection error,
   * and then falls back to InsertAll.</p>
   */
  @Test
  void testWriteChannelFallbackToInsertAll() {
    String fullUrl = container.getEmulatorHttpEndpoint()
    assertNotNull(fullUrl)

    URI emulatorUri = new URI(fullUrl)
    String hostAndPort = emulatorUri.getAuthority()

    String projectId = container.getProjectId()

    // Set host but deliberately do NOT set bigquery.enable_write_api=false
    // This forces the code to try write channel first, fail, and fall back to InsertAll
    System.setProperty("bigquery.host", hostAndPort)
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
      String dsName = "FallbackTest"

      bq.createDataset(dsName)

      // Add a delay to ensure the emulator service is fully ready
      Thread.sleep(2000)

      // Use a smaller dataset for this test
      Matrix testData = Matrix.builder()
          .columnNames(['id', 'name', 'value'])
          .rows([
              [1, 'Alice', 100.5],
              [2, 'Bob', 200.75],
              [3, 'Charlie', 300.25]
          ])
          .types([Integer, String, BigDecimal])
          .matrixName('fallback_test')
          .build()

      // This should:
      // 1. Try write channel API (will fail with connection error on emulator)
      // 2. Detect connection error and fall back to InsertAll
      // 3. Succeed with InsertAll
      assertTrue(bq.saveToBigQuery(testData, dsName), "Failed to save matrix - fallback should have worked")

      // Verify the data was actually inserted
      Matrix retrieved = bq.query("select * from `${projectId}.${dsName}.${testData.matrixName}` order by id")
          .withMatrixName(testData.matrixName)

      assertEquals(3, retrieved.rowCount(), "Should have inserted 3 rows")
      assertEquals('Alice', retrieved[0, 'name'], "First row name should be Alice")
      assertEquals('Bob', retrieved[1, 'name'], "Second row name should be Bob")
      assertEquals('Charlie', retrieved[2, 'name'], "Third row name should be Charlie")

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt()
      throw new RuntimeException("Test interrupted during sleep", e)
    } finally {
      // Always clean up the system properties after the test
      System.clearProperty("bigquery.host")
      System.clearProperty("bigquery.enable_write_api")
      System.clearProperty("google.cloud.project.id")
    }
  }
}
