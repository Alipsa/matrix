package se.alipsa.matrix.bigquery

import static se.alipsa.matrix.bigquery.TypeMapper.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.*
import com.google.cloud.bigquery.BigQuery.DatasetDeleteOption
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
      System.out.println("Query failed due to error: \n" + e.toString());
      throw new RuntimeException(e)
    }
  }

  boolean saveToBigQuery(Matrix matrix, String datasetName) throws BigQueryException {
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
        insert(matrix, definitions.table.tableId)
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

  Map createTable(Matrix matrix, String datasetName) {
    createTable(matrix, datasetName, projectId)
  }

  Map createTable(Matrix matrix, String datasetName, String projectId) {
    Schema schema = createSchema(matrix)
    createTable(matrix, datasetName, projectId, schema)
  }

  Map createTable(Matrix matrix, String datasetName, String projectId, Schema schema) {
    String tableName = matrix.matrixName
    TableId tableId = TableId.of(projectId, datasetName, tableName)
    TableDefinition tableDefinition = StandardTableDefinition.of(schema)
    TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
    Table table = bigQuery.create(tableInfo)
    println("Table ${datasetName}.$tableName created successfully")
    return [table: table, schema: schema]
  }

  Map<Long, List<BigQueryError>> insert(Matrix matrix, String dataSet) {
    insert(matrix, dataSet, projectId)
  }

  Map<Long, List<BigQueryError>> insert(Matrix matrix, TableId tableId) {
    def mapList = matrix.rows().collect(m -> m.toMap())
    def builder = InsertAllRequest.newBuilder(tableId)
    mapList.each {
      builder.addRow(it)
    }
    println "Inserting into $tableId"
    InsertAllResponse response = bigQuery.insertAll(builder.build())

    //if (response.hasErrors()) {
    return response.getInsertErrors()
    //}
  }

  Map<Long, List<BigQueryError>> insert(Matrix matrix, String dataSet, String projectId) {
    String tableName = matrix.matrixName
    TableId tableId = TableId.of(projectId, dataSet, tableName)
    insert(matrix, tableId)
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

  static Schema createSchema(Matrix matrix) {
    def fields = []
    matrix.columns().each { c ->
      fields << Field.of(c.name, toStandardSqlType(c.type))
    }
    Schema.of(fields as Field[])
  }

  Dataset createDataset(String datasetName) {
    try {
      Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetName))
      if (dataset != null) {
        System.out.println("Dataset $datasetName already exists.")
        return dataset
      }
      DatasetInfo datasetInfo = DatasetInfo.newBuilder(datasetName).build()
      Dataset newDataset = bigQuery.create(datasetInfo)
      String newDatasetName = newDataset.getDatasetId().getDataset()
      System.out.println(newDatasetName + " created successfully")
      newDataset
    } catch (BigQueryException e) {
      System.out.println("Dataset was not created. \n" + e.toString());
    }
  }

  boolean dropDataset(String datasetName) {
    DatasetId datasetId = DatasetId.of(projectId, datasetName);
    boolean success = bigQuery.delete(datasetId, DatasetDeleteOption.deleteContents())
    if (success) {
      System.out.println("Dataset $datasetName deleted successfully");
    } else {
      System.out.println("Dataset $datasetName was not found");
    }
    return success
  }

  boolean dropTable(String datasetName, Matrix matrix) {
    dropTable(datasetName, matrix.matrixName)
  }

  boolean dropTable(String datasetName, String tableName) {
    boolean success = bigQuery.delete(TableId.of(datasetName, tableName))
    if (success) {
      System.out.println("Table ${datasetName}.${tableName} deleted successfully")
    } else {
      System.out.println("Table ${datasetName}.${tableName} was not found")
    }
    return success
  }

  boolean datasetExist(String datasetName) {
    Dataset dataset = bigQuery.getDataset(DatasetId.of(datasetName));
    if (dataset != null) {
      return true
    }
    return false
  }

  boolean tableExist(String datasetName, String tableName) {
    def table = bigQuery.getTable(TableId.of(datasetName, tableName))
    if (table != null && table.exists()) {
      // table will be null if it is not found and setThrowNotFound is not set to `true`
      //System.out.println("Table already exist")
      return true
    } else {
      //System.out.println("Table not found")
      return false
    }
  }
}
