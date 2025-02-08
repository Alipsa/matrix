package test.alipsa.matrix.bigquery

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

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
    println m.content()
    println m.types()
  }

  @Test
  void testCreateAndRetrieve() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    String datasetName = 'BqTest'
    bq.createDataset(datasetName)
    Assertions.assertTrue(bq.datasetExist(datasetName))
    def mtcars = Dataset.mtcars()
    bq.dropTable(datasetName, mtcars)
    bq.saveToBigQuery(Dataset.mtcars(), datasetName)
    String qry = "select * from `${projectId}.${datasetName}.mtcars`"
    println qry
    Matrix mtcars2 = bq.query(qry).withMatrixName(mtcars.matrixName)
    assert mtcars == mtcars2
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
    //println "datasets are"
    // ds.each { println it }
    List<String> tableNames = bq.getTableNames(ds[0])
    //println "Listing tables in ${ds[0]}"
    //tableNames.each {println it}
    // println "Table info for tableNames[0]"
    println bq.getTableInfo(ds[0], tableNames[0]).content()
  }
}
