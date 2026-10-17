import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.*

import java.time.*

class OptimizedParquetTest {
  @TempDir File directory

  @Test
  void externalCdcAndIndexes() {
    [0, 1].each { cdc ->
      [0, 1].each { index ->
        File file = new File('src/test/resources/optimized', "cdc-${cdc}-index-${index}.parquet")
        def result = MatrixParquetReader.builder().zoneId('Europe/Stockholm').read(file)
        assertEquals(4096, result.rowCount())
        assertEquals(LocalDateTime.of(2024, 3, 31, 1, 30), result[0, 'local'])
        assertArrayEquals([0, 0, -1] as byte[], (byte[]) result[0, 'payload'])
        assertEquals(1.23G, result[123, 'price'])
        assertEquals(['α', null, 'β'], result[1, 'tags'])
        assertArrayEquals([1, 0, -1] as byte[], (byte[]) result[1, 'media'].bytes)
        assertEquals('image-1.jpg', result[1, 'media'].path)
        assertNull(result[2048, 'nulls'])
        assertEquals('café-3-' + 'abcd' * 32, result[4095, 'text'])
        [true, false].each { useIndex ->
          def selected = MatrixParquetReader.builder().columns(['text', 'id'])
              .filter(Predicates.ge('id', 3000L)).limit(13).useColumnIndexFilter(useIndex).read(file)
          assertEquals(['text', 'id'], selected.columnNames())
          assertEquals((3000L..<3013L).toList(), selected.column('id'))
        }
        def groups = MatrixParquetReader.builder().rowGroups([1, 0]).limit(3).read(file)
        assertEquals([2048L, 2049L, 2050L], groups.column('id'))
        MatrixParquetReader.builder().maxBatchRows(127).batches(file).withCloseable { batches ->
          int count = 0
          while (batches.hasNext()) {
            Matrix batch = batches.next()
            assertTrue(batch.rowCount() <= 127)
            count += batch.rowCount()
          }
          assertEquals(4096, count)
        }
      }
    }
  }

  @Test
  void alternatePairwiseFixtures() {
    new File('src/test/resources/optimized').listFiles().findAll { it.name.endsWith('-alternate.parquet') }.each { file ->
      def data = MatrixParquetReader.read(file)
      assertEquals(4096, data.rowCount())
      assertEquals(1.23G, data[123, 'price'])
      assertEquals(['α', null, 'β'], data[1, 'tags'])
    }
  }

  @Test
  @SuppressWarnings('ClassForName') // Characterize JVM array-name lookup; production uses a fixed allowlist.
  void fixtureEntryPointsAndLegacyCharacterization() {
    File file = new File('src/test/resources/optimized/cdc-1-index-1.parquet')
    [MatrixParquetReader.read(file.bytes), MatrixParquetReader.read(new ByteArrayInputStream(file.bytes)),
     MatrixParquetReader.read(file.toURI().toURL()), new ParquetFormatProvider().read(file, [:])].each { result ->
      assertEquals(4096, result.rowCount())
      assertEquals(['α', null, 'β'], result[1, 'tags'])
      assertArrayEquals([0, 0, -1] as byte[], (byte[]) result[0, 'payload'])
    }
    def legacy = MatrixParquetReader.read(new File('src/test/resources/optimized/matrix-0.7-date-units.parquet'))
    assertEquals(1000L, legacy[0, 'value'].time)
    assertEquals(2_000_000L, legacy[1, 'value'].time) // Original Timestamp(2000) was written as micros into MILLIS.
    def metadata = MatrixParquetReader.builder().zoneId('UTC').read(new File('src/test/resources/optimized/matrix-0.7-metadata.parquet'))
    assertEquals(BigDecimal, metadata.type('amount'))
    assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), metadata[0, 'local'])
    assertThrows(IllegalArgumentException) {
      MatrixParquetReader.read(new File('src/test/resources/optimized/legacy-binary-metadata.parquet'))
    }
    assertThrows(ClassNotFoundException) { getClass().classLoader.loadClass('[B') }
    assertEquals(byte[], Class.forName('[B', false, getClass().classLoader))
  }

  @Test
  void canonicalTypesAndBinaryRoundTrip() {
    def uuid = UUID.fromString('00112233-4455-6677-8899-aabbccddeeff')
    def instant = Instant.parse('1500-01-01T00:00:00.123456Z')
    def data = Matrix.builder('policies').columns(
        id: [1, 2], text: ['α', null], price: [1.5G, 2.25G],
        binary: [[0, -1] as byte[], [] as byte[]], tiny: [(byte) 1, (byte) -2],
        small: [(short) 1000, (short) -1000], time: [LocalTime.NOON, null],
        instant: [instant, null], uuid: [uuid, null], nested: [[label: 'hello'], [:]])
        .build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    def result = MatrixParquetReader.read(bytes)
    assertEquals(BigDecimal, result.type('price'))
    assertEquals([1.50G, 2.25G], result.column('price'))
    assertArrayEquals([0, -1] as byte[], (byte[]) result[0, 'binary'])
    assertEquals(Byte, result.type('tiny'))
    assertEquals((byte) -2, result[1, 'tiny'])
    assertEquals(LocalTime.NOON, result[0, 'time'])
    assertEquals(instant, result[0, 'instant'])
    assertEquals(uuid, result[0, 'uuid'])
    assertEquals([label: 'hello'], result[0, 'nested'])
  }

  @Test
  void validationBeforeWriting() {
    File target = new File(directory, 'untouched.parquet')
    target.text = 'keep'
    [Matrix.builder().columns(value: [1, 'x']).build(),
     Matrix.builder().columns(value: [new Date(0), new java.sql.Timestamp(0)]).types(Date).build(),
     Matrix.builder().columns(value: [1]).types(BigDecimal).build()].each { data ->
      assertThrows(IllegalArgumentException) { MatrixParquetWriter.write(data, target) }
      assertEquals('keep', target.text)
    }
  }

  @Test
  void legacyStringsAndWriterTuning() {
    def old = MatrixParquetReader.read(new File('src/test/resources/optimized/matrix-0.7-strings.parquet'))
    assertEquals('héllo', old[0, 'text'])
    assertEquals([label: 'α'], old[0, 'nested'])
    def data = Matrix.builder().columns(id: (0..<10000).toList(), text: ['label'] * 10000).build()
    File file = new File(directory, 'tuned.parquet')
    MatrixParquetWriter.builder(data).pageSize(4096).rowGroupSize(32768).writerVersion('V2')
        .bloomFilterEnabled([id: true]).bloomFilterNDV([id: 10000L]).write(file)
    assertEquals(10000, MatrixParquetReader.read(file).rowCount())
  }
}
