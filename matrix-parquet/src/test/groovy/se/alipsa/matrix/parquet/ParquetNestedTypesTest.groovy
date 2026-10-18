package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.sql.Timestamp
import java.time.*

class ParquetNestedTypesTest {
  @TempDir File directory

  @Test
  void leafTypesSurviveListsMapKeysValuesAndStructs() {
    [Instant.parse('2024-01-01T00:00:00.123456Z'), new Date(1234), Timestamp.from(Instant.EPOCH),
     java.sql.Date.valueOf('2024-01-01'), (byte) 12, (short) 1234, BigInteger.TEN,
     LocalTime.of(1, 2, 3, 456789123), Time.valueOf('12:00:00')].each { value ->
      def data = Matrix.builder().columns(l: [[value]], m: [[(value): value]], s: [[[field: value]]]).types(List, Map, List).build()
      byte[] bytes = MatrixParquetWriter.writeBytes(data)
      def result = MatrixParquetReader.builder().zoneId('Pacific/Honolulu').read(bytes)
      def actuals = [result[0, 'l'][0], result[0, 'm'].keySet().first(), result[0, 'm'].values().first(), result[0, 's'][0].field]
      actuals.each { actual -> assertEquals(value, actual); assertEquals(value.getClass(), actual.getClass()) }
      def metadata = ParquetFixFixtures.footer(bytes).metadata
      if (value instanceof LocalTime || value instanceof Time) { assertFalse(metadata.containsKey('matrix.nestedTypes'), "$value ${value.getClass()} $metadata") }
    }
  }

  @Test
  void logicalPathsProjectionAndDatasets() {
    def data = Matrix.builder().columns(l: [[[Instant.EPOCH]]], m: [[a: [(byte) 1]]]).types(List, Map).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    def nested = MatrixParquetReader.parseNestedTypes(ParquetFixFixtures.footer(bytes).metadata['matrix.nestedTypes'])
    assertEquals(['l.element.element': Instant.name, 'm.value.element': Byte.name], nested)
    assertEquals(Instant.EPOCH, MatrixParquetReader.read(bytes)[0, 'l'][0][0])
    assertEquals(Byte, MatrixParquetReader.builder().columns(['m']).read(bytes)[0, 'm'].a[0].getClass())
    File first = new File(directory, 'a.parquet'); first.bytes = bytes
    File second = new File(directory, 'b.parquet'); second.bytes = bytes
    assertEquals(2, MatrixParquetReader.read([first, second]).rowCount())
    def other = Matrix.builder().columns(l: [[[LocalDateTime.of(1970, 1, 1, 0, 0)]]], m: [[a: [(byte) 1]]]).types(List, Map).build()
    second.bytes = MatrixParquetWriter.writeBytes(other)
    assertThrows(IllegalArgumentException) { MatrixParquetReader.read([first, second]) }
  }

  @Test
  void legacyTwoLevelPathsAndUntrustedEntries() {
    def schema = MessageTypeParser.parseMessageType('message legacy { optional group l (LIST) { repeated int32 array; } }')
    ['java.lang.Short', 'java.lang.String', 'no.such.Class', null, 'malformed'].eachWithIndex { type, index ->
      String json = type == null ? null : type == 'malformed' ? '{broken}' : "{\"l.element\":\"$type\"}"
      File file = ParquetFixFixtures.write(directory, "legacy-$index", schema, json == null ? [:] : ['matrix.nestedTypes': json]) { writer, factory ->
        def row = factory.newGroup(); row.addGroup('l').append('array', 12); writer.write(row)
      }
      def value = MatrixParquetReader.read(file)[0, 'l'][0]
      assertEquals(12, value)
      assertEquals(type == 'java.lang.Short' ? Short : Integer, value.getClass())
    }
    assertEquals([:], MatrixParquetReader.parseNestedTypes('{"l.element","java.lang.Short"}'))
    assertEquals(['a"b': 'java.lang.Short'], MatrixParquetReader.parseNestedTypes('{"a\\"b":"java.lang.Short"}'))
  }
  @Test
  void nestedMetadataCannotOverrideTopLevelTypesEvenWithDottedColumnNames() {
    def schema = MessageTypeParser.parseMessageType('message external { optional int32 value; }')
    File file = ParquetFixFixtures.write(directory, 'top-level-guard', schema,
        ['matrix.nestedTypes': '{"value":"java.lang.Short"}']) { writer, factory ->
      writer.write(factory.newGroup().append('value', 12))
    }
    def result = MatrixParquetReader.read(file)
    assertEquals(Integer, result.type('value'))
    assertEquals(Integer, result[0, 'value'].getClass())
    byte[] dotted = MatrixParquetWriter.writeBytes(Matrix.builder().columns(['a.b': [(byte) 1]]).build())
    assertFalse(ParquetFixFixtures.footer(dotted).metadata.containsKey('matrix.nestedTypes'))
    assertEquals(Byte, MatrixParquetReader.read(dotted).type('a.b'))
  }

  @Test
  void dottedTopLevelColumnDoesNotCollideWithStructChildPath() {
    def data = Matrix.builder().columns('s.a': [(short) 1000], s: [[a: (byte) 1, c: 'x']]).types(Short, Map).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    assertEquals(['s.a': 'java.lang.Byte'],
        MatrixParquetReader.parseNestedTypes(ParquetFixFixtures.footer(bytes).metadata['matrix.nestedTypes']))
    def result = MatrixParquetReader.read(bytes)
    assertEquals(Short, result.type('s.a'))
    assertEquals((short) 1000, result[0, 's.a'])
    assertEquals((byte) 1, result[0, 's'].a)
    assertEquals(Byte, result[0, 's'].a.getClass())
  }

  @Test
  void dottedStructKeyDoesNotCollideWithNestedStructPath() {
    def data = Matrix.builder().columns(s: [['a.b': (short) 1000, a: [b: (byte) 1, c: 'x']]]).types(Map).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(data)
    assertEquals(['s.a\\.b': 'java.lang.Short', 's.a.b': 'java.lang.Byte'],
        MatrixParquetReader.parseNestedTypes(ParquetFixFixtures.footer(bytes).metadata['matrix.nestedTypes']))
    def struct = MatrixParquetReader.read(bytes)[0, 's']
    assertEquals((short) 1000, struct['a.b'])
    assertEquals(Short, struct['a.b'].getClass())
    assertEquals((byte) 1, struct.a.b)
    assertEquals(Byte, struct.a.b.getClass())
  }

  @Test
  void decimalMetaKeepsRawDottedPathsForColumnsAndStructKeys() {
    def top = Matrix.builder().columns('a.b': [1.25G]).build()
    byte[] bytes = MatrixParquetWriter.writeBytes(top, new ParquetWriteOptions().decimalMeta(['a.b': [10, 2] as int[]]))
    assertEquals(1.25G, MatrixParquetReader.read(bytes)[0, 'a.b'])
    def nested = Matrix.builder().columns(s: [['p.q': 1.25G, n: 'x']]).types(Map).build()
    bytes = MatrixParquetWriter.writeBytes(nested, new ParquetWriteOptions().decimalMeta(['s.p.q': [10, 2] as int[]]))
    def struct = MatrixParquetReader.read(bytes)[0, 's']
    assertEquals(1.25G, struct['p.q'])
    assertEquals('x', struct.n)
  }

}
