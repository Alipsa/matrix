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
}
