import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetIO

class CarpetTest {

  @Test
  void testWriteRead() {
    Matrix data = Dataset.cars().withMatrixName('cars.parquet')
    File file = new File("build/cars.parquet")
    MatrixParquetIO.write(data, file)

    Matrix d2 = MatrixParquetIO.read(file)
    Assertions.assertEquals(data, d2)

    Matrix mtcars = Dataset.mtcars()
    File f = new File("build/mtcars.parquet")
    MatrixParquetIO.write(mtcars, f, [precision: 6, scale: 3])

    Matrix mtcars2 = MatrixParquetIO.read(f, 'mtcars')
    Assertions.assertEquals(mtcars, mtcars2)
  }

  @Test
  void testExternalDatasets() {
    File file = new File("src/test/resources/mtcars.parquet")
    Matrix table = MatrixParquetIO.read(file, 'mtcars')
    def expected = Dataset.mtcars()
    MatrixAssertions.assertContentMatches(expected, table)
  }
}
