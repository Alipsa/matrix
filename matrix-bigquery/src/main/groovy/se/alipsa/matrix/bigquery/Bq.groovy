package se.alipsa.matrix.bigquery

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.google.api.gax.paging.Page
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.*
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption
import com.google.cloud.bigquery.BigQuery.DatasetListOption
import com.google.cloud.bigquery.BigQuery.TableListOption
import com.google.cloud.resourcemanager.v3.Project
import com.google.cloud.resourcemanager.v3.ProjectsClient
import com.google.cloud.resourcemanager.v3.ProjectsSettings
import groovy.transform.CompileStatic
import me.tongfei.progressbar.ProgressBar
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.util.Logger

import java.nio.channels.Channels
import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Collections
import static se.alipsa.matrix.bigquery.TypeMapper.*
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert

/**
 * BigQuery client wrapper providing Matrix-oriented operations for Google BigQuery.
 *
 * <p>This class provides a high-level API for interacting with BigQuery, including:
 * <ul>
 *   <li>Executing queries and returning results as Matrix objects</li>
 *   <li>Inserting Matrix data into BigQuery tables</li>
 *   <li>Managing datasets and tables (create, drop, check existence)</li>
 *   <li>Schema creation based on Matrix column types</li>
 * </ul>
 *
 * <h2>Query Execution Modes</h2>
 * <p>The class supports two query execution modes controlled by the {@code useAsyncQueries} parameter:</p>
 * <ul>
 *   <li><b>Synchronous (default)</b>: 10 GB response size limit, compatible with BigQuery emulators.
 *       Best for development/testing and smaller datasets.</li>
 *   <li><b>Asynchronous</b>: No size limit, recommended for production use with large datasets.
 *       Enable with {@code new Bq(true)} or {@code new Bq(projectId, true)}.</li>
 * </ul>
 *
 * <h2>Insert Strategy</h2>
 * <p>Data insertion uses a two-tier fallback strategy:</p>
 * <ol>
 *   <li><b>Write Channel API</b> (primary): Streams NDJSON data for optimal performance with large datasets.</li>
 *   <li><b>InsertAll API</b> (fallback): Used when write channel fails due to connection errors,
 *       common with BigQuery emulators.</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p><b>This class is NOT thread-safe.</b> Each thread should use its own Bq instance.
 * The underlying BigQuery client is thread-safe, but this wrapper maintains state
 * (projectId, useAsyncQueries) that could cause issues if shared across threads.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create client using environment variable GOOGLE_CLOUD_PROJECT
 * Bq bq = new Bq()
 *
 * // Or with explicit project ID and async mode for production
 * Bq bq = new Bq("my-project-id", true)
 *
 * // Query data
 * Matrix result = bq.query("SELECT * FROM dataset.table LIMIT 100")
 *
 * // Insert data
 * bq.saveToBigQuery(myMatrix, "my_dataset")
 * }</pre>
 *
 * @see Matrix
 * @see TypeMapper
 */
@CompileStatic
class Bq {

  private static final Logger log = Logger.getLogger(Bq)

  /** Date formatter for BigQuery DATE type (yyyy-MM-dd). Not thread-safe - use only within single-threaded context. */
  static final SimpleDateFormat bqSimpledateFormat = new SimpleDateFormat("yyyy-MM-dd")

  /** Date formatter for BigQuery DATE type using java.time API. Thread-safe. */
  static final DateTimeFormatter bqDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /** DateTime formatter for BigQuery DATETIME type with microsecond precision. Thread-safe. */
  static final DateTimeFormatter bqDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

  /** Time formatter for BigQuery TIME type with optional microsecond precision. Thread-safe. */
  static final DateTimeFormatter bqTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss[.SSSSSS]")

  private BigQuery bigQuery
  private String projectId
  private boolean useAsyncQueries = false

  /**
   * Creates a Bq instance from pre-configured BigQueryOptions.
   *
   * <p>Use this constructor when you need full control over BigQuery client configuration,
   * such as custom retry settings, timeouts, or transport options.</p>
   *
   * @param options pre-configured BigQueryOptions
   * @param useAsyncQueries if true, uses asynchronous job-based queries (no size limit);
   *        if false (default), uses synchronous queries (10 GB limit, emulator compatible)
   */
  Bq(BigQueryOptions options, boolean useAsyncQueries = false) {
    bigQuery = options.getService()
    projectId = options.getProjectId()
    this.useAsyncQueries = useAsyncQueries
  }

  /**
   * Creates a Bq instance with explicit credentials.
   *
   * <p>Use this constructor when you need to authenticate with specific credentials,
   * such as a service account key file or impersonated credentials.</p>
   *
   * @param credentials Google Cloud credentials for authentication
   * @param projectId the Google Cloud project ID
   * @param useAsyncQueries if true, uses asynchronous job-based queries (no size limit);
   *        if false (default), uses synchronous queries (10 GB limit, emulator compatible)
   */
  Bq(GoogleCredentials credentials, String projectId, boolean useAsyncQueries = false) {
    this.projectId = projectId
    this.useAsyncQueries = useAsyncQueries
    bigQuery = BigQueryOptions.newBuilder()
        .setCredentials(credentials)
        .setProjectId(projectId)
        .build()
        .getService()
  }

  /**
   * Creates a Bq instance with a specific project ID using Application Default Credentials.
   *
   * <p>Credentials are resolved using Google Cloud's Application Default Credentials (ADC) mechanism,
   * which checks (in order): GOOGLE_APPLICATION_CREDENTIALS environment variable, gcloud CLI credentials,
   * or compute engine/cloud shell default service account.</p>
   *
   * @param projectId the Google Cloud project ID (must not be null)
   * @param useAsyncQueries if true, uses asynchronous job-based queries (no size limit);
   *        if false (default), uses synchronous queries (10 GB limit, emulator compatible)
   * @throws IllegalArgumentException if projectId is null
   */
  Bq(String projectId, boolean useAsyncQueries = false) {
    if (projectId == null) {
      throw new IllegalArgumentException("ProjectId cannot be null")
    }
    this.projectId = projectId
    this.useAsyncQueries = useAsyncQueries
    bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .getService()
  }

  /**
   * Creates a Bq instance using the GOOGLE_CLOUD_PROJECT environment variable.
   *
   * <p>This is the simplest constructor for environments where the project ID
   * is already configured via environment variables (e.g., Cloud Run, GKE, local development
   * with gcloud configured).</p>
   *
   * @param useAsyncQueries if true, uses asynchronous job-based queries (no size limit);
   *        if false (default), uses synchronous queries (10 GB limit, emulator compatible)
   * @throws RuntimeException if GOOGLE_CLOUD_PROJECT environment variable is not set
   */
  Bq(boolean useAsyncQueries = false) {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      throw new RuntimeException("Please set the environment variable GOOGLE_CLOUD_PROJECT prior to creating this class (or pass it as a parameter)")
    }
    this.projectId = projectId
    this.useAsyncQueries = useAsyncQueries
    bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .getService()
  }

  /**
   * Execute an insert, update or delete query. Technically it is possible to
   * also execute a select query but then only the number of rows in the result
   * will be returned so doing that is not very useful. Note: If your sql contains multiple statements,
   * the total number of rows affected will be returned. The job statistics that we retrieve via
   * queryJob.getStatistics().getDmlStats() will provide the aggregate count of all affected rows
   * across all statements. There is no built-in mechanism in the BigQuery client library to get
   * a row count for each individual statement within a single job.
   *
   * <p>Query execution mode is controlled by the useAsyncQueries flag:</p>
   * <ul>
   * <li>Synchronous (default, useAsyncQueries=false): 10 GB response size limit, compatible with BigQuery emulators</li>
   * <li>Asynchronous (useAsyncQueries=true): No size limit, recommended for production use with large datasets</li>
   * </ul>
   *
   * @param qry the query to execute
   * @param useLegacySql Sets whether to use BigQuery's legacy SQL dialect for this query.
   * @return the number of rows affected by the query
   * @throws BqException if the query fails
   */
  int execute(String qry, boolean useLegacySql = false) throws BqException {
    try {
      if (useAsyncQueries) {
        // Async job-based approach: no size limit, recommended for production
        Job queryJob = runQuery(qry, useLegacySql)

        // Retrieve the job statistics to check for DML operations.
        JobStatistics.QueryStatistics stats = queryJob.getStatistics()
        DmlStats dmlStats = stats.getDmlStats()

        if (dmlStats != null) {
          // This is a DML query (INSERT, UPDATE, or DELETE).
          // We get the affected row count from the DML stats.
          long insertedRows = dmlStats.getInsertedRowCount() != null ? dmlStats.getInsertedRowCount() : 0
          long updatedRows = dmlStats.getUpdatedRowCount() != null ? dmlStats.getUpdatedRowCount() : 0
          long deletedRows = dmlStats.getDeletedRowCount() != null ? dmlStats.getDeletedRowCount() : 0

          return (int) (insertedRows + updatedRows + deletedRows)
        } else {
          // This is likely a SELECT query.
          // We get the result set and return the total number of rows.
          TableResult result = queryJob.getQueryResults()
          return result.getTotalRows().intValue()
        }
      } else {
        // Synchronous approach: 10 GB response size limit, emulator compatible
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(qry)
            .setUseLegacySql(useLegacySql)
            .build()

        TableResult result = bigQuery.query(queryConfig)

        // When running a query synchronously, TableResult.getTotalRows() returns
        // either the number of rows returned (SELECT) or the number of rows affected (DML/DDL).
        return result.getTotalRows().intValue()
      }

    } catch (BigQueryException | InterruptedException e) {
      throw new BqException("Query execution failed due to error: " + e.toString(), e)
    }
  }

  private Job runQuery(String qry, boolean useLegacySql) throws BqException {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(qry)
        .setUseLegacySql(useLegacySql)
        .build()
    JobId jobId = JobId.newBuilder().setProject(projectId).build()
    Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build())
    // Wait for the query to complete.
    queryJob = queryJob.waitFor()

    // Check for errors
    if (queryJob == null) {
      throw new BqException("Job no longer exists")
    } else if (queryJob.getStatus().getError() != null) {
      throw new BqException(queryJob.getStatus().getError().toString())
    }
    queryJob
  }

  /**
   * Execute a query and return the results as a Matrix.
   *
   * <p>Query execution mode is controlled by the useAsyncQueries flag:</p>
   * <ul>
   * <li>Synchronous (default, useAsyncQueries=false): 10 GB response size limit, compatible with BigQuery emulators</li>
   * <li>Asynchronous (useAsyncQueries=true): No size limit, recommended for production use with large datasets</li>
   * </ul>
   *
   * @param qry the query to execute
   * @param useLegacySql Sets whether to use BigQuery's legacy SQL dialect for this query.
   * @return Matrix containing the query results
   * @throws BqException if the query fails
   */
  Matrix query(String qry, boolean useLegacySql = false) throws BqException {
    try {
      TableResult result

      if (useAsyncQueries) {
        // Async job-based approach: no size limit, recommended for production
        Job queryJob = runQuery(qry, useLegacySql)
        result = queryJob.getQueryResults()
      } else {
        // Synchronous approach: 10 GB response size limit, emulator compatible
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(qry)
            .setUseLegacySql(useLegacySql)
            .build()

        result = bigQuery.query(queryConfig)
      }

      // Convert to a Matrix and return the results.
      return convertToMatrix(result)

    } catch (BigQueryException | InterruptedException e) {
      throw new BqException("Query failed due to error: " + e.toString(), e)
    }
  }

  /**
   * Saves a Matrix to BigQuery, creating the table if it doesn't exist.
   *
   * <p>This is a convenience method that handles the complete workflow:
   * <ol>
   *   <li>Checks if the table exists (using {@link #tableExist})</li>
   *   <li>Creates the table with auto-generated schema if needed (using {@link #createTable})</li>
   *   <li>Waits for the table to be ready (newly created tables may have propagation delay)</li>
   *   <li>Inserts the data (using {@link #insert})</li>
   * </ol>
   *
   * <p>The table name is derived from {@link Matrix#getMatrixName()}. The schema is
   * automatically generated based on the Matrix column types using {@link TypeMapper#toStandardSqlType}.</p>
   *
   * @param matrix the Matrix to save (matrixName will be used as table name)
   * @param datasetName the BigQuery dataset name
   * @return true on success
   * @throws BqException if any step fails
   * @see #createTable
   * @see #insert
   */
  boolean saveToBigQuery(Matrix matrix, String datasetName) throws BqException {
    String tableName = matrix.matrixName
    if (!tableExist(datasetName, tableName)) {
      TableSchema defs = createTable(matrix, datasetName)
      waitForTable(defs.table.tableId)
    }
    TableId tableId = TableId.of(projectId, datasetName, tableName)
    insert(matrix, tableId)
    return true
  }

  /**
   * Waits for a table to become available after creation.
   *
   * <p>Newly created BigQuery tables may not be immediately available due to
   * eventual consistency. This method polls until the table exists or timeout occurs.</p>
   *
   * <h3>Backoff Strategy</h3>
   * <p>Uses exponential backoff starting at 300ms, multiplied by 1.7x each iteration,
   * capped at 5 seconds between attempts. This balances responsiveness (fast initial checks)
   * with efficiency (reduced API calls for slow propagation).</p>
   *
   * <p>Example backoff sequence: 300ms → 510ms → 867ms → 1.5s → 2.5s → 4.2s → 5s → 5s...</p>
   *
   * @param tableId the table identifier to wait for
   * @param timeoutMs maximum time to wait in milliseconds (default: 60 seconds)
   * @throws BqException if the table is not ready within the timeout period
   */
  private void waitForTable(TableId tableId, long timeoutMs = 60_000L) {
    long start = System.currentTimeMillis()
    long backoff = 300L
    while (System.currentTimeMillis() - start < timeoutMs) {
      Table t = bigQuery.getTable(tableId)
      if (t != null && t.exists()) return
      Thread.sleep(backoff)
      backoff = Math.min((long)(backoff * 1.7), 5000L)
    }
    throw new BqException("Timed out waiting for table ${tableId} to be ready.")
  }

  /**
   * Creates a BigQuery table based on a Matrix structure using auto-generated schema.
   *
   * <p>Convenience method that uses the instance's projectId.</p>
   *
   * @param matrix the Matrix whose structure defines the table schema (matrixName becomes table name)
   * @param datasetName the BigQuery dataset name
   * @return TableSchema containing the created table and schema
   * @throws BqException if table creation fails
   * @see #createSchema
   */
  TableSchema createTable(Matrix matrix, String datasetName) throws BqException {
    createTable(matrix, datasetName, projectId)
  }

  /**
   * Creates a BigQuery table based on a Matrix structure with auto-generated schema.
   *
   * <p>The schema is automatically generated from the Matrix column types using
   * {@link TypeMapper#toStandardSqlType}.</p>
   *
   * @param matrix the Matrix whose structure defines the table schema (matrixName becomes table name)
   * @param datasetName the BigQuery dataset name
   * @param projectId the Google Cloud project ID
   * @return TableSchema containing the created table and schema
   * @throws BqException if table creation fails
   * @see #createSchema
   */
  TableSchema createTable(Matrix matrix, String datasetName, String projectId) throws BqException {
    Schema schema = createSchema(matrix)
    createTable(matrix, datasetName, projectId, schema)
  }

  /**
   * Creates a BigQuery table with a custom schema.
   *
   * <p>Use this method when you need explicit control over the BigQuery schema,
   * such as specifying nullable fields, descriptions, or custom type mappings.</p>
   *
   * @param matrix the Matrix (matrixName becomes table name)
   * @param datasetName the BigQuery dataset name
   * @param projectId the Google Cloud project ID
   * @param schema the BigQuery schema to use for the table
   * @return TableSchema containing the created table and schema
   * @throws BqException if table creation fails
   */
  TableSchema createTable(Matrix matrix, String datasetName, String projectId, Schema schema) throws BqException {
    try {
      String tableName = matrix.matrixName
      TableId tableId = TableId.of(projectId, datasetName, tableName)
      TableDefinition tableDefinition = StandardTableDefinition.of(schema)
      TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
      Table table = bigQuery.create(tableInfo)
      log.info("Table $datasetName.$tableName created successfully")
      return new TableSchema(table, schema)
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Inserts data from a Matrix into a BigQuery table.
   *
   * <p>Convenience method that uses the instance's projectId. The table must already exist.</p>
   *
   * @param matrix the Matrix containing data to insert (matrixName must match table name)
   * @param dataSet the BigQuery dataset name
   * @return load statistics from the insert operation
   * @throws BqException if the insert fails
   * @see #insert(Matrix, TableId)
   */
  JobStatistics.LoadStatistics insert(Matrix matrix, String dataSet) throws BqException {
    insert(matrix, dataSet, projectId)
  }

  /**
   * Inserts data from a Matrix into a BigQuery table.
   *
   * <p>This method first attempts to use the write channel API for streaming JSON data.
   * If a connection error occurs (common with emulators), it automatically falls back
   * to the InsertAll API.</p>
   *
   * @param matrix the Matrix containing data to insert
   * @param tableId the BigQuery table identifier
   * @return load statistics from the insert operation
   * @throws BqException if both insert methods fail
   */
  JobStatistics.LoadStatistics insert(Matrix matrix, TableId tableId) throws BqException {
    try {
      return insertViaWriteChannel(matrix, tableId)
    } catch (Exception e) {
      if (isConnectionError(e)) {
        log.warn("Streaming insert failed with connection error. Falling back to InsertAll...")
        return insertViaInsertAll(matrix, tableId, e)
      }
      throw e
    }
  }

  /**
   * Checks if an exception represents a connection error that warrants fallback.
   *
   * @param e the exception to check
   * @return true if this is a connection-related error
   */
  private static boolean isConnectionError(Exception e) {
    return e.cause instanceof ConnectException || e.message?.contains("Connection refused")
  }

  /**
   * Inserts data using BigQuery's write channel API with NDJSON format.
   *
   * <p>This is the preferred method for large datasets as it supports streaming
   * and has better performance characteristics.</p>
   *
   * @param matrix the Matrix containing data to insert
   * @param tableId the BigQuery table identifier
   * @return load statistics from the insert operation
   * @throws BqException if the insert fails
   */
  private JobStatistics.LoadStatistics insertViaWriteChannel(Matrix matrix, TableId tableId) throws BqException {
    int rowIdx = 0
    Object lastValue = null
    final BigDecimal tickPercent = 0.05

    WriteChannelConfiguration wcfg = WriteChannelConfiguration.newBuilder(tableId)
        .setFormatOptions(FormatOptions.json())
        .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE)
        .build()

    JobId jobId = JobId.newBuilder().setProject(projectId).setRandomJob().build()
    TableDataWriteChannel writer = bigQuery.writer(jobId, wcfg)

    try {
      OutputStream out = Channels.newOutputStream(writer)
      JsonGenerator json = new JsonFactory().createGenerator(out, JsonEncoding.UTF8)

      List<String> columnNames = matrix.columnNames()
      int rowCount = matrix.rowCount()
      int stepSize = Math.max(1, (int) (rowCount * tickPercent))
      ProgressBar pb = new ProgressBar("Inserting into ${tableId.dataset}.${tableId.table}", rowCount)

      try {
        for (Row row : matrix.rows()) {
          rowIdx++
          lastValue = writeRowAsJson(json, row, columnNames)

          if (rowIdx % stepSize == 0) {
            pb.stepBy(stepSize)
          }
        }
        pb.stepTo(rowIdx)
      } finally {
        pb.close()
        json.flush()
        json.close()
      }

      return waitForLoadJobAndGetStats(writer, tableId)

    } catch (Exception e) {
      throw new BqException("Error writing value '${lastValue}' (type=${lastValue?.class?.name}) on row ${rowIdx}", e)
    }
  }

  /**
   * Writes a single row to the JSON generator in NDJSON format.
   *
   * @param json the JSON generator
   * @param row the row data
   * @param columnNames the column names to write
   * @return the last value written (for error reporting)
   */
  private static Object writeRowAsJson(JsonGenerator json, Row row, List<String> columnNames) {
    Object lastValue = null
    json.writeStartObject()

    for (String name : columnNames) {
      Object value = row[name]
      lastValue = value
      json.writeFieldName(name)
      writeJsonValue(json, value)
    }

    json.writeEndObject()
    json.writeRaw('\n')
    return lastValue
  }

  /**
   * Writes a single value to the JSON generator with appropriate type handling.
   *
   * @param json the JSON generator
   * @param value the value to write
   */
  private static void writeJsonValue(JsonGenerator json, Object value) {
    if (needsConversion(value)) {
      json.writeString(sanitizeString(convertObjectValue(value)))
    } else if (value instanceof Number) {
      json.writeNumber(value.toString())
    } else if (value instanceof Boolean) {
      json.writeBoolean((Boolean) value)
    } else if (value == null) {
      json.writeNull()
    } else if (value instanceof byte[]) {
      json.writeBinary(value as byte[])
    } else if (value instanceof CharSequence) {
      json.writeString(sanitizeString(value.toString()))
    } else {
      json.writeObject(value)
    }
  }

  /**
   * Waits for a load job to complete and returns the statistics.
   *
   * @param writer the table data write channel
   * @param tableId the table identifier (for error messages)
   * @return load statistics from the completed job
   * @throws BqException if the job fails
   */
  private JobStatistics.LoadStatistics waitForLoadJobAndGetStats(TableDataWriteChannel writer, TableId tableId) throws BqException {
    Job loadJob = writer.getJob().waitFor()

    JobStatus status = loadJob.getStatus()
    BigQueryError primary = status?.getError()
    List<BigQueryError> execErrs = status?.getExecutionErrors()

    if (primary != null || (execErrs != null && !execErrs.isEmpty())) {
      String details = buildErrorDetails(primary, execErrs)
      throw new BqException("Write-channel load (JSON) failed: ${details}")
    }

    JobStatistics.LoadStatistics stats = loadJob.getStatistics()
    long rowsInserted = stats.getOutputRows()
    log.info("Load job completed successfully. Inserted $rowsInserted rows")
    return stats
  }

  /**
   * Builds a detailed error message from BigQuery errors.
   *
   * @param primary the primary error (may be null)
   * @param execErrs execution errors (may be null or empty)
   * @return formatted error details string
   */
  private static String buildErrorDetails(BigQueryError primary, List<BigQueryError> execErrs) {
    List<BigQueryError> allErrors = []
    if (primary != null) {
      allErrors << primary
    }
    if (execErrs != null) {
      allErrors.addAll(execErrs)
    }
    return allErrors
        .collect { e -> "${e.message} (reason=${e.reason}, location=${e.location})" }
        .join("; ")
  }

  /**
   * Inserts data using BigQuery's InsertAll API.
   *
   * <p>This is the fallback method used when write channel fails (e.g., with emulators).
   * It's less efficient for large datasets but more compatible.</p>
   *
   * @param matrix the Matrix containing data to insert
   * @param tableId the BigQuery table identifier
   * @param originalException the original exception that triggered the fallback
   * @return load statistics (placeholder, as InsertAll doesn't provide detailed stats)
   * @throws BqException if the insert fails
   */
  private JobStatistics.LoadStatistics insertViaInsertAll(Matrix matrix, TableId tableId, Exception originalException) throws BqException {
    try {
      List<String> columnNames = matrix.columnNames()
      List<RowToInsert> rows = matrix.rows().collect { Row row ->
        RowToInsert.of(convertRowForInsertAll(row, columnNames))
      }

      InsertAllRequest request = InsertAllRequest.newBuilder(tableId)
          .setRows(rows)
          .build()

      InsertAllResponse response = bigQuery.insertAll(request)

      if (response.hasErrors()) {
        String errorDetails = buildInsertAllErrorDetails(response)
        throw new BqException("InsertAll fallback failed with errors:\n${errorDetails}")
      }

      log.info("InsertAll fallback successful. Inserted ${matrix.rowCount()} rows")
      return createPlaceholderLoadStatistics(tableId)

    } catch (Exception fallbackException) {
      log.error("InsertAll fallback also failed: ${fallbackException.message}")
      throw new BqException("Streaming insert failed: ${originalException.message}. Fallback also failed: ${fallbackException.message}", originalException)
    }
  }

  /**
   * Converts a matrix row to a map suitable for InsertAll API.
   *
   * @param row the row data
   * @param columnNames the column names
   * @return a map with converted values
   */
  private static Map<String, Object> convertRowForInsertAll(Row row, List<String> columnNames) {
    Map<String, Object> content = new LinkedHashMap<>()

    for (String name : columnNames) {
      Object val = row[name]
      if (needsConversion(val)) {
        content.put(name, sanitizeString(convertObjectValue(val)))
      } else if (val instanceof CharSequence) {
        content.put(name, sanitizeString(val.toString()))
      } else {
        content.put(name, val)
      }
    }

    return content
  }

  /**
   * Builds error details from an InsertAllResponse.
   *
   * @param response the response containing errors
   * @return formatted error details string
   */
  private static String buildInsertAllErrorDetails(InsertAllResponse response) {
    return response.getInsertErrors().collect { Map.Entry<Long, List<BigQueryError>> entry ->
      String rowErrors = entry.getValue().collect { it.getMessage() }.join(", ")
      "Row ${entry.getKey()}: ${rowErrors}"
    }.join("\n")
  }

  /**
   * Creates a placeholder LoadStatistics for InsertAll operations.
   *
   * <p>InsertAll doesn't provide detailed statistics like write channel does,
   * so we create a minimal placeholder to maintain API compatibility.</p>
   *
   * @param tableId the table identifier
   * @return placeholder load statistics (with null values)
   */
  private static JobStatistics.LoadStatistics createPlaceholderLoadStatistics(TableId tableId) {
    List<String> emptySourceUris = Collections.emptyList()
    LoadJobConfiguration placeholderConfig = LoadJobConfiguration
        .newBuilder(tableId, emptySourceUris)
        .setFormatOptions(FormatOptions.json())
        .build()
    JobInfo placeholderJobInfo = JobInfo.newBuilder(placeholderConfig).build()
    return (JobStatistics.LoadStatistics) placeholderJobInfo.getStatistics()
  }

  /**
   * Checks if a value requires conversion before insertion into BigQuery.
   *
   * <p>BigQuery's JSON insert API requires certain types to be converted to strings
   * in specific formats. This method identifies values that need such conversion.</p>
   *
   * <h3>Types requiring conversion:</h3>
   * <ul>
   *   <li>{@link BigDecimal} - to avoid scientific notation</li>
   *   <li>{@link BigInteger} - to string representation</li>
   *   <li>{@link java.sql.Time} - to HH:mm:ss[.SSSSSS] format</li>
   *   <li>{@link LocalDateTime} - to yyyy-MM-dd'T'HH:mm:ss.SSSSSS format</li>
   *   <li>{@link LocalTime} - to HH:mm:ss[.SSSSSS] format</li>
   *   <li>{@link Instant} - to ISO-8601 format</li>
   *   <li>{@link Timestamp} - to ISO-8601 format</li>
   *   <li>{@link ZonedDateTime} - to ISO-8601 format with timezone</li>
   *   <li>{@link Date} - to yyyy-MM-dd format</li>
   *   <li>{@link LocalDate} - to yyyy-MM-dd format</li>
   * </ul>
   *
   * @param orgVal the value to check
   * @return true if the value requires conversion via {@link #convertObjectValue}
   * @see #convertObjectValue
   */
  static boolean needsConversion(Object orgVal) {
    return orgVal instanceof BigDecimal
        || orgVal instanceof BigInteger
        || orgVal instanceof Time
        || orgVal instanceof LocalDateTime
        || orgVal instanceof LocalTime
        || orgVal instanceof Instant
        || orgVal instanceof Timestamp
        || orgVal instanceof ZonedDateTime
        || orgVal instanceof Date
        || orgVal instanceof LocalDate
  }

  /**
   * Converts a Java object to a string format suitable for BigQuery JSON insertion.
   *
   * <p>BigQuery's JSON API requires specific string formats for certain data types.
   * This method performs the necessary conversions:</p>
   *
   * <h3>Type Conversion Rules:</h3>
   * <table border="1">
   *   <tr><th>Java Type</th><th>Output Format</th><th>Example</th></tr>
   *   <tr><td>BigDecimal</td><td>Plain string (no scientific notation)</td><td>"123456.789"</td></tr>
   *   <tr><td>BigInteger</td><td>String representation</td><td>"123456789"</td></tr>
   *   <tr><td>LocalDate</td><td>yyyy-MM-dd</td><td>"2024-01-15"</td></tr>
   *   <tr><td>LocalDateTime</td><td>yyyy-MM-dd'T'HH:mm:ss.SSSSSS</td><td>"2024-01-15T10:30:00.000000"</td></tr>
   *   <tr><td>LocalTime</td><td>HH:mm:ss[.SSSSSS]</td><td>"10:30:00.123456"</td></tr>
   *   <tr><td>Timestamp</td><td>ISO-8601 instant</td><td>"2024-01-15T10:30:00Z"</td></tr>
   *   <tr><td>ZonedDateTime</td><td>ISO-8601 with zone</td><td>"2024-01-15T10:30:00+01:00[Europe/Paris]"</td></tr>
   *   <tr><td>Time (SQL)</td><td>HH:mm:ss[.SSSSSS]</td><td>"10:30:00"</td></tr>
   *   <tr><td>Date (java.util)</td><td>yyyy-MM-dd</td><td>"2024-01-15"</td></tr>
   *   <tr><td>Other</td><td>String.valueOf()</td><td>varies</td></tr>
   * </table>
   *
   * <p><b>Note:</b> Time values are truncated to microsecond precision as BigQuery
   * does not support nanoseconds.</p>
   *
   * @param orgVal the value to convert
   * @return string representation suitable for BigQuery JSON insertion
   * @see #needsConversion
   */
  static String convertObjectValue(Object orgVal) {
    if (orgVal instanceof BigDecimal) {
      BigDecimal val = (BigDecimal) orgVal
      // toPlainString() is crucial to avoid scientific notation
      return val.toPlainString()
    }
    if (orgVal instanceof BigInteger) {
      BigInteger val = (BigInteger) orgVal
      return val.toString()
    }
    if (orgVal instanceof LocalDate) {
      LocalDate date = (LocalDate) orgVal
      return date.format(bqDateFormatter)
    }
    if (orgVal instanceof LocalDateTime) {
      LocalDateTime date = (LocalDateTime) orgVal
      return date.format(bqDateTimeFormatter)
    }
    if (orgVal instanceof Timestamp) {
      Timestamp ts = (Timestamp) orgVal
      return ts.toInstant().toString()
    }
    if (orgVal instanceof ZonedDateTime) {
      ZonedDateTime ts = (ZonedDateTime) orgVal
      return ts.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
    if (orgVal instanceof LocalTime) {
      LocalTime t = ((LocalTime) orgVal).truncatedTo(ChronoUnit.MICROS)
      return bqTimeFormatter.format(t)
    }
    // Time is a subclass of Date so must come before Date
    if (orgVal instanceof Time) {
      Time t = (Time) orgVal
      LocalTime lt = t.toLocalTime().truncatedTo(ChronoUnit.MICROS)
      return bqTimeFormatter.format(lt)
    }
    if (orgVal instanceof Date) {
      Date date = (Date) orgVal
      return bqSimpledateFormat.format(date)
    }
    String.valueOf(orgVal)
  }

  /**
   * Inserts data from a Matrix into a BigQuery table with explicit project ID.
   *
   * <p>The table must already exist. Use {@link #saveToBigQuery} for automatic table creation.</p>
   *
   * @param matrix the Matrix containing data to insert (matrixName must match table name)
   * @param dataSet the BigQuery dataset name
   * @param projectId the Google Cloud project ID
   * @return load statistics from the insert operation
   * @throws BqException if the insert fails
   * @see #insert(Matrix, TableId)
   */
  JobStatistics.LoadStatistics insert(Matrix matrix, String dataSet, String projectId) throws BqException {
    String tableName = matrix.matrixName
    TableId tableId = TableId.of(projectId, dataSet, tableName)
    insert(matrix, tableId)
  }

  /**
   * Converts a BigQuery TableResult to a Matrix.
   *
   * <p>This method handles type conversion from BigQuery types to Java types using
   * {@link TypeMapper#convertType} and {@link TypeMapper#convertFieldValue}.</p>
   *
   * @param result the BigQuery query result
   * @return a Matrix containing the query results with appropriate Java types
   * @see TypeMapper#convertType
   * @see TypeMapper#convertFieldValue
   */
  static Matrix convertToMatrix(TableResult result) {
    Schema schema = result.getSchema()
    List<String> colNames = []
    List<LegacySQLTypeName> colTypes = []
    schema.fields.each {
      colNames << it.name
      colTypes << it.type
    }

    List<List> rows = []
    List<Object> row
    for (FieldValueList fvl : result.iterateAll()) {
      row = []
      int i = 0
      for (FieldValue fv : fvl.iterator()) {
        row << convertFieldValue(fv, colTypes[i++])
      }
      rows << row
    }
    Matrix.builder()
        .rows(rows)
        .types(colTypes.collect{convertType(it)})
        .columnNames(colNames)
        .build()
  }

  /**
   * Lists all dataset names in the current project.
   *
   * @return list of dataset names, empty list if none exist
   * @throws BqException if the API call fails
   */
  List<String> getDatasets() throws BqException {
    try {
      Page<Dataset> datasets = bigQuery.listDatasets(projectId, DatasetListOption.pageSize(100))
      if (datasets == null) {
        log.debug("Dataset does not contain any models")
        return []
      }
      return datasets
          .iterateAll()
          .collect {
            it.getDatasetId().dataset
          }
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Lists all Google Cloud projects accessible with current credentials.
   *
   * <p>Requires the Resource Manager API to be enabled and appropriate IAM permissions.</p>
   *
   * @return list of Project objects
   * @throws BqException if the API call fails
   */
  List<Project> getProjects() throws BqException {
    ProjectsSettings projectsSettings = ProjectsSettings.newBuilder().build()
    try (ProjectsClient pc = ProjectsClient.create(projectsSettings)) {
      return pc.searchProjects("").iterateAll().collect()
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Lists all table names in a dataset.
   *
   * @param datasetName the dataset to list tables from
   * @return list of table names
   * @throws BqException if the API call fails or dataset doesn't exist
   */
  List<String> getTableNames(String datasetName) throws BqException {
    try {
      DatasetId datasetId = DatasetId.of(projectId, datasetName)
      Page<Table> tables = bigQuery.listTables(datasetId, TableListOption.pageSize(100))
      return tables.iterateAll().collect {
        it.tableId.table
      }
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Retrieves column metadata for a table from INFORMATION_SCHEMA.
   *
   * <p>Returns column information including name, data type, nullability, and ordinal position.</p>
   *
   * @param datasetName the dataset containing the table
   * @param tableName the table name
   * @return Matrix containing column metadata from INFORMATION_SCHEMA.COLUMNS
   * @throws BqException if the query fails
   */
  Matrix getTableInfo(String datasetName, String tableName) throws BqException {
    query("""select * from ${datasetName}.INFORMATION_SCHEMA.COLUMNS
      WHERE table_name = '$tableName';
      """)
  }

  /**
   * Retrieves a Dataset object for the specified dataset name.
   *
   * @param datasetName the dataset name
   * @return the Dataset object, or null if not found
   * @throws BqException if the API call fails
   */
  Dataset getDataset(String datasetName) throws BqException {
    try {
      bigQuery.getDataset(datasetName)
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Retrieves a Dataset.Builder for modifying an existing dataset.
   *
   * <p>Use this to update dataset properties like description, labels, or access controls.</p>
   *
   * @param datasetName the dataset name
   * @return Dataset.Builder for the specified dataset
   * @throws BqException if the dataset doesn't exist or API call fails
   * @see #updateDataset
   */
  Dataset.Builder getDatasetBuilder(String datasetName) throws BqException {
    getDataset(datasetName).toBuilder()
  }

  /**
   * Creates a new dataset or returns existing one if it already exists.
   *
   * <p>This method is idempotent - calling it multiple times with the same dataset name
   * will not cause errors.</p>
   *
   * @param datasetName the name for the new dataset
   * @param description optional description for the dataset (null for no description)
   * @return the created or existing Dataset
   * @throws BqException if creation fails (other than already existing)
   */
  Dataset createDataset(String datasetName, String description = null) throws BqException {
    try {
      Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetName))
      if (dataset != null) {
        log.debug("Dataset $datasetName already exists")
        return dataset
      }
      DatasetInfo.Builder builder = DatasetInfo.newBuilder(datasetName)
      if (description != null) {
        builder.setDescription(description)
      }
      DatasetInfo datasetInfo = builder.build()
      Dataset ds = bigQuery.create(datasetInfo)
      String newDatasetName = ds.getDatasetId().getDataset()
      log.info("$newDatasetName created successfully")
      ds
    } catch (BigQueryException e) {
      throw new BqException("Dataset was not created: " + e.toString(), e)
    }
  }

  /**
   * Updates an existing dataset with modified properties.
   *
   * <p>Obtain a builder using {@link #getDatasetBuilder}, modify properties, then call this method.</p>
   *
   * @param ds the Dataset.Builder with updated properties
   * @return the updated Dataset
   * @throws BqException if the update fails
   * @see #getDatasetBuilder
   */
  Dataset updateDataset(Dataset.Builder ds) throws BqException {
    try {
      bigQuery.update(ds.build())
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Creates a BigQuery Schema from a Matrix structure.
   *
   * <p>Maps Matrix column types to BigQuery types using {@link TypeMapper#toStandardSqlType}.
   * All fields are created as NULLABLE by default.</p>
   *
   * @param matrix the Matrix to generate schema from
   * @return BigQuery Schema matching the Matrix structure
   * @see TypeMapper#toStandardSqlType
   */
  static Schema createSchema(Matrix matrix) {
    List<Field> fields = []
    matrix.columns().each { c ->
      fields << Field.of(c.name, toStandardSqlType(c.type))
    }
    Schema.of(fields as Field[])
  }

  /**
   * Deletes a dataset and all its contents.
   *
   * <p><b>Warning:</b> This permanently deletes the dataset and ALL tables within it.
   * This operation cannot be undone.</p>
   *
   * @param datasetName the dataset to delete
   * @return true if deleted, false if dataset was not found
   * @throws BqException if the deletion fails
   */
  boolean dropDataset(String datasetName) throws BqException {
    try {
      DatasetId datasetId = DatasetId.of(projectId, datasetName)
      boolean success = bigQuery.delete(datasetId, DatasetDeleteOption.deleteContents())
      if (success) {
        log.info("Dataset $datasetName deleted successfully")
      } else {
        log.warn("Dataset $datasetName was not found")
      }
      return success
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Deletes a table from a dataset.
   *
   * <p>Convenience method that uses the Matrix's matrixName as the table name.</p>
   *
   * @param datasetName the dataset containing the table
   * @param matrix the Matrix whose matrixName identifies the table to delete
   * @return true if deleted, false if table was not found
   * @throws BqException if the deletion fails
   */
  boolean dropTable(String datasetName, Matrix matrix) throws BqException {
    dropTable(datasetName, matrix.matrixName)
  }

  /**
   * Deletes a table from a dataset.
   *
   * <p><b>Warning:</b> This permanently deletes the table and all its data.
   * This operation cannot be undone.</p>
   *
   * @param datasetName the dataset containing the table
   * @param tableName the table to delete
   * @return true if deleted, false if table was not found
   * @throws BqException if the deletion fails
   */
  boolean dropTable(String datasetName, String tableName) throws BqException {
    try {
      boolean success = bigQuery.delete(TableId.of(datasetName, tableName))
      if (success) {
        log.info("Table $datasetName.$tableName deleted successfully")
      } else {
        log.warn("Table $datasetName.$tableName was not found")
      }
      return success
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Checks if a dataset exists.
   *
   * @param datasetName the dataset name to check
   * @return true if the dataset exists, false otherwise
   * @throws BqException if the API call fails
   */
  boolean datasetExist(String datasetName) throws BqException {
    try {
      Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetName))
      return dataset != null
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Checks if a table exists in a dataset.
   *
   * @param datasetName the dataset name
   * @param tableName the table name to check
   * @return true if the table exists, false otherwise
   * @throws BqException if the API call fails
   */
  boolean tableExist(String datasetName, String tableName) throws BqException {
    try {
      Table table = bigQuery.getTable(TableId.of(datasetName, tableName))
      return table != null && table.exists()
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  /**
   * Removes Unicode control characters from a string.
   *
   * <p>BigQuery's JSON API cannot handle certain Unicode control characters (category C),
   * which include:</p>
   * <ul>
   *   <li>Control characters (U+0000-U+001F, U+007F-U+009F)</li>
   *   <li>Format characters (e.g., zero-width joiners)</li>
   *   <li>Private use characters</li>
   *   <li>Surrogates and non-characters</li>
   * </ul>
   *
   * <p>These characters commonly appear in data imported from external sources
   * (web scraping, legacy systems, binary data incorrectly interpreted as text)
   * and cause JSON serialization errors during BigQuery insertion.</p>
   *
   * <p>This method uses the regex pattern {@code \p{C}} which matches all Unicode
   * characters in the "Other" general category (Cc, Cf, Cs, Co, Cn).</p>
   *
   * @param input the object to sanitize (toString() is called)
   * @return sanitized string with control characters removed, or null if input is null
   */
  static String sanitizeString(Object input) {
    if (input == null) return null
    String str = input.toString()
    return str.replaceAll("\\p{C}", "")
  }
}
