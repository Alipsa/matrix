import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

class ParquetInteropTest {
  @Test
  void exportAndReadRewrittenFiles() {
    File directory = new File('build/test-interop')
    directory.mkdirs()
    def data = Matrix.builder('interop').columns(text: ['héllo', null],
        payload: [[0, -1] as byte[], [] as byte[]], price: [1.25G, -2.50G],
        tags: [['α', null], []]).build()
    ['SNAPPY', 'ZSTD'].each { codec ->
      File file = new File(directory, "${codec}.parquet")
      MatrixParquetWriter.builder(data).compressionCodec(codec).write(file)
      File rewritten = new File(directory, "${codec}.cdc.parquet")
      if (rewritten.isFile()) {
        def result = MatrixParquetReader.read(rewritten)
        assertEquals(['héllo', null], result.column('text'))
        assertArrayEquals([0, -1] as byte[], (byte[]) result[0, 'payload'])
        assertEquals([1.25G, -2.50G], result.column('price'))
        assertEquals([['α', null], []], result.column('tags'))
      }
    }
  }

  @Test
  void defaultCdcFixtureWhenGenerated() {
    File file = new File('build/default-cdc/default-cdc.parquet')
    assumeTrue(file.isFile(), 'SKIP default CDC: enable -PrunSlowTests=true with pinned PyArrow 21.0.0')
    def data = MatrixParquetReader.read(file)
    assertEquals(16384, data.rowCount())
    assertEquals('café-1-' + 'abcd' * 32, data[1, 'text'])
  }
}
