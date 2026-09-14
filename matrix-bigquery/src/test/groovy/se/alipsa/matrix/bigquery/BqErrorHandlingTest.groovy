package se.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryError
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Job
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobStatistics
import com.google.cloud.bigquery.JobStatus
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
  void missingDatasetHasContextualError() {
    Bq bq = new Bq(fakeBigQueryForMissingDataset(), 'matrix-project')

    BqException ex = assertThrows(BqException) {
      bq.getDatasetBuilder('missing')
    }

    assertEquals('Dataset matrix-project:missing does not exist', ex.message)
  }

  @Test
  void loadJobLifecycleFailuresAreReportedWithContext() {
    TableId tableId = Bq.tableId('matrix-project', 'analytics', 'events')
    Bq bq = new Bq(fakeBigQueryForWriter(), 'matrix-project')
    TableDataWriteChannel writer = mock(TableDataWriteChannel)
    when(writer.getJob()).thenReturn(null)

    BqException missing = assertThrows(BqException) {
      bq.waitForLoadJobAndGetStats(writer, tableId)
    }

    assertEquals("Load job was not created for ${tableId}", missing.message)

    Job deletedJob = mock(Job)
    when(deletedJob.waitFor()).thenReturn(null)
    BqException deleted = assertThrows(BqException) {
      bq.waitForLoadJobAndGetStats(deletedJob, tableId)
    }

    assertEquals("Load job no longer exists for ${tableId}", deleted.message)

    Job completedJob = mock(Job)
    when(completedJob.status).thenReturn(null)
    when(completedJob.statistics).thenReturn(null)
    Job waitingJob = mock(Job)
    when(waitingJob.waitFor()).thenReturn(completedJob)
    BqException missingStatistics = assertThrows(BqException) {
      bq.waitForLoadJobAndGetStats(waitingJob, tableId)
    }

    assertEquals("Load job completed without statistics for ${tableId}", missingStatistics.message)
  }

  @Test
  void completedLoadJobFailureUsesThePublicDefiniteOutcomeType() {
    TableId tableId = Bq.tableId('matrix-project', 'analytics', 'events')
    JobStatus status = mock(JobStatus)
    when(status.error).thenReturn(new BigQueryError('invalid', 'name', 'schema mismatch'))
    Job completedJob = mock(Job)
    when(completedJob.status).thenReturn(status)
    Job waitingJob = mock(Job)
    when(waitingJob.waitFor()).thenReturn(completedJob)
    Bq bq = new Bq(fakeBigQueryForWriter(), 'matrix-project')

    LoadJobFailedException exception = assertThrows(LoadJobFailedException) {
      bq.waitForLoadJobAndGetStats(waitingJob, tableId)
    }

    assertTrue(exception.message.contains('schema mismatch'))
  }

  @Test
  void synchronousQueryAndExecuteRestoreTheInterruptFlag() {
    Bq bq = new Bq(fakeBigQueryThatInterrupts(), 'matrix-project')

    try {
      BqException executeFailure = assertThrows(BqException) {
        bq.execute('delete from analytics.events')
      }
      assertTrue(executeFailure.cause instanceof InterruptedException)
      assertTrue(Thread.currentThread().isInterrupted())
      Thread.interrupted()

      BqException queryFailure = assertThrows(BqException) {
        bq.query('select * from analytics.events')
      }
      assertTrue(queryFailure.cause instanceof InterruptedException)
      assertTrue(Thread.currentThread().isInterrupted())
    } finally {
      Thread.interrupted()
    }
  }

  @Test
  void projectSettingsUseExplicitCredentialsWhenAvailable() {
    GoogleCredentials credentials = GoogleCredentials.create(new AccessToken('token-value', new Date(System.currentTimeMillis() + 60_000L)))
    Bq bq = new Bq(credentials, 'matrix-project')

    ProjectsSettings settings = bq.createProjectsSettings()

    assertSame(credentials, settings.credentialsProvider.credentials)
  }

  @Test
  void projectSettingsDoNotReuseBigQueryOptionsCredentials() {
    GoogleCredentials credentials = GoogleCredentials.create(new AccessToken('token-value', new Date(System.currentTimeMillis() + 60_000L)))
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(credentials)
        .build()
    Bq bq = new Bq(options)

    ProjectsSettings settings = bq.createProjectsSettings()

    assertFalse(settings.credentialsProvider instanceof FixedCredentialsProvider)
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
        getDataset: { Object... ignored -> null },
        writer    : { JobId jobId, WriteChannelConfiguration config -> null as TableDataWriteChannel }
    ] as BigQuery
  }

  @CompileDynamic
  private static BigQuery fakeBigQueryForMissingDataset() {
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(NoCredentials.getInstance())
        .build()
    [
        getOptions : { -> options },
        getDataset: { Object... ignored -> null }
    ] as BigQuery
  }

  @CompileDynamic
  private static BigQuery fakeBigQueryThatInterrupts() {
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(NoCredentials.getInstance())
        .build()
    [
        getOptions: { -> options },
        query     : { Object ignored, Object... ignoredOptions -> throw new InterruptedException('test interruption') }
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
