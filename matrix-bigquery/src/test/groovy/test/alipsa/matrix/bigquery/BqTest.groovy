package test.alipsa.matrix.bigquery

import groovy.transform.CompileStatic
import org.junit.jupiter.api.*
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.datasets.Dataset

import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

import static org.junit.jupiter.api.Assertions.*

/**
 * The GCloud testcontainer project is not yet mature enough to rely on so
 * these tests run against the actual Google Big Query. This means there must be a valid project
 * set. Since billing is connected to a project it cannot be hard coded here. Each user that
 * wants to run it must set the environment variable GOOGLE_CLOUD_PROJECT prior to running the
 * tests.
 *
 * Note: These tests use async query execution (useAsyncQueries=true) since they run against
 * real BigQuery in production. Async mode has no response size limits and is optimal for
 * production workloads.
 *
 * Performance optimization: Uses shared dataset and Bq instance to minimize network round-trips.
 */
@Tag("external")
@CompileStatic
class BqTest {

  private static final Logger log = Logger.getLogger(BqTest)
  private static Bq bq
  private static String projectId
  private static final String DATASET_NAME = 'BqTestShared'

  @BeforeAll
  static void setupClass() {
    projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run tests!")
      return
    }
    bq = new Bq(true)  // Use async queries for production BigQuery

    // Create shared dataset once for all tests
    if (!bq.datasetExist(DATASET_NAME)) {
      bq.createDataset(DATASET_NAME, "Shared test dataset for BqTest suite")
    }
  }

  @AfterAll
  static void teardownClass() {
    if (bq != null && DATASET_NAME != null) {
      // Clean up shared dataset after all tests
      if (bq.datasetExist(DATASET_NAME)) {
        bq.dropDataset(DATASET_NAME)
      }
    }
  }

  @Test
  void testExample() {
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Matrix m = bq.query("""SELECT CONCAT('https://stackoverflow.com/questions/', 
        CAST(id as STRING)) as url, view_count 
        FROM `bigquery-public-data.stackoverflow.posts_questions` 
        WHERE tags like '%google-bigquery%' 
        ORDER BY view_count DESC
        LIMIT 10
        """.stripIndent())
    assertEquals(10, m.rowCount(), "Unexpected row count")
    assertEquals(2, m.columnCount(), "Unexpected column count")
  }

  @Test
  void testListProjects() {
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    List projects = bq.getProjects().collect{it.displayName + ' (' + it.projectId + ')'}
    assertTrue(projects.size() > 0, "Projects are ${String.join('\n',projects)}")
  }

  @Test
  void testCreateAndRetrieve() {
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Matrix mtcars = Dataset.mtcars()
    bq.dropTable(DATASET_NAME, mtcars)
    bq.saveToBigQuery(mtcars, DATASET_NAME)
    String qry = "select * from `${projectId}.${DATASET_NAME}.mtcars`"
    Matrix mtcars2 = bq.query(qry).withMatrixName(mtcars.matrixName)
    MatrixAssertions.assertContentMatches(mtcars.orderBy("model"), mtcars2.orderBy("model"))
  }

  @Test
  void testCreateAndRetrieveAirquality() {
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Matrix airq = Dataset.airquality()
    airq.rename('Solar.R', 'solar_r')
    bq.dropTable(DATASET_NAME, airq)
    assertTrue(bq.saveToBigQuery(airq, DATASET_NAME))
    String qry = "select * from `${DATASET_NAME}.${airq.matrixName}`"
    Matrix airq2 = bq.query(qry).withMatrixName(airq.matrixName)
    MatrixAssertions.assertContentMatches(airq, airq2)
  }

  @Test
  void testListTableInfo() {
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    List<String> ds = bq.datasets
    if (ds.size() ==  0) {
      log.info("No datasets found")
      return
    }
    //println "datasets are"
    // ds.each { println it }
    List<String> tableNames = bq.getTableNames(ds[0])

    if (tableNames.size() == 0) {
      log.info("No tables found in ${ds[0]}")
      return
    }
    //println "Listing tables in ${ds[0]}"
    //tableNames.each {println it}
    // println "Table info for tableNames[0]"
    Matrix tableInfo = bq.getTableInfo(ds[0], tableNames[0])
    assertNotNull(tableInfo)
    //println tableInfo.content()
  }
}
