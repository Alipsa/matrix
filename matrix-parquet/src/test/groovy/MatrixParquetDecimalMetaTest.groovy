import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.math.RoundingMode
import java.nio.file.Path

class MatrixParquetDecimalMetaTest {

  @TempDir
  Path tempDir

  @Test
  void testDirectDecimalMetaRejectsInvalidPrecisionAndScale() {
    def data = Matrix.builder('badDecimalMeta').data(amount: [12.30]).types([BigDecimal]).build()
    File file = tempDir.resolve('bad_decimal_meta.parquet').toFile()

    assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.write(data, file, [amount: [0, 0] as int[]])
    }
    assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.writeBytes(data, [amount: [5, 6] as int[]])
    }
  }

  @Test
  void testExplicitDecimalPrecisionAndLosslessScaleAreValidated() {
    File file = tempDir.resolve('decimal_precision.parquet').toFile()
    Matrix overflow = Matrix.builder('overflow').data(amount: [10.00G, -10.00G]).types([BigDecimal]).build()

    IllegalArgumentException precisionException = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.write(overflow, file, [amount: [3, 2] as int[]])
    }
    assertTrue(precisionException.message.contains("field 'amount'"))
    assertTrue(precisionException.message.contains('DECIMAL(3, 2)'))

    Matrix boundary = Matrix.builder('boundary').data(amount: [9.99G, -9.99G]).types([BigDecimal]).build()
    MatrixParquetWriter.write(boundary, file, [amount: [3, 2] as int[]])
    assertEquals(boundary, MatrixParquetReader.read(file))

    Matrix lossy = Matrix.builder('lossy').data(amount: [1.235G]).types([BigDecimal]).build()
    IllegalArgumentException roundingException = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.write(lossy, file, [amount: [5, 2] as int[]])
    }
    assertTrue(roundingException.message.contains("field 'amount'"))
    assertTrue(roundingException.message.contains('roundingMode UNNECESSARY'))

    Matrix exact = Matrix.builder('exact').data(amount: [1.2G]).types([BigDecimal]).build()
    MatrixParquetWriter.write(exact, file, [amount: [5, 2] as int[]])
    assertEquals(1.20G, MatrixParquetReader.read(file).amount[0])
  }

  @Test
  void testExplicitRoundingModeIsAvailableThroughBuilderAndDirectOverload() {
    Matrix source = Matrix.builder('rounded').data(amount: [1.235G]).types([BigDecimal]).build()
    File builderFile = tempDir.resolve('builder_rounding.parquet').toFile()
    MatrixParquetWriter.builder(source)
        .precision(5)
        .scale(2)
        .roundingMode('half_up')
        .write(builderFile)
    assertEquals(1.24G, MatrixParquetReader.read(builderFile).amount[0])

    File directFile = tempDir.resolve('direct_rounding.parquet').toFile()
    MatrixParquetWriter.write(source, directFile, 5, 2, RoundingMode.DOWN)
    assertEquals(1.23G, MatrixParquetReader.read(directFile).amount[0])
  }
}
