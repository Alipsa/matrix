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
  void testFrequencyUsesExplicitMissingMarker() {
    BooleanColumn column = BooleanColumn.create('flags')
    column.append(true)
    column.appendMissing()
    column.append(false)
    column.appendMissing()

    def freq = TableUtil.frequency(column)
    def counts = frequencyCounts(freq)

    assertEquals(2, counts['<missing>'])
    assertEquals(1, counts['true'])
    assertEquals(1, counts['false'])
    assertEquals(50.0d, frequencyPercent(freq, '<missing>'), 1e-9)
  }

  @Test
  void testFrequencyDistinguishesStringNullFromMissing() {
    StringColumn column = StringColumn.create('labels')
    column.append('null')
    column.appendMissing()
    column.append('value')

    def freq = TableUtil.frequency(column)
    def counts = frequencyCounts(freq)

    assertEquals(1, counts['null'])
    assertEquals(1, counts['<missing>'])
    assertEquals(1, counts['value'])
  }

  @Test
  void testFrequencyRejectsLiteralMissingMarker() {
    StringColumn column = StringColumn.create('labels')
    column.append('<missing>')
    column.appendMissing()

    def ex = assertThrows(IllegalArgumentException) { -> TableUtil.frequency(column) }

    assertTrue(ex.message.contains("The value '<missing>' is reserved for missing values"))
    assertTrue(ex.message.contains("column 'labels'"))
  }

  @Test
  void testRound() {
    def csv = getClass().getResource('/glaciers.csv')
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, BigDecimalColumnType.instance(), INTEGER] as ColumnType[])

    def glaciers = Table.read().usingOptions(builder.build())
    BigDecimalColumn col = glaciers.column(1) as BigDecimalColumn
    BigDecimalColumn rounded = TableUtil.round(col, 2) as BigDecimalColumn
    rounded.forEach(v -> assertEquals(2, v.scale()))
    assertTrue(col.any { it.scale() != 2 })
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
  void testRoundDoubleColumnPreservesMissingValues() {
    DoubleColumn col = DoubleColumn.create('values', [1.234d, Double.NaN, 5.678d] as double[])

    DoubleColumn rounded = TableUtil.round(col, 2) as DoubleColumn

    assertEquals(1.23d, rounded.getDouble(0), 1e-9)
    assertTrue(rounded.isMissing(1))
    assertEquals(5.68d, rounded.getDouble(2), 1e-9)
    assertEquals(1.234d, col.getDouble(0), 1e-9)
  }

  @Test
  void testRoundFloatColumnPreservesMissingValues() {
    FloatColumn col = FloatColumn.create('values', [1.234f, Float.NaN, 5.678f] as float[])

    FloatColumn rounded = TableUtil.round(col, 2) as FloatColumn

    assertEquals(1.23f, rounded.getFloat(0), 1e-6f)
    assertTrue(rounded.isMissing(1))
    assertEquals(5.68f, rounded.getFloat(2), 1e-6f)
    assertEquals(1.234f, col.getFloat(0), 1e-6f)
  }

  @Test
  void testRoundCopiesIntegerColumnsAndLeavesNonNumericColumnsAlone() {
    IntColumn ints = IntColumn.create('ints', [1, 2] as int[])
    def rounded = TableUtil.round(ints, 2)
    assertTrue(rounded !== ints)
    assertEquals(ints.asList(), rounded.asList())

    StringColumn strings = StringColumn.create('strings', ['a'])
    assertTrue(TableUtil.round(strings, 2).is(strings))
  }

  @Test
  void testCreateColumnValidatesEveryValueAndAllowsMissing() {
    def integer = TableUtil.createColumn(INTEGER, 'age', [1, null, 3])
    assertTrue(integer.isMissing(1))

    [
        (INTEGER): [1, 'x'],
        (DOUBLE): [1.0d, true],
        (BOOLEAN): [true, 'false'],
        (LOCAL_DATE): [LocalDate.now(), '2026-09-08'],
        (BigDecimalColumnType.instance()): [1.0, true]
    ].each { ColumnType type, List<?> values ->
      def exception = assertThrows(IllegalArgumentException) {
        TableUtil.createColumn(type, 'mixed', values)
      }
      assertTrue(exception.message.contains("Column 'mixed' row 1"))
      assertTrue(exception.message.contains('expects'))
    }

    assertThrows(IllegalArgumentException) {
      TableUtil.createColumn(SKIP, 'skip', [])
    }
  }

  @Test
  void testCreateColumnWidensNumbersLosslessly() {
    def widened = TableUtil.createColumn(BigDecimalColumnType.instance(), 'salary', [50000, null, 70000])
    assertEquals(3, widened.size())
    assertTrue(widened.isMissing(1))
    assertEquals(new BigDecimal('50000'), widened.getBigDecimal(0))

    def fromInts = TableUtil.createColumn(BigDecimalColumnType.instance(), 'bd', [1, 2L, (short) 3, (byte) 4, 5G])
    assertEquals(new BigDecimal('5'), fromInts.getBigDecimal(4))

    def doubles = TableUtil.createColumn(DOUBLE, 'd', [1, 9007199254740991L, 3.5f, (short) 4, (byte) 5, 6G])
    assertEquals(1.0d, doubles.getDouble(0), 0.0d)
    assertEquals(9007199254740991d, doubles.getDouble(1), 0.0d)
    assertEquals(3.5d, doubles.getDouble(2), 0.0d)

    def floats = TableUtil.createColumn(FLOAT, 'f', [1, 16777216, (short) 2, (byte) 3])
    assertEquals(1.0f, floats.getFloat(0), 0.0f)
    assertEquals(16777216.0f, floats.getFloat(1), 0.0f)

    def longs = TableUtil.createColumn(LONG, 'l', [1, (short) 2, (byte) 3])
    assertEquals(3L, longs.getLong(2))

    def ints = TableUtil.createColumn(INTEGER, 'i', [(short) 1, (byte) 2])
    assertEquals(2, ints.getInt(1))

    def shorts = TableUtil.createColumn(SHORT, 's', [(byte) 7])
    assertEquals((short) 7, shorts.getShort(0))
  }

  @Test
  void testCreateColumnRejectsLossyOrIncompatibleValues() {
    // beyond the exact range of the target type: precision loss or overflow
    assertThrows(IllegalArgumentException) {
      TableUtil.createColumn(DOUBLE, 'd', [9007199254740993L])
    }
    assertThrows(IllegalArgumentException) {
      TableUtil.createColumn(DOUBLE, 'd', [10G ** 400])
    }
    assertThrows(IllegalArgumentException) {
      TableUtil.createColumn(FLOAT, 'f', [16777217])
    }
    assertThrows(IllegalArgumentException) {
      TableUtil.createColumn(INTEGER, 'i', [1L])
    }
  }

  @Test
  void testCreateColumnConvertsFloatingToBigDecimalLikeTheColumnDoes() {
    def col = TableUtil.createColumn(BigDecimalColumnType.instance(), 'bd', [1.5d, 2.5f])
    assertEquals(1.5G, col.getBigDecimal(0))
    assertEquals(2.5G, col.getBigDecimal(1))

    // NaN becomes missing, infinities are rejected, mirroring BigDecimalColumn.toBigDecimal
    def withNan = TableUtil.createColumn(BigDecimalColumnType.instance(), 'bd', [Double.NaN])
    assertTrue(withNan.isMissing(0))
    assertThrows(IllegalArgumentException) {
      TableUtil.createColumn(BigDecimalColumnType.instance(), 'bd', [Double.POSITIVE_INFINITY])
    }
  }

  @Test
  void testCreateColumnAcceptsGStringForStringColumns() {
    def who = 'Alice'
    def column = TableUtil.createColumn(STRING, 'name', ["${who}", 'Ann', null])
    assertEquals('Alice', column.getString(0))
    assertEquals('Ann', column.getString(1))
    assertTrue(column.isMissing(2))
  }

  @Test
  void testFromMatrixWidensDeclaredTypesLosslessly() {
    // The documented tutorial example: values narrower than the declared Matrix types
    def matrix = Matrix.builder().data(
        name: ['Alice', 'Bob', 'Charlie', 'David', 'Eve'],
        age: [25, 30, 35, 40, 45],
        salary: [50000, 60000, 70000, 80000, 90000],
        dept: ['Eng', 'Eng', 'Ops', 'Ops', 'HR']
    ).types(String, Integer, BigDecimal, String).build()

    def gTable = TableUtil.fromMatrix(matrix)
    assertEquals(5, gTable.rowCount())
    assertEquals(BigDecimalColumnType.instance(), gTable.column('salary').type())
    assertEquals(new BigDecimal('60000'), gTable.column('salary').getBigDecimal(1))
  }

  @Test
  void testRoundRejectsNegativeDecimals() {
    assertThrows(IllegalArgumentException) { -> TableUtil.round(1.0d, -1) }
    assertThrows(IllegalArgumentException) { -> TableUtil.round(1.0f, -1) }
  }

  private static Map<String, Integer> frequencyCounts(Table frequency) {
    (0..<frequency.rowCount()).collectEntries { int i ->
      [(frequency.get(i, 0) as String): (frequency.get(i, 1) as Number).intValue()]
    } as Map<String, Integer>
  }

  private static double frequencyPercent(Table frequency, String value) {
    int row = (0..<frequency.rowCount()).find { int i -> frequency.get(i, 0) == value } as int
    (frequency.get(row, 2) as Number).doubleValue()
  }

}
