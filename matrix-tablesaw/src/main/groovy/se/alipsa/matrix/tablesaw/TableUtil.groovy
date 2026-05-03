package se.alipsa.matrix.tablesaw

import tech.tablesaw.api.*
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.columns.Column

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.gtable.Gtable

import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility methods for working with Tablesaw tables and converting between Matrix and Tablesaw formats.
 *
 * <p>This class provides various utility functions including:
 * <ul>
 *   <li>Frequency analysis for columns and tables</li>
 *   <li>Rounding operations for numeric values and columns</li>
 *   <li>Conversion between Matrix and Tablesaw Table formats</li>
 *   <li>Column creation and type mapping</li>
 * </ul>
 */
class TableUtil {

  /**
   * Generate a frequency table for the given column.
   *
   * <p>Creates a table with three columns:
   * <ul>
   *   <li>Value: distinct values from the column</li>
   *   <li>Frequency: count of occurrences for each value</li>
   *   <li>Percent: percentage of total (rounded to 2 decimals)</li>
   * </ul>
   *
   * <p>The resulting table is sorted by frequency in descending order.
   *
   * @param column the column to analyze
   * @return a frequency table sorted by descending frequency
   */
  private static final String COL_VALUE = 'Value'
  private static final String COL_FREQUENCY = 'Frequency'
  private static final String COL_PERCENT = 'Percent'

  static Table frequency(Column<?> column) {
    Map<Object, AtomicInteger> freq = [:]
    column.forEach { v ->
      def counter = freq.computeIfAbsent(v) { k -> new AtomicInteger() }
      counter.incrementAndGet()
    }
    int size = column.size()
    def table = Table.create(column.name())
    def valueCol = ColumnType.STRING.create(COL_VALUE)
    def freqCol = ColumnType.INTEGER.create(COL_FREQUENCY)
    def percentCol = ColumnType.DOUBLE.create(COL_PERCENT)
    table.addColumns(valueCol, freqCol, percentCol)
    for (Map.Entry<Object, AtomicInteger> entry : freq.entrySet()) {
      Row row = table.appendRow()
      row.setString(COL_VALUE, String.valueOf(entry.getKey()))
      int numOccurrence = entry.getValue().intValue()
      row.setInt(COL_FREQUENCY, numOccurrence)
      row.setDouble(COL_PERCENT, round(numOccurrence * 100.0 / size, 2))
    }
    return table.sortDescendingOn(COL_FREQUENCY)
  }

  /**
   * Generate a frequency table for the specified column in a table.
   *
   * @param table the table containing the column
   * @param columnName the name of the column to analyze
   * @return a frequency table for the specified column
   * @see #frequency(Column)
   */
  static Table frequency(Table table, String columnName) {
    return frequency(table.column(columnName))
  }

  /**
   * Round a double value to the specified number of decimal places.
   *
   * <p>Uses {@link RoundingMode#HALF_UP} for rounding.
   *
   * @param value the value to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return the rounded value
   * @throws IllegalArgumentException if numDecimals is negative
   */
  private static String numDecimalsError(int numDecimals) {
    'numDecimals cannot be a negative number: was ' + numDecimals
  }

  static double round(double value, int numDecimals) {
    if (numDecimals < 0) {
      throw new IllegalArgumentException(numDecimalsError(numDecimals))
    }

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_UP)
    return bd.doubleValue()
  }

  /**
   * Round a float value to the specified number of decimal places.
   *
   * <p>Uses {@link RoundingMode#HALF_UP} for rounding.
   *
   * @param value the value to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return the rounded value
   * @throws IllegalArgumentException if numDecimals is negative
   */
  static float round(float value, int numDecimals) {
    if (numDecimals < 0) {
      throw new IllegalArgumentException(numDecimalsError(numDecimals))
    }

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_UP)
    return bd.floatValue()
  }

  /**
   * Round all values in a column to the specified number of decimal places.
   *
   * <p>If the column is not a {@link NumberColumn}, it is returned unchanged.
   *
   * @param column the column to round
   * @param numDecimals the number of decimal places
   * @return the rounded column (or original column if not numeric)
   */
  static Column<?> round(Column<?> column, int numDecimals) {
    if (column in NumberColumn) {
      return round(column as NumberColumn, numDecimals)
    }
    return column
  }

  /**
   * Round all values in a numeric column to the specified number of decimal places.
   *
   * <p>Supported column types:
   * <ul>
   *   <li>{@link BigDecimalColumn} - uses setScale</li>
   *   <li>{@link DoubleColumn} - rounds each value</li>
   *   <li>{@link FloatColumn} - rounds each value</li>
   *   <li>Integer types (IntColumn, ShortColumn, LongColumn) - returned unchanged</li>
   * </ul>
   *
   * @param column the numeric column to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return the column with rounded values
   * @throws IllegalArgumentException if numDecimals is negative
   */
  static NumberColumn round(NumberColumn column, int numDecimals) {
    if (numDecimals < 0) {
      throw new IllegalArgumentException(numDecimalsError(numDecimals))
    }

    if (column in BigDecimalColumn) {
      (column as BigDecimalColumn).setScale(numDecimals)
    }

    if (column in DoubleColumn) {
      def dc = column as DoubleColumn
      for (int i = 0; i < dc.size(); i++) {
        double val = dc.getDouble(i)
        dc.set(i, round(val, numDecimals))
      }
    }

    if (column in FloatColumn) {
      def fc = column as FloatColumn
      for (int i = 0; i < fc.size(); i++) {
        float val = fc.getFloat(i)
        fc.set(i, round(val, numDecimals))
      }
    }
    // everything else (IntColumn, ShortColumn, LongColumn cannot be rounded as they have no decimals
    return column
  }

  /**
   * Convert a Tablesaw table to a list of rows.
   *
   * <p>Each row is represented as a list of objects corresponding to the column values.
   *
   * @param table the table to convert
   * @return a list of rows, where each row is a list of column values
   */
  static List<List<Object>> toRowList(Table table) {
    List<List<Object>> rowList = []
    int ncol = table.columnCount()
    for (Row row : table) {
      rowList.add((0..<ncol).collect { i -> row.getObject(i) })
    }
    rowList
  }

  /**
   * Convert a Tablesaw table to a Matrix.
   *
   * <p>Preserves the table name, column names, and all data. Column types are mapped
   * to their corresponding Java classes where supported; unknown or custom Tablesaw
   * column types are represented as {@code Object} in the resulting Matrix.
   *
   * @param table the Tablesaw table to convert
   * @return a Matrix with the same data and structure
   */
  static Matrix fromTablesaw(Table table) {
    List<List<?>> rows = toRowList(table)
    List<Class<?>> columnTypes = []
    for (ColumnType type : table.types()) {
      columnTypes.add(classForColumnType(type))
    }
    return Matrix.builder(table.name())
        .columnNames(table.columnNames())
        .rows(rows)
        .types(columnTypes)
        .build()
  }

  /**
   * Convert a Matrix to a Gtable.
   *
   * @param matrix the Matrix to convert
   * @return a Gtable with the same data and structure
   * @see #toTablesaw(Matrix)
   */
  static Gtable fromMatrix(Matrix matrix) {
    Gtable.create(toTablesaw(matrix))
  }

  /**
   * Convert a Gtable to a Matrix.
   *
   * @param gtable the Gtable to convert
   * @return a Matrix with the same data and structure
   * @see #fromTablesaw(Table)
   */
  static Matrix toMatrix(Gtable gtable) {
    return fromTablesaw(gtable)
  }

  /**
   * Convert a Matrix to a Tablesaw table.
   *
   * <p>Preserves the matrix name, column names, column types, and all data for supported
   * column types. Columns whose Java types map to {@link ColumnType#SKIP} (or that cannot
   * be created by {@link #createColumn(Object, String, List)}) are omitted from the result.
   *
   * @param matrix the Matrix to convert
   * @return a Tablesaw Table with the same data and structure
   */
  static Table toTablesaw(Matrix matrix) {
    toTablesaw(matrix, false)
  }

  /**
   * Convert a Matrix to a Tablesaw table with explicit control over unsupported columns.
   *
   * @param matrix the Matrix to convert
   * @param skipUnsupported if {@code true}, columns whose types are not supported are silently omitted;
   *                        if {@code false}, an {@link IllegalArgumentException} is thrown on the first unsupported column
   * @return a Tablesaw Table with the same data and structure
   */
  static Table toTablesaw(Matrix matrix, boolean skipUnsupported) {
    List<Column<?>> columns = []
    for (int i = 0; i < matrix.columnCount(); i++) {
      ColumnType type = columnTypeForClass(matrix.type(i))
      if (type == ColumnType.SKIP) {
        if (!skipUnsupported) {
          throw new IllegalArgumentException(
              "Unsupported column type for column '${matrix.columnNames().get(i)}': ${matrix.type(i).name}")
        }
        continue
      }
      Column<?> col = createColumn(type, matrix.columnNames().get(i), matrix.column(i))
      if (col != null) {
        columns.add(col)
      }
    }
    return Table.create(matrix.getMatrixName(), columns)
  }

  /**
   * Create a Tablesaw column of the specified type with the given name and values.
   *
   * <p>Supported types include:
   * <ul>
   *   <li>STRING - {@link StringColumn}</li>
   *   <li>BOOLEAN - {@link BooleanColumn}</li>
   *   <li>LOCAL_DATE - {@link DateColumn}</li>
   *   <li>LOCAL_DATE_TIME - {@link DateTimeColumn}</li>
   *   <li>INSTANT - {@link InstantColumn}</li>
   *   <li>LOCAL_TIME - {@link TimeColumn}</li>
   *   <li>BigDecimalColumnType - {@link BigDecimalColumn}</li>
   *   <li>DOUBLE - {@link DoubleColumn}</li>
   *   <li>FLOAT - {@link FloatColumn}</li>
   *   <li>INTEGER - {@link IntColumn}</li>
   *   <li>LONG - {@link LongColumn}</li>
   *   <li>SHORT - {@link ShortColumn}</li>
   * </ul>
   *
   * @param type the column type
   * @param name the column name
   * @param values the values to populate the column
   * @param <T> the type parameter
   * @return a column of the specified type, or null if type is not supported
   */
  @SuppressWarnings('unchecked')
  static <T> Column<T> createColumn(T type, String name, List<?> values) {
    if (type == ColumnType.STRING) {
      var col = StringColumn.create(name)
      for (Object val : values) {
        col.append((String) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.BOOLEAN) {
      var col = BooleanColumn.create(name)
      for (Object val : values) {
        col.append((Boolean) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LOCAL_DATE) {
      var col = DateColumn.create(name)
      for (Object val : values) {
        col.append((LocalDate) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LOCAL_DATE_TIME) {
      var col = DateTimeColumn.create(name)
      for (Object val : values) {
        col.append((LocalDateTime) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.INSTANT) {
      var col = InstantColumn.create(name)
      for (Object val : values) {
        col.append((Instant) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LOCAL_TIME) {
      var col = TimeColumn.create(name)
      for (Object val : values) {
        col.append((LocalTime) val)
      }
      return (Column<T>) col
    }
    if (type == BigDecimalColumnType.instance()) {
      var col = BigDecimalColumn.create(name)
      for (Object val : values) {
        col.append((BigDecimal) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.DOUBLE) {
      var col = DoubleColumn.create(name)
      for (Object val : values) {
        col.append((Double) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.FLOAT) {
      var col = FloatColumn.create(name)
      for (Object val : values) {
        col.append((Float) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.INTEGER) {
      var col = IntColumn.create(name)
      for (Object val : values) {
        col.append((Integer) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.LONG) {
      var col = LongColumn.create(name)
      for (Object val : values) {
        col.append((Long) val)
      }
      return (Column<T>) col
    }
    if (type == ColumnType.SHORT) {
      var col = ShortColumn.create(name)
      for (Object val : values) {
        col.append((Short) val)
      }
      return (Column<T>) col
    }

    return null
  }

  /**
   * Get the Tablesaw {@link ColumnType} for a given Java class.
   *
   * <p>Maps common Java types to their corresponding Tablesaw column types.
   *
   * @param columnType the Java class
   * @return the corresponding ColumnType, or {@link ColumnType#SKIP} if not recognized
   */
  static ColumnType columnTypeForClass(Class<?> columnType) {
    if (columnType == String) {
      return ColumnType.STRING
    } else if (columnType == Boolean) {
      return ColumnType.BOOLEAN
    } else if (columnType == LocalDate) {
      return ColumnType.LOCAL_DATE
    } else if (columnType == LocalDateTime) {
      return ColumnType.LOCAL_DATE_TIME
    } else if (columnType == Instant) {
      return ColumnType.INSTANT
    } else if (columnType == LocalTime) {
      return ColumnType.LOCAL_TIME
    } else if (columnType == BigDecimal) {
      return BigDecimalColumnType.instance()
    } else if (columnType == Double) {
      return ColumnType.DOUBLE
    } else if (columnType == Float) {
      return ColumnType.FLOAT
    } else if (columnType == Integer) {
      return ColumnType.INTEGER
    } else if (columnType == Long) {
      return ColumnType.LONG
    } else if (columnType == Short) {
      return ColumnType.SHORT
    }
    return ColumnType.SKIP
  }

  /**
   * Get the Java class for a given Tablesaw {@link ColumnType}.
   *
   * <p>Maps Tablesaw column types to their corresponding Java classes.
   *
   * @param type the Tablesaw ColumnType
   * @return the corresponding Java class, or {@link Object} for custom/unknown types
   */
  static Class<?> classForColumnType(ColumnType type) {
    if (type == ColumnType.STRING) {
      return String
    } else if (type == ColumnType.BOOLEAN) {
      return Boolean
    } else if (type == ColumnType.LOCAL_DATE) {
      return LocalDate
    } else if (type == ColumnType.LOCAL_DATE_TIME) {
      return LocalDateTime
    } else if (type == ColumnType.INSTANT) {
      return Instant
    } else if (type == ColumnType.LOCAL_TIME) {
      return LocalTime
    } else if (type == BigDecimalColumnType.instance()) {
      return BigDecimal
    } else if (type == ColumnType.DOUBLE) {
      return Double
    } else if (type == ColumnType.FLOAT) {
      return Float
    } else if (type == ColumnType.INTEGER) {
      return Integer
    } else if (type == ColumnType.LONG) {
      return Long
    } else if (type == ColumnType.SHORT) {
      return Short
    }
    // it is some custom column type made outside the "official" tablesaw api
    return Object
  }

}
