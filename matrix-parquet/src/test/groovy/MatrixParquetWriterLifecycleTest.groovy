import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.nio.file.Path

class MatrixParquetWriterLifecycleTest {

  @TempDir
  Path tempDir

  @Test
  void testWriterClosesFileWhenRowWriteFails() {
    def invalid = Matrix.builder('invalidDecimal').data(amount: [623.30]).types([BigDecimal]).build()
    File file = tempDir.resolve('failed_write_reuse.parquet').toFile()

    assertThrows(Exception) {
      MatrixParquetWriter.write(invalid, file, [amount: [4, 2] as int[]])
    }

    def valid = Matrix.builder('validDecimal').data(amount: [12.30]).types([BigDecimal]).build()
    MatrixParquetWriter.write(valid, file, [amount: [4, 2] as int[]])
    Matrix matrix = MatrixParquetReader.read(file)
    assertEquals(valid, matrix)
  }

  @Test
  void testWriterClosesMemoryOutputWhenRowWriteFails() {
    def invalid = Matrix.builder('invalidBytes').data(amount: [623.30]).types([BigDecimal]).build()

    assertThrows(Exception) {
      MatrixParquetWriter.writeBytes(invalid, [amount: [4, 2] as int[]])
    }

    def valid = Matrix.builder('validBytes').data(amount: [12.30]).types([BigDecimal]).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(valid, [amount: [4, 2] as int[]])
    Matrix matrix = MatrixParquetReader.read(bytes)
    assertEquals(valid, matrix)
  }
}
