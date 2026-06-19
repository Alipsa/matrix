import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path as HadoopPath
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.ParquetFormatProvider
import se.alipsa.matrix.parquet.ParquetReadOptions
import se.alipsa.matrix.parquet.ParquetWriteOptions

import java.nio.file.Path
import java.time.LocalDateTime

class ParquetFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testOptionDescriptions() {
    assertTrue(Matrix.listReadOptions('parquet').contains('zoneId'))
    assertTrue(Matrix.listWriteOptions('parquet').contains('precision'))
    assertTrue(ParquetReadOptions.describe().contains('matrixName'))
    assertTrue(ParquetWriteOptions.describe().contains('decimalMeta'))
  }

  @Test
  void testSpiWriteAndReadRoundTrip() {
    Matrix source = Matrix.builder('payments')
        .columns(
            amount: [12.30, 45.67],
            createdAt: [
                LocalDateTime.of(2024, 1, 2, 10, 30, 45, 123_000_000),
                LocalDateTime.of(2024, 1, 3, 11, 15, 0, 0)
            ]
        )
        .types([BigDecimal, LocalDateTime])
        .build()

    File file = tempDir.resolve('payments.parquet').toFile()
    source.write([precision: 8, scale: 2, zoneId: 'Europe/Stockholm'], file)

    Matrix matrix = Matrix.read([matrixName: 'loaded-payments', zoneId: 'Europe/Stockholm'], file)
    assertEquals('loaded-payments', matrix.matrixName)
    assertEquals(source.columnNames(), matrix.columnNames())
    assertEquals(12.30, matrix[0, 'amount'])
    assertEquals(source[0, 'createdAt'], matrix[0, 'createdAt'])
  }

  @Test
  void testCompressionCodecViaOptionsMap() {
    Matrix source = Matrix.builder('compressedSpi')
        .columns(id: [1, 2, 3], name: ['a', 'b', 'c'])
        .types([Integer, String])
        .build()

    File file = tempDir.resolve('spi_compressed.parquet').toFile()
    source.write([compressionCodec: 'GZIP'], file)

    def footer = ParquetFileReader.readFooter(new Configuration(), new HadoopPath(file.toURI()))
    assertEquals(CompressionCodecName.GZIP, footer.blocks[0].columns[0].codec)

    Matrix matrix = Matrix.read(file)
    assertIterableEquals(source.id, matrix.id)
  }

  @Test
  void testProviderMetadata() {
    def provider = new ParquetFormatProvider()
    assertEquals(['parquet'] as Set, provider.supportedExtensions())
    assertEquals('Parquet', provider.formatName())
  }

  @Test
  void testWriteOptionsAcceptDecimalMetaListShape() {
    ParquetWriteOptions options = ParquetWriteOptions.fromMap([decimalMeta: [amount: [8, 2]]])
    assertEquals(8, options.decimalMeta.amount[0])
    assertEquals(2, options.decimalMeta.amount[1])
  }

  @Test
  void testWriteOptionsRejectDecimalMetaListNonNumber() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ParquetWriteOptions.fromMap([decimalMeta: [amount: [8, '2']]])
    }

    assertTrue(exception.message.contains("decimalMeta['amount'][1] must be a Number"))
  }

  @Test
  void testSpiWriteAcceptsDecimalMetaListShape() {
    Matrix source = Matrix.builder('payments')
        .data(amount: [12.30, 45.67])
        .types([BigDecimal])
        .build()

    File file = tempDir.resolve('decimal_meta_list.parquet').toFile()
    source.write([decimalMeta: [amount: [8, 2]]], file)
    Matrix matrix = Matrix.read(file)

    assertEquals(source, matrix)
    assertEquals(2, matrix.amount[0].scale())
  }

  @Test
  void testReadAndWriteOptionsIgnoreNullTextValues() {
    ParquetReadOptions readOptions = ParquetReadOptions.fromMap([matrixName: null, zoneId: null])
    ParquetWriteOptions writeOptions = ParquetWriteOptions.fromMap([
        inferPrecisionAndScale: null,
        precision             : null,
        scale                 : null,
        decimalMeta           : null,
        zoneId                : null
    ])

    assertEquals(null, readOptions.matrixName)
    assertEquals(null, readOptions.zoneId)
    assertTrue(writeOptions.inferPrecisionAndScale)
    assertEquals(null, writeOptions.precision)
    assertEquals(null, writeOptions.scale)
    assertEquals([:], writeOptions.decimalMeta)
    assertEquals(null, writeOptions.zoneId)
  }

  @Test
  void testTypedWriteOptionsRejectInvalidDecimalMetaShape() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      new ParquetWriteOptions().decimalMeta([amount: [8] as int[]]).validate()
    }

    assertTrue(exception.message.contains("decimalMeta['amount'] must be an int[] of length 2"))
  }

  @Test
  void testWriteOptionsRejectInvalidPrecision() {
    IllegalArgumentException zeroPrecision = assertThrows(IllegalArgumentException) {
      new ParquetWriteOptions().precision(0).scale(0).validate()
    }
    assertTrue(zeroPrecision.message.contains('precision must be > 0'))

    IllegalArgumentException negativePrecision = assertThrows(IllegalArgumentException) {
      new ParquetWriteOptions().precision(-1).scale(0).validate()
    }
    assertTrue(negativePrecision.message.contains('precision must be > 0'))

    IllegalArgumentException negativeScale = assertThrows(IllegalArgumentException) {
      new ParquetWriteOptions().precision(10).scale(-1).validate()
    }
    assertTrue(negativeScale.message.contains('scale must be >= 0'))

    IllegalArgumentException scaleExceedsPrecision = assertThrows(IllegalArgumentException) {
      new ParquetWriteOptions().precision(5).scale(6).validate()
    }
    assertTrue(scaleExceedsPrecision.message.contains('scale (6) must not exceed precision (5)'))
  }

  @Test
  void testWriteOptionsRejectInvalidDecimalMetaValues() {
    IllegalArgumentException zeroPrecision = assertThrows(IllegalArgumentException) {
      ParquetWriteOptions.fromMap([decimalMeta: [amount: [0, 0] as int[]]])
    }
    assertTrue(zeroPrecision.message.contains("decimalMeta['amount'] precision must be > 0"))

    IllegalArgumentException negativeScale = assertThrows(IllegalArgumentException) {
      ParquetWriteOptions.fromMap([decimalMeta: [amount: [10, -1] as int[]]])
    }
    assertTrue(negativeScale.message.contains("decimalMeta['amount'] scale must be >= 0"))

    IllegalArgumentException scaleExceedsPrecision = assertThrows(IllegalArgumentException) {
      ParquetWriteOptions.fromMap([decimalMeta: [amount: [5, 6] as int[]]])
    }
    assertTrue(scaleExceedsPrecision.message.contains("decimalMeta['amount'] scale (6) must not exceed precision (5)"))
  }
}
