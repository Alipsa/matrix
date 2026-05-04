package se.alipsa.matrix.tablesaw.gtable

import tech.tablesaw.api.*
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.columns.Column
import tech.tablesaw.table.Relation

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.Normalizer
import se.alipsa.matrix.tablesaw.TableUtil

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * This is an extension of a Tablesaw Table adding some "grooviness" to Table e.g.
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
    new Gtable(table)
  }

  static Gtable create(List<Column<?>> columns) {
    Gtable table = new Gtable()
    for (final Column<?> column : columns) {
      table.addColumns(column)
    }
    table
  }

  @SuppressWarnings('ImplementationAsType')
  static Gtable create(LinkedHashMap<String, List<?>> data, List<ColumnType> columnTypes) {
    validateColumnLengths(data)
    List<Column<?>> columns = []
    int i = 0
    data.each {
      columns << TableUtil.createColumn(columnTypes.get(i++), it.key, it.value)
    }
    create(columns)
  }

  @SuppressWarnings('ImplementationAsType')
  static Gtable create(LinkedHashMap<String, List<?>> data) {
    def inferredTypes = data.collect { entry ->
      def firstNonNull = entry.value.find { it != null }
      def inferred = firstNonNull == null ? ColumnType.STRING : TableUtil.columnTypeForClass(firstNonNull.class)
      if (inferred == ColumnType.SKIP) {
        throw new IllegalArgumentException(
            "Cannot infer column type for '${entry.key}': ${firstNonNull.class.name} is not supported")
      }
      inferred
    }
    create(data, inferredTypes)
  }

  @SuppressWarnings('ImplementationAsType')
  static Gtable create(LinkedHashMap<String, List<?>> data, LinkedHashMap<String, ColumnType> typeOverrides) {
    def types = data.collect { entry ->
      typeOverrides.get(entry.key) ?: {
        def firstNonNull = entry.value.find { it != null }
        def inferred = firstNonNull == null ? ColumnType.STRING : TableUtil.columnTypeForClass(firstNonNull.class)
        if (inferred == ColumnType.SKIP) {
          throw new IllegalArgumentException(
              "Cannot infer column type for '${entry.key}': ${firstNonNull.class.name} is not supported")
        }
        inferred
      }()
    }
    create(data, types)
  }

  private static final int UNSET = -1

  @SuppressWarnings('ImplementationAsType')
  private static void validateColumnLengths(LinkedHashMap<String, List<?>> data) {
    if (data.isEmpty()) {
      return
    }
    int expectedSize = UNSET
    data.each { name, values ->
      if (expectedSize == UNSET) {
        expectedSize = values.size()
      } else if (values.size() != expectedSize) {
        throw new IllegalArgumentException(
            "Column '$name' has ${values.size()} rows but expected $expectedSize. All columns must have the same number of rows.")
      }
    }
  }

  static Gtable create() {
    new Gtable()
  }

  /** Returns a new, empty table (without rows or columns) with the given name */
  static Gtable create(String tableName) {
    new Gtable(tableName)
  }

  /**
   * Returns a new gtable with the given columns
   *
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(Column<?>... columns) {
    new Gtable(null, columns)
  }

  /**
   * Returns a new gtable with the given columns
   *
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(Collection<Column<?>> columns) {
    new Gtable(null, columns)
  }

  /**
   * Returns a new gtable with the given columns
   *
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(Stream<Column<?>> columns) {
    new Gtable(null, columns.collect(Collectors.toList()))
  }

  /**
   * Returns a new gtable with the given columns and given name
   *
   * @param name the name for this gtable
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(String name, Column<?>... columns) {
    new Gtable(name, columns)
  }

  /**
   * Returns a new gtable with the given columns and given name
   *
   * @param name the name for this table
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(String name, Collection<Column<?>> columns) {
    new Gtable(name, columns)
  }

  /**
   * Returns a new Gtable with the given columns and given name
   *
   * @param name the name for this table
   * @param columns one or more columns, all of the same @code{column.size()}
   */
  static Gtable create(String name, Stream<Column<?>> columns) {
    new Gtable(name, columns.collect(Collectors.toList()))
  }

  /**
   * Adds a new {@link StringColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addStringColumn(String name, List data) {
    addColumns(StringColumn.create(name, data)) as Gtable
  }

  /**
   * Adds a new {@link DoubleColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addDoubleColumn(String name, List data) {
    addColumns(DoubleColumn.create(name, data)) as Gtable
  }

  /**
   * Adds a new {@link BigDecimalColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addBigDecimalColumn(String name, List data) {
    addColumns(BigDecimalColumn.create(name, data)) as Gtable
  }

  /**
   * Adds a new {@link FloatColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addFloatColumn(String name, List data) {
    addColumns(FloatColumn.create(name, data as Float[])) as Gtable
  }

  /**
   * Adds a new {@link IntColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addIntColumn(String name, List data) {
    addColumns(IntColumn.create(name, data as Integer[])) as Gtable
  }

  /**
   * Adds a new {@link LongColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addLongColumn(String name, List data) {
    addColumns(LongColumn.create(name, data as long[])) as Gtable
  }

  /**
   * Adds a new {@link ShortColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addShortColumn(String name, List data) {
    addColumns(ShortColumn.create(name, data as short[])) as Gtable
  }

  /**
   * Adds a new {@link BooleanColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addBooleanColumn(String name, List data) {
    addColumns(BooleanColumn.create(name, data)) as Gtable
  }

  /**
   * Adds a new {@link DateColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addDateColumn(String name, List data) {
    addColumns(DateColumn.create(name, data)) as Gtable
  }

  /**
   * Adds a new {@link DateTimeColumn} with the given name and data.
   * @param name the name
   * @param data the data
   * @return a new Gtable
   */
  Gtable addDateTimeColumn(String name, List data) {
    addColumns(DateTimeColumn.create(name, data)) as Gtable
  }

  /**
   * Retrieves the value or column at the specified position.
   * @param row the row
   * @param column the column
   * @return the value
   */
  Object getAt(int row, int column) {
    get(row, column)
  }

  /**
   * Retrieves the value or column at the specified position.
   * @param row the row
   * @param columnName the columnName
   * @return the value
   */
  Object getAt(int row, String columnName) {
    column(columnName).get(row)
  }

  /**
   * Retrieves the value or column at the specified position.
   * @param columnIndex the columnIndex
   * @return the column
   */
  Column<?> getAt(int columnIndex) {
    column(columnIndex)
  }

  /**
   * Retrieves the value or column at the specified position.
   * @param name the name
   * @return the column
   */
  Column<?> getAt(String name) {
    column(name)
  }

  /**
   * Sets the value at the specified row and column.
   * @param args the args
   * @param value the value
   */
  void putAt(List args, Object value) {
    def rowIndex = args[0] as Integer
    def col = args[1]
    Integer colIndex
    if (col in Number) {
      colIndex = (col as Number).intValue()
    } else {
      colIndex = columnIndex(String.valueOf(col))
    }
    putAt(rowIndex, colIndex, value)
  }

  /**
   * Sets the value at the specified row and column.
   * @param rowIndex the rowIndex
   * @param columnIndex the columnIndex
   * @param value the value
   */
  @SuppressWarnings('unchecked')
  void putAt(int rowIndex, int columnIndex, Object value) {
    def v = value.asType(asJavaClass(columnIndex))
    ((Column) column(columnIndex)).set(rowIndex, v)
  }

  /**
   * Returns a {@link GdataFrameReader} for reading data into a Gtable.
   * @return a {@link GdataFrameReader}
   */
  static GdataFrameReader read() {
    new GdataFrameReader(defaultReaderRegistry)
  }

  /**
   * Returns a new table with duplicate rows removed.
   * @return a new Gtable
   */
  Gtable dropDuplicateRows() {
    create(super.dropDuplicateRows())
  }

  /**
   * Returns a new table with rows containing missing values removed.
   * @return a new Gtable
   */
  Gtable dropRowsWithMissingValues() {
    create(super.dropRowsWithMissingValues())
  }

  /**
   * Returns a new table containing only the specified columns.
   * @param columns the columns
   * @return a new Gtable
   */
  Gtable selectColumns(Column<?>... columns) {
    create(super.selectColumns(columns))
  }

  /**
   * Returns a new table containing only the specified columns.
   * @param columnNames the columnNames
   * @return a new Gtable
   */
  Gtable selectColumns(String... columnNames) {
    create(super.selectColumns(columnNames))
  }

  /**
   * Returns a new table with the specified columns removed.
   * @param columnIndexes the columnIndexes
   * @return a new Gtable
   */
  Gtable rejectColumns(int... columnIndexes) {
    create(super.rejectColumns(columnIndexes))
  }

  /**
   * Returns a new table with the specified columns removed.
   * @param columnNames the columnNames
   * @return a new Gtable
   */
  Gtable rejectColumns(String... columnNames) {
    create(super.rejectColumns(columnNames))
  }

  /**
   * Returns a new table with the specified columns removed.
   * @param columns the columns
   * @return a new Gtable
   */
  Gtable rejectColumns(Column<?>... columns) {
    create(super.rejectColumns(columns))
  }

  /**
   * Returns a new table containing only the specified columns.
   * @param columnIndexes the columnIndexes
   * @return a new Gtable
   */
  Gtable selectColumns(int... columnIndexes) {
    create(super.selectColumns(columnIndexes))
  }

  /**
   * Returns a new table with the specified columns removed.
   * @param columns the columns
   * @return a new Gtable
   */
  Gtable removeColumns(Column<?>... columns) {
    create(super.removeColumns(columns))
  }

  /**
   * Returns a new table with columns containing missing values removed.
   * @return a new Gtable
   */
  Gtable removeColumnsWithMissingValues() {
    create(super.removeColumnsWithMissingValues())
  }

  /**
   * Returns a new table retaining only the specified columns.
   * @param columns the columns
   * @return a new Gtable
   */
  Gtable retainColumns(Column<?>... columns) {
    create(super.retainColumns(columns))
  }

  /**
   * Returns a new table retaining only the specified columns.
   * @param columnIndexes the columnIndexes
   * @return a new Gtable
   */
  Gtable retainColumns(int... columnIndexes) {
    create(super.retainColumns(columnIndexes))
  }

  /**
   * Returns a new table retaining only the specified columns.
   * @param columnNames the columnNames
   * @return a new Gtable
   */
  Gtable retainColumns(String... columnNames) {
    create(super.retainColumns(columnNames))
  }

  /**
   * Appends the given table or row to this table.
   * @param tableToAppend the tableToAppend
   * @return a new Gtable
   */
  Gtable append(Relation tableToAppend) {
    super.append(tableToAppend) as Gtable
  }

  /**
   * Appends the given table or row to this table.
   * @param row the row
   * @return a new Gtable
   */
  Gtable append(Row row) {
    super.append(row) as Gtable
  }

  /**
   * Concatenates the given table to this table.
   * @param tableToConcatenate the tableToConcatenate
   * @return a new Gtable
   */
  Gtable concat(Table tableToConcatenate) {
    super.concat(tableToConcatenate) as Gtable
  }

  /**
   * Returns a cross-tabulation count table.
   * @param column1Name the column1Name
   * @param column2Name the column2Name
   * @return a new Gtable
   */
  Gtable xTabCounts(String column1Name, String column2Name) {
    create(super.xTabCounts(column1Name, column2Name))
  }

  /**
   * Returns a cross-tabulation row percentage table.
   * @param column1Name the column1Name
   * @param column2Name the column2Name
   * @return a new Gtable
   */
  Gtable xTabRowPercents(String column1Name, String column2Name) {
    create(super.xTabRowPercents(column1Name, column2Name))
  }

  /**
   * Returns a cross-tabulation column percentage table.
   * @param column1Name the column1Name
   * @param column2Name the column2Name
   * @return a new Gtable
   */
  Gtable xTabColumnPercents(String column1Name, String column2Name) {
    create(super.xTabColumnPercents(column1Name, column2Name))
  }

  /**
   * Returns a cross-tabulation table percentage table.
   * @param column1Name the column1Name
   * @param column2Name the column2Name
   * @return a new Gtable
   */
  Gtable xTabTablePercents(String column1Name, String column2Name) {
    create(super.xTabTablePercents(column1Name, column2Name))
  }

  /**
   * Returns a cross-tabulation percentage table.
   * @param column1Name the column1Name
   * @return a new Gtable
   */
  Gtable xTabPercents(String column1Name) {
    create(super.xTabPercents(column1Name))
  }

  /**
   * Returns a cross-tabulation count table.
   * @param column1Name the column1Name
   * @return a new Gtable
   */
  Gtable xTabCounts(String column1Name) {
    create(super.xTabCounts(column1Name))
  }

  /**
   * Returns a count table grouped by the specified columns.
   * @param groupingColumns the groupingColumns
   * @return a new Gtable
   */
  Gtable countBy(CategoricalColumn<?>... groupingColumns) {
    create(super.countBy(groupingColumns))
  }

  /**
   * Returns a count table grouped by the specified columns.
   * @param categoricalColumnNames the categoricalColumnNames
   * @return a new Gtable
   */
  Gtable countBy(String... categoricalColumnNames) {
    create(super.countBy(categoricalColumnNames))
  }

  /**
   * Returns a table with the count of missing values per column.
   * @return a new Gtable
   */
  Gtable missingValueCounts() {
    create(super.missingValueCounts())
  }

  /**
   * Returns the transpose of this table.
   * @return a new Gtable
   */
  Gtable transpose() {
    create(super.transpose())
  }

  /**
   * Returns the transpose of this table.
   * @param includeColumnHeadingsAsFirstColumn the includeColumnHeadingsAsFirstColumn
   * @param useFirstColumnForHeadings the useFirstColumnForHeadings
   * @return a new Gtable
   */
  Gtable transpose(boolean includeColumnHeadingsAsFirstColumn, boolean useFirstColumnForHeadings) {
    create(super.transpose(includeColumnHeadingsAsFirstColumn, useFirstColumnForHeadings))
  }

  /**
   * Returns a melted (unpivoted) version of this table.
   * @param idVariables the idVariables
   * @param measuredVariables the measuredVariables
   * @param dropMissing the dropMissing
   * @return a new Gtable
   */
  Gtable melt(List<String> idVariables, List<NumericColumn<? extends Number>> measuredVariables, Boolean dropMissing) {
    create(super.melt(idVariables, measuredVariables, dropMissing))
  }

  /**
   * Returns a casted (pivoted) version of this table.
   * @return a new Gtable
   */
  Gtable cast() {
    create(super.cast())
  }

  /**
   * Converts this table to the specified type.
   * @param clazz the clazz
   * @return the value
   */
  Object asType(Class clazz) {
    if (clazz == Matrix) {
      return TableUtil.fromTablesaw(this)
    } else if (clazz == Grid) {
      return new Grid(TableUtil.toRowList(this))
    }
    super.asType(clazz)
  }

  /**
   * Returns a {@link GdataFrameJoiner} for joining this table on the specified columns.
   * @param columnNames the columnNames
   * @return a {@link GdataFrameJoiner}
   */
  GdataFrameJoiner joinOn(String... columnNames) {
    new GdataFrameJoiner(this, columnNames)
  }

  /**
   * Normalize a numeric column using min-max scaling (0 to 1).
   *
   * @param columnName the name of the column to normalize
   * @param outputColumnName the name for the normalized column; if null, replaces the source column
   * @param decimals optional number of decimal places
   * @return a new Gtable with the normalized column
   * @throws IllegalArgumentException if the column is not a supported numeric type
   */
  Gtable normalizeMinMax(String columnName, String outputColumnName = null, int... decimals) {
    def col = column(columnName)
    Column<?> normalized
    if (col in DoubleColumn) {
      normalized = Normalizer.minMaxNorm((DoubleColumn) col, decimals)
    } else if (col in FloatColumn) {
      normalized = Normalizer.minMaxNorm((FloatColumn) col, decimals)
    } else if (col in BigDecimalColumn) {
      normalized = Normalizer.minMaxNorm((BigDecimalColumn) col, decimals)
    } else {
      throw new IllegalArgumentException(
          "Column '$columnName' has type ${col.type()} which does not support min-max normalization")
    }
    applyNormalizedColumn(normalized, columnName, outputColumnName)
  }

  /**
   * Normalize a numeric column using mean normalization (-1 to 1).
   *
   * @param columnName the name of the column to normalize
   * @param outputColumnName the name for the normalized column; if null, replaces the source column
   * @param decimals optional number of decimal places
   * @return a new Gtable with the normalized column
   * @throws IllegalArgumentException if the column is not a supported numeric type
   */
  Gtable normalizeMean(String columnName, String outputColumnName = null, int... decimals) {
    def col = column(columnName)
    Column<?> normalized
    if (col in DoubleColumn) {
      normalized = Normalizer.meanNorm((DoubleColumn) col, decimals)
    } else if (col in FloatColumn) {
      normalized = Normalizer.meanNorm((FloatColumn) col, decimals)
    } else if (col in BigDecimalColumn) {
      normalized = Normalizer.meanNorm((BigDecimalColumn) col, decimals)
    } else {
      throw new IllegalArgumentException(
          "Column '$columnName' has type ${col.type()} which does not support mean normalization")
    }
    applyNormalizedColumn(normalized, columnName, outputColumnName)
  }

  /**
   * Normalize a numeric column using standard scaling (Z-score, mean 0, std dev 1).
   *
   * @param columnName the name of the column to normalize
   * @param outputColumnName the name for the normalized column; if null, replaces the source column
   * @param decimals optional number of decimal places
   * @return a new Gtable with the normalized column
   * @throws IllegalArgumentException if the column is not a supported numeric type
   */
  Gtable normalizeStdScale(String columnName, String outputColumnName = null, int... decimals) {
    def col = column(columnName)
    Column<?> normalized
    if (col in DoubleColumn) {
      normalized = Normalizer.stdScaleNorm((DoubleColumn) col, decimals)
    } else if (col in FloatColumn) {
      normalized = Normalizer.stdScaleNorm((FloatColumn) col, decimals)
    } else if (col in BigDecimalColumn) {
      normalized = Normalizer.stdScaleNorm((BigDecimalColumn) col, decimals)
    } else {
      throw new IllegalArgumentException(
          "Column '$columnName' has type ${col.type()} which does not support standard-scale normalization")
    }
    applyNormalizedColumn(normalized, columnName, outputColumnName)
  }

  /**
   * Normalize a numeric column using natural-log (ln) normalization.
   *
   * @param columnName the name of the column to normalize
   * @param outputColumnName the name for the normalized column; if null, replaces the source column
   * @param decimals optional number of decimal places
   * @return a new Gtable with the normalized column
   * @throws IllegalArgumentException if the column is not a supported numeric type
   */
  Gtable normalizeLog(String columnName, String outputColumnName = null, int... decimals) {
    def col = column(columnName)
    Column<?> normalized
    if (col in DoubleColumn) {
      normalized = Normalizer.logNorm((DoubleColumn) col, decimals)
    } else if (col in FloatColumn) {
      normalized = Normalizer.logNorm((FloatColumn) col, decimals)
    } else if (col in BigDecimalColumn) {
      normalized = Normalizer.logNorm((BigDecimalColumn) col, decimals)
    } else {
      throw new IllegalArgumentException(
          "Column '$columnName' has type ${col.type()} which does not support log normalization")
    }
    applyNormalizedColumn(normalized, columnName, outputColumnName)
  }

  private Gtable applyNormalizedColumn(Column<?> normalized, String columnName, String outputColumnName) {
    def result = copy()
    if (outputColumnName != null) {
      normalized.setName(outputColumnName)
      result.addColumns(normalized)
    } else {
      normalized.setName(columnName)
      result.replaceColumn(columnName, normalized)
    }
    result
  }

  /**
   * Returns a copy of this table.
   * @return a new Gtable
   */
  Gtable copy() {
    create(name(), columns()*.copy())
  }

  /**
   * Returns the Java class for the column at the specified index.
   * @param columnIndex the columnIndex
   * @return the Java class
   */
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
    Object
  }

}
