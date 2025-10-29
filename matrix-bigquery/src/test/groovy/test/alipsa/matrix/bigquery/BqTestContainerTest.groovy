package test.alipsa.matrix.bigquery

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
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
class BqTestContainerTest {

  // --- Reverting to the original BQ Emulator Container ---
  // https://github.com/goccy/bigquery-emulator/pkgs/container/bigquery-emulator
  @Container
  private static final BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.6")

  @Test
  void testCreateAndRetrieve() {
    String fullUrl = container.getEmulatorHttpEndpoint()
    assertNotNull(fullUrl)

    // Extract authority (host:port) without the http:// prefix.
    URI emulatorUri = new URI(fullUrl)
    String hostAndPort = emulatorUri.getAuthority()

    // Define projectId explicitly for use in options and query
    String projectId = container.getProjectId()

    // --- Configuration for BigQuery Emulator ---

    // CRITICAL 1: Set the system property to the raw host:port string (used by low-level data transfer).
    System.setProperty("bigquery.host", hostAndPort)

    // CRITICAL 2: Explicitly disable the newer gRPC-based Write API.
    // This forces the client to use the older, HTTP/JSON streaming path.
    System.setProperty("bigquery.enable_write_api", "false")

    // CRITICAL 4: Set the project ID as a system property to ensure the client resolves project context for queries.
    System.setProperty("google.cloud.project.id", projectId)

    try {
      BigQueryOptions options = BigQueryOptions
          .newBuilder()
          .setProjectId(projectId)
      // CRITICAL 3: Use the FULL URL for the host setting (required for metadata/query API calls).
          .setHost(fullUrl)
          .setLocation(fullUrl) // Explicitly set location to the emulator URL
          .setCredentials(NoCredentials.getInstance())
          .build()

      Bq bq = new Bq(options)
      String dsName = "BigQueryModuleTest"

      // This line should now succeed with setHost(fullUrl)
      bq.createDataset(dsName)

      // Added a small delay to ensure the emulator service is fully ready for streaming inserts
      Thread.sleep(500)

      Matrix airq = Dataset.airquality().rename('Solar.R', 'Solar_r')

      // This line relies on the bigquery.host system property for the low-level socket connection
      // It now correctly falls back to InsertAll if streaming insert fails.
      assertTrue(bq.saveToBigQuery(airq, dsName), "Failed to save matrix to big query")

      // FIX: Use fully qualified table name including projectId to prevent NullPointerException
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
