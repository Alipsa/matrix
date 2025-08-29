package test.alipsa.matrix

import groovy.sql.Sql
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.input.ReaderInputStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvExporter
import se.alipsa.matrix.csv.CsvImporter
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.json.JsonExporter
import se.alipsa.matrix.json.JsonImporter
import se.alipsa.matrix.parquet.MatrixParquetIO
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter
import se.alipsa.matrix.spreadsheet.SpreadsheetExporter
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.stats.Sampler
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.xchart.PieChart

import java.nio.charset.StandardCharsets

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * We do some basic tests for each module to verify they work together.
 */
class MatrixModulesTest {


  @Test
  void testStat() {
    Matrix mtcars = Dataset.mtcars()
    def (train, test) = Sampler.split(mtcars, 0.5)
    assertEquals(16, train.rowCount(), 'train size')
    assertEquals(16, test.rowCount(), 'test size')

    // regression model between mpg as outcome and am as predictor
    // Equivalent R code:
    // fit<-lm(mpg~am,data=input)
    // summary(fit)
    def model = new LinearRegression(mtcars, 'am', 'mpg')
    assertEquals(17.147, model.getIntercept(3))
    assertEquals(0.3598, model.getRsquared(4))
  }

  @Test
  void testCsv() {
    Matrix mtcars = Dataset.mtcars()

    StringWriter writer = new StringWriter()
    CsvExporter.exportToCsv(mtcars, CSVFormat.DEFAULT, writer)
    ReaderInputStream sr = ReaderInputStream.builder()
        .setReader(new StringReader(writer.toString()))
        .setCharset(StandardCharsets.UTF_8)
        .get()
    //ReaderInputStream sr = new ReaderInputStream(new StringReader(writer.toString()), StandardCharsets.UTF_8)
    Matrix m2 = CsvImporter.importCsv(sr, CSVFormat.DEFAULT, true)
        .withMatrixName('mtcars')
    assertEquals(mtcars, m2)

  }

  @Test
  void testJson() {
    Matrix mtcars = Dataset.mtcars()
    def exporter = new JsonExporter(mtcars)
    Matrix m2 = JsonImporter.parse(exporter.toJson()).withMatrixName('mtcars')
    assertEquals(mtcars, m2)
  }

  @Test
  void testSpreadsheet() {
    Matrix mtcars = Dataset.mtcars()
    def file = new File("target/mtcars.xlsx")
    if (file.exists()) file.delete()
    SpreadsheetExporter.exportSpreadsheet(file, mtcars)
    println "wrote $file"

    Matrix m2 = SpreadsheetImporter.importSpreadsheet([
        file: file,
        sheet: 'mtcars',
        endRow: 33,
        endCol: 'L',
        firstRowAsColNames: true]
    ).withMatrixName('mtcars')
    assertEquals(mtcars, m2)
  }

  @Test
  void testSql() {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.3.232')
    ci.setUrl("jdbc:h2:mem:h2testdb")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    Matrix airq = Dataset.airquality()
    try (MatrixSql matrixSql = new MatrixSql(ci)) {

      String tableName = matrixSql.tableName(airq)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(airq)

      // For h2 we MUST piggyback on the existing connection as there can only be one
      // for another db we could have done Sql sql = new Sql(matrixSql.dbConnect(ci))
      // But since this is surrounded in a "try with resources", this is actually better
      Sql sql = new Sql(matrixSql.connect())
      int i = 0
      // Explicitly call toString to force interpolation in a closure
      sql.query("select * from $tableName".toString()) { rs ->
        while (rs.next()) {
          assertEquals(airq[i, 0, Double, 0.0], rs.getDouble("Ozone"), "ozone on row $i differs")
          assertEquals(airq[i, 1, Double, 0.0], rs.getDouble("Solar.R"), "Solar.R on row $i differs")
          assertEquals(airq[i, 2, Double, 0.0], rs.getDouble("Wind"), "Wind on row $i differs")
          assertEquals(airq[i, 3, Double, 0.0], rs.getDouble("Temp"), "Temp on row $i differs")
          assertEquals(airq[i, 4, Short], rs.getShort("Month"), "Month on row $i differs")
          assertEquals(airq[i, 5, Short], rs.getShort("Day"), "Day on row $i differs")
          i++
        }
      }
    }
  }

  @Test
  void testParquet() {
    Matrix data = Dataset.mtcars()
    File file = new File("target/mtcars.parquet")
    MatrixParquetIO.write(data, file, [precision: 6, scale: 3])

    Matrix d2 = MatrixParquetIO.read(file, 'mtcars')
    Assertions.assertEquals(data, d2)

    file.delete()
    MatrixParquetWriter.write(data, file)
    Matrix d3 = MatrixParquetReader.read(file)
    Assertions.assertEquals(data, d3, "Data read from Parquet file does not match original data")
    Assertions.assertEquals(data.types(), d3.types(),
        "Types read from Parquet file do not match expected types")
  }

  @Test
  void testXChart() {
    Matrix matrix = Matrix.builder(
        metal: ['Gold', 'Silver', 'Platinum', 'Copper', 'Zinc'],
        ratio: [24, 21, 39, 17, 40],
        [String, Number],
        'Metal ratio',
    ).build()

    File file = new File("target/testPieChart.png")
    def pc = PieChart.create(matrix)
        .addSeries(matrix.metal, matrix.ratio)

    pc.exportPng(file)
    assertTrue(file.exists())
  }
}