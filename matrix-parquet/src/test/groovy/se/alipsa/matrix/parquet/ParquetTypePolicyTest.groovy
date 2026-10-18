package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.io.LocalOutputFile
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.LogicalTypeAnnotation as Logical
import org.apache.parquet.schema.MessageTypeParser
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName as Physical
import org.apache.parquet.schema.Types
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix

import java.sql.Timestamp
import java.time.*
import java.time.temporal.Temporal

class ParquetTypePolicyTest {
  @TempDir File directory

  private enum Category {
    FIRST
    @Override String toString() { 'display label' }
  }

  @Test
  void everyWriterMetadataNameIsAllowlistedAndCanonical() {
    Map<Class, Object> examples = [(String): 'hello', (Integer): 1, (Long): 1L, (Float): 1F,
        (Double): 1D, (Boolean): true, (Byte): (byte) 1, (Short): (short) 1,
        (BigInteger): BigInteger.TEN, (BigDecimal): 1.25G, (Character): 'x' as char,
        (byte[]): [0, -1] as byte[], (LocalDate): LocalDate.of(2024, 1, 1),
        (LocalDateTime): LocalDateTime.of(2024, 1, 1, 0, 0), (LocalTime): LocalTime.of(1, 2, 3, 456789123),
        (Instant): Instant.parse('2024-01-01T00:00:00.123456Z'), (UUID): UUID.randomUUID(),
        (Date): new Date(0), (java.sql.Date): java.sql.Date.valueOf('2024-01-01'),
        (java.sql.Time): java.sql.Time.valueOf('12:00:00'), (Timestamp): Timestamp.from(Instant.EPOCH),
        (List): ['label', null], (Map): [label: 'value'], (Category): Category.FIRST]
    examples.each { Class declaration, Object value ->
      def matrix = Matrix.builder('types').columns(value: [value, null]).types(declaration).build()
      byte[] bytes = MatrixParquetWriter.writeBytes(matrix)
      File file = new File(directory, declaration.simpleName + '.parquet')
      def spi = new ParquetFormatProvider()
      spi.write(matrix, file, [:])
      def stream = new ByteArrayOutputStream()
      MatrixParquetWriter.builder(matrix).write(stream)
      [MatrixParquetReader.read(bytes), spi.read(file, [:]),
       MatrixParquetReader.read(new ByteArrayInputStream(stream.toByteArray()))].each { result ->
        assertTrue(ColumnTypePolicy.METADATA_TYPES.containsKey(result.type('value').name))
        if (declaration == Category) { assertEquals('FIRST', result[0, 'value']) }
        else if (declaration == Character) { assertEquals('x', result[0, 'value']) }
        else if (declaration == byte[]) { assertArrayEquals((byte[]) value, (byte[]) result[0, 'value']) }
        else { assertEquals(value, result[0, 'value'], "Round trip $declaration") }
        assertNull(result[1, 'value'])
      }
    }
  }

  @Test
  void supertypesAllNullAndConcreteMismatch() {
    [(Number): 1, (CharSequence): 'a', (Comparable): 1, (Serializable): 1,
     (Temporal): LocalDate.of(2024, 1, 1), (Object): 1.25G].each { Class type, Object value ->
      def result = MatrixParquetReader.read(MatrixParquetWriter.writeBytes(
          Matrix.builder().columns(value: [value, null]).types(type).build()))
      assertEquals(value.getClass(), result.type('value'))
    }
    [Number, Temporal].each { Class type ->
      assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(Matrix.builder().columns(value: [null]).types(type).build()) }
    }
    def empty = MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(value: [null]).build()))
    assertEquals(String, empty.type('value'))
    [ZonedDateTime.now(), OffsetDateTime.now(), new Object()].each { value ->
      assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(Matrix.builder().columns(value: [value]).build()) }
    }
    [Instant.MIN, Instant.MAX].each { instant ->
      assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(Matrix.builder().columns(value: [instant]).build()) }
    }
  }

  @Test
  void temporalSubclassPoliciesAreValidatedBeforeOutput() {
    File target = new File(directory, 'dates.parquet')
    target.text = 'preserve'
    [[new Date(1000), new Timestamp(2000)], [new java.sql.Date(1000), new Timestamp(2000)]].each { pair ->
      [pair, pair.reverse()].each { values ->
        def failure = assertThrows(IllegalArgumentException) {
          MatrixParquetWriter.write(Matrix.builder().columns(value: values).types(Date).build(), target)
        }
        assertTrue(failure.message.contains('row 1'))
        assertEquals('preserve', target.text)
      }
    }
    [new Timestamp(2000), java.sql.Date.valueOf('2024-01-01'), java.sql.Time.valueOf('12:00:00')].each { value ->
      def result = MatrixParquetReader.read(MatrixParquetWriter.writeBytes(
          Matrix.builder().columns(value: [value]).types(Date).build()))
      assertEquals(Date, result.type('value'))
      assertEquals(value.time, result[0, 'value'].time)
    }
  }

  @Test
  void decimalsUseNestedAndUniformSettingsWithoutDoubleFallback() {
    def matrix = Matrix.builder().columns(price: [1.5G, 2.25G], nested: [[[amount: 1.25G]], [[amount: 2.75G]]]).types(Number, List).build()
    def options = new ParquetWriteOptions().inferPrecisionAndScale(false).precision(8).scale(2)
        .decimalMeta(['nested.element.amount': [6, 3] as int[]])
    def result = MatrixParquetReader.read(MatrixParquetWriter.writeBytes(matrix, options))
    assertEquals(BigDecimal, result.type('price'))
    assertEquals(1.50G, result[0, 'price'])
    assertEquals([[amount: 1.250G]], result[0, 'nested'])
    assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(matrix, new ParquetWriteOptions().inferPrecisionAndScale(false)) }
    assertThrows(IllegalArgumentException) { MatrixParquetWriter.writeBytes(matrix, new ParquetWriteOptions().decimalMeta([missing: [6, 2] as int[]])) }
    def precise = 12345678901234567890.123456789012345678G
    def exact = MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(value: [precise]).build()))
    assertEquals(precise, exact[0, 'value'])
    def allNull = MatrixParquetReader.read(MatrixParquetWriter.writeBytes(Matrix.builder().columns(value: [null]).types(BigDecimal).build()))
    assertEquals(BigDecimal, allNull.type('value'))
    assertNull(allNull[0, 'value'])
    def negative = Matrix.builder().columns(value: [new BigDecimal('1E+9')]).build()
    assertEquals(new BigDecimal('1000000000'), MatrixParquetReader.read(MatrixParquetWriter.writeBytes(negative))[0, 'value'])
  }

  @Test
  void externalIntegerDecimalsAndTimestampUnits() {
    def builder = Types.buildMessage()
        .optional(Physical.INT32).as(Logical.decimalType(2, 9)).named('small')
        .optional(Physical.INT64).as(Logical.decimalType(4, 18)).named('large')
    Logical.TimeUnit.values().each { unit ->
      [false, true].each { adjusted -> builder.optional(Physical.INT64).as(Logical.timestampType(adjusted, unit)).named("${unit}_${adjusted}") }
    }
    def schema = builder.named('external')
    File file = new File(directory, 'external.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration()).withType(schema).build().withCloseable { writer ->
      def group = new SimpleGroupFactory(schema).newGroup().append('small', -123).append('large', -123456L)
      schema.fields.drop(2).each { field -> group.append(field.name, -1L) }
      writer.write(group)
    }
    def result = MatrixParquetReader.builder().zoneId('Europe/Stockholm').read(file)
    assertEquals(-1.23G, result[0, 'small'])
    assertEquals(-12.3456G, result[0, 'large'])
    Logical.TimeUnit.values().each { unit ->
      long nanos = unit == Logical.TimeUnit.MILLIS ? 1_000_000L : unit == Logical.TimeUnit.MICROS ? 1000L : 1L
      Instant instant = Instant.EPOCH.minusNanos(nanos)
      assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), result[0, "${unit}_false".toString()])
      assertEquals(LocalDateTime.ofInstant(instant, ZoneId.of('Europe/Stockholm')), result[0, "${unit}_true".toString()])
    }
  }

  @Test
  void explicitUnsupportedTypesAndUntrustedMetadata() {
    ['optional fixed_len_byte_array(2) value (FLOAT16);'].eachWithIndex { declaration, index ->
      def schema = MessageTypeParser.parseMessageType("message external { $declaration }")
      File file = new File(directory, "unsupported${index}.parquet")
      ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration()).withType(schema).build().withCloseable { writer ->
        def group = new SimpleGroupFactory(schema).newGroup()
        group.add('value', Binary.fromConstantByteArray(new byte[2]))
        writer.write(group)
      }
      assertThrows(IllegalArgumentException) { MatrixParquetReader.read(file) }
    }
    def schema = Types.buildMessage().optional(Physical.INT32).named('value').named('unknown')
    File file = new File(directory, 'unknown.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration()).withType(schema)
        .withExtraMetaData(['matrix.columnTypes': 'does.not.Exist']).build().withCloseable { writer -> writer.write(new SimpleGroupFactory(schema).newGroup().append('value', 42)) }
    assertEquals(Integer, MatrixParquetReader.read(file).type('value'))
  }

  @Test
  void knownButIncompatibleMetadataFallsBackToSchema() {
    def schema = MessageTypeParser.parseMessageType('message external { optional binary payload; optional group record { optional int32 id; } }')
    File file = new File(directory, 'incompatible-metadata.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration()).withType(schema)
        .withExtraMetaData(['matrix.columnTypes': 'java.util.UUID,java.util.List', 'matrix.parquet.version': '2'])
        .build().withCloseable { writer ->
          def row = new SimpleGroupFactory(schema).newGroup()
          row.add('payload', Binary.fromConstantByteArray([0, -1] as byte[]))
          row.addGroup('record').append('id', 42)
          writer.write(row)
        }
    def result = MatrixParquetReader.read(file)
    assertEquals(byte[], result.type('payload'))
    assertArrayEquals([0, -1] as byte[], (byte[]) result[0, 'payload'])
    assertEquals(Map, result.type('record'))
    assertEquals([id: 42], result[0, 'record'])
  }

  @Test
  void legacyListLayoutsAndNestedValidation() {
    def primitive = MessageTypeParser.parseMessageType('message legacy { optional group tags (LIST) { repeated binary array (STRING); } }')
    File file = new File(directory, 'legacy-list.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration()).withType(primitive).build().withCloseable { writer ->
      def row = new SimpleGroupFactory(primitive).newGroup()
      row.addGroup('tags').append('array', 'α').append('array', 'β')
      writer.write(row)
    }
    assertEquals(['α', 'β'], MatrixParquetReader.read(file)[0, 'tags'])
    def structs = MessageTypeParser.parseMessageType('message legacy { optional group tags (LIST) { repeated group array { optional binary text (STRING); } } }')
    File structured = new File(directory, 'legacy-struct-list.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(structured.toPath())).withConf(new PlainParquetConfiguration()).withType(structs).build().withCloseable { writer ->
      def row = new SimpleGroupFactory(structs).newGroup()
      row.addGroup('tags').addGroup('array').append('text', 'α')
      writer.write(row)
    }
    assertEquals([[text: 'α']], MatrixParquetReader.read(structured)[0, 'tags'])
    [Matrix.builder().columns(value: [[[1], 'mixed']]).build(),
     Matrix.builder().columns(value: [[(null): 'missing key']]).build()].each { invalid ->
      File target = new File(directory, 'preserved.parquet')
      target.text = 'preserve'
      assertThrows(IllegalArgumentException) { MatrixParquetWriter.write(invalid, target) }
      assertEquals('preserve', target.text)
    }
  }

  @Test
  void utf8OrderingAndLegacyBinaryPredicatesMatchPushdown() {
    def values = ['a', '\uE000', '\uD800\uDC00']
    File file = new File(directory, 'unicode.parquet')
    MatrixParquetWriter.builder(Matrix.builder().columns(text: values).build())
        .pageSize(64).dictionaryEncoding(false).write(file)
    [true, false].each { indexed ->
      def result = MatrixParquetReader.builder().useColumnIndexFilter(indexed)
          .filter(Predicates.lt('text', '\uD800\uDC00')).read(file)
      assertEquals(['a', '\uE000'], result.column('text'))
    }
    File legacy = new File('src/test/resources/optimized/matrix-0.7-strings.parquet')
    def selected = MatrixParquetReader.builder().filter(Predicates.eq('text', 'héllo'.getBytes('UTF-8'))).read(legacy)
    assertEquals(['héllo'], selected.column('text'))
  }

  @Test
  void logicalPredicatesAndPrecisionValidation() {
    def matrix = Matrix.builder().columns(price: [1.25G, 2.75G, null],
        instant: [Instant.EPOCH, Instant.EPOCH.plusSeconds(1), null],
        time: [LocalTime.NOON, LocalTime.NOON.plusNanos(123456789), null]).build()
    File file = new File(directory, 'predicates.parquet')
    MatrixParquetWriter.write(matrix, file)
    [Predicates.ge('price', 2.00G), Predicates.ge('instant', Instant.EPOCH.plusSeconds(1)),
     Predicates.gt('time', LocalTime.NOON), Predicates.isNull('price')].each { filter ->
      def result = MatrixParquetReader.builder().columns(['price']).filter(filter).read(file)
      assertEquals(1, result.rowCount())
    }
    assertEquals(0, MatrixParquetReader.builder().filter(Predicates.eq('price', 1.251G)).read(file).rowCount())
    assertEquals(0, MatrixParquetReader.builder().filter(Predicates.eq('instant', Instant.EPOCH.plusNanos(1))).read(file).rowCount())
  }
}
