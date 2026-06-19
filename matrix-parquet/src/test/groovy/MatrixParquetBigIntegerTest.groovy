import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader
import se.alipsa.matrix.parquet.MatrixParquetWriter

import java.nio.file.Path

class MatrixParquetBigIntegerTest {

  @TempDir
  Path tempDir

  @Test
  void testBigIntegerRoundTrip() {
    def data = Matrix.builder('bigIntTest').data(
        id: [1, 2, 3],
        big: [BigInteger.TEN, BigInteger.valueOf(-12345), BigInteger.ZERO]
    ).types([Integer, BigInteger]).build()

    File file = tempDir.resolve('bigint.parquet').toFile()
    MatrixParquetWriter.write(data, file)
    Matrix matrix = MatrixParquetReader.read(file)

    assertEquals(BigInteger.TEN, matrix.big[0])
    assertEquals(BigInteger.valueOf(-12345), matrix.big[1])
    assertEquals(BigInteger.ZERO, matrix.big[2])
    assertEquals(BigInteger, matrix.types()[1])
  }

  @Test
  void testBigIntegerBeyondLongRangeRoundTrip() {
    BigInteger huge = BigInteger.TEN.pow(30)
    BigInteger hugeNegative = huge.negate().subtract(BigInteger.ONE)

    def data = Matrix.builder('hugeBigInt').data(
        id: [1, 2],
        big: [huge, hugeNegative]
    ).types([Integer, BigInteger]).build()

    File file = tempDir.resolve('huge_bigint.parquet').toFile()
    MatrixParquetWriter.write(data, file)
    Matrix matrix = MatrixParquetReader.read(file)

    assertEquals(huge, matrix.big[0])
    assertEquals(hugeNegative, matrix.big[1])
  }

  @Test
  void testBigIntegerExplicitDecimalMetaWrongScaleThrows() {
    def data = Matrix.builder('badMeta').data(big: [BigInteger.ONE]).types([BigInteger]).build()
    File file = tempDir.resolve('bad_bigint_meta.parquet').toFile()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.write(data, file, [big: [10, 2] as int[]])
    }
    assertTrue(ex.message.contains('scale must be 0'))
  }

  @Test
  void testBigIntegerExplicitDecimalMetaPrecisionHonored() {
    def data = Matrix.builder('explicitMeta').data(big: [BigInteger.valueOf(42)]).types([BigInteger]).build()
    File file = tempDir.resolve('explicit_bigint_meta.parquet').toFile()

    MatrixParquetWriter.write(data, file, [big: [5, 0] as int[]])
    Matrix matrix = MatrixParquetReader.read(file)
    assertEquals(BigInteger.valueOf(42), matrix.big[0])
  }

  @Test
  void testBigIntegerWithNullsRoundTrip() {
    def data = Matrix.builder('bigIntNulls').data(
        id: [1, 2, 3],
        big: [BigInteger.valueOf(7), null, BigInteger.valueOf(-9)]
    ).types([Integer, BigInteger]).build()

    File file = tempDir.resolve('bigint_nulls.parquet').toFile()
    MatrixParquetWriter.write(data, file)
    Matrix matrix = MatrixParquetReader.read(file)

    assertEquals(BigInteger.valueOf(7), matrix.big[0])
    assertNull(matrix.big[1])
    assertEquals(BigInteger.valueOf(-9), matrix.big[2])
  }

  @Test
  void testBigIntegerExplicitPrecisionTooSmallThrows() {
    def data = Matrix.builder('smallPrecision').data(big: [BigInteger.valueOf(9999)]).types([BigInteger]).build()
    File file = tempDir.resolve('small_precision.parquet').toFile()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      MatrixParquetWriter.write(data, file, [big: [3, 0] as int[]])
    }
    assertTrue(ex.message.contains('too small'))
  }

  @Test
  void testNestedBigIntegerListPrecisionIsInferred() {
    BigInteger huge = BigInteger.TEN.pow(30)
    def data = Matrix.builder('nestedBigInt').data(
        id: [1],
        bigs: [[huge, BigInteger.ONE]]
    ).types([Integer, List]).build()

    File file = tempDir.resolve('nested_bigint.parquet').toFile()
    MatrixParquetWriter.write(data, file)
    def schema = ParquetFileReader
        .readFooter(new Configuration(), new org.apache.hadoop.fs.Path(file.toURI()))
        .fileMetaData.schema
    def elementField = schema.getType('bigs')
        .asGroupType()
        .getType('list')
        .asGroupType()
        .getType('element')
        .asPrimitiveType()
    def logical = elementField.logicalTypeAnnotation as LogicalTypeAnnotation.DecimalLogicalTypeAnnotation

    assertEquals(huge.toString().length(), logical.precision)
    assertEquals(0, logical.scale)

    Matrix matrix = MatrixParquetReader.read(file)

    assertEquals([huge as BigDecimal, BigInteger.ONE as BigDecimal], matrix.bigs[0])
  }
}
