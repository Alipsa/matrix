package test.alipsa.matrix.smile

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.DataframeConverter
import smile.data.DataFrame
import smile.data.type.*
import smile.data.vector.ValueVector

import java.sql.Timestamp
import java.time.*

import static org.junit.jupiter.api.Assertions.*

class DataframeConverterTest {

  @Test
  void testMatrixToDataFrameWithIntegers() {
    def matrix = Matrix.builder()
        .data(
            id: [1, 2, 3, 4, 5],
            value: [10, 20, 30, 40, 50]
        )
        .types(Integer, Integer)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(5, df.nrow())
    assertEquals(2, df.ncol())
    assertEquals('id', df.schema().fields()[0].name())
    assertEquals('value', df.schema().fields()[1].name())
    assertEquals(1, df.get(0).get('id'))
    assertEquals(50, df.get(4).get('value'))
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveInts() {
    // Note: Matrix internally stores objects, but we can specify primitive type hints
    // The converter will use the primitive-friendly ValueVector.of() method
    def matrix = Matrix.builder()
        .data(
            id: [1, 2, 3],
            value: [10, 20, 30]
        )
        .types(int, int)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(2, df.ncol())
    assertEquals(1, df.get(0).get('id'))
    assertEquals(30, df.get(2).get('value'))
  }

  @Test
  void testMatrixToDataFrameWithDoubles() {
    def matrix = Matrix.builder()
        .data(
            x: [1.5, 2.5, 3.5],
            y: [10.1, 20.2, 30.3]
        )
        .types(Double, Double)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1.5, df.get(0).get('x') as double, 0.001)
    assertEquals(30.3, df.get(2).get('y') as double, 0.001)
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveDoubles() {
    def matrix = Matrix.builder()
        .data(
            x: [1.5d, 2.5d, 3.5d],
            y: [10.1d, 20.2d, 30.3d]
        )
        .types(double, double)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1.5d, df.get(0).get('x') as double, 0.001)
  }

  @Test
  void testMatrixToDataFrameWithStrings() {
    def matrix = Matrix.builder()
        .data(
            name: ['Alice', 'Bob', 'Charlie'],
            city: ['New York', 'London', 'Tokyo']
        )
        .types(String, String)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals('Alice', df.get(0).get('name'))
    assertEquals('Tokyo', df.get(2).get('city'))
  }

  @Test
  void testMatrixToDataFrameWithBooleans() {
    def matrix = Matrix.builder()
        .data(
            active: [true, false, true],
            verified: [false, true, false]
        )
        .types(Boolean, Boolean)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertTrue(df.get(0).get('active') as boolean)
    assertFalse(df.get(1).get('active') as boolean)
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveBooleans() {
    def matrix = Matrix.builder()
        .data(
            active: [true, false, true],
            verified: [false, true, false]
        )
        .types(boolean, boolean)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertTrue(df.get(0).get('active') as boolean)
  }

  @Test
  void testMatrixToDataFrameWithFloats() {
    def matrix = Matrix.builder()
        .data(
            x: [1.5f, 2.5f, 3.5f],
            y: [10.1f, 20.2f, 30.3f]
        )
        .types(Float, Float)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1.5f, df.get(0).get('x') as float, 0.001f)
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveFloats() {
    def matrix = Matrix.builder()
        .data(
            x: [1.5f, 2.5f, 3.5f],
            y: [10.1f, 20.2f, 30.3f]
        )
        .types(float, float)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1.5f, df.get(0).get('x') as float, 0.001f)
  }

  @Test
  void testMatrixToDataFrameWithLongs() {
    def matrix = Matrix.builder()
        .data(
            id: [1000000000L, 2000000000L, 3000000000L]
        )
        .types(Long)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1000000000L, df.get(0).get('id'))
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveLongs() {
    def matrix = Matrix.builder()
        .data(
            id: [1000000000L, 2000000000L, 3000000000L]
        )
        .types(long)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1000000000L, df.get(0).get('id'))
  }

  @Test
  void testMatrixToDataFrameWithShorts() {
    def matrix = Matrix.builder()
        .data(
            value: [(short)100, (short)200, (short)300]
        )
        .types(Short)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals((short)100, df.get(0).get('value'))
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveShorts() {
    def matrix = Matrix.builder()
        .data(
            value: [(short)100, (short)200, (short)300]
        )
        .types(short)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals((short)100, df.get(0).get('value'))
  }

  @Test
  void testMatrixToDataFrameWithBytes() {
    def matrix = Matrix.builder()
        .data(
            value: [(byte)1, (byte)2, (byte)3]
        )
        .types(Byte)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals((byte)1, df.get(0).get('value'))
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveBytes() {
    def matrix = Matrix.builder()
        .data(
            value: [(byte)1, (byte)2, (byte)3]
        )
        .types(byte)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals((byte)1, df.get(0).get('value'))
  }

  @Test
  void testMatrixToDataFrameWithCharacters() {
    def matrix = Matrix.builder()
        .data(
            letter: ['A' as char, 'B' as char, 'C' as char]
        )
        .types(Character)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals('A' as char, df.get(0).get('letter'))
  }

  @Test
  void testMatrixToDataFrameWithPrimitiveChars() {
    def matrix = Matrix.builder()
        .data(
            letter: ['A' as char, 'B' as char, 'C' as char]
        )
        .types(char)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals('A' as char, df.get(0).get('letter'))
  }

  @Test
  void testMatrixToDataFrameWithBigDecimal() {
    def matrix = Matrix.builder()
        .data(
            amount: [new BigDecimal('100.50'), new BigDecimal('200.75'), new BigDecimal('300.25')]
        )
        .types(BigDecimal)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(new BigDecimal('100.50'), df.get(0).get('amount'))
  }

  @Test
  void testMatrixToDataFrameWithLocalDate() {
    def matrix = Matrix.builder()
        .data(
            date: [LocalDate.of(2023, 1, 15), LocalDate.of(2023, 6, 20), LocalDate.of(2023, 12, 31)]
        )
        .types(LocalDate)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(LocalDate.of(2023, 1, 15), df.get(0).get('date'))
  }

  @Test
  void testMatrixToDataFrameWithLocalDateTime() {
    def matrix = Matrix.builder()
        .data(
            timestamp: [
                LocalDateTime.of(2023, 1, 15, 10, 30, 0),
                LocalDateTime.of(2023, 6, 20, 14, 45, 30)
            ]
        )
        .types(LocalDateTime)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(2, df.nrow())
    assertEquals(LocalDateTime.of(2023, 1, 15, 10, 30, 0), df.get(0).get('timestamp'))
  }

  @Test
  void testMatrixToDataFrameWithLocalTime() {
    def matrix = Matrix.builder()
        .data(
            time: [LocalTime.of(10, 30, 0), LocalTime.of(14, 45, 30)]
        )
        .types(LocalTime)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(2, df.nrow())
    assertEquals(LocalTime.of(10, 30, 0), df.get(0).get('time'))
  }

  @Test
  void testMatrixToDataFrameWithTimestamp() {
    def ts1 = Timestamp.valueOf('2023-01-15 10:30:00')
    def ts2 = Timestamp.valueOf('2023-06-20 14:45:30')
    def matrix = Matrix.builder()
        .data(
            ts: [ts1, ts2]
        )
        .types(Timestamp)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(2, df.nrow())
    assertEquals(ts1, df.get(0).get('ts'))
  }

  @Test
  void testMatrixToDataFrameWithInstant() {
    def i1 = Instant.parse('2023-01-15T10:30:00Z')
    def i2 = Instant.parse('2023-06-20T14:45:30Z')
    def matrix = Matrix.builder()
        .data(
            instant: [i1, i2]
        )
        .types(Instant)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(2, df.nrow())
    assertEquals(i1, df.get(0).get('instant'))
  }

  @Test
  void testMatrixToDataFrameWithMixedTypes() {
    def matrix = Matrix.builder()
        .data(
            id: [1, 2, 3],
            name: ['Alice', 'Bob', 'Charlie'],
            salary: [50000.50, 60000.75, 70000.25],
            active: [true, false, true],
            startDate: [LocalDate.of(2020, 1, 1), LocalDate.of(2021, 6, 15), LocalDate.of(2022, 3, 20)]
        )
        .types(Integer, String, Double, Boolean, LocalDate)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(5, df.ncol())
    assertEquals(3, df.nrow())
    assertEquals(1, df.get(0).get('id'))
    assertEquals('Bob', df.get(1).get('name'))
    assertEquals(70000.25, df.get(2).get('salary') as double, 0.001)
    assertFalse(df.get(1).get('active') as boolean)
    assertEquals(LocalDate.of(2022, 3, 20), df.get(2).get('startDate'))
  }

  @Test
  void testDataFrameToMatrixWithIntegers() {
    DataFrame df = new DataFrame(
        ValueVector.of('id', [1, 2, 3] as int[]),
        ValueVector.of('value', [10, 20, 30] as int[])
    )

    Matrix matrix = DataframeConverter.convert(df)

    assertEquals(3, matrix.rowCount())
    assertEquals(2, matrix.columnCount())
    assertEquals(['id', 'value'], matrix.columnNames() as List)
    assertEquals(1, matrix[0, 'id'])
    assertEquals(30, matrix[2, 'value'])
  }

  @Test
  void testDataFrameToMatrixWithDoubles() {
    DataFrame df = new DataFrame(
        ValueVector.of('x', [1.5, 2.5, 3.5] as double[]),
        ValueVector.of('y', [10.1, 20.2, 30.3] as double[])
    )

    Matrix matrix = DataframeConverter.convert(df)

    assertEquals(3, matrix.rowCount())
    assertEquals(1.5, matrix[0, 'x'] as double, 0.001)
    assertEquals(30.3, matrix[2, 'y'] as double, 0.001)
  }

  @Test
  void testDataFrameToMatrixWithStrings() {
    DataFrame df = new DataFrame(
        ValueVector.of('name', ['Alice', 'Bob', 'Charlie'] as String[]),
        ValueVector.of('city', ['New York', 'London', 'Tokyo'] as String[])
    )

    Matrix matrix = DataframeConverter.convert(df)

    assertEquals(3, matrix.rowCount())
    assertEquals('Alice', matrix[0, 'name'])
    assertEquals('Tokyo', matrix[2, 'city'])
  }

  @Test
  void testDataFrameToMatrixWithBooleans() {
    DataFrame df = new DataFrame(
        ValueVector.of('active', [true, false, true] as boolean[])
    )

    Matrix matrix = DataframeConverter.convert(df)

    assertEquals(3, matrix.rowCount())
    assertTrue(matrix[0, 'active'] as boolean)
    assertFalse(matrix[1, 'active'] as boolean)
  }

  @Test
  void testDataFrameToMatrixWithMixedTypes() {
    DataFrame df = new DataFrame(
        ValueVector.of('id', [1, 2, 3] as int[]),
        ValueVector.of('name', ['Alice', 'Bob', 'Charlie'] as String[]),
        ValueVector.of('salary', [50000.50, 60000.75, 70000.25] as double[]),
        ValueVector.of('active', [true, false, true] as boolean[])
    )

    Matrix matrix = DataframeConverter.convert(df)

    assertEquals(4, matrix.columnCount())
    assertEquals(3, matrix.rowCount())
    assertEquals(2, matrix[1, 'id'])
    assertEquals('Charlie', matrix[2, 'name'])
    assertEquals(50000.50, matrix[0, 'salary'] as double, 0.001)
    assertFalse(matrix[1, 'active'] as boolean)
  }

  @Test
  void testRoundTripConversionIntegers() {
    def original = Matrix.builder()
        .data(
            id: [1, 2, 3],
            value: [100, 200, 300]
        )
        .types(Integer, Integer)
        .build()

    DataFrame df = DataframeConverter.convert(original)
    Matrix roundTrip = DataframeConverter.convert(df)

    assertEquals(original.rowCount(), roundTrip.rowCount())
    assertEquals(original.columnCount(), roundTrip.columnCount())
    assertEquals(original.columnNames() as List, roundTrip.columnNames() as List)

    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original[i, 'id'], roundTrip[i, 'id'])
      assertEquals(original[i, 'value'], roundTrip[i, 'value'])
    }
  }

  @Test
  void testRoundTripConversionStrings() {
    def original = Matrix.builder()
        .data(
            name: ['Alice', 'Bob', 'Charlie'],
            city: ['NYC', 'LA', 'SF']
        )
        .types(String, String)
        .build()

    DataFrame df = DataframeConverter.convert(original)
    Matrix roundTrip = DataframeConverter.convert(df)

    assertEquals(original.rowCount(), roundTrip.rowCount())
    assertEquals(original.columnCount(), roundTrip.columnCount())

    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original[i, 'name'], roundTrip[i, 'name'])
      assertEquals(original[i, 'city'], roundTrip[i, 'city'])
    }
  }

  @Test
  void testRoundTripConversionMixedTypes() {
    def original = Matrix.builder()
        .data(
            id: [1, 2, 3],
            name: ['Alice', 'Bob', 'Charlie'],
            salary: [50000.50, 60000.75, 70000.25]
        )
        .types(Integer, String, Double)
        .build()

    DataFrame df = DataframeConverter.convert(original)
    Matrix roundTrip = DataframeConverter.convert(df)

    assertEquals(original.rowCount(), roundTrip.rowCount())
    assertEquals(original.columnCount(), roundTrip.columnCount())

    for (int i = 0; i < original.rowCount(); i++) {
      assertEquals(original[i, 'id'], roundTrip[i, 'id'])
      assertEquals(original[i, 'name'], roundTrip[i, 'name'])
      assertEquals(original[i, 'salary'] as double, roundTrip[i, 'salary'] as double, 0.001)
    }
  }

  @Test
  void testGetTypeFloat() {
    assertEquals(Float, DataframeConverter.getType(DataTypes.FloatType))
  }

  @Test
  void testGetTypeDouble() {
    assertEquals(Double, DataframeConverter.getType(DataTypes.DoubleType))
  }

  @Test
  void testGetTypeInt() {
    assertEquals(Integer, DataframeConverter.getType(DataTypes.IntType))
  }

  @Test
  void testGetTypeString() {
    assertEquals(String, DataframeConverter.getType(DataTypes.StringType))
  }

  @Test
  void testGetTypeBoolean() {
    assertEquals(Boolean, DataframeConverter.getType(DataTypes.BooleanType))
  }

  @Test
  void testGetTypeChar() {
    assertEquals(Character, DataframeConverter.getType(DataTypes.CharType))
  }

  @Test
  void testGetTypeByte() {
    assertEquals(Byte, DataframeConverter.getType(DataTypes.ByteType))
  }

  @Test
  void testGetTypeShort() {
    assertEquals(Short, DataframeConverter.getType(DataTypes.ShortType))
  }

  @Test
  void testGetTypeLong() {
    assertEquals(Long, DataframeConverter.getType(DataTypes.LongType))
  }

  @Test
  void testGetTypeDecimal() {
    assertEquals(BigDecimal, DataframeConverter.getType(DataTypes.DecimalType))
  }

  @Test
  void testGetTypeDateTime() {
    assertEquals(LocalDateTime, DataframeConverter.getType(DataTypes.DateTimeType))
  }

  @Test
  void testGetTypeDate() {
    assertEquals(LocalDate, DataframeConverter.getType(DataTypes.DateType))
  }

  @Test
  void testGetTypeTime() {
    assertEquals(LocalTime, DataframeConverter.getType(DataTypes.TimeType))
  }

  @Test
  void testMatrixToDataFrameWithNullableIntegers() {
    def matrix = Matrix.builder()
        .data(
            id: [1, null, 3, null, 5]
        )
        .types(Integer)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(5, df.nrow())
    assertEquals(1, df.get(0).get('id'))
    assertNull(df.get(1).get('id'))
    assertEquals(3, df.get(2).get('id'))
    assertNull(df.get(3).get('id'))
    assertEquals(5, df.get(4).get('id'))
  }

  @Test
  void testMatrixToDataFrameWithNullableDoubles() {
    def matrix = Matrix.builder()
        .data(
            value: [1.5, null, 3.5]
        )
        .types(Double)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(3, df.nrow())
    assertEquals(1.5, df.get(0).get('value') as double, 0.001)
    assertNull(df.get(1).get('value'))
    assertEquals(3.5, df.get(2).get('value') as double, 0.001)
  }

  @Test
  void testEmptyMatrix() {
    def matrix = Matrix.builder()
        .columnNames(['id', 'name'])
        .types(Integer, String)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(0, df.nrow())
    assertEquals(2, df.ncol())
    assertEquals('id', df.schema().fields()[0].name())
    assertEquals('name', df.schema().fields()[1].name())
  }

  @Test
  void testSingleRowMatrix() {
    def matrix = Matrix.builder()
        .data(
            id: [1],
            name: ['Alice']
        )
        .types(Integer, String)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(1, df.nrow())
    assertEquals(2, df.ncol())
    assertEquals(1, df.get(0).get('id'))
    assertEquals('Alice', df.get(0).get('name'))
  }

  @Test
  void testSingleColumnMatrix() {
    def matrix = Matrix.builder()
        .data(
            values: [1, 2, 3, 4, 5]
        )
        .types(Integer)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)

    assertEquals(5, df.nrow())
    assertEquals(1, df.ncol())
    assertEquals('values', df.schema().fields()[0].name())
  }

  @Test
  void testLargeMatrix() {
    def ids = (1..1000).toList()
    def names = (1..1000).collect { "Name$it" }

    def matrix = Matrix.builder()
        .data(
            id: ids,
            name: names
        )
        .types(Integer, String)
        .build()

    DataFrame df = DataframeConverter.convert(matrix)
    Matrix roundTrip = DataframeConverter.convert(df)

    assertEquals(1000, df.nrow())
    assertEquals(1000, roundTrip.rowCount())
    assertEquals(1, roundTrip[0, 'id'])
    assertEquals(1000, roundTrip[999, 'id'])
    assertEquals('Name500', roundTrip[499, 'name'])
  }
}
