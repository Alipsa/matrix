import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetWriter

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
}
