import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

/** Normal codec round trips; the opt-in no-Hadoop lane also reports required runtime classes. */
class ParquetCodecProbeTest {
  @Test
  void codecs() {
    ['SNAPPY', 'ZSTD', 'GZIP', 'UNCOMPRESSED'].each { codec ->
      try {
        def matrix = Matrix.builder('codec').columns(id: [1, 2], text: ['a', 'b']).build()
        byte[] bytes = MatrixParquetWriter.builder(matrix).compressionCodec(codec).writeBytes()
        assertEquals([1, 2], MatrixParquetReader.read(bytes).column('id'))
        println "CODEC $codec OK"
      } catch (NoClassDefFoundError failure) {
        if (System.getProperty('parquet.codecSpike') != 'true') { throw failure }
        println "CODEC $codec requires ${failure.message}"
      }
    }
  }
}
