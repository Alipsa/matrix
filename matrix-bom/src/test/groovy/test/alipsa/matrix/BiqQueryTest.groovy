package test.alipsa.matrix

import static org.junit.jupiter.api.Assertions.*
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Testcontainers
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

@Testcontainers
class BiqQueryTest {

  private static final BigQueryEmulatorContainer container = null
  //@org.testcontainers.junit.jupiter.Container
  //private static final BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3")

  // Issue with testContainers:
  // fails with se.alipsa.matrix.bigquery.BqException: Error writing value '30' (type=java.lang.Short) on row 153
  // The same test works in the real BigQuery
  @Disabled
  @Test
  void testBigQuery() {
    String url = container.getEmulatorHttpEndpoint()
    assertNotNull(url)
    BigQueryOptions options = BigQueryOptions
        .newBuilder()
        .setProjectId(container.getProjectId())
        .setHost(url)
        .setLocation(url)
        .setCredentials(NoCredentials.getInstance())
        .build()
    Bq bq = new Bq(options)
    String dsName = "BigQueryModuleTest"
    bq.createDataset(dsName)
    Matrix airq = Dataset.airquality().renameColumn('Solar.R', 'Solar_r')
    assertTrue(bq.saveToBigQuery(airq, dsName), "Failed to save matrix to big query")
    Matrix m = bq.query("select * from `${dsName}.${airq.matrixName}`")
        .withMatrixName(airq.matrixName)
    assert airq == m
  }
}
