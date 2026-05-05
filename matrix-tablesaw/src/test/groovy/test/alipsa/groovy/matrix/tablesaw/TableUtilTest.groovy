package test.alipsa.groovy.matrix.tablesaw

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static tech.tablesaw.api.ColumnType.*

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.BooleanColumn
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.DateColumn
import tech.tablesaw.api.DateTimeColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.FloatColumn
import tech.tablesaw.api.InstantColumn
import tech.tablesaw.api.IntColumn
import tech.tablesaw.api.LongColumn
import tech.tablesaw.api.ShortColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.api.TimeColumn
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.io.csv.CsvReadOptions

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.TableUtil

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TableUtilTest {

  @Test
  void testFrequency() {
    def csv = getClass().getResource('/glaciers.csv')
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, DOUBLE, INTEGER] as ColumnType[])

    def glaciers = Table.read().usingOptions(builder.build())
    def freq = TableUtil.frequency(glaciers, 'Number of observations')
    Assertions.assertEquals(20, freq.size())
    Assertions.assertEquals(31, freq.get(0, 1))
  }

  @Test
  void testRound() {
    def csv = getClass().getResource('/glaciers.csv')
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, BigDecimalColumnType.instance(), INTEGER] as ColumnType[])

    def glaciers = Table.read().usingOptions(builder.build())
    BigDecimalColumn col = glaciers.column(1) as BigDecimalColumn
    TableUtil.round(col, 2)
    col.forEach(v -> assertEquals(2, v.scale()))
  }

  @Test
  void testColumnTypeForClass() {
    assertEquals(STRING, TableUtil.columnTypeForClass(String))
    assertEquals(BOOLEAN, TableUtil.columnTypeForClass(Boolean))
    assertEquals(BigDecimalColumnType.instance(), TableUtil.columnTypeForClass(BigDecimal))
  }

  @Test
  void testConvertMatrixToTablesaw() {
    Matrix glaciers = Matrix.builder().data(getClass().getResource('/glaciers.csv')).build()
    Table table = TableUtil.toTablesaw(glaciers)
    assertEquals('glaciers', table.name())
    assertEquals(glaciers.columnCount(), table.columnCount(), 'number of columns')
    assertEquals(glaciers.rowCount(), table.rowCount(), 'number of rows')
    assertEquals(glaciers.get(1, 0), table.get(1, 0))
    assertEquals(glaciers.get(2, 1), table.get(2, 1))
    assertEquals(glaciers.get(3, 2), table.get(3, 2))
  }

  @Test
  void testConvertTablesawToMatrix() throws IOException {
    var csv = getClass().getResource('/tornadoes_1950-2014.csv')
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes(new ColumnType[]{LOCAL_DATE, LOCAL_TIME, STRING, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE})

    var table = Table.read().usingOptions(builder.build())
    Matrix matrix = TableUtil.fromTablesaw(table)
    assertEquals(table.name(), matrix.getMatrixName())
    assertEquals(table.columnCount(), matrix.columnCount(), 'number of columns')
    assertEquals(table.rowCount(), matrix.rowCount(), 'number of rows')
    assertEquals(table.get(0, 1), matrix.get(0, 1))
    assertEquals(table.get(2, 3), matrix.get(2, 3))
  }

  @Test
  void testFromTablesawPreservesColumnTypes() {
    def table = Table.create('type-test')
        .addColumns(StringColumn.create('s', ['a']))
        .addColumns(BooleanColumn.create('b', [true]))
        .addColumns(DateColumn.create('d', [LocalDate.parse('2024-06-24')]))
        .addColumns(DateTimeColumn.create('dt', [LocalDateTime.parse('2024-06-24T12:34:56')]))
        .addColumns(InstantColumn.create('i', [Instant.parse('2024-06-24T12:34:56Z')]))
        .addColumns(TimeColumn.create('t', [LocalTime.parse('12:34:56')]))
        .addColumns(BigDecimalColumn.create('bd', [123.45]))
        .addColumns(DoubleColumn.create('dbl', [1.2d]))
        .addColumns(FloatColumn.create('f', [3.4f] as float[]))
        .addColumns(IntColumn.create('int', [5] as int[]))
        .addColumns(LongColumn.create('lng', [6L] as long[]))
        .addColumns(ShortColumn.create('sh', [(short) 7] as short[]))

    def matrix = TableUtil.fromTablesaw(table)
    assertEquals(String, matrix.type(0), 'String')
    assertEquals(Boolean, matrix.type(1), 'Boolean')
    assertEquals(LocalDate, matrix.type(2), 'LocalDate')
    assertEquals(LocalDateTime, matrix.type(3), 'LocalDateTime')
    assertEquals(Instant, matrix.type(4), 'Instant')
    assertEquals(LocalTime, matrix.type(5), 'LocalTime')
    assertEquals(BigDecimal, matrix.type(6), 'BigDecimal')
    assertEquals(Double, matrix.type(7), 'Double')
    assertEquals(Float, matrix.type(8), 'Float')
    assertEquals(Integer, matrix.type(9), 'Integer')
    assertEquals(Long, matrix.type(10), 'Long')
    assertEquals(Short, matrix.type(11), 'Short')
  }

  @Test
  void testClassForColumnTypeUnknownType() {
    def customType = new ColumnType() {

      @Override
      public tech.tablesaw.columns.Column create(String name) { return null }

      @Override
      public String name() { return 'CUSTOM' }

      @Override
      public int byteSize() { return 0 }

      @Override
      @SuppressWarnings('GetterMethodCouldBeProperty')
      public String getPrinterFriendlyName() { return 'Custom' }

      @Override
      public tech.tablesaw.columns.AbstractColumnParser customParser(tech.tablesaw.io.ReadOptions options) { return null }

    }
    assertEquals(Object, TableUtil.classForColumnType(customType))
  }

  @Test
  void testToTablesawThrowsOnUnsupportedColumn() {
    def matrix = Matrix.builder('mixed')
        .columnNames(['id', 'value'])
        .rows([[UUID.randomUUID(), 10], [UUID.randomUUID(), 20]])
        .types([UUID, Integer])
        .build()

    def ex = assertThrows(IllegalArgumentException) { -> TableUtil.toTablesaw(matrix) }
    assertTrue(ex.message.contains('id'))
    assertTrue(ex.message.contains('UUID'))
  }

  @Test
  void testToTablesawSkipUnsupported() {
    def matrix = Matrix.builder('mixed')
        .columnNames(['id', 'value'])
        .rows([[UUID.randomUUID(), 10], [UUID.randomUUID(), 20]])
        .types([UUID, Integer])
        .build()

    def table = TableUtil.toTablesaw(matrix, true)
    assertEquals(1, table.columnCount())
    assertEquals(['value'], table.columnNames())
    assertEquals(2, table.rowCount())
    assertEquals(10, table.get(0, 0))
  }

  @Test
  void testRoundDouble() {
    assertEquals(3.14d, TableUtil.round(3.14159d, 2), 1e-9)
    assertEquals(3.14d, TableUtil.round(3.145d, 2), 1e-9)
    assertEquals(3.14d, TableUtil.round(3.135d, 2), 1e-9)
  }

  @Test
  void testRoundFloat() {
    assertEquals(3.14f, TableUtil.round(3.14159f, 2), 1e-6f)
  }

  @Test
  void testRoundRejectsNegativeDecimals() {
    assertThrows(IllegalArgumentException) { -> TableUtil.round(1.0d, -1) }
    assertThrows(IllegalArgumentException) { -> TableUtil.round(1.0f, -1) }
  }

}
