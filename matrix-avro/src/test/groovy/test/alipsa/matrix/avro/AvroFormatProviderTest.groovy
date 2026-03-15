package test.alipsa.matrix.avro

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.avro.AvroFormatProvider
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class AvroFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testOptionDescriptions() {
    assertTrue(Matrix.listReadOptions('avro').contains('matrixName'))
    assertTrue(Matrix.listWriteOptions('avro').contains('schemaName'))
    assertTrue(AvroReadOptions.describe().contains('readerSchema'))
    assertTrue(AvroWriteOptions.describe().contains('compression'))
  }

  @Test
  void testSpiWriteAndReadRoundTrip() {
    Matrix source = Matrix.builder('orders')
        .columns(
            id: [1, 2],
            amount: [new BigDecimal('12.34'), new BigDecimal('56.78')]
        )
        .types([Integer, BigDecimal])
        .build()

    File file = tempDir.resolve('orders.avro').toFile()
    source.write([
        inferPrecisionAndScale: true,
        schemaName            : 'Orders',
        namespace             : 'se.alipsa.matrix.spi'
    ], file)

    Matrix matrix = Matrix.read([matrixName: 'loaded-orders'], file)
    assertEquals('loaded-orders', matrix.matrixName)
    assertEquals(source.columnNames(), matrix.columnNames())
    assertEquals(new BigDecimal('12.34'), matrix[0, 'amount'])
  }

  @Test
  void testProviderMetadata() {
    def provider = new AvroFormatProvider()
    assertEquals(['avro'] as Set, provider.supportedExtensions())
    assertEquals('Avro', provider.formatName())
  }

  @Test
  void testReadAndWriteOptionsIgnoreNullStringValues() {
    AvroReadOptions readOptions = AvroReadOptions.fromMap([matrixName: null])
    AvroWriteOptions writeOptions = AvroWriteOptions.fromMap([
        inferPrecisionAndScale: null,
        namespace             : null,
        schemaName            : null,
        compression           : null,
        compressionLevel      : null,
        syncInterval          : null
    ])

    assertEquals(null, readOptions.matrixName)
    assertEquals(false, writeOptions.inferPrecisionAndScale)
    assertEquals('se.alipsa.matrix.avro', writeOptions.namespace)
    assertEquals('MatrixSchema', writeOptions.schemaName)
    assertEquals(AvroWriteOptions.Compression.NULL, writeOptions.compression)
    assertEquals(-1, writeOptions.compressionLevel)
    assertEquals(0, writeOptions.syncInterval)
  }
}
