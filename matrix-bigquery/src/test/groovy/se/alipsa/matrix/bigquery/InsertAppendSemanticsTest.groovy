package se.alipsa.matrix.bigquery

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.JobStatistics
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
import java.net.ConnectException

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * These tests live in `se.alipsa.matrix.bigquery` so they can exercise package-scope
 * routing helpers without invoking the real BigQuery write channel.
 */
@CompileStatic
class InsertAppendSemanticsTest {

  @Test
  void writeChannelReceivesAppendFalseWhenEnabled() {
    FakeBigQueryState state = new FakeBigQueryState(tableId('events'))
    RecordingBq bq = new RecordingBq(createFakeBigQuery(state), 'matrix-project')

    bq.insert(sampleMatrix(), state.tableId, false)

    assertFalse(bq.lastAppendValue)
    assertEquals(0, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
  }

  @Test
  void writeChannelReceivesAppendTrueWhenEnabled() {
    FakeBigQueryState state = new FakeBigQueryState(tableId('events'))
    RecordingBq bq = new RecordingBq(createFakeBigQuery(state), 'matrix-project')

    bq.insert(sampleMatrix(), state.tableId, true)

    assertTrue(bq.lastAppendValue)
    assertEquals(0, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
  }

  @Test
  void disabledWriteApiRecreatesTableForOverwrite() {
    FakeBigQueryState state = new FakeBigQueryState(tableId('events'))
    Bq bq = new Bq(createFakeBigQuery(state), 'matrix-project')

    withWriteApiDisabled {
      bq.insert(sampleMatrix(), state.tableId, false)
    }

    assertEquals(1, state.insertAllCalls)
    assertEquals(1, state.deleteCalls)
    assertEquals(1, state.createCalls)
    assertEquals(['getTable', 'getTable', 'delete', 'create', 'getTable', 'getTable', 'insertAll'], state.events)
    assertEquals(2, state.lastInsertRequest.rows.size())
  }

  @Test
  void disabledWriteApiLeavesTableUntouchedForAppend() {
    FakeBigQueryState state = new FakeBigQueryState(tableId('events'))
    Bq bq = new Bq(createFakeBigQuery(state), 'matrix-project')

    withWriteApiDisabled {
      bq.insert(sampleMatrix(), state.tableId, true)
    }

    assertEquals(1, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
    assertEquals(['insertAll'], state.events)
    assertEquals(2, state.lastInsertRequest.rows.size())
  }

  @Test
  void fallbackToInsertAllPreservesOverwriteSemantics() {
    FakeBigQueryState state = new FakeBigQueryState(tableId('events'))
    RecordingBq bq = new RecordingBq(createFakeBigQuery(state), 'matrix-project', true)

    bq.insert(sampleMatrix(), state.tableId, false)

    assertFalse(bq.lastAppendValue)
    assertEquals(1, state.insertAllCalls)
    assertEquals(1, state.deleteCalls)
    assertEquals(1, state.createCalls)
    assertEquals(['getTable', 'getTable', 'delete', 'create', 'getTable', 'getTable', 'insertAll'], state.events)
  }

  @Test
  void fallbackToInsertAllPreservesAppendSemantics() {
    FakeBigQueryState state = new FakeBigQueryState(tableId('events'))
    RecordingBq bq = new RecordingBq(createFakeBigQuery(state), 'matrix-project', true)

    bq.insert(sampleMatrix(), state.tableId, true)

    assertTrue(bq.lastAppendValue)
    assertEquals(1, state.insertAllCalls)
    assertEquals(0, state.deleteCalls)
    assertEquals(0, state.createCalls)
    assertEquals(['insertAll'], state.events)
  }

  @Test
  void createInsertAllReplacementTableInfoPreservesWritableMetadata() {
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
    FakeBigQueryState state = new FakeBigQueryState(tableId('missing'))
    state.initializeTable = false
    Bq bq = new Bq(createFakeBigQuery(state), 'matrix-project')

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

  private static class RecordingBq extends Bq {
    boolean throwConnectionError
    Boolean lastAppendValue

    RecordingBq(BigQuery bigQuery, String projectId, boolean throwConnectionError = false) {
      super(bigQuery, projectId)
      this.throwConnectionError = throwConnectionError
    }

    @Override
    JobStatistics.LoadStatistics insertViaWriteChannel(Matrix matrix, TableId tableId, boolean append) throws BqException {
      lastAppendValue = append
      if (throwConnectionError) {
        throw new BqException('Simulated write-channel failure', new ConnectException('Connection refused'))
      }
      null
    }
  }

  private static final class FakeBigQueryState {
    final TableId tableId
    boolean initializeTable = true
    Table currentTable
    int deleteCalls
    int createCalls
    int insertAllCalls
    InsertAllRequest lastInsertRequest
    final List<String> events = []

    FakeBigQueryState(TableId tableId) {
      this.tableId = tableId
    }
  }

  @CompileDynamic
  private static BigQuery createFakeBigQuery(FakeBigQueryState state) {
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId('matrix-project')
        .setCredentials(NoCredentials.getInstance())
        .build()
    BigQuery fakeBigQuery
    fakeBigQuery = [
        getOptions: { -> options },
        getTable : { TableId requestedTableId, Object... ignored ->
          state.events << 'getTable'
          state.currentTable
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
          state.currentTable = createTable(fakeBigQuery, (TableInfo) info)
          state.currentTable
        },
        insertAll: { InsertAllRequest request ->
          state.events << 'insertAll'
          state.insertAllCalls++
          state.lastInsertRequest = request
          emptyInsertAllResponse()
        }
    ] as BigQuery
    if (state.initializeTable && state.currentTable == null && state.tableId != null) {
      state.currentTable = createTable(fakeBigQuery, TableInfo.newBuilder(state.tableId, sampleDefinition()).build())
    }
    fakeBigQuery
  }

  @CompileDynamic
  private static Table createTable(BigQuery bigQuery, TableInfo tableInfo) {
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
    Constructor<InsertAllResponse> ctor = (Constructor<InsertAllResponse>) InsertAllResponse.declaredConstructors[0]
    ctor.setAccessible(true)
    ctor.newInstance([:])
  }
}
