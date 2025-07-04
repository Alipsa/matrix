package se.alipsa.matrix.bigquery

import com.google.api.gax.paging.Page
import com.google.cloud.resourcemanager.v3.Project
import com.google.cloud.resourcemanager.v3.ProjectsClient
import com.google.cloud.resourcemanager.v3.ProjectsSettings

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import static se.alipsa.matrix.bigquery.TypeMapper.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.*
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption
import com.google.cloud.bigquery.BigQuery.DatasetListOption
import com.google.cloud.bigquery.BigQuery.TableListOption
import se.alipsa.matrix.core.Matrix

class Bq {

  // BigQuery serializes complex datatypes into structs so we must convert things like BigDecimal, Date, LocalDate etc
  // into plain text strings that BigQuery will understand hen inserting data
  static final SimpleDateFormat bqSimpledateFormat = new SimpleDateFormat("yyyy-MM-dd")
  static final DateTimeFormatter bqDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  static final DateTimeFormatter bqDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:SS.SSSSSS")

  private BigQuery bigQuery
  private String projectId

  Bq(BigQueryOptions options) {
    bigQuery = options.getService()
    projectId = options.getProjectId()
  }

  Bq(GoogleCredentials credentials, String projectId) {
    this.projectId = projectId
    bigQuery = BigQueryOptions.newBuilder()
        .setCredentials(credentials)
        .setProjectId(projectId)
        .build()
        .getService()
  }

  Bq(String projectId) {
    if (projectId == null) {
      throw new IllegalArgumentException("ProjectId cannot be null")
    }
    this.projectId = projectId
    bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .getService()
  }

  Bq() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      throw new RuntimeException("Please set the environment variable GOOGLE_CLOUD_PROJECT prior to creating this class (or pass it as a parameter)")
    }
    this.projectId = projectId
    bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .getService()
  }

  Matrix query(String qry, boolean useLegacySql = false) throws BqException {
    try {
      QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(qry)
          .setUseLegacySql(useLegacySql)
          .build()
      JobId jobId = JobId.newBuilder().setProject(projectId).build()
      Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build())
      // Wait for the query to complete.
      queryJob = queryJob.waitFor()

      // Check for errors
      if (queryJob == null) {
        throw new RuntimeException("Job no longer exists")
      } else if (queryJob.getStatus().getError() != null) {
        // You can also look at queryJob.getStatus().getExecutionErrors() for all
        // errors, not just the latest one.
        throw new RuntimeException(queryJob.getStatus().getError().toString())
      }

      // Convert to a Matrix and return the results.
      TableResult result = queryJob.getQueryResults()
      return convertToMatrix(result)
    } catch (BigQueryException | InterruptedException e) {
      throw new BqException("Query failed due to error: " + e.toString(), e)
    }
  }

  boolean saveToBigQuery(Matrix matrix, String datasetName) throws BqException {
    String tableName = matrix.matrixName
    if (tableExist(datasetName, tableName)) {
      println "${datasetName}.$tableName already exists"
      return false
    }
    def definitions = createTable(matrix, datasetName)
    Thread.sleep(5000)
    // TODO: this is fragile, we cannot insert directly but must wait "a while"
    //  for the table to be ready for insert (but how long)
    while(true) {
      try {
        Map<Long, List<BigQueryError>> errors = insert(matrix, definitions.table.tableId)
        if (!errors.isEmpty()) {
          errors.each {
            println("$it.key: $it.value")
          }
          throw new BqException("Inserting data into table $tableName failed!")
        }
        break
      } catch (BigQueryException e) {
        if (e.code != 404) {
          throw e
        }
        println("Table is not ready for insert, sleeping for 5 seconds")
        Thread.sleep(5000)
      }
    }
    return true
  }

  Map createTable(Matrix matrix, String datasetName) throws BqException {
    createTable(matrix, datasetName, projectId)
  }

  Map createTable(Matrix matrix, String datasetName, String projectId) throws BqException {
    Schema schema = createSchema(matrix)
    createTable(matrix, datasetName, projectId, schema)
  }

  Map createTable(Matrix matrix, String datasetName, String projectId, Schema schema) throws BqException {
    try {
      String tableName = matrix.matrixName
      TableId tableId = TableId.of(projectId, datasetName, tableName)
      TableDefinition tableDefinition = StandardTableDefinition.of(schema)
      TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
      Table table = bigQuery.create(tableInfo)
      println("Table ${datasetName}.$tableName created successfully")
      return [table: table, schema: schema]
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  Map<Long, List<BigQueryError>> insert(Matrix matrix, String dataSet) throws BqException {
    insert(matrix, dataSet, projectId)
  }

  Map<Long, List<BigQueryError>> insert(Matrix matrix, TableId tableId) throws BqException {
    try {
      def mapList = matrix.rows().collect(m -> m.toMap())
      def builder = InsertAllRequest.newBuilder(tableId)
      mapList.each { rowMap ->
          // Create a new map to hold the processed row to avoid modifying the original
          Map<String, Object> processedRow = new HashMap<>(rowMap)

          processedRow.each { entry ->
            if (needsConversion(entry.value))
              processedRow[entry.key] = convertObjectValue(entry.value)
          }

          builder.addRow(processedRow) // Add the processed row
      }
      //println "Bq: Inserting into $tableId"
      InsertAllResponse response = bigQuery.insertAll(builder.build())

      //if (response.hasErrors()) {
      return response.getInsertErrors()
      //}
    } catch (BigQueryException e) {
      throw new BqException(e)
    }
  }

  static boolean needsConversion(Object orgVal) {
    return orgVal instanceof BigDecimal
        || orgVal instanceof BigInteger
        || orgVal instanceof Date
        || orgVal instanceof LocalDate
        || orgVal instanceof LocalDateTime
    || orgVal instanceof Instant // The default toString works fine
    || orgVal instanceof Timestamp
    || orgVal instanceof ZonedDateTime
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
    if (orgVal instanceof Date) {
      Date date = (Date) orgVal
      return bqSimpledateFormat.format(date)
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
      return ts.format(DateTimeFormatter.ISO_INSTANT)
    }
    String.valueOf(orgVal)
  }

  Map<Long, List<BigQueryError>> insert(Matrix matrix, String dataSet, String projectId) throws BqException {
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

    //println "Bq.convertToMatrix: result contains $result.totalRows rows"
    //println "Bq.convertToMatrix: Column names in bq result are ${colNames}"
    //println "Bq.convertToMatrix: Column types in bq result are ${colTypes}"
    List<List> rows = []
    def row
    for (FieldValueList fvl : result.iterateAll()) {
      row = []
      int i = 0
      for (FieldValue fv : fvl.iterator()) {
        row << convert(fv.value, colTypes[i++])
      }
      //println "Bq.convertToMatrix: adding $row"
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
        println("Dataset does not contain any models")
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
    try {
      ProjectsSettings projectsSettings = ProjectsSettings.newBuilder().build()
      ProjectsClient projectsClient = ProjectsClient.create(projectsSettings)
      ProjectsClient.SearchProjectsPagedResponse searchProjectsPagedResponse = projectsClient.searchProjects("")
      searchProjectsPagedResponse.iterateAll().collect()
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
        println("Dataset $datasetName already exists.")
        return dataset
      }
      def builder = DatasetInfo.newBuilder(datasetName)
      if (description != null) {
        builder.setDescription(description)
      }
      DatasetInfo datasetInfo = builder.build()
      Dataset ds = bigQuery.create(datasetInfo)
      String newDatasetName = ds.getDatasetId().getDataset()
      System.out.println(newDatasetName + " created successfully")
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
        println("Dataset $datasetName deleted successfully");
      } else {
        println("Dataset $datasetName was not found");
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
        System.out.println("Table ${datasetName}.${tableName} deleted successfully")
      } else {
        System.out.println("Table ${datasetName}.${tableName} was not found")
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
}
