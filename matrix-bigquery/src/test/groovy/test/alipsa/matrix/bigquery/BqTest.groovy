package test.alipsa.matrix.bigquery

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

import static org.junit.jupiter.api.Assertions.assertNotNull

/**
 * The GCloud testcontainer project is not yet mature enough to rely on so
 * these tests run against the actual Google Big Query. This means there must be a valid project
 * set. Since billing is connected to a project it cannot be hard coded here. Each user that
 * wants to run it must set the environment variable GOOGLE_CLOUD_PROJECT prior to running the
 * tests.
 */
@CompileStatic
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
  void testListProjects() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    println "Projects are ${bq.getProjects().collect{it.displayName + ' (' + it.projectId + ')'}}"

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
    assert mtcars.orderBy("model") == mtcars2.orderBy("model")
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
    assertNotNull(tableInfo)
    //println tableInfo.content()
  }

  @Test
  void testComplexData() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      println("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq()
    String datasetName = 'BqTestComplexData'
    bq.createDataset(datasetName)
    Assertions.assertTrue(bq.datasetExist(datasetName))
    // === Number Classes ===
    def byteList = [10g, 20g, 30g, 40g]
    def shortList = ListConverter.toShorts(5000, 6000, 7000, 8000)
    def integerList = [10000, 20000, 30000, 40000]
    def longList = [12345678901L, 23456789012L, 34567890123L, 45678901234L]
    def floatList = [1.1f, 2.2f, 3.3f, 4.4f]
    def doubleList = [10.01d, 20.02d, 30.03d, 40.04d]
    def bigDecimalList = [new BigDecimal("9876.543"), new BigDecimal("8765.432"), new BigDecimal("7654.321"), new BigDecimal("6543.210")]
    def bigIntegerList = [new BigInteger("10000000000"), new BigInteger("20000000000"), new BigInteger("30000000000"), new BigInteger("40000000000")]

// === Date and Time Classes ===
    def localDateList = ListConverter.toLocalDates('2020-01-01', '2021-02-02', '2022-03-03', '2023-04-04')
    def localTimeList = ListConverter.toLocalDateTimes("12:01", "13:02", "14:02", "15:03")
    def localDateTimeList = [
        LocalDateTime.of(2020, 1, 1, 10, 0),
        LocalDateTime.of(2021, 2, 2, 11, 0),
        LocalDateTime.of(2022, 3, 3, 12, 0),
        LocalDateTime.of(2023, 4, 4, 13, 0)
    ]
    def zonedDateTimeList = [
        ZonedDateTime.of(2024, 5, 5, 14, 0, 0, 0, ZoneId.of('America/New_York')),
        ZonedDateTime.of(2024, 6, 6, 15, 0, 0, 0, ZoneId.of('Europe/Paris')),
        ZonedDateTime.of(2024, 7, 7, 16, 0, 0, 0, ZoneId.of('Asia/Tokyo')),
        ZonedDateTime.of(2024, 8, 8, 17, 0, 0, 0, ZoneId.of('Australia/Sydney'))
    ]
    def instantList = [
        Instant.parse('2024-01-01T10:00:00Z'),
        Instant.parse('2024-02-02T11:00:00Z'),
        Instant.parse('2024-03-03T12:00:00Z'),
        Instant.parse('2024-04-04T13:00:00Z')
    ]
    def legacyDateList = [
        Date.parse("yyyy-MM-dd", "2020-10-10"),
        Date.parse("yyyy-MM-dd", "2020-11-11"),
        Date.parse("yyyy-MM-dd", "2020-12-12"),
        Date.parse("yyyy-MM-dd", "2021-01-01")
    ]
    def legacyTimeList = [
        Time.valueOf("10:00:00"),
        Time.valueOf("11:00:00"),
        Time.valueOf("12:00:00"),
        Time.valueOf("13:00:00")
    ]


// === Build the Matrix ===
    Matrix m = Matrix.builder("allTypes").data(
        // Number types
        id: bigIntegerList,
        a_byte: byteList,
        a_short: shortList,
        an_integer: integerList,
        a_long: longList,
        a_float: floatList,
        a_double: doubleList,
        a_bigDecimal: bigDecimalList,

        // Date and time types
        localDate: localDateList,
        times: localTimeList,
        a_localDateTime: localDateTimeList,
        a_zonedDateTime: zonedDateTimeList,
        an_instant: instantList,
        a_legacyDate: legacyDateList,
        a_legacyTime: legacyTimeList
    ).types(
        // Define the type for each column in the same order as the data above.
        BigInteger, Byte, Short, Integer, Long, Float, Double, BigDecimal,
        LocalDate, LocalTime, LocalDateTime, ZonedDateTime, Instant, Date, Time
    ).build()

    println "Matrix 'allTypes' successfully built."
    println "Columns: " + m.columnNames()
    println "Types: " + m.types()
    println "Number of rows: " + m.rowCount()
  }
}
