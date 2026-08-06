package test.alipsa.matrix.api

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path as HadoopPath
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.nio.file.Files
import java.time.ZoneId

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers Parquet round trips, names, byte/Path overloads, nulls, and compression options. */
@Tag('parquet')
class ParquetApiIT implements ApiItSupport {

  @Test
  void parquetRoundTripsValuesTypesNamesAndNulls() {
    Matrix data = Matrix.builder([id: [1, 2, 3], name: ['a', null, 'c'], amount: [1.5G, null, 3.5G]],
        [Integer, String, BigDecimal], 'parquet').build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    Matrix fromBytes = MatrixParquetReader.read(bytes, 'from_bytes')
    assertTrue(fromBytes.matrixName == 'from_bytes')
    assertTrue(fromBytes.columnNames() == data.columnNames())
    assertTrue(fromBytes.types() == data.types())
    MatrixAssertions.assertContentMatches(data, fromBytes, data.diff(fromBytes))
    File file = tempFile('.parquet').toFile()
    MatrixParquetWriter.builder(data)
        .compressionCodec(CompressionCodecName.GZIP)
        .write(file)
    def footer = ParquetFileReader.readFooter(new Configuration(), new HadoopPath(file.toURI()))
    assertEquals(CompressionCodecName.GZIP, footer.blocks[0].columns[0].codec)
    Matrix fromPath = MatrixParquetReader.read(file.toPath(), 'from_path')
    assertTrue(fromPath.rowCount() == data.rowCount())
    assertTrue(MatrixParquetReader.read(bytes, ZoneId.of('UTC')).rowCount() == data.rowCount())
    assertThrows(IllegalArgumentException) { MatrixParquetReader.read(Files.createTempDirectory('not-a-file').toFile()) }
  }
}
