package se.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*

import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

import java.nio.ByteBuffer
import java.time.*

class AvroReadTypesTest {

  @Test
  void convertedValuesAndTypesAgree() {
    Schema enumSchema = Schema.createEnum('E', null, null, ['A'])
    Schema fixed = Schema.createFixed('F', null, null, 1)
    Schema record = Schema.createRecord('Nested', null, null, false)
    record.setFields([new Schema.Field('a', Schema.create(Schema.Type.INT))])
    def nested = new GenericData.Record(record)
    nested.put('a', 1)
    Schema bytes = Schema.create(Schema.Type.BYTES)
    Schema decimal = LogicalTypes.decimal(5, 0).addToSchema(Schema.create(Schema.Type.BYTES))
    Schema bigInteger = new Schema.Parser().parse('''
      {"type":"bytes","logicalType":"decimal","precision":5,"scale":0,"se.alipsa.matrix.javaType":"java.math.BigInteger"}
    ''')
    Schema unknown = new Schema.Parser().parse('{"type":"int","logicalType":"future-type"}')
    List cases = [
        [Schema.create(Schema.Type.BOOLEAN), true, Boolean, true],
        [Schema.create(Schema.Type.INT), 1, Integer, 1],
        [Schema.create(Schema.Type.LONG), 1L, Long, 1L],
        [Schema.create(Schema.Type.FLOAT), 1.5f, Float, 1.5f],
        [Schema.create(Schema.Type.DOUBLE), 1.5d, Double, 1.5d],
        [Schema.create(Schema.Type.STRING), 'hello', String, 'hello'],
        [bytes, ByteBuffer.wrap([1] as byte[]), byte[], [1] as byte[]],
        [fixed, new GenericData.Fixed(fixed, [1] as byte[]), byte[], [1] as byte[]],
        [enumSchema, new GenericData.EnumSymbol(enumSchema, 'A'), String, 'A'],
        [Schema.createArray(Schema.create(Schema.Type.INT)), [1], List, [1]],
        [Schema.createMap(Schema.create(Schema.Type.INT)), [a: 1], Map, [a: 1]],
        [record, nested, Map, [a: 1]],
        [decimal, ByteBuffer.wrap([12] as byte[]), BigDecimal, 12G],
        [bigInteger, ByteBuffer.wrap([12] as byte[]), BigInteger, 12G.toBigInteger()],
        [unknown, 1, Integer, 1],
        [logical('date', 'int'), 1, LocalDate, LocalDate.ofEpochDay(1)],
        [logical('time-millis', 'int'), 1, LocalTime, LocalTime.ofNanoOfDay(1000000)],
        [logical('time-micros', 'long'), 1L, LocalTime, LocalTime.ofNanoOfDay(1000)],
        [logical('timestamp-millis', 'long'), 1L, Instant, Instant.ofEpochMilli(1)],
        [logical('timestamp-micros', 'long'), 1L, Instant, Instant.EPOCH.plusNanos(1000)],
        [logical('local-timestamp-millis', 'long'), 1L, LocalDateTime, LocalDateTime.ofEpochSecond(0, 1000000, ZoneOffset.UTC)],
        [logical('local-timestamp-micros', 'long'), 1L, LocalDateTime, LocalDateTime.ofEpochSecond(0, 1000, ZoneOffset.UTC)],
        [logical('uuid', 'string'), '00000000-0000-0000-0000-000000000001', String, '00000000-0000-0000-0000-000000000001'],
        [Schema.create(Schema.Type.NULL), null, Object, null]
    ]
    cases.each { Schema schema, raw, Class type, expected ->
      Matrix matrix = MatrixAvroReader.read(fixture(schema, [raw]))
      assertEquals(type, matrix.type('value'), schema.toString())
      if (expected instanceof byte[]) {
        assertArrayEquals(expected, matrix[0, 'value'])
      } else {
        assertEquals(expected, matrix[0, 'value'], schema.toString())
      }
      if (matrix[0, 'value'] != null) {
        assertTrue(type.isInstance(matrix[0, 'value']))
      }
      assertEquals(type, MatrixAvroReader.read(fixture(schema, [])).type('value'))
    }
  }

  @Test
  void unionsUseConvertedClassAndEffectiveReaderSchema() {
    Schema string = Schema.create(Schema.Type.STRING)
    Schema bytes = Schema.create(Schema.Type.BYTES)
    def cases = [
        [Schema.createUnion(string), 'a', String],
        [Schema.createUnion(Schema.create(Schema.Type.NULL), string, Schema.createEnum('E', null, null, ['A'])), 'a', String],
        [Schema.createUnion(Schema.create(Schema.Type.NULL), bytes, Schema.createFixed('F', null, null, 1)), ByteBuffer.wrap([1] as byte[]), byte[]],
        [Schema.createUnion(Schema.create(Schema.Type.INT), string), 1, Object],
        [Schema.createUnion(Schema.create(Schema.Type.NULL)), null, Object]
    ]
    cases.each { Schema schema, raw, Class type ->
      Matrix matrix = MatrixAvroReader.read(fixture(schema, [raw]))
      assertEquals(type, matrix.type('value'))
      assertEquals(type, MatrixAvroReader.read(fixture(schema, [])).type('value'))
      assertTrue(raw == null || type.isInstance(matrix[0, 'value']))
    }
    Schema reader = Schema.createRecord('Types', null, null, false)
    reader.setFields([new Schema.Field('value', Schema.create(Schema.Type.LONG))])
    Matrix evolved = MatrixAvroReader.read(fixture(Schema.create(Schema.Type.INT), [1]),
        AvroReadOptions.defaults().readerSchema(reader))
    assertEquals(1L, evolved[0, 'value'])
    assertEquals(Long, evolved.type('value'))
  }

  @Test
  void typedEmptyNullAndSmallValuesPreserveNumericSchemasOnExport() {
    [Schema.Type.INT, Schema.Type.LONG, Schema.Type.FLOAT].each { type ->
      Schema nullable = Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(type))
      Object small = 1
      if (type == Schema.Type.FLOAT) small = Float.valueOf(1f)
      if (type == Schema.Type.LONG) small = Long.valueOf(1L)
      [[], [null], [small]].each { values ->
        Matrix result = MatrixAvroReader.read(fixture(nullable, values))
        2.times {
          byte[] exported = MatrixAvroWriter.writeBytes(result)
          Schema field = MatrixAvroReader.schema(exported).getField('value').schema()
          assertEquals(type, field.types.find { it.type != Schema.Type.NULL }.type)
          result = MatrixAvroReader.read(exported)
        }
      }
    }
  }

  private static Schema logical(String name, String storage) {
    new Schema.Parser().parse("""{"type":"$storage","logicalType":"$name"}""")
  }

  private static byte[] fixture(Schema field, List values) {
    Schema schema = Schema.createRecord('Types', null, null, false)
    schema.setFields([new Schema.Field('value', field)])
    ByteArrayOutputStream out = new ByteArrayOutputStream()
    def writer = new DataFileWriter(new GenericDatumWriter(schema))
    writer.create(schema, out)
    values.each { value ->
      def record = new GenericData.Record(schema)
      record.put('value', value)
      writer.append(record)
    }
    writer.close()
    out.toByteArray()
  }
}
