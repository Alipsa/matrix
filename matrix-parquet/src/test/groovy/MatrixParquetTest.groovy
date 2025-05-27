import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import static org.junit.jupiter.api.Assertions.assertEquals

class MatrixParquetTest {

  @Test
  void testMatrixParquet() {
    Matrix data = Dataset.cars().withMatrixName('cars')
    File file = new File("build/cars.parquet")
    MatrixParquetWriter.write(data, file)
    assert file.exists() : "Parquet file was not created: ${file.absolutePath}"
    def matrix = MatrixParquetReader.read(file)
    println matrix.content()
    assertEquals(data, matrix, "Data read from Parquet file does not match original data")
  }
}
