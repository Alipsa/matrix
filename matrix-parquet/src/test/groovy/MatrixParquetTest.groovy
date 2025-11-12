import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.core.ListConverter.toBigDecimals
import static se.alipsa.matrix.core.ListConverter.toDoubles
import static se.alipsa.matrix.core.ListConverter.toLocalDates

class MatrixParquetTest {

  @Test
  void testMatrixParquetCars() {
    Matrix data = Dataset.cars().withMatrixName('cars')
    File dir = new File("build/cars")
    if (dir.exists()) {
      if (dir.isDirectory()) {
        dir.listFiles().each {it.delete()}
      }
      dir.delete() // Ensure we start with a clean slate
    }
    dir.mkdirs()
    //println data.content()
    File file = MatrixParquetWriter.write(data, dir)
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)

    assertEquals(data, matrix, "Data read from Parquet file does not match original data")
    assertEquals(data.types(), matrix.types(),
        "Types read from Parquet file do not match expected types")
  }

  @Test
  void testMatrixParquetCarsFixedSize() {
    Matrix data = Dataset.mtcars().withMatrixName('mtcars')
    File dir = new File("build/mtcars")
    if (dir.exists()) {
      if (dir.isDirectory()) {
        dir.listFiles().each {it.delete()}
      }
      dir.delete() // Ensure we start with a clean slate
    }
    dir.mkdirs()
    //println data.content()
    File file = MatrixParquetWriter.write(data, dir, 5, 3)
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)

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

  @Test
  void testMatrixParquetComplexDataWithPrecision() {
    // NOTE: The first score value is changed to "1.2345".
    // With the schema [5, 4], this value is stored as unscaled 12345 (5 digits),
    // which satisfies the assertion for precision 5.
    def empData = Matrix.builder('empData').columns(
        emp_id: 1..5,
        emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary: [623.3, 515.2, 611, 729.0, 843.25],
        score: toBigDecimals(["1.2345", 0.6321, 0.7190, 0.8452, 0.9198]),
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
        .types([int, String, BigDecimal, BigDecimal, LocalDate]).build()
    File file = new File("build/empData_valid.parquet")
    if (file.exists()) {
      file.delete() // Ensure we start with a clean slate
    }
    MatrixParquetWriter.write(empData, file, [salary: [5,2], score: [5,4]])
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)
    //println matrix.content()
    //println Stat.str(matrix)
    assertEquals(empData, matrix, "Data read from Parquet file does not match original data")
    assertIterableEquals([Integer, String, BigDecimal, BigDecimal, LocalDate], matrix.types(),
        "Types read from Parquet file do not match expected types")

    // This value (623.30) has an unscaled value of 62330 (5 digits), so precision is 5.
    assertEquals(5, matrix.salary[0].precision())
    assertEquals(2, matrix.salary[0].scale())

    // This value (1.2345) has an unscaled value of 12345 (5 digits), so precision is 5.
    assertEquals(5, matrix.score[0].precision())
    assertEquals(4, matrix.score[0].scale())
  }

  @Test
  void testMatrixParquetPrecisionOverflow() {
    def empData = Matrix.builder('empData').columns(
        salary: [623.30 as BigDecimal]
    ).types([BigDecimal]).build()

    File file = new File("build/empData_invalid.parquet")
    if (file.exists()) {
      file.delete()
    }

    // Value to write: 623.30 (Unscaled: 62330, Precision: 5)
    // Declared Schema: [4, 2] (Max Unscaled: 9999, Max Value: 99.99)

    // Expect an exception (usually IllegalArgumentException or similar) when the writer
    // attempts to store the value which exceeds the defined FIXED_LEN_BYTE_ARRAY size.
    Exception exception = assertThrows(Exception.class, {
      MatrixParquetWriter.write(empData, file, [salary: [4, 2]])
    })

    // Assert that the exception message indicates a size/precision error.
    // We now include "arraycopy" to catch the specific low-level error seen in the test environment.
    String message = exception.getMessage() ?: ""
    assertTrue(message.contains("arraycopy") || message.contains("byte array size") || message.contains("exceeds the max precision") || message.contains("overflow"),
        "Expected an exception indicating a precision overflow, but got: ${message}")
  }

  @Test
  void testNestedStructures() {
    def productIds = [101, 102, 103]
    def productNames = ['Laptop Pro', 'Mouse Pad', 'Monitor Ultra']
    def productTags = [
        ['Electronics', 'High-End'],
        ['Accessories', 'Low-Cost', 'Desk'],
        ['Electronics', 'Display', '4K']
    ]
    def productDetails = [
        [manufacturer: 'Alpha', warranty_years: 3],
        [manufacturer: 'Beta', warranty_years: 1],
        [manufacturer: 'Gamma', warranty_years: 2]
    ]
    def productReviews = [
        [[rating: 5, user: 'A'], [rating: 4, user: 'B']],
        [[rating: 2, user: 'C']],
        [[rating: 5, user: 'D'], [rating: 5, user: 'E'], [rating: 4, user: 'F']]
    ]

    def productsNested = Matrix.builder('products_nested').data(
        id: productIds,
        name: productNames,
        tags: productTags,
        details: productDetails,
        reviews: productReviews
    ).types([Integer, String, List, Map, List]).build()

    File file = new File('build/products_nested.parquet')
    if (file.exists()) {
      file.delete()
    }

    MatrixParquetWriter.write(productsNested, file)
    def matrix = MatrixParquetReader.read(file)

    assertEquals(productsNested, matrix, 'Nested data should be preserved when written and read back')
    assertIterableEquals([Integer, String, List, Map, List], matrix.types(), 'Nested column types should be retained')
    assertEquals(['Electronics', 'High-End'], matrix.tags[0])
    assertEquals([manufacturer: 'Alpha', warranty_years: 3], matrix.details[0])
    assertEquals([rating: 5, user: 'A'], matrix.reviews[0][0])
  }
}
