package se.alipsa.matrix.sql

import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix

import java.sql.ResultSetMetaData
import java.sql.SQLException

/**
 * Provides {@link ResultSetMetaData} backed by a {@link Matrix}, exposing column names,
 * types, and display sizes derived from the matrix structure.
 *
 * <p>Precision and scale are computed lazily per column and cached. For decimal columns,
 * both metrics are computed together so they describe a compatible numeric shape. Values
 * updated after either metric is first requested are not reflected in either metric.</p>
 */
class MatrixResultSetMetaData implements ResultSetMetaData {

  private static final int NOT_CALCULATED = -1

  Matrix matrix
  SqlTypeMapper sqlTypeMapper = SqlTypeMapper.create(DataBaseProvider.UNKNOWN)
  private final int[] precisionByColumn
  private final int[] scaleByColumn

  MatrixResultSetMetaData(Matrix matrix) {
    this.matrix = matrix
    precisionByColumn = new int[matrix.columnCount()]
    scaleByColumn = new int[matrix.columnCount()]
    Arrays.fill(precisionByColumn, NOT_CALCULATED)
    Arrays.fill(scaleByColumn, NOT_CALCULATED)
  }

  private int checkedColumnIndex(int column) throws SQLException {
    if (column < 1 || column > matrix.columnCount()) {
      throw new SQLException("Column index out of range: $column")
    }
    column - 1
  }

  private int calculatePrecision(int columnIndex) {
    Class type = matrix.type(columnIndex)
    if (type == BigDecimal) {
      calculateDecimalMetrics(columnIndex)
      return precisionByColumn[columnIndex]
    }
    if (type == BigInteger) {
      int precision = 0
      matrix.column(columnIndex).each { Object value ->
        if (value instanceof Number) {
          int valuePrecision = new BigDecimal(value.toString()).precision()
          precision = valuePrecision > precision ? valuePrecision : precision
        }
      }
      return precision
    }
    if (CharSequence.isAssignableFrom(type)) {
      int precision = 0
      matrix.column(columnIndex).each { Object value ->
        int length = value == null ? 0 : value.toString().length()
        precision = length > precision ? length : precision
      }
      return precision
    }
    switch (type) {
      case Byte, Byte.TYPE -> 3
      case Short, Short.TYPE -> 5
      case Integer, Integer.TYPE -> 10
      case Long, Long.TYPE -> 19
      case Float, Float.TYPE -> 7
      case Double, Double.TYPE -> 15
      default -> 0
    }
  }

  private int calculateScale(int columnIndex) {
    if (matrix.type(columnIndex) != BigDecimal) {
      return 0
    }
    calculateDecimalMetrics(columnIndex)
    scaleByColumn[columnIndex]
  }

  private void calculateDecimalMetrics(int columnIndex) {
    int maxIntegerDigits = 0
    int maxScale = 0
    matrix.column(columnIndex).each { Object value ->
      if (value instanceof Number) {
        BigDecimal decimal = value instanceof BigDecimal
            ? value
            : new BigDecimal(value.toString())
        int integerDigits = Math.max(1, decimal.precision() - decimal.scale())
        maxIntegerDigits = Math.max(maxIntegerDigits, integerDigits)
        int valueScale = decimal.scale()
        maxScale = Math.max(maxScale, valueScale)
      }
    }
    precisionByColumn[columnIndex] = maxIntegerDigits + maxScale
    scaleByColumn[columnIndex] = maxScale
  }
  /**
   * Returns the number of columns in this {@code ResultSet} object.
   *
   * @return the number of columns
   * @throws SQLException if a database access error occurs
   */
  @Override
  int getColumnCount() throws SQLException {
    return matrix.columnCount()
  }

  /**
   * Indicates whether the designated column is automatically numbered.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isAutoIncrement(int column) throws SQLException {
    checkedColumnIndex(column)
    return false
  }

  /**
   * Indicates whether a column's case matters.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isCaseSensitive(int column) throws SQLException {
    checkedColumnIndex(column)
    return true
  }

  /**
   * Indicates whether the designated column can be used in a where clause.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isSearchable(int column) throws SQLException {
    checkedColumnIndex(column)
    return true
  }

  /**
   * Indicates whether the designated column is a cash value.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isCurrency(int column) throws SQLException {
    checkedColumnIndex(column)
    return false
  }

  /**
   * Indicates the nullability of values in the designated column.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the nullability status of the given column; one of {@code columnNoNulls},
   * {@code columnNullable} or {@code columnNullableUnknown}
   * @throws SQLException if a database access error occurs
   */
  @Override
  int isNullable(int column) throws SQLException {
    checkedColumnIndex(column)
    return columnNullable
  }

  /**
   * Indicates whether values in the designated column are signed numbers.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isSigned(int column) throws SQLException {
    return Number.isAssignableFrom(matrix.type(checkedColumnIndex(column)))
  }

  /**
   * Indicates the designated column's normal maximum width in characters.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the normal maximum number of characters allowed as the width
   *          of the designated column
   * @throws SQLException if a database access error occurs
   */
  @Override
  int getColumnDisplaySize(int column) throws SQLException {
    matrix.maxContentLength(matrix.columnName(checkedColumnIndex(column)), false)
  }

  /**
   * Gets the designated column's suggested title for use in printouts and
   * displays. The suggested title is usually specified by the SQL {@code AS}
   * clause.  If a SQL {@code AS} is not specified, the value returned from
   * {@code getColumnLabel} will be the same as the value returned by the
   * {@code getColumnName} method.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the suggested column title
   * @throws SQLException if a database access error occurs
   */
  @Override
  String getColumnLabel(int column) throws SQLException {
    return matrix.columnName(checkedColumnIndex(column))
  }

  /**
   * Get the designated column's name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return column name
   * @throws SQLException if a database access error occurs
   */
  @Override
  String getColumnName(int column) throws SQLException {
    return matrix.columnName(checkedColumnIndex(column))
  }

  /**
   * Get the designated column's table's schema.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return schema name or "" if not applicable
   * @throws SQLException if a database access error occurs
   */
  @Override
  String getSchemaName(int column) throws SQLException {
    checkedColumnIndex(column)
    return ''
  }

  /**
   * Get the designated column's specified column size.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. 0 is returned for data types where the
   * column size is not applicable.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return precision
   * @throws SQLException if a database access error occurs
   */
  @Override
  int getPrecision(int column) throws SQLException {
    int columnIndex = checkedColumnIndex(column)
    if (precisionByColumn[columnIndex] < 0) {
      precisionByColumn[columnIndex] = calculatePrecision(columnIndex)
    }
    precisionByColumn[columnIndex]
  }

  /**
   * Gets the designated column's number of digits to right of the decimal point.
   * 0 is returned for data types where the scale is not applicable.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return scale
   * @throws SQLException if a database access error occurs
   */
  @Override
  int getScale(int column) throws SQLException {
    int columnIndex = checkedColumnIndex(column)
    if (scaleByColumn[columnIndex] < 0) {
      scaleByColumn[columnIndex] = calculateScale(columnIndex)
    }
    scaleByColumn[columnIndex]
  }

  /**
   * Gets the designated column's table name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return table name or "" if not applicable
   * @throws SQLException if a database access error occurs
   */
  @Override
  String getTableName(int column) throws SQLException {
    checkedColumnIndex(column)
    return matrix.matrixName
  }

  /**
   * Gets the designated column's table's catalog name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the name of the catalog for the table in which the given column
   *          appears or "" if not applicable
   * @throws SQLException if a database access error occurs
   */
  @Override
  String getCatalogName(int column) throws SQLException {
    checkedColumnIndex(column)
    return ''
  }

  /**
   * Retrieves the designated column's SQL type.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return SQL type from java.sql.Types
   * @throws SQLException if a database access error occurs
   * @see java.sql.Types
   */
  @Override
  int getColumnType(int column) throws SQLException {
    sqlTypeMapper.jdbcType(matrix.type(checkedColumnIndex(column)))
  }

  /**
   * Retrieves the designated column's database-specific type name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return type name used by the database. If the column type is
   * a user-defined type, then a fully-qualified type name is returned.
   * @throws SQLException if a database access error occurs
   */
  @Override
  String getColumnTypeName(int column) throws SQLException {
    sqlTypeMapper.sqlType(matrix.type(checkedColumnIndex(column)))
  }

  /**
   * Indicates whether the designated column is definitely not writable.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isReadOnly(int column) throws SQLException {
    checkedColumnIndex(column)
    return false
  }

  /**
   * Indicates whether it is possible for a write on the designated column to succeed.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isWritable(int column) throws SQLException {
    checkedColumnIndex(column)
    return true
  }

  /**
   * Indicates whether a write on the designated column will definitely succeed.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return {@code true} if so; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   */
  @Override
  boolean isDefinitelyWritable(int column) throws SQLException {
    checkedColumnIndex(column)
    return true
  }

  /**
   * <p>Returns the fully-qualified name of the Java class whose instances
   * are manufactured if the method {@code ResultSet.getObject}
   * is called to retrieve a value
   * from the column.  {@code ResultSet.getObject} may return a subclass of the
   * class returned by this method.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the fully-qualified name of the class in the Java programming
   *         language that would be used by the method
   * {@code ResultSet.getObject} to retrieve the value in the specified
   * column. This is the class name used for custom mapping.
   * @throws SQLException if a database access error occurs
   * @since 1.2
   */
  @Override
  String getColumnClassName(int column) throws SQLException {
    return matrix.type(checkedColumnIndex(column)).getName()
  }

  /**
   * Returns an object that implements the given interface to allow access to
   * non-standard methods, or standard methods not exposed by the proxy.
   *
   * If the receiver implements the interface then the result is the receiver
   * or a proxy for the receiver. If the receiver is a wrapper
   * and the wrapped object implements the interface then the result is the
   * wrapped object or a proxy for the wrapped object. Otherwise return the
   * the result of calling {@code unwrap} recursively on the wrapped object
   * or a proxy for that result. If the receiver is not a
   * wrapper and does not implement the interface, then an {@code SQLException} is thrown.
   *
   * @param iface A Class defining an interface that the result must implement.
   * @return an object that implements the interface. May be a proxy for the actual implementing object.
   * @throws java.sql.SQLException If no object found that implements the interface
   * @since 1.6
   */
  @Override
  <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface?.isInstance(this)) {
      return this as T
    }
    if (iface == Matrix) {
      return matrix as T
    }
    if (iface == List) {
      return matrix.rows() as T
    }
    throw new SQLException("No wrapper for ${iface?.name}")
  }

  /**
   * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
   * for an object that does. Returns false otherwise. If this implements the interface then return true,
   * else if this is a wrapper then return the result of recursively calling {@code isWrapperFor} on the wrapped
   * object. If this does not implement the interface and is not a wrapper, return false.
   * This method should be implemented as a low-cost operation compared to {@code unwrap} so that
   * callers can use this method to avoid expensive {@code unwrap} calls that may fail. If this method
   * returns true then calling {@code unwrap} with the same argument should succeed.
   *
   * @param iface a Class defining an interface.
   * @return true if this implements the interface or directly or indirectly wraps an object that does.
   * @throws java.sql.SQLException  if an error occurs while determining whether this is a wrapper
   * for an object with the given interface.
   * @since 1.6
   */
  @Override
  boolean isWrapperFor(Class<?> iface) throws SQLException {
    iface != null && (iface.isInstance(this) || iface == Matrix || iface == List)
  }

}
