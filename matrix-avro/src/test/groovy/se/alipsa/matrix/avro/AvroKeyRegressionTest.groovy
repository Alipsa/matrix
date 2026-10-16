package se.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*

import org.apache.avro.Schema
import org.junit.jupiter.api.Test

import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.core.Matrix

class AvroKeyRegressionTest {

  private enum Key { NAME }

  @Test
  void normalizedKeysRoundTripThroughInferredAndDeclaredRecords() {
    String name = 'NAME'
    [[("${name}"): 12], [(Key.NAME): 12], new TreeMap([(Key.NAME): 12]), [('null'): 12]].each { Map value ->
      [AvroWriteOptions.defaults(), new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.mapOf(Integer)]),
       new AvroWriteOptions().columnSchemas([
          value: AvroSchemaDecl.record([(value.keySet().first().toString()): AvroSchemaDecl.type(Integer),
                                        optional: AvroSchemaDecl.type(Integer)])
      ])].each { options ->
        Matrix result = MatrixAvroReader.read(MatrixAvroWriter.writeBytes(matrix([value]), options))
        assertEquals(12, result[0, 'value'][value.keySet().first().toString()])
      }
    }
    Map presentNull = [("${name}"): null]
    Matrix result = MatrixAvroReader.read(MatrixAvroWriter.writeBytes(matrix([presentNull]),
        new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.record([NAME: AvroSchemaDecl.type(Integer)])])))
    assertTrue(result[0, 'value'].containsKey('NAME'))
    assertNull(result[0, 'value'].NAME)
  }

  @Test
  void collisionsAndNullKeysFailForInferredAndDeclaredMapsAndRecords() {
    String name = 'NAME'
    Map identity = new IdentityHashMap()
    identity.put(new String('NAME'), 1)
    identity.put(new String('NAME'), 2)
    Map sorted = new TreeMap(Comparator.comparingInt { System.identityHashCode(it) })
    sorted.put(new String('NAME'), 1)
    sorted.put(new String('NAME'), 2)
    List<Map> invalid = [[NAME: 1, ("${name}"): 2], [(Key.NAME): 1, NAME: 2],
                         [(1): 1, ('1'): 2], [(null): 1], identity,
                         Collections.unmodifiableMap(identity), sorted]
    invalid.each { Map value ->
      [AvroWriteOptions.defaults(),
       new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.mapOf(Integer)]),
       new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.record([NAME: AvroSchemaDecl.type(Integer)])])
      ].each { options ->
        AvroSchemaException error = assertThrows(AvroSchemaException) {
          MatrixAvroWriter.writeBytes(matrix([[other: 0], value]), options)
        }
        assertEquals('value', error.columnName)
        assertEquals(1, error.rowNumber)
        assertTrue(error.message.contains('value'))
        assertTrue(error.message.contains(value.containsKey(null) ? 'Null map key' : 'Clashing map keys'))
      }
    }
  }

  @Test
  void inferredRecordRejectsBadKeysBeforeSchemaConstruction() {
    String name = 'NAME'
    [[NAME: 1, ("${name}"): 2], [(null): 1]].each { Map value ->
      def error = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(matrix([value])) }
      assertEquals('value', error.columnName)
      assertEquals(0, error.rowNumber)
    }
    def error = assertThrows(AvroSchemaException) {
      MatrixAvroWriter.writeBytes(matrix([[("${name}"): 'bad']]),
          new AvroWriteOptions().columnSchemas([value: AvroSchemaDecl.record([NAME: AvroSchemaDecl.type(Integer)])]))
    }
    assertTrue(error.message.contains('value.NAME'))
    assertEquals(0, error.rowNumber)
  }

  @Test
  void nestedMapErrorsRetainPathAndRow() {
    String name = 'name'
    [[name: 1, ("${name}"): 2], [(null): 1]].each { bad ->
      def options = new AvroWriteOptions().columnSchemas([
          value: AvroSchemaDecl.record([nested: AvroSchemaDecl.mapOf(Integer)])])
      def error = assertThrows(AvroSchemaException) {
        MatrixAvroWriter.writeBytes(matrix([[nested: bad]]), options)
      }
      assertEquals('value', error.columnName)
      assertEquals(0, error.rowNumber)
      assertTrue(error.message.contains('value.nested'))
    }
  }

  @Test
  void compatibilityUsesNormalizedKeysAndUnionKeyErrorsAreHardFailures() {
    Schema record = new Schema.Parser().parse('''
      {"type":"record","name":"R","fields":[{"name":"NAME","type":"int"}]}
    ''')
    String name = 'NAME'
    assertTrue(AvroSchemaCompatibility.isCompatible(record, [("${name}"): 1]))
    assertFalse(AvroSchemaCompatibility.isCompatible(record, [("${name}"): 'bad']))
    assertFalse(AvroSchemaCompatibility.isCompatible(record, [:]))
    assertFalse(AvroSchemaCompatibility.isCompatible(record, [("${name}"): null]))
    Schema union = Schema.createUnion(record, Schema.create(Schema.Type.STRING))
    Map collision = [NAME: 1, ("${name}"): 2]
    def error = assertThrows(AvroSchemaException) {
      AvroSchemaCompatibility.findIncompatibleValue(union, collision, 'root.child')
    }
    assertTrue(error.message.contains('root.child'))
    assertTrue(error.message.contains('String'))
    assertTrue(error.message.contains('GString'))
    assertNull(error.columnName)
    assertEquals(-1, error.rowNumber)
    Schema mapUnion = Schema.createUnion(Schema.createMap(Schema.create(Schema.Type.INT)), Schema.create(Schema.Type.STRING))
    assertThrows(AvroSchemaException) { AvroSchemaCompatibility.isCompatible(mapUnion, collision) }
    assertThrows(AvroSchemaException) { AvroSchemaCompatibility.isCompatible(mapUnion, [(null): 1]) }
  }

  @Test
  void ordinaryMapsAndBothImmutableImplementationsRemainSupported() {
    [new HashMap([NAME: 1]), new LinkedHashMap([NAME: 1]),
     new java.util.concurrent.ConcurrentHashMap([NAME: 1]), new TreeMap([NAME: 1]),
     Map.of(), Map.of('NAME', 1), Map.of('NAME', 1, 'other', 2)].each { Map value ->
      def context = new AvroWriteContext('value')
      assertSame(value, context.keys(value, 'value').normalized)
      assertSame(context.keys(value, 'value'), context.keys(value, 'value'))
    }
  }

  private static Matrix matrix(List<Map> values) {
    Matrix.builder('Keys').columns([value: values]).types(Map).build()
  }
}
