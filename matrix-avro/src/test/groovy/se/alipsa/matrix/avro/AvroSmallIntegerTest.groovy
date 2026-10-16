package se.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*

import org.apache.avro.Schema
import org.junit.jupiter.api.Test

import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.core.Matrix

class AvroSmallIntegerTest {

  @Test
  void byteAndShortScalarsWidenToInt() {
    [Byte, byte, Short, short].each { Class type ->
      def value = type in [Byte, byte] ? (byte) 12 : (short) 12
      Matrix matrix = Matrix.builder('Small').columns([value: [value]]).types(type).build()
      byte[] bytes = MatrixAvroWriter.writeBytes(matrix)
      assertEquals(Schema.Type.INT, nonNull(MatrixAvroReader.schema(bytes).getField('value').schema()).type)
      Matrix result = MatrixAvroReader.read(bytes)
      assertEquals(12, result[0, 'value'])
      assertEquals(Integer, result.type('value'))
      assertInstanceOf(Integer, result[0, 'value'])
    }
  }

  @Test
  void nestedSmallIntegersUseIntAndMixedListsAreRejected() {
    [(byte) 1, (short) 1].each { value ->
      Matrix matrix = Matrix.builder('Nested').columns([
          list: [[value]], record: [[a: value]], map: [[a: value], [b: value]]
      ].collectEntries { k, v -> [(k): k == 'map' ? v : v * 2] }).types(List, Map, Map).build()
      byte[] bytes = MatrixAvroWriter.writeBytes(matrix)
      Schema schema = MatrixAvroReader.schema(bytes)
      assertEquals(Schema.Type.INT, nonNull(nonNull(schema.getField('list').schema()).elementType).type)
      assertEquals(Schema.Type.INT, nonNull(nonNull(schema.getField('map').schema()).valueType).type)
      assertEquals(Schema.Type.INT, nonNull(nonNull(schema.getField('record').schema()).getField('a').schema()).type)
      Matrix result = MatrixAvroReader.read(bytes)
      assertEquals(List, result.type('list'))
      assertEquals(Map, result.type('map'))
      assertEquals(Map, result.type('record'))
      assertInstanceOf(Integer, result[0, 'list'][0])
      assertInstanceOf(Integer, result[0, 'map'].a)
      assertInstanceOf(Integer, result[0, 'record'].a)
    }
    [(byte) 1, (short) 1, 1].each { value ->
      def matrix = Matrix.builder('Mixed').columns([value: [[value, 'a']]]).types(List).build()
      def error = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(matrix) }
      assertEquals('value', error.columnName)
      assertTrue(error.message.contains('value[1]'))
    }
  }

  private static Schema nonNull(Schema schema) {
    schema.type == Schema.Type.UNION ? schema.types.find { it.type != Schema.Type.NULL } : schema
  }
}
