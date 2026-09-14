package se.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import com.fasterxml.jackson.core.JsonFactory
import com.google.api.services.bigquery.model.Dataset as DatasetPb
import com.google.api.services.bigquery.model.DatasetReference
import com.google.api.services.bigquery.model.JobStatistics as JobStatisticsPb
import com.google.api.services.bigquery.model.JobStatistics3
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryError
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Dataset
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.Job
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.JobStatistics
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * These tests live in `se.alipsa.matrix.bigquery` so they can exercise package-scope
 * routing helpers without invoking the real BigQuery write channel.
 */
@CompileStatic
class InsertAppendSemanticsTest {

  @Test
  void writeChannelReceivesAppendFalseWhenEnabled() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    RecordingInsertClient bq = new RecordingInsertClient(fakeBigQueryFor(state), 'matrix-project')

    bq.insert(sampleMatrix(), state.tableId, false)

    assertFalse(bq.lastAppendValue)
    assertEquals(0, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
  }

  @Test
  void writeChannelReceivesAppendTrueWhenEnabled() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    RecordingInsertClient bq = new RecordingInsertClient(fakeBigQueryFor(state), 'matrix-project')

    bq.insert(sampleMatrix(), state.tableId, true)

    assertTrue(bq.lastAppendValue)
    assertEquals(0, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
  }

  @Test
  void disabledWriteApiRecreatesTableForOverwrite() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    Bq bq = new Bq(fakeBigQueryFor(state), 'matrix-project')

    withWriteApiDisabled {
      BqInsertResult result = bq.insertRows(sampleMatrix(), state.tableId, false)
      assertEquals(BqInsertResult.WriteMechanism.INSERT_ALL, result.writeMechanism)
      assertEquals(2L, result.successfulRowCount)
      assertEquals(null, result.loadStatistics)
    }

    assertEquals(1, state.insertAllCalls)
    assertEquals(1, state.deleteCalls)
    assertEquals(1, state.createCalls)
    assertEquals(['getTable', 'getTable', 'delete', 'create', 'getTable', 'getTable', 'insertAll'], state.events)
    assertEquals(2, state.lastInsertRequest.rows.size())
  }

  @Test
  void disabledWriteApiLeavesTableUntouchedForAppend() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    Bq bq = new Bq(fakeBigQueryFor(state), 'matrix-project')

    withWriteApiDisabled {
      assertEquals(null, bq.insert(sampleMatrix(), state.tableId, true))
    }

    assertEquals(1, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
    assertEquals(['insertAll'], state.events)
    assertEquals(2, state.lastInsertRequest.rows.size())
  }

  @Test
  void fallbackToInsertAllPreservesOverwriteSemantics() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    RecordingInsertClient bq = new RecordingInsertClient(fakeBigQueryFor(state), 'matrix-project', true)

    bq.insert(sampleMatrix(), state.tableId, false)

    assertFalse(bq.lastAppendValue)
    assertEquals(1, state.insertAllCalls)
    assertEquals(1, state.deleteCalls)
    assertEquals(1, state.createCalls)
    assertEquals(['getTable', 'getTable', 'delete', 'create', 'getTable', 'getTable', 'insertAll'], state.events)
  }

  @Test
  void fallbackToInsertAllPreservesAppendSemantics() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    RecordingInsertClient bq = new RecordingInsertClient(fakeBigQueryFor(state), 'matrix-project', true)

    bq.insert(sampleMatrix(), state.tableId, true)

    assertTrue(bq.lastAppendValue)
    assertEquals(1, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
    assertEquals(['insertAll'], state.events)
  }

  @Test
  void fallbackFailureKeepsInsertAllFailureAsCauseAndStreamingFailureSuppressed() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    state.insertAllFailure = new IllegalStateException('InsertAll unavailable')
    RecordingInsertClient bq = new RecordingInsertClient(fakeBigQueryFor(state), 'matrix-project', true)

    BqException ex = assertThrows(BqException) {
      bq.insert(sampleMatrix(), state.tableId, true)
    }

    assertSame(state.insertAllFailure, ex.cause)
    assertEquals(1, ex.suppressed.length)
    assertTrue(ex.suppressed[0].message.contains('Simulated write-channel failure'))
    assertTrue(ex.message.contains('Fallback also failed: InsertAll unavailable'))
  }

  @Test
  void insertAllReplacementTableInfoPreservesWritableMetadata() {
    TableId tableId = tableId('events')
    TableInfo existingTable = TableInfo.newBuilder(tableId, sampleDefinition())
        .setFriendlyName('Events')
        .setDescription('Test table')
        .setExpirationTime(123456L)
        .setLabels([env: 'test'])
        .build()

    TableInfo replacement = Bq.createInsertAllReplacementTableInfo(existingTable)

    assertEquals(tableId, replacement.tableId)
    assertSame(existingTable.definition, replacement.definition)
    assertEquals('Events', replacement.friendlyName)
    assertEquals('Test table', replacement.description)
    assertEquals(123456L, replacement.expirationTime)
    assertEquals([env: 'test'], replacement.labels)
  }

  @Test
  void overwriteFailsFastWhenInsertAllTargetTableIsMissing() {
    InsertAllTestState state = new InsertAllTestState(tableId('missing'))
    state.initializeTable = false
    Bq bq = new Bq(fakeBigQueryFor(state), 'matrix-project')

    BqException ex = withWriteApiDisabled {
      assertThrows(BqException) {
        bq.insert(sampleMatrix(), state.tableId, false)
      } as BqException
    }

    assertTrue(ex.message.contains('Cannot overwrite'))
    assertEquals(0, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
  }

  @Test
  void classifyExpectedInsertAllPreconditionFailures() {
    assertTrue(Bq.isExpectedInsertAllPreconditionFailure(
        new InsertAllPreconditionException('Cannot overwrite table via InsertAll because the table does not exist')
    ))
    assertTrue(Bq.isExpectedInsertAllPreconditionFailure(
        new InsertAllPreconditionException('Failed to recreate table for InsertAll overwrite because the existing table could not be deleted')
    ))
    assertFalse(Bq.isExpectedInsertAllPreconditionFailure(
        new BqException('InsertAll failed with errors:\nrow 1: invalid value')
    ))
    assertFalse(Bq.isExpectedInsertAllPreconditionFailure(
        new RuntimeException('Connection refused')
    ))
  }

  @Test
  void insertAllUsesStableVersionedIdsAndNormalizesFallbackValues() {
    TableId target = tableId('events')

    assertEquals('v1:matrix-project.analytics.events:7:test-salt', Bq.formatInsertId(target, 7, 'test-salt'))
    assertEquals('ACTIVE', Bq.normalizeValue(Status.ACTIVE))
    assertEquals('123e4567-e89b-12d3-a456-426614174000', Bq.normalizeValue(UUID.fromString('123e4567-e89b-12d3-a456-426614174000')))
    assertEquals('["one",2]', Bq.normalizeValue(['one', 2]))
    assertEquals('{"ok":true}', Bq.normalizeValue([ok: true]))
  }

  @Test
  void insertAllSplitsRowsAtConfiguredLimit() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    Bq bq = new Bq(fakeBigQueryFor(state), 'matrix-project')
    bq.insertAllMaxRows = 1

    withWriteApiDisabled {
      bq.insertRows(sampleMatrix(), state.tableId, true)
    }

    assertEquals(2, state.insertAllCalls)
    assertEquals([1, 1], state.insertRequests*.rows*.size())
  }

  @Test
  void graceLookupReturnsAJobThatAppearsAfterTheFirstNull() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    BigQuery fakeBigQuery = fakeBigQueryFor(state)
    Job expected = jobFrom(fakeBigQuery)
    state.jobLookups.addAll([null, expected])
    Bq bq = new Bq(fakeBigQuery, 'matrix-project')
    bq.jobLookupGraceAttempts = 2
    bq.jobLookupGraceIntervalMs = 0L

    Job actual = bq.findCreatedLoadJob(JobId.of('matrix-project', 'grace-test'), state.tableId)

    assertSame(expected, actual)
    assertEquals(2, state.getJobCalls)
  }

  @Test
  void regionalDatasetLocationIsIncludedInLoadJobLookupId() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    BigQuery fakeBigQuery = fakeBigQueryFor(state)
    state.dataset = datasetFrom(fakeBigQuery, DatasetId.of('matrix-project', 'analytics'), 'europe-north1')
    RecordingInsertClient bq = new RecordingInsertClient(fakeBigQuery, 'matrix-project', true)

    bq.insertRows(sampleMatrix(), state.tableId, true)

    assertEquals('europe-north1', state.lookupJobId.location)
    assertEquals(1, state.insertAllCalls)
  }

  @Test
  void failedDatasetLocationLookupUsesTheConfiguredLocation() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    state.datasetLookupFailure = new BigQueryException(403, 'dataset access denied')
    state.optionsLocation = 'US'
    Bq bq = new Bq(fakeBigQueryFor(state), 'matrix-project')

    JobId jobId = bq.createLoadJobId(state.tableId)

    assertEquals('US', jobId.location)
  }

  @Test
  void existingLoadJobReturnsItsOutcomeWithoutInsertAllSubmission() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    ExistingJobClient bq = new ExistingJobClient(fakeBigQueryFor(state), 'matrix-project', jobFrom(fakeBigQueryFor(state)), loadStatistics(2L))

    BqInsertResult result = bq.insertRows(sampleMatrix(), state.tableId, true)

    assertEquals(BqInsertResult.WriteMechanism.WRITE_CHANNEL, result.writeMechanism)
    assertEquals(2L, result.successfulRowCount)
    assertEquals(0, state.insertAllCalls)
    assertEquals(1, bq.waitCalls)
  }

  @Test
  void lookupAndWaitFailuresPreserveTheOriginalWriteChannelFailure() {
    InsertAllTestState lookupState = new InsertAllTestState(tableId('events'))
    lookupState.lookupFailure = new IllegalStateException('lookup unavailable')
    RecordingInsertClient lookupClient = new RecordingInsertClient(fakeBigQueryFor(lookupState), 'matrix-project', true)

    BqException lookupException = assertThrows(BqException) {
      lookupClient.insertRows(sampleMatrix(), lookupState.tableId, true)
    }

    assertTrue(lookupException.message.startsWith('Write outcome is unknown'))
    assertSame(lookupState.lookupFailure, lookupException.cause)
    assertEquals(1, lookupException.suppressed.length)
    assertTrue(lookupException.suppressed[0].message.contains('Simulated write-channel failure'))
    assertEquals(0, lookupState.insertAllCalls)

    InsertAllTestState waitState = new InsertAllTestState(tableId('events'))
    BqException waitFailure = new BqException('wait unavailable')
    ExistingJobClient waitClient = new ExistingJobClient(fakeBigQueryFor(waitState), 'matrix-project', jobFrom(fakeBigQueryFor(waitState)), waitFailure)

    BqException waitException = assertThrows(BqException) {
      waitClient.insertRows(sampleMatrix(), waitState.tableId, true)
    }

    assertTrue(waitException.message.startsWith('Write outcome is unknown'))
    assertSame(waitFailure, waitException.cause)
    assertEquals(1, waitException.suppressed.length)
    assertTrue(waitException.suppressed[0].message.contains('Simulated write-channel failure'))
    assertEquals(0, waitState.insertAllCalls)
  }

  @Test
  void insertAllSplitsByBytesAndUsesGlobalErrorRowNumbers() {
    InsertAllTestState splitState = new InsertAllTestState(tableId('events'))
    Bq splitClient = new Bq(fakeBigQueryFor(splitState), 'matrix-project')
    splitClient.insertAllTargetRequestBytes = 200

    withWriteApiDisabled {
      splitClient.insertRows(longStringMatrix(), splitState.tableId, true)
    }

    assertEquals(2, splitState.insertAllCalls)
    assertEquals([1, 1], splitState.insertRequests*.rows*.size())

    InsertAllTestState errorState = new InsertAllTestState(tableId('events'))
    errorState.insertAllResponses.addAll([
        emptyInsertAllResponse(),
        insertAllResponse([(0L): [new BigQueryError('invalid', 'name', 'invalid value')]])
    ])
    Bq errorClient = new Bq(fakeBigQueryFor(errorState), 'matrix-project')
    errorClient.insertAllMaxRows = 1

    BqException error = withWriteApiDisabled {
      assertThrows(BqException) {
        errorClient.insertRows(sampleMatrix(), errorState.tableId, true)
      } as BqException
    }

    assertTrue(error.message.contains('Row 1: invalid value'))
  }

  @Test
  void insertAllRejectsASingleRowAtTheTenMiBLimit() {
    InsertAllTestState state = new InsertAllTestState(tableId('events'))
    Bq bq = new Bq(fakeBigQueryFor(state), 'matrix-project')
    Matrix oversized = Matrix.builder()
        .columnNames(['name'])
        .rows([['x' * (10 * 1024 * 1024)]])
        .types([String])
        .matrixName('events')
        .build()

    BqException exception = withWriteApiDisabled {
      assertThrows(BqException) {
        bq.insertRows(oversized, state.tableId, true)
      } as BqException
    }

    assertTrue(exception.message.contains('exceeding BigQuery'))
    assertEquals(0, state.insertAllCalls)
  }

  @Test
  void insertAllSizeEstimateSupportsEmptyContent() {
    String insertId = 'row-id'

    assertTrue(Bq.estimateInsertAllRowBytes([:], insertId) > insertId.length())
  }

  @Test
  void writeChannelSerializerUsesTheSameFallbackValuesAsInsertAll() {
    ByteArrayOutputStream output = new ByteArrayOutputStream()
    def json = new JsonFactory().createGenerator(output)
    UUID uuid = UUID.fromString('123e4567-e89b-12d3-a456-426614174000')
    Map<String, Object> map = [ok: true, count: 2]

    json.writeStartArray()
    [uuid, Status.ACTIVE, ['one', 2], map].each { value -> Bq.writeJsonValue(json, value) }
    json.writeEndArray()
    json.close()

    assertEquals('["123e4567-e89b-12d3-a456-426614174000","ACTIVE","[\\"one\\",2]","{\\"ok\\":true,\\"count\\":2}"]', output.toString('UTF-8'))
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

  private static Matrix longStringMatrix() {
    Matrix.builder()
        .columnNames(['name'])
        .rows([['a' * 100], ['b' * 100]])
        .types([String])
        .matrixName('events')
        .build()
  }

  private static TableId tableId(String tableName) {
    Bq.tableId('matrix-project', 'analytics', tableName)
  }

  private static TableDefinition sampleDefinition() {
    StandardTableDefinition.of(Schema.of(
        Field.of('id', StandardSQLTypeName.INT64),
        Field.of('name', StandardSQLTypeName.STRING)
    ))
  }

  private static <T> T withWriteApiDisabled(Closure<T> action) {
    String previous = System.getProperty('bigquery.enable_write_api')
    System.setProperty('bigquery.enable_write_api', 'false')
    try {
      action.call()
    } finally {
      if (previous == null) {
        System.clearProperty('bigquery.enable_write_api')
      } else {
        System.setProperty('bigquery.enable_write_api', previous)
      }
    }
  }

  @SuppressWarnings('ClassName')
  private static class RecordingInsertClient extends Bq {
    boolean throwConnectionError
    Boolean lastAppendValue

    RecordingInsertClient(BigQuery bigQuery, String projectId, boolean throwConnectionError = false) {
      super(bigQuery, projectId)
      this.throwConnectionError = throwConnectionError
      jobLookupGraceAttempts = 1
      jobLookupGraceIntervalMs = 0L
    }

    @Override
    JobStatistics.LoadStatistics insertViaWriteChannel(Matrix matrix, TableId tableId, boolean append, JobId jobId) throws BqException {
      lastAppendValue = append
      if (throwConnectionError) {
        throw new BqException('Simulated write-channel failure', new ConnectException('Connection refused'))
      }
      loadStatistics(matrix.rowCount())
    }
  }

  @SuppressWarnings('ClassName')
  private static final class ExistingJobClient extends RecordingInsertClient {
    private final Job existingJob
    private final JobStatistics.LoadStatistics statistics
    private final BqException failure
    int waitCalls

    ExistingJobClient(BigQuery bigQuery, String projectId, Job existingJob, JobStatistics.LoadStatistics statistics) {
      super(bigQuery, projectId, true)
      this.existingJob = existingJob
      this.statistics = statistics
      this.failure = null
    }

    ExistingJobClient(BigQuery bigQuery, String projectId, Job existingJob, BqException failure) {
      super(bigQuery, projectId, true)
      this.existingJob = existingJob
      this.statistics = null
      this.failure = failure
    }

    @Override
    Job findCreatedLoadJob(JobId jobId, TableId tableId) {
      existingJob
    }

    @Override
    JobStatistics.LoadStatistics waitForLoadJobAndGetStats(Job job, TableId tableId) throws BqException {
      waitCalls++
      if (failure != null) {
        throw failure
      }
      statistics
    }
  }

  private enum Status {
    ACTIVE
  }

  @SuppressWarnings('ClassName')
  private static final class InsertAllTestState {
    final TableId tableId
    boolean initializeTable = true
    Table currentTable
    int deleteCalls
    int createCalls
    int insertAllCalls
    Exception insertAllFailure
    Exception lookupFailure
    BigQueryException datasetLookupFailure
    Dataset dataset
    String optionsLocation
    JobId lookupJobId
    InsertAllRequest lastInsertRequest
    final List<InsertAllRequest> insertRequests = []
    final List<InsertAllResponse> insertAllResponses = []
    final List<Job> jobLookups = []
    int getJobCalls
    final List<String> events = []

    InsertAllTestState(TableId tableId) {
      this.tableId = tableId
    }
  }

  @CompileDynamic
  private static BigQuery fakeBigQueryFor(InsertAllTestState state) {
    BigQueryOptions.Builder optionsBuilder = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(NoCredentials.getInstance())
    if (state.optionsLocation != null) {
      optionsBuilder.setLocation(state.optionsLocation)
    }
    BigQueryOptions options = optionsBuilder.build()
    BigQuery fakeBigQuery
    fakeBigQuery = [
        getOptions: { -> options },
        getTable : { TableId requestedTableId, Object... ignored ->
          state.events << 'getTable'
          state.currentTable
        },
        getJob   : { JobId ignored, Object... ignoredOptions ->
          state.getJobCalls++
          state.lookupJobId = ignored
          if (state.lookupFailure != null) {
            throw state.lookupFailure
          }
          state.jobLookups.isEmpty() ? null : state.jobLookups.remove(0)
        },
        getDataset: { DatasetId ignored, Object... ignoredOptions ->
          if (state.datasetLookupFailure != null) {
            throw state.datasetLookupFailure
          }
          state.dataset
        },
        delete   : { TableId requestedTableId ->
          state.events << 'delete'
          state.deleteCalls++
          state.currentTable = null
          true
        },
        create   : { Object info, Object... ignored ->
          assertTrue(info instanceof TableInfo)
          state.events << 'create'
          state.createCalls++
          state.currentTable = tableFrom(fakeBigQuery, (TableInfo) info)
          state.currentTable
        },
        insertAll: { InsertAllRequest request ->
          state.events << 'insertAll'
          state.insertAllCalls++
          state.lastInsertRequest = request
          state.insertRequests << request
          if (state.insertAllFailure != null) {
            throw state.insertAllFailure
          }
          state.insertAllResponses.isEmpty() ? emptyInsertAllResponse() : state.insertAllResponses.remove(0)
        }
    ] as BigQuery
    if (state.initializeTable && state.currentTable == null && state.tableId != null) {
      state.currentTable = tableFrom(fakeBigQuery, TableInfo.newBuilder(state.tableId, sampleDefinition()).build())
    }
    fakeBigQuery
  }

  @CompileDynamic
  private static Table tableFrom(BigQuery bigQuery, TableInfo tableInfo) {
    Method toPb = TableInfo.getDeclaredMethod('toPb')
    toPb.setAccessible(true)
    Object tablePb = toPb.invoke(tableInfo)

    Method fromPb = Table.getDeclaredMethod('fromPb', BigQuery, tablePb.getClass())
    fromPb.setAccessible(true)
    Table table = (Table) fromPb.invoke(null, bigQuery, tablePb)
    assertNotNull(table)
    table
  }

  @CompileDynamic
  private static InsertAllResponse emptyInsertAllResponse() {
    insertAllResponse([:])
  }

  @CompileDynamic
  private static InsertAllResponse insertAllResponse(Map<Long, List<BigQueryError>> errors) {
    Constructor<InsertAllResponse> ctor = (Constructor<InsertAllResponse>) InsertAllResponse.declaredConstructors[0]
    ctor.setAccessible(true)
    ctor.newInstance(errors)
  }

  @CompileDynamic
  private static Dataset datasetFrom(BigQuery bigQuery, DatasetId datasetId, String location) {
    DatasetPb datasetPb = new DatasetPb()
        .setDatasetReference(new DatasetReference()
            .setProjectId(datasetId.project)
            .setDatasetId(datasetId.dataset))
        .setLocation(location)
    Method fromPb = Dataset.getDeclaredMethod('fromPb', BigQuery, datasetPb.getClass())
    fromPb.setAccessible(true)
    (Dataset) fromPb.invoke(null, bigQuery, datasetPb)
  }

  @CompileDynamic
  private static JobStatistics.LoadStatistics loadStatistics(long outputRows) {
    JobStatisticsPb statisticsPb = new JobStatisticsPb()
        .setLoad(new JobStatistics3().setOutputRows(outputRows))
    Method fromPb = JobStatistics.LoadStatistics.getDeclaredMethod('fromPb', statisticsPb.getClass())
    fromPb.setAccessible(true)
    (JobStatistics.LoadStatistics) fromPb.invoke(null, statisticsPb)
  }

  @CompileDynamic
  private static Job jobFrom(BigQuery bigQuery) {
    JobInfo.Builder builder = JobInfo.newBuilder(QueryJobConfiguration.newBuilder('select 1').build())
        .setJobId(JobId.of('matrix-project', 'grace-test'))
    Constructor<Job> ctor = (Constructor<Job>) Job.declaredConstructors.find { it.parameterCount == 2 }
    ctor.setAccessible(true)
    ctor.newInstance(bigQuery, builder)
  }
}
