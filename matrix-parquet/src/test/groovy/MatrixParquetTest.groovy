import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static se.alipsa.matrix.core.ListConverter.toBigDecimals
import static se.alipsa.matrix.core.ListConverter.toDoubles
import static se.alipsa.matrix.core.ListConverter.toLocalDates

class MatrixParquetTest {

  @Test
  void testMatrixParquetCars() {
    Matrix data = Dataset.cars().withMatrixName('cars')
    File file = new File("build/cars.parquet")
    if (file.exists()) {
      file.delete() // Ensure we start with a clean slate
    }
    MatrixParquetWriter.write(data, file)
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)
    //println matrix.content()
    assertEquals(data, matrix, "Data read from Parquet file does not match original data")
    assertEquals(data.types(), matrix.types(),
        "Types read from Parquet file do not match expected types")
  }

  @Test
  void testMatrixParquetComplexData() {
    def empData = Matrix.builder('empData').columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        score: toDoubles([0.5, 0.6, 0.7, 0.8, 0.9]),
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types([int, String, BigDecimal, Double, LocalDate]).build()
    File file = new File("build/empData.parquet")
    if (file.exists()) {
      file.delete() // Ensure we start with a clean slate
    }
    MatrixParquetWriter.write(empData, file)
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)
    //println matrix.content()
    assertEquals(empData, matrix, "Data read from Parquet file does not match original data")
    assertEquals([Integer, String, BigDecimal, Double, LocalDate], matrix.types(),
        "Types read from Parquet file do not match expected types")
  }

  @Test
  void testMatrixParquetComplexDataInferredPrecision() {
    def empData = Matrix.builder('empData').columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        score: toBigDecimals([0.5123, 0.6321, 0.7190, 0.8452, 0.9198]),
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types([int, String, BigDecimal, BigDecimal, LocalDate]).build()
    File file = new File("build/empData.parquet")
    if (file.exists()) {
      file.delete() // Ensure we start with a clean slate
    }
    MatrixParquetWriter.write(empData, file, true)
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)
    //println matrix.content()
    //println Stat.str(matrix)
    assertEquals(empData, matrix, "Data read from Parquet file does not match original data")
    assertIterableEquals([Integer, String, BigDecimal, BigDecimal, LocalDate], matrix.types(),
        "Types read from Parquet file do not match expected types")
  }
}
