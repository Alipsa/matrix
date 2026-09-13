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

    BigInteger value = -92233720368547758081234567890G
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
  void classOnlyBigIntegerDeclarationsUseTheDefaultPrecision() {
    assertEquals([kind: 'bigInteger', precision: 19], AvroSchemaDecl.type(BigInteger).toMap())
    assertEquals([kind: 'array', elementType: [kind: 'bigInteger', precision: 19]], AvroSchemaDecl.arrayOf(BigInteger).toMap())
    assertEquals([kind: 'map', valueType: [kind: 'bigInteger', precision: 19]], AvroSchemaDecl.mapOf(BigInteger).toMap())
  }

  @Test
  void classOnlyBigIntegerDeclarationSupportsTheFormerLongRange() {
    BigInteger value = 1234567890123456789G
    Matrix matrix = Matrix.builder('DeclaredBigInteger')
        .columns(id: [value])
        .types(BigInteger)
        .build()

    byte[] bytes = MatrixAvroWriter.writeBytes(matrix, AvroWriteOptions.defaults()
        .columnSchema('id', AvroSchemaDecl.type(BigInteger)))

    assertEquals(value, MatrixAvroReader.read(bytes)[0, 'id'])
  }

  @Test
  void bigIntegerPrecisionValidationUsesThePublicFactoryName() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      AvroSchemaDecl.bigInteger(0)
    }
    assertEquals('bigInteger precision must be > 0 but was 0', exception.message)
  }
}
