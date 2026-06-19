import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

class MatrixParquetReaderMetadataTest {

  @Test
  void testByteArrayReadUsesSchemaNameWhenNoMatrixNameProvided() {
    def data = Matrix.builder('schemaNamedMatrix').data(id: [1, 2]).types([Integer]).build()

    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    Matrix matrix = MatrixParquetReader.read(bytes)

    assertEquals('schemaNamedMatrix', matrix.matrixName)
  }

  @Test
  void testInputStreamReadUsesExplicitMatrixNameWhenProvided() {
    def data = Matrix.builder('schemaName').data(id: [1]).types([Integer]).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)

    Matrix matrix = MatrixParquetReader.read(new ByteArrayInputStream(bytes), 'explicitName')

    assertEquals('explicitName', matrix.matrixName)
  }
}
