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

  private static final String COL_VALUE = 'Value'
  private static final String COL_FREQUENCY = 'Frequency'
  private static final String COL_PERCENT = 'Percent'
  private static final String MISSING_VALUE = '<missing>'
  private static final String MISSING_VALUE_ERROR = "The value '${MISSING_VALUE}' is reserved for missing values"
  private static final String NUM_DECIMALS_ERROR = 'numDecimals cannot be a negative number: was '

  /**
   * Generate a frequency table for the given column.
   *
   * <p>Creates a table with three columns:
   * <ul>
   *   <li>Value: distinct values from the column, with missing values grouped as {@code <missing>}</li>
   *   <li>Frequency: count of occurrences for each value</li>
   *   <li>Percent: percentage of total (rounded to 2 decimals)</li>
   * </ul>
   *
   * <p>The resulting table is sorted by frequency in descending order.
   *
   * @param column the column to analyze
   * @return a frequency table sorted by descending frequency
   * @throws IllegalArgumentException if a non-missing value would collide with the missing-value marker
   */
  static Table frequency(Column<?> column) {
    Map<Object, AtomicInteger> freq = [:]
    for (int i = 0; i < column.size(); i++) {
      boolean missing = column.isMissing(i)
      Object value = missing ? MISSING_VALUE : column.get(i)
      if (!missing && String.valueOf(value) == MISSING_VALUE) {
        throw new IllegalArgumentException("${MISSING_VALUE_ERROR}: column '${column.name()}', row ${i}")
      }
      def counter = freq.computeIfAbsent(value) { k -> new AtomicInteger() }
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
    table.sortDescendingOn(COL_FREQUENCY)
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
    frequency(table.column(columnName))
  }

  /**
   * Round a double value to the specified number of decimal places.
   *
   * <p>Uses {@link RoundingMode#HALF_EVEN} for rounding.
   *
   * @param value the value to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return the rounded value
   * @throws IllegalArgumentException if numDecimals is negative
   */
  static double round(double value, int numDecimals) {
    if (numDecimals < 0) {
      throw new IllegalArgumentException(NUM_DECIMALS_ERROR + numDecimals)
    }

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_EVEN)
    bd.doubleValue()
  }

  /**
   * Round a float value to the specified number of decimal places.
   *
   * <p>Uses {@link RoundingMode#HALF_EVEN} for rounding.
   *
   * @param value the value to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return the rounded value
   * @throws IllegalArgumentException if numDecimals is negative
   */
  static float round(float value, int numDecimals) {
    if (numDecimals < 0) {
      throw new IllegalArgumentException(NUM_DECIMALS_ERROR + numDecimals)
    }

    BigDecimal bd = BigDecimal.valueOf(value)
    bd = bd.setScale(numDecimals, RoundingMode.HALF_EVEN)
    bd.floatValue()
  }

  /**
   * Round all values in a column to the specified number of decimal places.
   *
   * <p>If the column is not a {@link NumberColumn}, it is returned unchanged.
   *
   * @param column the column to round
   * @param numDecimals the number of decimal places
   * @return a rounded copy, or the original column if it is not numeric
   */
  static Column<?> round(Column<?> column, int numDecimals) {
    if (column in NumberColumn) {
      return round(column as NumberColumn, numDecimals)
    }
    column
  }

  /**
   * Round all values in a numeric column to the specified number of decimal places.
   *
   * <p>Supported column types:
   * <ul>
   *   <li>{@link BigDecimalColumn} - uses setScale on a copy</li>
   *   <li>{@link DoubleColumn} - rounds each value</li>
   *   <li>{@link FloatColumn} - rounds each value</li>
   *   <li>Integer types (IntColumn, ShortColumn, LongColumn) - copied unchanged</li>
   * </ul>
   *
   * @param column the numeric column to round
   * @param numDecimals the number of decimal places (must be non-negative)
   * @return an independent copy with rounded values
   * @throws IllegalArgumentException if numDecimals is negative
   */
  static NumberColumn round(NumberColumn column, int numDecimals) {
    if (numDecimals < 0) {
      throw new IllegalArgumentException(NUM_DECIMALS_ERROR + numDecimals)
    }

    NumberColumn rounded = column.copy() as NumberColumn

    if (rounded in BigDecimalColumn) {
      return (rounded as BigDecimalColumn).setScale(numDecimals)
    }

    if (rounded in DoubleColumn) {
      def dc = rounded as DoubleColumn
      for (int i = 0; i < dc.size(); i++) {
        if (dc.isMissing(i)) {
          continue
        }
        double val = dc.getDouble(i)
        dc.set(i, round(val, numDecimals))
      }
    }

    if (rounded in FloatColumn) {
      def fc = rounded as FloatColumn
      for (int i = 0; i < fc.size(); i++) {
        if (fc.isMissing(i)) {
          continue
        }
        float val = fc.getFloat(i)
        fc.set(i, round(val, numDecimals))
      }
    }
    // everything else (IntColumn, ShortColumn, LongColumn cannot be rounded as they have no decimals
    rounded
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
    Matrix.builder(table.name())
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
    fromTablesaw(gtable)
  }

  /**
   * Convert a Matrix to a Tablesaw table.
   *
   * <p>Preserves the matrix name, column names, column types, and all data for supported
   * column types. Columns whose Java types map to {@link ColumnType#SKIP} (or that cannot
   * be created by {@link #createColumn(ColumnType, String, List)}) cause an
   * {@link IllegalArgumentException} to be thrown. Use {@link #toTablesaw(Matrix, boolean)}
   * with {@code skipUnsupported = true} to omit unsupported columns instead.
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
      columns.add(createColumn(type, matrix.columnNames().get(i), matrix.column(i)))
    }
    Table.create(matrix.getMatrixName(), columns)
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
   * @return a column of the specified type
   * @throws IllegalArgumentException if the type is unsupported or a non-null value has a type that
   *         is neither the expected type nor a losslessly widenable one
   */
  static Column<?> createColumn(ColumnType type, String name, List<?> values) {
    Class<?> expectedType = classForColumnType(type)
    if (type == null || type == ColumnType.SKIP || expectedType == Object) {
      throw new IllegalArgumentException("Unsupported column type for column '${name}': ${type}")
    }
    Column<?> column = type.create(name)
    values.eachWithIndex { Object value, int row ->
      column.appendObj(coerceValue(value, expectedType, name, row))
    }
    column
  }

  /**
   * Validates a value against the expected Java type, converting lossless widenings.
   *
   * <p>{@code null} is returned as-is (missing). Values already of the expected type are returned
   * unchanged. A {@link CharSequence} (including Groovy {@code GString}) is accepted for
   * {@link String} columns and converted with {@code toString()}. Numeric widenings are accepted
   * only when exact: {@code Integer}/{@code Long}/{@code Short}/{@code Byte}/{@code BigInteger}/
   * {@code Float}/{@code Double} to {@link BigDecimal} through
   * {@link BigDecimalColumn#toBigDecimal(Number)} (NaN becomes missing, infinities are rejected);
   * {@code Float} (always exact), {@code Integer}/{@code Short}/{@code Byte}, and exactly
   * representable {@code Long}/{@code BigInteger} to {@link Double}; {@code Short}/{@code Byte}
   * and exactly representable {@code Integer} to {@link Float}; {@code Integer}/{@code Short}/
   * {@code Byte} to {@link Long}; {@code Short}/{@code Byte} to {@link Integer}; and {@code Byte}
   * to {@link Short}. Values that would lose precision or overflow are rejected rather than
   * coerced.
   *
   * @param value the value to validate
   * @param expectedType the Java type required by the column type
   * @param name the column name (used in the error message)
   * @param row the zero-based row index (used in the error message)
   * @return the value, converted when a lossless widening applies
   * @throws IllegalArgumentException if the value cannot be represented exactly in the expected
   *         type, or is an infinity for a {@link BigDecimal} column; the message names the
   *         column, row index, expected type, and actual type
   */
  @SuppressWarnings('BigDecimalInstantiation')
  private static Object coerceValue(Object value, Class<?> expectedType, String name, int row) {
    if (value == null || expectedType.isInstance(value)) {
      return value
    }
    if (expectedType == String && value instanceof CharSequence) {
      return value.toString()
    }
    if (value instanceof Number) {
      Number num = (Number) value
      if (expectedType == BigDecimal) {
        try {
          return BigDecimalColumn.toBigDecimal(num)
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
              "Column '${name}' row ${row} expects ${expectedType.name} but got ${value.class.name}: ${e.message}",
              e)
        }
      }
      if (expectedType == Double) {
        if (num instanceof Float || num instanceof Integer || num instanceof Short || num instanceof Byte) {
          return num.doubleValue()
        }
        if (num instanceof Long) {
          double d = num.doubleValue()
          // exact binary comparison: (long) d saturates, so a narrow-domain check would lie
          if (new BigDecimal(d) == BigDecimal.valueOf((Long) num)) {
            return d
          }
        }
        if (num instanceof BigInteger) {
          double d = num.doubleValue()
          // new BigDecimal(d) is the exact binary value; BigDecimal.valueOf would be the
          // shortest round-trip decimal and reject exactly representable integers such as 2^80
          if (Double.isFinite(d) && new BigDecimal(d) == new BigDecimal((BigInteger) num)) {
            return d
          }
        }
      }
      if (expectedType == Float) {
        if (num instanceof Short || num instanceof Byte) {
          return num.floatValue()
        }
        if (num instanceof Integer) {
          float f = num.floatValue()
          // compare in the double domain: both conversions are exact, so int-domain saturation
          // cannot hide an inexact conversion
          if ((double) f == num.doubleValue()) {
            return f
          }
        }
      }
      if (expectedType == Long && (num instanceof Integer || num instanceof Short || num instanceof Byte)) {
        return num.longValue()
      }
      if (expectedType == Integer && (num instanceof Short || num instanceof Byte)) {
        return num.intValue()
      }
      if (expectedType == Short && num instanceof Byte) {
        return num.shortValue()
      }
    }
    throw new IllegalArgumentException(
        "Column '${name}' row ${row} expects ${expectedType.name} but got ${value.class.name}")
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
    ColumnType.SKIP
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
    Object
  }

}
