package se.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*

import org.apache.avro.Schema
import org.junit.jupiter.api.Test

import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.core.Matrix

@SuppressWarnings(['GStringAsMapKey', 'UnnecessaryStringInstantiation'])
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
  void arrayFailuresRetainScalarAndNestedContainerPaths() {
    def numbers = new AvroWriteOptions().columnSchemas([
        value: AvroSchemaDecl.array(AvroSchemaDecl.arrayOf(Integer))])
    Matrix badNumbers = Matrix.builder('Arrays').columns([value: [[[1, 'bad']]]]).types(List).build()
    def scalarError = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(badNumbers, numbers) }
    assertTrue(scalarError.message.contains('value[0][1]'))
    assertEquals(0, scalarError.rowNumber)

    def records = new AvroWriteOptions().columnSchemas([
        value: AvroSchemaDecl.array(AvroSchemaDecl.record([nested: AvroSchemaDecl.mapOf(Integer)]))])
    Matrix badKeys = Matrix.builder('Arrays').columns([value: [[[nested: [(null): 1]]]]]).types(List).build()
    def keyError = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(badKeys, records) }
    assertTrue(keyError.message.contains('value[0].nested'))
    assertEquals('value', keyError.columnName)
    assertEquals(0, keyError.rowNumber)

    Matrix valid = Matrix.builder('Arrays').columns([value: [[[nested: [a: 1]]]]]).types(List).build()
    assertEquals([[nested: [a: 1]]], MatrixAvroReader.read(MatrixAvroWriter.writeBytes(valid, records))[0, 'value'])
  }

  @Test
  void linkedListsUseIterationAndRetainFailureIndexes() {
    List values = new IteratorOnlyList([1, 2, 3])
    Matrix input = Matrix.builder('Sequential').columns([value: [values]]).types(List).build()
    assertEquals([1, 2, 3], MatrixAvroReader.read(MatrixAvroWriter.writeBytes(input))[0, 'value'])

    List invalid = new IteratorOnlyList([1, 'bad'])
    List nested = new IteratorOnlyList([invalid])
    Matrix bad = Matrix.builder('Sequential').columns([value: [nested]]).types(List).build()
    def options = new AvroWriteOptions().columnSchemas([
        value: AvroSchemaDecl.array(AvroSchemaDecl.arrayOf(Integer))])
    def error = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(bad, options) }
    assertTrue(error.message.contains('value[0][1]'))
    assertEquals(0, error.rowNumber)
  }

  private static final class IteratorOnlyList extends LinkedList<Object> {

    IteratorOnlyList(Collection<?> values) {
      super(values)
    }

    @Override
    Object get(int index) {
      throw new AssertionError('Array processing must not use indexed access')
    }
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
