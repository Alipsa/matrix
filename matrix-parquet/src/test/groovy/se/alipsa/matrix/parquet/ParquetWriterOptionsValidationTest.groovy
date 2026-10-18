package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

class ParquetWriterOptionsValidationTest {
  @Test
  void bloomMapsNormalizeStrictlyAndActuallyWriteBloomFilters() {
    def options = ParquetWriteOptions.fromMap([bloomFilterEnabled: [id: 'true'], bloomFilterNDV: [id: '1000']])
    assertEquals([id: true], options.bloomFilterEnabled)
    assertEquals([id: 1000L], options.bloomFilterNDV)
    byte[] bytes = MatrixParquetWriter.writeBytes(Matrix.builder().columns(id: [1, 2]).build(), options)
    ParquetFileReader.open(new InMemoryInputFile(bytes), org.apache.parquet.ParquetReadOptions.builder(new PlainParquetConfiguration()).build()).withCloseable { reader ->
      assertNotNull(reader.readBloomFilter(reader.rowGroups[0].columns[0]))
    }
    [1000.0G, 1000, (short) 1000, 1000G].each { value ->
      assertEquals(1000L, ParquetWriteOptions.fromMap([bloomFilterNDV: [id: value]]).bloomFilterNDV.id)
    }
    [[bloomFilterEnabled: [id: 'maybe']], [bloomFilterEnabled: [id: null]], [bloomFilterNDV: [id: 'x']],
     [bloomFilterNDV: [id: 0]], [bloomFilterNDV: [id: 1.5]], [bloomFilterNDV: [id: '1.5']],
     [bloomFilterNDV: [id: null]], [bloomFilterNDV: [id: '9223372036854775808']], [bloomFilterNDV: [id: true]]].each { map ->
      assertTrue(assertThrows(IllegalArgumentException) { ParquetWriteOptions.fromMap(map) }.message.contains('id'))
    }
    [[id: 'bad'], [id: null]].each { map ->
      assertTrue(assertThrows(IllegalArgumentException) { new ParquetWriteOptions(bloomFilterEnabled: map).validate() }.message.contains('id'))
    }
    assertTrue(assertThrows(IllegalArgumentException) { new ParquetWriteOptions(bloomFilterNDV: [id: 'bad']).validate() }.message.contains('id'))
    assertEquals([id: 1000], new ParquetWriteOptions().bloomFilterNDV([id: 1000]).bloomFilterNDV)
  }

  @Test
  void nullOptionValuesMeanDefaultsEverywhere() {
    assertEquals('MICROS', ParquetWriteOptions.fromMap([timestampUnit: null]).timestampUnit)
    assertEquals('NANOS', ParquetWriteOptions.fromMap([timestampUnit: ' nanos ']).timestampUnit)
    assertEquals(CompressionCodecName.SNAPPY, ParquetWriteOptions.fromMap([compressionCodec: null]).compressionCodec)
    assertEquals(java.math.RoundingMode.UNNECESSARY, ParquetWriteOptions.fromMap([roundingMode: null]).roundingMode)
  }

  @Test
  @SuppressWarnings('LocaleSetDefault') // Regression for locale independence; restored in finally.
  void codecNamesIgnoreDefaultLocaleAndWhitespace() {
    Locale previous = Locale.default
    try {
      Locale.default = Locale.forLanguageTag('tr-TR')
      assertEquals(CompressionCodecName.GZIP, new ParquetWriteOptions().compressionCodec('gzip').compressionCodec)
      assertEquals(CompressionCodecName.ZSTD, new ParquetWriteOptions().compressionCodec(' zstd ').compressionCodec)
      assertTrue(assertThrows(IllegalArgumentException) { new ParquetWriteOptions().compressionCodec((String) null) }.message.contains('compressionCodec'))
      assertTrue(assertThrows(IllegalArgumentException) { new ParquetWriteOptions().compressionCodec('nope') }.message.contains('GZIP'))
    } finally { Locale.default = previous }
  }
}
