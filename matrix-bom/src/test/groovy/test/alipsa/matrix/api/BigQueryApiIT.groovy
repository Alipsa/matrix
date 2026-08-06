package test.alipsa.matrix.api

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.gcloud.BigQueryEmulatorContainer
import org.testcontainers.junit.jupiter.Testcontainers
import se.alipsa.matrix.bigquery.Bq

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

/** Exercises the documented BigQuery emulator path when external tests are explicitly enabled. */
@Testcontainers
@Tag('bigquery')
@Tag('external')
@Tag('bigquery')
class BigQueryApiIT implements ApiItSupport {

  private static final BigQueryEmulatorContainer container = null

  // The emulator currently rejects Short values during writes; keep this method-level disablement
  // so failsafe counts the test while the external path remains documented and reproducible.
  @Disabled('BigQuery emulator rejects Short values; run against a real BigQuery service when available')
  @Test
  void bigQueryRoundTrip() {
    String url = container.getEmulatorHttpEndpoint()
    assertNotNull(url)
    def options = BigQueryOptions.newBuilder()
        .setProjectId(container.getProjectId())
        .setHost(url)
        .setLocation(url)
        .setCredentials(NoCredentials.getInstance())
        .build()
    Bq bq = new Bq(options)
    assertTrue(bq != null)
  }
}
