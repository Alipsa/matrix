package test.alipsa.matrix.bigquery

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

/**
 * The GCloud testcontainer project is not yet mature enough to rely on so
 * these tests run against the actual Google Big Query. This means there must be a valid project
 * set. Since billing is connected to a project it cannot be hard coded here. Each user that
 * wants to run it must set the environment variable GOOGLE_CLOUD_PROJECT prior to running the
 * tests.
 */
class BqTest {

  @Test
  void testExample() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    Matrix m = bq.query("""SELECT CONCAT('https://stackoverflow.com/questions/', 
        CAST(id as STRING)) as url, view_count 
        FROM `bigquery-public-data.stackoverflow.posts_questions` 
        WHERE tags like '%google-bigquery%' 
        ORDER BY view_count DESC
        LIMIT 10
        """.stripIndent())
    Assertions.assertEquals(10, m.rowCount(), "Unexpected row count")
    Assertions.assertEquals(2, m.columnCount(), "Unexpected column count")
  }

  @Test
  void testCreateAndRetrieve() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    String datasetName = 'BqTestCars'
    bq.createDataset(datasetName)
    Assertions.assertTrue(bq.datasetExist(datasetName))
    def mtcars = Dataset.mtcars()
    bq.dropTable(datasetName, mtcars)
    bq.saveToBigQuery(mtcars, datasetName)
    String qry = "select * from `${projectId}.${datasetName}.mtcars`"
    println qry
    Matrix mtcars2 = bq.query(qry).withMatrixName(mtcars.matrixName)
    assert mtcars == mtcars2
    bq.dropDataset(datasetName)
    Assertions.assertFalse(bq.datasetExist(datasetName))
  }

  @Test
  void testCreateAndRetrieveAirquality() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    String datasetName = 'BqTestAirQuality'
    bq.createDataset(datasetName)
    Assertions.assertTrue(bq.datasetExist(datasetName))
    def airq = Dataset.airquality()
    airq.renameColumn('Solar.R', 'solar_r')
    bq.dropTable(datasetName, airq)
    Assertions.assertTrue(bq.saveToBigQuery(airq, datasetName))
    String qry = "select * from `${datasetName}.${airq.matrixName}`"
    Matrix airq2 = bq.query(qry).withMatrixName(airq.matrixName)
    assert airq == airq2
    bq.dropDataset(datasetName)
    Assertions.assertFalse(bq.datasetExist(datasetName))
  }

  @Test
  void testListTableInfo() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    List<String> ds = bq.datasets
    if (ds.size() ==  0) {
      println "No datasets found"
      return
    }
    //println "datasets are"
    // ds.each { println it }
    List<String> tableNames = bq.getTableNames(ds[0])

    if (tableNames.size() == 0) {
      println "no tables found in ${ds[0]}"
      return
    }
    //println "Listing tables in ${ds[0]}"
    //tableNames.each {println it}
    // println "Table info for tableNames[0]"
    Matrix tableInfo = bq.getTableInfo(ds[0], tableNames[0])
    println tableInfo.content()
  }
}
