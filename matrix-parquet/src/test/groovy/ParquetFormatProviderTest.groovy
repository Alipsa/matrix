import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.ParquetFormatProvider
import se.alipsa.matrix.parquet.ParquetReadOptions
import se.alipsa.matrix.parquet.ParquetWriteOptions

import java.nio.file.Path
import java.time.LocalDateTime

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

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
            amount: [new BigDecimal('12.30'), new BigDecimal('45.67')],
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
    assertEquals(new BigDecimal('12.30'), matrix[0, 'amount'])
    assertEquals(source[0, 'createdAt'], matrix[0, 'createdAt'])
  }

  @Test
  void testProviderMetadata() {
    def provider = new ParquetFormatProvider()
    assertEquals(['parquet'] as Set, provider.supportedExtensions())
    assertEquals('Parquet', provider.formatName())
  }

  @Test
  void testWriteOptionsRejectInvalidDecimalMetaShape() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ParquetWriteOptions.fromMap([decimalMeta: [amount: [8, 2]]])
    }

    assertTrue(exception.message.contains("decimalMeta['amount'] must be an int[] of length 2"))
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
}
