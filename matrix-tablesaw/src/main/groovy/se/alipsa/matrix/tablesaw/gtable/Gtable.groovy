package se.alipsa.matrix.tablesaw.gtable


import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import tech.tablesaw.api.*
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.columns.Column
import tech.tablesaw.table.Relation

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * This is an extansion of a Tablesaw Table adding some "grooviness" to Table e.g.
 * <ol>
 *   <li>Add a Map creation method to simplify programmatic creation of a Gtable</li>
 *   <li>add getAt method allowing the shorthand syntax table[0,1] and table[0, 'columnName'] to retrieve data.</li>
 *   <li>add putAt method allowing the shorthand syntax table[0,1] = 12 and table[0, 'columnName'] = 'foo' to change data.</li>
 * </ol>
 */
class Gtable extends Table {

  private Gtable() {
    super('', [])
  }

  Gtable(Table table) {
    this(table.name(), table.columns())
  }
  /** Returns a new table initialized with the given name */
  private Gtable(String name) {
    super(name, [])
  }

  protected Gtable(String name, Column<?>... columns) {
    super(name, columns)
  }

  protected Gtable(String name, Collection<Column<?>> columns) {
    super(name, columns)
  }

  static Gtable create(Table table) {
    return new Gtable(table)
  }

  static Gtable create(List<Column<?>> columns) {
    Gtable table = new Gtable()
    for (final Column<?> column : columns) {
      table.addColumns(column)
    }
    return table
  }

  static Gtable create(LinkedHashMap<String, List<?>> data, List<ColumnType> columnTypes) {
    List<Column<?>> columns = new ArrayList<>()
    int i = 0
    data.each {
      columns << TableUtil.createColumn(columnTypes.get(i++), it.key, it.value)
    }
    create(columns)
  }

  static Gtable create() {
    return new Gtable()
  }

  /** Returns a new, empty table (without rows or columns) with the given name */
  static Gtable create(String tableName) {
    return new Gtable(tableName)
  }

  /**
   * Returns a new gtable with the given columns
   *
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(Column<?>... columns) {
    return new Gtable(null, columns)
  }

  /**
   * Returns a new gtable with the given columns
   *
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(Collection<Column<?>> columns) {
    return new Gtable(null, columns)
  }

  /**
   * Returns a new gtable with the given columns
   *
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(Stream<Column<?>> columns) {
    return new Gtable(null, columns.collect(Collectors.toList()))
  }

  /**
   * Returns a new gtable with the given columns and given name
   *
   * @param name the name for this gtable
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(String name, Column<?>... columns) {
    return new Gtable(name, columns)
  }

  /**
   * Returns a new gtable with the given columns and given name
   *
   * @param name the name for this table
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(String name, Collection<Column<?>> columns) {
    return new Gtable(name, columns)
  }

  /**
   * Returns a new tgable with the given columns and given name
   *
   * @param name the name for this table
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(String name, Stream<Column<?>> columns) {
    return new Gtable(name, columns.collect(Collectors.toList()))
  }

  Gtable addStringColumn(String name, List data) {
    return addColumns(StringColumn.create(name, data)) as Gtable
  }

  Gtable addDoubleColumn(String name, List data) {
    return addColumns(DoubleColumn.create(name, data)) as Gtable
  }

  Gtable addBigDecimalColumn(String name, List data) {
    return addColumns(BigDecimalColumn.create(name, data)) as Gtable
  }

  Gtable addFloatColumn(String name, List data) {
    return addColumns(FloatColumn.create(name, data as Float[])) as Gtable
  }

  Gtable addIntColumn(String name, List data) {
    return addColumns(IntColumn.create(name, data as Integer[])) as Gtable
  }

  Gtable addLongColumn(String name, List data) {
    return addColumns(LongColumn.create(name, data as long[])) as Gtable
  }

  Gtable addShortColumn(String name, List data) {
    return addColumns(ShortColumn.create(name, data as short[])) as Gtable
  }

  Gtable addBooleanColumn(String name, List data) {
    return addColumns(BooleanColumn.create(name, data)) as Gtable
  }

  Gtable addDateColumn(String name, List data) {
    return addColumns(DateColumn.create(name, data)) as Gtable
  }

  Gtable addDateTimeColumn(String name, List data) {
    return addColumns(DateTimeColumn.create(name, data)) as Gtable
  }

  Object getAt(int row, int column) {
    return get(row, column)
  }

  Object getAt(int row, String columnName) {
    return column(columnName).get(row)
  }

  Column<?> getAt(int columnIndex) {
    return column(columnIndex)
  }

  Column<?> getAt(String name) {
    return column(name)
  }

  void putAt(List args, Object value) {
    def rowIndex = args[0] as Integer
    def col = args[1]
    Integer colIndex
    if (col instanceof Number) {
      colIndex = col.intValue()
    } else {
      colIndex = columnIndex(String.valueOf(col))
    }
    putAt(rowIndex, colIndex, value)
  }

  void putAt(int rowIndex, int columnIndex, Object value) {
    def v = value.asType(asJavaClass(columnIndex))
    column(columnIndex).set(rowIndex, v)
  }

  static GdataFrameReader read() {
    return new GdataFrameReader(defaultReaderRegistry)
  }

  Gtable dropDuplicateRows() {
    return create(super.dropDuplicateRows())
  }

  Gtable dropRowsWithMissingValues() {
    return create(super.dropRowsWithMissingValues())
  }

  Gtable selectColumns(Column<?>... columns) {
    return create(super.selectColumns(columns))
  }

  Gtable selectColumns(String... columnNames) {
    return create(super.selectColumns(columnNames))
  }

  Gtable rejectColumns(int... columnIndexes) {
    return create(super.rejectColumns(columnIndexes))
  }

  Gtable rejectColumns(String... columnNames) {
    return create(super.rejectColumns(columnNames))
  }

  Gtable rejectColumns(Column<?>... columns) {
    return create(super.rejectColumns(columns))
  }

  Gtable selectColumns(int... columnIndexes) {
    return create(super.selectColumns(columnIndexes))
  }

  Gtable removeColumns(Column<?>... columns) {
    return create(super.removeColumns(columns))
  }

  Gtable removeColumnsWithMissingValues() {
    return create(super.removeColumnsWithMissingValues())
  }

  Gtable retainColumns(Column<?>... columns) {
    return create(super.retainColumns(columns))
  }

  Gtable retainColumns(int... columnIndexes) {
    return create(super.retainColumns(columnIndexes))
  }

  Gtable retainColumns(String... columnNames) {
    return create(super.retainColumns(columnNames))
  }

  Gtable append(Relation tableToAppend) {
    return super.append(tableToAppend) as Gtable
  }

  Gtable append(Row row) {
    return super.append(row) as Gtable
  }

  Gtable concat(Table tableToConcatenate) {
    return super.concat(tableToConcatenate) as Gtable
  }

  Gtable xTabCounts(String column1Name, String column2Name) {
    return create(super.xTabCounts(column1Name, column2Name))
  }

  Gtable xTabRowPercents(String column1Name, String column2Name) {
    return create(super.xTabRowPercents(column1Name, column2Name))
  }

  Gtable xTabColumnPercents(String column1Name, String column2Name) {
    return create(super.xTabColumnPercents(column1Name, column2Name))
  }

  Gtable xTabTablePercents(String column1Name, String column2Name) {
    return create(super.xTabTablePercents(column1Name, column2Name))
  }

  Gtable xTabPercents(String column1Name) {
    return create(super.xTabPercents(column1Name))
  }

  Gtable xTabCounts(String column1Name) {
    return create(super.xTabCounts(column1Name))
  }

  Gtable countBy(CategoricalColumn<?>... groupingColumns) {
    return create(super.countBy(groupingColumns))
  }

  Gtable countBy(String... categoricalColumnNames) {
    return create(super.countBy(categoricalColumnNames))
  }

  Gtable missingValueCounts() {
    return create(super.missingValueCounts())
  }

  Gtable transpose() {
    return create(super.transpose())
  }

  Gtable transpose(boolean includeColumnHeadingsAsFirstColumn, boolean useFirstColumnForHeadings) {
    return create(super.transpose(includeColumnHeadingsAsFirstColumn, useFirstColumnForHeadings))
  }

  Gtable melt(List<String> idVariables, List<NumericColumn<? extends Number>> measuredVariables, Boolean dropMissing) {
    return create(super.melt(idVariables, measuredVariables, dropMissing))
  }

  Gtable cast() {
    return create(super.cast())
  }

  Object asType(Class clazz) {
    if (clazz == Matrix) {
      return TableUtil.fromTablesaw(this)
    } else if (clazz == Grid) {
      return new Grid(TableUtil.toRowList(this))
    }
    super.asType(clazz)
  }

  GdataFrameJoiner joinOn(String... columnNames) {
    return new GdataFrameJoiner(this, columnNames)
  }

  Class asJavaClass(int columnIndex) {
    def columnType = column(columnIndex).type()
    if (columnType == ColumnType.BOOLEAN) {
      return Boolean
    }
    if (columnType == ColumnType.DOUBLE) {
      return Double
    }
    if (columnType == ColumnType.FLOAT) {
      return Float
    }
    if (columnType == ColumnType.INSTANT) {
      return Instant
    }
    if (columnType == ColumnType.INTEGER) {
      return Integer
    }
    if (columnType == ColumnType.LOCAL_DATE) {
      return LocalDate
    }
    if (columnType == ColumnType.LOCAL_DATE_TIME) {
      return LocalDateTime
    }
    if (columnType == ColumnType.LOCAL_TIME) {
      return LocalTime
    }
    if (columnType == ColumnType.LONG) {
      return Long
    }
    if (columnType == ColumnType.SHORT) {
      return Short
    }
    if (columnType == ColumnType.STRING) {
      return String
    }
    if (columnType == BigDecimalColumnType.instance()) {
      return BigDecimal
    }
    return Object
  }
}
