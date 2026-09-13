package test.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*
import static test.alipsa.matrix.avro.AvroSchemaTestSupport.*

import org.apache.avro.Schema
import org.junit.jupiter.api.Test

import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.core.Matrix

import java.lang.reflect.Method

class MatrixAvroWriterNumericTest {

  @Test
  void nestedSchemaMismatchReportsFalsyValuesAtTheFailingPosition() {
    Matrix matrix = Matrix.builder('ListMismatch').columns(vals: [[1, false]]).types(List).build()
    AvroSchemaException exception = assertThrows(AvroSchemaException) {
      MatrixAvroWriter.writeBytes(matrix, AvroWriteOptions.defaults().columnSchema('vals', AvroSchemaDecl.arrayOf(Integer)))
    }
    assertEquals('vals', exception.columnName)
    assertEquals(0, exception.rowNumber)
    assertEquals('UNION[NULL, INT]', exception.expectedType)
    assertEquals('Boolean', exception.actualType)
    assertTrue(exception.message.contains('at vals[1]'))
  }

  @Test
  void nonFiniteFloatingValuesUseInferredDoubleSchemas() {
    Matrix scalars = Matrix.builder('NonFiniteScalars').columns(value: [1.5d, Double.NaN]).types(Object).build()
    Matrix lists = Matrix.builder('NonFiniteLists').columns(values: [[1.5d, Double.NaN]]).types(List).build()
    Matrix maps = Matrix.builder('NonFiniteMaps').columns(props: [[a: 1.5d], [b: Double.NaN]]).types(Map).build()

    assertEquals(Schema.Type.DOUBLE, nonNullFieldSchema(MatrixAvroWriter.buildSchema(scalars, true), 'value').type)
    assertEquals(Schema.Type.DOUBLE, nonNullSchema(nonNullFieldSchema(MatrixAvroWriter.buildSchema(lists, true), 'values').elementType).type)
    assertEquals(Schema.Type.DOUBLE, nonNullSchema(nonNullFieldSchema(MatrixAvroWriter.buildSchema(maps, true), 'props').valueType).type)
    assertTrue(Double.isNaN((Double) MatrixAvroReader.read(MatrixAvroWriter.writeBytes(scalars, true))[1, 'value']))
    assertTrue(Double.isNaN((Double) MatrixAvroReader.read(MatrixAvroWriter.writeBytes(lists, true))[0, 'values'][1]))
    assertTrue(Double.isNaN((Double) (MatrixAvroReader.read(MatrixAvroWriter.writeBytes(maps, true))[1, 'props'] as Map).b))
  }

  @Test
  void declaredDecimalRejectsBigDecimalScaleReductionWithoutRounding() {
    Matrix matrix = Matrix.builder('ExactDecimal').columns(amount: [1.239g]).types(BigDecimal).build()
    AvroSchemaException exception = assertThrows(AvroSchemaException) {
      MatrixAvroWriter.writeBytes(matrix, AvroWriteOptions.defaults().columnSchema('amount', AvroSchemaDecl.decimal(10, 2)))
    }
    assertEquals('amount', exception.columnName)
    assertEquals(0, exception.rowNumber)
    assertTrue(exception.message.contains('declared scale'))
  }

  @Test
  void nestedCompatibilityFailureIdentifiesTheOffendingMapValue() {
    Matrix matrix = Matrix.builder('InvalidMap').columns(props: [[expected: 1, invalid: 'x']]).types(Map).build()
    AvroSchemaException exception = assertThrows(AvroSchemaException) {
      MatrixAvroWriter.writeBytes(matrix, AvroWriteOptions.defaults().columnSchema('props', AvroSchemaDecl.map(AvroSchemaDecl.type(Integer))))
    }
    assertEquals('props', exception.columnName)
    assertEquals(0, exception.rowNumber)
    assertEquals('String', exception.actualType)
    assertTrue(exception.message.contains("props['invalid']"))
  }

  @Test
  void nestedNumericProfilesScanEveryUntypedListRow() {
    BigInteger beyondLong = 92233720368547758081234567890G
    Matrix decimals = Matrix.builder('NestedDecimals').columns(values: [[1.5g], [2.25g]]).types(Object).build()
    Matrix integers = Matrix.builder('NestedIntegers').columns(values: [[1], [beyondLong]]).types(Object).build()
    Schema decimalSchema = nonNullFieldSchema(MatrixAvroWriter.buildSchema(decimals, true), 'values').elementType.types[1]
    assertEquals('decimal', decimalSchema.logicalType.name)
    assertEquals(3, decimalSchema.logicalType.precision)
    assertEquals(2, decimalSchema.logicalType.scale)
    Matrix decimalResult = MatrixAvroReader.read(MatrixAvroWriter.writeBytes(decimals, true))
    assertEquals(1.50g, decimalResult[0, 'values'][0])
    assertEquals(2.25g, decimalResult[1, 'values'][0])
    Schema integerSchema = nonNullFieldSchema(MatrixAvroWriter.buildSchema(integers, false), 'values').elementType.types[1]
    assertEquals(Schema.Type.BYTES, integerSchema.type)
    assertEquals('java.math.BigInteger', integerSchema.getProp('se.alipsa.matrix.javaType'))
    assertEquals(beyondLong, MatrixAvroReader.read(MatrixAvroWriter.writeBytes(integers, false))[1, 'values'][0])
  }

  @Test
  void declaredNumericConversionErrorsUseTheMatrixColumnName() {
    Matrix decimalMatrix = Matrix.builder('InvalidDecimal').columns(amount: [Double.NaN]).types(Object).build()
    AvroSchemaException decimalException = assertThrows(AvroSchemaException) {
      MatrixAvroWriter.writeBytes(decimalMatrix, AvroWriteOptions.defaults().columnSchema('amount', AvroSchemaDecl.decimal(4, 1)))
    }
    assertEquals('amount', decimalException.columnName)
    assertTrue(decimalException.message.contains('Non-finite decimal value'))
    Matrix longMatrix = Matrix.builder('InvalidLong').columns(count: [2.5g]).types(Object).build()
    AvroSchemaException longException = assertThrows(AvroSchemaException) {
      MatrixAvroWriter.writeBytes(longMatrix, AvroWriteOptions.defaults().columnSchema('count', AvroSchemaDecl.type(Long)))
    }
    assertEquals('count', longException.columnName)
  }

  @Test
  void longCompatibilityRejectsNonFiniteValuesWithoutThrowing() {
    Schema longSchema = Schema.create(Schema.Type.LONG)
    assertFalse(isCompatible(longSchema, Double.NaN))
    assertFalse(isCompatible(longSchema, Double.POSITIVE_INFINITY))
    assertTrue(isCompatible(longSchema, 42))
    assertTrue(isCompatible(longSchema, 42L))
    Schema intSchema = Schema.create(Schema.Type.INT)
    assertFalse(isCompatible(intSchema, 2.5g))
    assertFalse(isCompatible(intSchema, 3_000_000_000L))
    assertTrue(isCompatible(intSchema, 5L))
    assertTrue(isCompatible(intSchema, 2.0d))
    assertTrue(isCompatible(intSchema, 42))
  }

  @Test
  void bigIntegerValuesFittingInLongStillUseMarkedDecimalSchema() {
    Matrix matrix = Matrix.builder('FitsLong').columns(value: [7g, 42g, null]).types(BigInteger).build()
    [false, true].each { boolean infer ->
      Schema schema = nonNullFieldSchema(MatrixAvroWriter.buildSchema(matrix, infer), 'value')
      assertEquals(Schema.Type.BYTES, schema.type, "infer=$infer")
      assertEquals('decimal', schema.logicalType.name, "infer=$infer")
      assertEquals(0, schema.logicalType.scale, "infer=$infer")
      assertEquals(2, schema.logicalType.precision, "infer=$infer")
      assertEquals('java.math.BigInteger', schema.getProp('se.alipsa.matrix.javaType'), "infer=$infer")
    }
    assertEquals([7g, 42g, null], (0..<3).collect { MatrixAvroReader.read(MatrixAvroWriter.writeBytes(matrix, false))[it, 'value'] })
  }

  @Test
  void allNullAndEmptyBigIntegerColumnsDefaultToPrecision10MarkedDecimal() {
    Matrix allNull = Matrix.builder('AllNullBigInteger').columns(value: [null, null]).types(BigInteger).build()
    Matrix zeroRows = Matrix.builder('EmptyBigInteger').columns(value: []).types(BigInteger).build()
    [allNull, zeroRows].each { Matrix matrix ->
      Schema schema = nonNullFieldSchema(MatrixAvroWriter.buildSchema(matrix, false), 'value')
      assertEquals(Schema.Type.BYTES, schema.type)
      assertEquals('decimal', schema.logicalType.name)
      assertEquals(10, schema.logicalType.precision)
      assertEquals(0, schema.logicalType.scale)
      assertEquals('java.math.BigInteger', schema.getProp('se.alipsa.matrix.javaType'))
    }
    assertEquals([null, null], (0..<2).collect { MatrixAvroReader.read(MatrixAvroWriter.writeBytes(allNull, false))[it, 'value'] })
  }

  @Test
  void declaredLongSchemaRejectsOutOfRangeValuesWithRowContext() {
    BigInteger beyondLong = 92233720368547758081234567890G
    [beyondLong, 1e19d].each { Number outOfRange ->
      Matrix matrix = Matrix.builder('LongOutOfRange').columns(count: [1L, outOfRange]).types(Object).build()
      AvroSchemaException exception = assertThrows(AvroSchemaException) {
        MatrixAvroWriter.writeBytes(matrix, AvroWriteOptions.defaults().columnSchema('count', AvroSchemaDecl.type(Long)))
      }
      assertEquals('count', exception.columnName)
      assertEquals(1, exception.rowNumber)
      assertTrue(exception.message.contains('row: 1'))
    }
  }

  @Test
  void matrixDeclaredLongColumnRejectsFractionalValuesWithRowContext() {
    Matrix matrix = Matrix.builder('DeclaredLongColumn').columns(count: [1L, 2.5g]).types(Long).build()
    AvroSchemaException exception = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(matrix) }
    assertEquals('count', exception.columnName)
    assertEquals(1, exception.rowNumber)
    Matrix integral = Matrix.builder('DeclaredLongIntegral').columns(count: [1, 2.0d]).types(Long).build()
    assertEquals([1L, 2L], (0..<2).collect { MatrixAvroReader.read(MatrixAvroWriter.writeBytes(integral))[it, 'count'] })
  }

  @Test
  void nonFiniteDeclaredLongValueFailureIncludesRowContext() {
    Matrix matrix = Matrix.builder('NonFiniteLong').columns(count: [1L, Double.NaN]).types(Long).build()
    AvroSchemaException exception = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(matrix) }
    assertEquals('count', exception.columnName)
    assertEquals(1, exception.rowNumber)
  }

  @Test
  void declaredIntegerColumnAcceptsLosslessNumericValues() {
    Matrix matrix = Matrix.builder('DeclaredIntegerLossless').columns(count: [1, 5L, 2.0d, 2.0g]).types(Integer).build()
    assertEquals([1, 5, 2, 2], (0..<4).collect { MatrixAvroReader.read(MatrixAvroWriter.writeBytes(matrix))[it, 'count'] })
  }

  @Test
  void declaredIntegerColumnRejectsLossyValuesWithRowContext() {
    [2.5g, 3_000_000_000L, Double.NaN].each { Number lossy ->
      Matrix matrix = Matrix.builder('DeclaredIntegerLossy').columns(count: [1, lossy]).types(Integer).build()
      AvroSchemaException exception = assertThrows(AvroSchemaException) { MatrixAvroWriter.writeBytes(matrix) }
      assertEquals('count', exception.columnName, "value=$lossy")
      assertEquals(1, exception.rowNumber, "value=$lossy")
    }
  }

  @Test
  void declaredLongColumnAcceptsLosslessFloatingValues() {
    Matrix matrix = Matrix.builder('DeclaredLongLossless').columns(count: [1L, 2.0d, 3.0g]).types(Long).build()
    assertEquals([1L, 2L, 3L], (0..<3).collect { MatrixAvroReader.read(MatrixAvroWriter.writeBytes(matrix))[it, 'count'] })
  }

  private static boolean isCompatible(Schema schema, Object value) {
    Method method = MatrixAvroWriter.getDeclaredMethod('isCompatible', Schema, Object)
    method.accessible = true
    (boolean) method.invoke(null, schema, value)
  }
}
