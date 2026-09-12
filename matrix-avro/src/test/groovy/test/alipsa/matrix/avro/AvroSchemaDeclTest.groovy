package test.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*

import org.apache.avro.Schema
import org.junit.jupiter.api.Test

import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.Matrix

class AvroSchemaDeclTest {

  @Test
  void bigIntegerDeclarationRoundTripsThroughSpiMapsAndNestedSchemas() {
    AvroSchemaDecl declaration = AvroSchemaDecl.array(AvroSchemaDecl.bigInteger(30))
    assertEquals([kind: 'array', elementType: [kind: 'bigInteger', precision: 30]], declaration.toMap())
    assertEquals(declaration, AvroSchemaDecl.fromMap(declaration.toMap()))

    BigInteger value = new BigInteger('-92233720368547758081234567890')
    Matrix matrix = Matrix.builder('DeclaredBigInteger')
        .columns(values: [[BigInteger.ZERO, value]])
        .types(List)
        .build()
    byte[] bytes = MatrixAvroWriter.writeBytes(matrix, AvroWriteOptions.defaults().columnSchema('values', declaration))
    Schema schema = MatrixAvroReader.schema(bytes)
    Schema valueSchema = schema.getField('values').schema().types[1].elementType.types[1]
    assertEquals(Schema.Type.BYTES, valueSchema.type)
    assertEquals(0, valueSchema.logicalType.scale)
    assertEquals('java.math.BigInteger', valueSchema.getProp('se.alipsa.matrix.javaType'))
    assertEquals(value, MatrixAvroReader.read(bytes)[0, 'values'][1])
  }

  @Test
  void classOnlyBigIntegerDeclarationsExplainHowToSupplyPrecision() {
    [
        { AvroSchemaDecl.type(BigInteger) },
        { AvroSchemaDecl.arrayOf(BigInteger) },
        { AvroSchemaDecl.mapOf(BigInteger) }
    ].each { Closure<?> action ->
      IllegalArgumentException exception = assertThrows(IllegalArgumentException, action)
      assertTrue(exception.message.contains('bigInteger(precision)'))
    }
  }
}
