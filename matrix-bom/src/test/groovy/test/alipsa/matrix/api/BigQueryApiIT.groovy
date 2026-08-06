package test.alipsa.matrix.api

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.gcloud.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.bigquery.BqException
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions

import java.net.URI

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

/** Exercises the documented BigQuery emulator path when Docker is available. */
@Testcontainers
@Tag('bigquery')
@Tag('emulator')
class BigQueryApiIT implements ApiItSupport {

  @Container
  private static final BigQueryEmulatorContainer container = new BigQueryEmulatorContainer(
      'ghcr.io/goccy/bigquery-emulator:0.6.6'
  )

  @Test
  void bigQueryRoundTrip() {
    String fullUrl = container.getEmulatorHttpEndpoint()
    assertNotNull(fullUrl)
    String projectId = container.getProjectId()
    String hostAndPort = new URI(fullUrl).getAuthority()
    System.setProperty('bigquery.host', hostAndPort)
    System.setProperty('bigquery.enable_write_api', 'false')
    System.setProperty('google.cloud.project.id', projectId)
    try {
      def options = BigQueryOptions.newBuilder()
          .setProjectId(projectId)
          .setHost(fullUrl)
          .setLocation(fullUrl)
          .setCredentials(NoCredentials.getInstance())
          .build()
      Bq bq = new Bq(options)
      String datasetName = 'BomApiBigQuery'
      bq.createDataset(datasetName)
      Thread.sleep(2000L)
      Matrix data = Matrix.builder([id: [1L, 2L], name: ['alpha', 'beta']],
          [Long, String], 'bom_api_round_trip').build()
      assertTrue(bq.saveToBigQuery(data, datasetName), 'Failed to save matrix to BigQuery')
      Matrix roundTrip = bq.query("select * from `${projectId}.${datasetName}.${data.matrixName}` order by id")
          .withMatrixName(data.matrixName)
      MatrixAssertions.assertContentMatches(data, roundTrip, data.diff(roundTrip))
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt()
      throw new BqException('Test interrupted while waiting for the BigQuery emulator', exception)
    } finally {
      System.clearProperty('bigquery.host')
      System.clearProperty('bigquery.enable_write_api')
      System.clearProperty('google.cloud.project.id')
    }
  }
}
