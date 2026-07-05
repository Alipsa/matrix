package se.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobStatistics
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableDataWriteChannel
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.WriteChannelConfiguration
import com.google.cloud.resourcemanager.v3.ProjectsSettings
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

/**
 * Tests error-handling behavior in the BigQuery client wrapper.
 */
@CompileStatic
class BqErrorHandlingTest {

  @Test
  void waitForTableWrapsInterruptedExceptionAndRestoresInterruptStatus() {
    Bq bq = new Bq(fakeBigQueryForWait(), 'matrix-project')

    try {
      Thread.currentThread().interrupt()
      BqException ex = assertThrows(BqException) {
        bq.waitForTable(Bq.tableId('matrix-project', 'analytics', 'events'), 10_000L)
      }

      assertTrue(ex.message.contains('Interrupted while waiting for table'))
      assertTrue(ex.cause instanceof InterruptedException)
      assertTrue(Thread.currentThread().isInterrupted())
    } finally {
      Thread.interrupted()
    }
  }

  @Test
  void writeChannelLoadJobFailuresKeepLoadJobMessage() {
    BqException loadFailure = new BqException('Write-channel load (JSON) failed for matrix-project:analytics.events: schema mismatch')
    LoadFailureClient bq = new LoadFailureClient(fakeBigQueryForWriter(), 'matrix-project', loadFailure)

    BqException ex = assertThrows(BqException) {
      bq.insertViaWriteChannel(sampleMatrix(), Bq.tableId('matrix-project', 'analytics', 'events'), false)
    }

    assertSame(loadFailure, ex)
    assertFalse(ex.message.contains('Error writing value'))
  }

  @Test
  void writeChannelSetupFailuresAreWrappedAsBqException() {
    IllegalStateException setupFailure = new IllegalStateException('stream unavailable')
    SetupFailureClient bq = new SetupFailureClient(fakeBigQueryForWriter(), 'matrix-project', setupFailure)

    BqException ex = assertThrows(BqException) {
      bq.insertViaWriteChannel(sampleMatrix(), Bq.tableId('matrix-project', 'analytics', 'events'), false)
    }

    assertTrue(ex.message.contains('Error opening BigQuery write channel'))
    assertSame(setupFailure, ex.cause)
  }

  @Test
  void projectSettingsUseExplicitCredentialsWhenAvailable() {
    GoogleCredentials credentials = GoogleCredentials.create(new AccessToken('token-value', new Date(System.currentTimeMillis() + 60_000L)))
    Bq bq = new Bq(credentials, 'matrix-project')

    ProjectsSettings settings = bq.createProjectsSettings()

    assertSame(credentials, settings.credentialsProvider.credentials)
  }

  private static Matrix sampleMatrix() {
    Matrix.builder()
        .columnNames(['id', 'name'])
        .rows([
            [1, 'Alice'],
            [2, 'Bob']
        ])
        .types([Integer, String])
        .matrixName('events')
        .build()
  }

  @CompileDynamic
  private static BigQuery fakeBigQueryForWait() {
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(NoCredentials.getInstance())
        .build()
    [
        getOptions: { -> options },
        getTable : { TableId tableId, Object... ignored -> null as Table }
    ] as BigQuery
  }

  @CompileDynamic
  private static BigQuery fakeBigQueryForWriter() {
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(NoCredentials.getInstance())
        .build()
    [
        getOptions: { -> options },
        writer    : { JobId jobId, WriteChannelConfiguration config -> null as TableDataWriteChannel }
    ] as BigQuery
  }

  @SuppressWarnings('ClassName')
  private static final class LoadFailureClient extends Bq {
    private final BqException loadFailure

    LoadFailureClient(BigQuery bigQuery, String projectId, BqException loadFailure) {
      super(bigQuery, projectId)
      this.loadFailure = loadFailure
    }

    @Override
    OutputStream openWriterStream(TableDataWriteChannel writer) {
      new ByteArrayOutputStream()
    }

    @Override
    JobStatistics.LoadStatistics waitForLoadJobAndGetStats(TableDataWriteChannel writer, TableId tableId) throws BqException {
      throw loadFailure
    }
  }

  @SuppressWarnings('ClassName')
  private static final class SetupFailureClient extends Bq {
    private final RuntimeException setupFailure

    SetupFailureClient(BigQuery bigQuery, String projectId, RuntimeException setupFailure) {
      super(bigQuery, projectId)
      this.setupFailure = setupFailure
    }

    @Override
    OutputStream openWriterStream(TableDataWriteChannel writer) {
      throw setupFailure
    }
  }
}
