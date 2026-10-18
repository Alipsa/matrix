package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.apache.parquet.schema.MessageTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ParquetMapCompatibilityTest {
  @TempDir File directory

  @Test
  void mapRolesDoNotDependOnPhysicalFieldNames() {
    [['MAP', '', 'map', 'k', 'v'], ['MAP_KEY_VALUE', '', 'key_value', 'key', 'value'],
     ['MAP', ' (MAP_KEY_VALUE)', 'key_value', 'key', 'value'], ['MAP', '', 'map', 'k', null]].eachWithIndex { shape, index ->
      def (annotation, repeatedAnnotation, wrapper, key, value) = shape
      String valueField = value == null ? '' : "optional int32 $value;"
      def schema = MessageTypeParser.parseMessageType("message external { optional group m ($annotation) { repeated group $wrapper$repeatedAnnotation { required binary $key (STRING); $valueField } } }")
      File file = ParquetFixFixtures.write(directory, "map-$index", schema) { writer, factory ->
        def row = factory.newGroup(); def map = row.addGroup('m')
        ['a', 'b'].eachWithIndex { text, number ->
          def pair = map.addGroup(wrapper).append(key, text)
          if (value != null) { pair.append(value, number + 1) }
        }
        writer.write(row)
      }
      def result = MatrixParquetReader.read(file)
      assertEquals(Map, result.type('m'))
      assertEquals(value == null ? [a: null, b: null] : [a: 1, b: 2], result[0, 'm'])
    }
    def schema = MessageTypeParser.parseMessageType('message struct { optional group m { optional int32 key_value; } }')
    File file = ParquetFixFixtures.write(directory, 'struct', schema) { writer, factory ->
      def row = factory.newGroup(); row.addGroup('m').append('key_value', 7); writer.write(row)
    }
    assertEquals([key_value: 7], MatrixParquetReader.read(file)[0, 'm'])
  }
}
