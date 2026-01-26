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

@CompileStatic
class Bq {

  private static final Logger log = Logger.getLogger(Bq)

  // BigQuery serializes complex datatypes into structs so we must convert things like BigDecimal, Date, LocalDate etc
  // into plain text strings that BigQuery will understand when inserting data
  static final SimpleDateFormat bqSimpledateFormat = new SimpleDateFormat("yyyy-MM-dd")
  static final DateTimeFormatter bqDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  static final DateTimeFormatter bqDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  static final DateTimeFormatter bqTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss[.SSSSSS]")

  private BigQuery bigQuery
  private String projectId
  private boolean useAsyncQueries = false

  Bq(BigQueryOptions options, boolean useAsyncQueries = false) {
    bigQuery = options.getService()
    projectId = options.getProjectId()
    this.useAsyncQueries = useAsyncQueries
  }

  Bq(GoogleCredentials credentials, String projectId, boolean useAsyncQueries = false) {
    this.projectId = projectId
    this.useAsyncQueries = useAsyncQueries
    bigQuery = BigQueryOptions.newBuilder()
        .setCredentials(credentials)
        .setProjectId(projectId)
        .build()
        .getService()
  }

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

  private void waitForTable(TableId tableId, long timeoutMs = 60_000L) {
    long start = System.currentTimeMillis()
    long backoff = 300L
    while (System.currentTimeMillis() - start < timeoutMs) {
      def t = bigQuery.getTable(tableId)
      if (t != null && t.exists()) return
      Thread.sleep(backoff)
      backoff = Math.min((long)(backoff * 1.7), 5000L)
    }
    throw new BqException("Timed out waiting for table ${tableId} to be ready.")
  }

  TableSchema createTable(Matrix matrix, String datasetName) throws BqException {
    createTable(matrix, datasetName, projectId)
  }

  TableSchema createTable(Matrix matrix, String datasetName, String projectId) throws BqException {
    Schema schema = createSchema(matrix)
    createTable(matrix, datasetName, projectId, schema)
  }

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

  JobStatistics.LoadStatistics insert(Matrix matrix, String dataSet) throws BqException {
    insert(matrix, dataSet, projectId)
  }

  JobStatistics.LoadStatistics insert(Matrix matrix, TableId tableId) throws BqException {
    int rowIdx = 0
    Object value = null
    final BigDecimal tickPercent = 0.05
    try {
      def wcfg = WriteChannelConfiguration.newBuilder(tableId)
          .setFormatOptions(FormatOptions.json())
          .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE) // or WRITE_APPEND
          .build()

      JobId jobId = JobId.newBuilder().setProject(projectId).setRandomJob().build()
      TableDataWriteChannel writer = bigQuery.writer(jobId, wcfg)
      OutputStream out = Channels.newOutputStream(writer)
      JsonGenerator json = new JsonFactory().createGenerator(out, JsonEncoding.UTF8)

      def names = matrix.columnNames()
      int rowCount = matrix.rowCount()
      int stepSize = Math.max(1, (int) (rowCount * tickPercent)) // every 5% of total
      ProgressBar pb = new ProgressBar("Inserting into ${tableId.dataset}.${tableId.table}", rowCount)

      try {
        for (def row : matrix.rows()) {
          rowIdx++
          json.writeStartObject()
          for (String name : names) {
            value = row[name]
            json.writeFieldName(name)
            if (needsConversion(value)) {
              json.writeString(sanitizeString(convertObjectValue(value))) // dates/decimals as strings
            } else if (value instanceof Number) {
              json.writeNumber(value.toString())  // keep numbers as numbers
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
          json.writeEndObject()
          json.writeRaw('\n') // NDJSON newline

          // update progress bar every tickPercent of total rows
          if (rowIdx % stepSize == 0) {
            pb.stepBy(stepSize)
          }
        }

        // make sure we reach 100% even if total not multiple of stepSize
        pb.stepTo(rowIdx)
      } finally {
        pb.close()
        json.flush()
        json.close()
      }

      Job loadJob = writer.getJob().waitFor()

      def status = loadJob.getStatus()
      def primary = status?.getError()
      def execErrs = status?.getExecutionErrors()

      if (primary != null || (execErrs != null && !execErrs.isEmpty())) {
        String details = ([primary] + (execErrs ?: []))
            .findAll { it != null }
            .collect { e -> "${e.message} (reason=${e.reason}, location=${e.location})" }
            .join("; ")
        throw new BqException("Write-channel load (JSON) failed: ${details}")
      }

      JobStatistics.LoadStatistics stats = loadJob.getStatistics()
      long rowsInserted = stats.getOutputRows()

      log.info("Load job completed successfully. Inserted $rowsInserted rows")
      return stats

    } catch (Exception e) {
      // Check if the failure is a known connection error that necessitates the fallback
      if (e.cause instanceof ConnectException || e.message?.contains("Connection refused")) {

        log.warn("Streaming insert failed with connection error. Falling back to InsertAll...")
        try {

          List<RowToInsert> rows = matrix.rows().collect { row ->
            Map<String, Object> content = new LinkedHashMap<>()

            matrix.columnNames().each { name ->
              Object val = row[name]
              if (needsConversion(val)) {
                // Apply conversion for complex types (Dates, BigDecimal, etc.)
                content.put(name, sanitizeString(convertObjectValue(val)))
              } else if (val instanceof CharSequence) {
                // Sanitize regular strings
                content.put(name, sanitizeString(val.toString()))
              } else {
                // Pass simple types (Number, Boolean, byte[], null) directly
                content.put(name, val)
              }
            }

            return RowToInsert.of(content)
          }

          InsertAllRequest request = InsertAllRequest.newBuilder(tableId)
              .setRows(rows)
              .build()

          InsertAllResponse response = bigQuery.insertAll(request)

          if (response.hasErrors()) {
            def errorDetails = response.getInsertErrors().collect { entry ->
              def rowErrors = entry.getValue().collect { it.getMessage() }.join(", ")
              return "Row ${entry.getKey()}: ${rowErrors}"
            }.join("\n")
            throw new BqException("InsertAll fallback failed with errors:\n${errorDetails}")
          }

          // Success: Return a placeholder LoadStatistics object
          log.info("InsertAll fallback successful. Inserted ${matrix.rowCount()} rows")

          List<String> emptySourceUris = Collections.emptyList()

          LoadJobConfiguration.Builder loadConfigBuilder = (LoadJobConfiguration.Builder) LoadJobConfiguration
              .newBuilder(tableId, emptySourceUris)
              .setFormatOptions(FormatOptions.json())

          LoadJobConfiguration placeholderLoadConfig = loadConfigBuilder.build()

          JobInfo.Builder jobInfoBuilder = JobInfo.newBuilder(placeholderLoadConfig)
          JobInfo placeholderJobInfo = jobInfoBuilder.build()

          return (JobStatistics.LoadStatistics) placeholderJobInfo.getStatistics()

        } catch (Exception fallbackException) {
          // If fallback fails, log and throw a detailed exception
          log.error("InsertAll fallback also failed: ${fallbackException.message}")
          throw new BqException("Streaming insert failed: ${e.message}. Fallback also failed: ${fallbackException.message}", e)
        }
      }
      // for non-connection errors or if fallback not used:
      throw new BqException("Error writing value '${value}' (type=${value?.class?.name}) on row ${rowIdx}", e)
    }
  }

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

  static def convertObjectValue(Object orgVal) {
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

  JobStatistics.LoadStatistics insert(Matrix matrix, String dataSet, String projectId) throws BqException {
    String tableName = matrix.matrixName
    TableId tableId = TableId.of(projectId, dataSet, tableName)
    insert(matrix, tableId)
  }

  static Matrix convertToMatrix(TableResult result) {
    Schema schema = result.getSchema()
    List<String> colNames = []
    List<LegacySQLTypeName> colTypes = []
    schema.fields.each {
      colNames << it.name
      colTypes << it.type
    }

    List<List> rows = []
    def row
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

  List<Project> getProjects() throws BqException {
    ProjectsSettings projectsSettings = ProjectsSettings.newBuilder().build()
    try (ProjectsClient pc = ProjectsClient.create(projectsSettings)) {
      return pc.searchProjects("").iterateAll().collect()
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

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

  Matrix getTableInfo(String datasetName, String tableName)  throws BqException {
    query("""select * from ${datasetName}.INFORMATION_SCHEMA.COLUMNS
      WHERE table_name = '$tableName';
      """)
  }

  Dataset getDataset(String datasetName) throws BqException {
    try {
      bigQuery.getDataset(datasetName)
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  Dataset.Builder getDatasetBuilder(String datasetName) throws BqException {
    getDataset(datasetName).toBuilder()
  }

  Dataset createDataset(String datasetName, String description = null) throws BqException {
    try {
      Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetName))
      if (dataset != null) {
        log.debug("Dataset $datasetName already exists")
        return dataset
      }
      def builder = DatasetInfo.newBuilder(datasetName)
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

  Dataset updateDataset(Dataset.Builder ds) throws BqException {
    try {
      bigQuery.update(ds.build())
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  static Schema createSchema(Matrix matrix) {
    def fields = []
    matrix.columns().each { c ->
      fields << Field.of(c.name, toStandardSqlType(c.type))
    }
    Schema.of(fields as Field[])
  }

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

  boolean dropTable(String datasetName, Matrix matrix) throws BqException {
    dropTable(datasetName, matrix.matrixName)
  }

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

  boolean datasetExist(String datasetName) throws BqException {
    try {
      Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetName));
      if (dataset != null) {
        return true
      }
      return false
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  boolean tableExist(String datasetName, String tableName) throws BqException {
    try {
      def table = bigQuery.getTable(TableId.of(datasetName, tableName))
      if (table != null && table.exists()) {
        // table will be null if it is not found and setThrowNotFound is not set to `true`
        return true
      }
      return false
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  static String sanitizeString(Object input) {
    if (input == null) return null
    String str = input.toString()
    // This regular expression removes all Unicode control characters (the C category)
    // which are the most common cause of JSON serialization errors.
    return str.replaceAll("\\p{C}", "")
  }
}
