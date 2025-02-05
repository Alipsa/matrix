package se.alipsa.matrix.bigquery

import com.google.api.gax.paging.Page
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.*
import com.google.cloud.bigquery.BigQuery.DatasetListOption
import com.google.cloud.bigquery.BigQuery.TableListOption
import se.alipsa.matrix.core.Matrix

class Bq {

  private BigQuery bigQuery
  private String projectId

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

  Matrix query(String qry, boolean useLegacySql = false) {
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
      System.out.println("Query failed due to error: \n" + e.toString())
      throw new RuntimeException(e)
    }
  }

  void saveToBigQuery(Matrix, String dataSet) {

  }

  static Matrix convertToMatrix(TableResult result) {
    Schema schema = result.getSchema()
    List<String> columnNames = []
    List<Class> columnTypes = []
    schema.fields.each {
      columnNames << it.name
      columnTypes << convertType(it.type)
    }

    List<List> rows = []
    def row
    for (FieldValueList fvl : result.iterateAll()) {
      row = []
      for (FieldValue fv : fvl.iterator()) {
        row << fv.value
      }
      rows << row
    }
    // TODO: all values are Strings, must be converted to the types in the columnTypes list
    //  we need a custom converter for this
    Matrix.builder()
        .rows(rows)
        .types(columnTypes)
        .columnNames(columnNames)
        .build()
  }

  List<String> getDatasets() {
    Page<Dataset> datasets = bigQuery.listDatasets(projectId, DatasetListOption.pageSize(100))
    if (datasets == null) {
      System.out.println("Dataset does not contain any models")
      return []
    }
    return datasets
        .iterateAll()
        .collect {
          it.getDatasetId().dataset
        }
  }

  List<String> getTableNames(String datasetName) {
    DatasetId datasetId = DatasetId.of(projectId, datasetName)
    Page<Table> tables = bigQuery.listTables(datasetId, TableListOption.pageSize(100))
    return tables.iterateAll().collect {
      it.tableId.table
    }
  }

  Matrix getTableInfo(String datasetName, String tableName) {
    query("""select * from ${datasetName}.INFORMATION_SCHEMA.COLUMNS
      WHERE table_name = '$tableName';
      """)
  }

  Dataset getDataset(String datasetName) {
    bigQuery.getDataset(datasetName)
  }

  Dataset.Builder getDatasetBuilder(String datasetName) {
    getDataset(datasetName).toBuilder()
  }

  Dataset createDataset(String datasetName, String description = null) {
    try {
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
      throw new RuntimeException("Dataset was not created: " + e.toString(), e)
    }
    null
  }

  Dataset updateDataset(Dataset.Builder ds) {
    bigQuery.update(ds.build())
  }


  // TODO: check each one and finish mapping
  static Class convertType(LegacySQLTypeName typeName) {
    return switch (typeName) {
    //case LegacySQLTypeName.BIGNUMERIC -> BigDecimal
    //case LegacySQLTypeName.BOOLEAN -> Boolean
    //case LegacySQLTypeName.BYTES -> byte[]
    //case LegacySQLTypeName.DATE -> LocalDate
    //case LegacySQLTypeName.DATETIME -> LocalDateTime
    //case LegacySQLTypeName.FLOAT -> Double
    //case LegacySQLTypeName.GEOGRAPHY -> Object
    //case LegacySQLTypeName.INTEGER -> Integer
    //case LegacySQLTypeName.INTERVAL -> Duration
    //case LegacySQLTypeName.JSON ->
    //case LegacySQLTypeName.NUMERIC -> BigDecimal
    //case LegacySQLTypeName.RANGE -> Range
    //case LegacySQLTypeName.RECORD
    //case LegacySQLTypeName.STRING -> String
    //case LegacySQLTypeName.TIMESTAMP -> Instant
      //case LegacySQLTypeName.TIME -> LocalTime
    default -> Object
    }
  }
}
