package se.alipsa.groovy.matrix.sql

import org.apache.commons.io.input.ReaderInputStream
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.groovy.matrix.Matrix

import java.nio.charset.StandardCharsets
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.RowId
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp

/**
 * This is Result set representation of a Matrix.
 * It is detached from the Matrix used to clone it so any update operation or similar will
 * not affect the underlying Matrix. To get the Matrix from this resultset you call unwrap. E.g:
 * <code><pre>
 * // update the first column in the first row:
 * rs.first().updateString(1, "Foo")
 * Matrix m = rs.unwrap(Matrix)
 * assert m[0,0] == "Foo"
 * </pre></code>
 */
class MatrixResultSet implements ResultSet{

  int rowIdx = -1
  Matrix matrix
  Object lastReadValue
  SqlTypeMapper sqlTypeMapper = SqlTypeMapper.create(DataBaseProvider.UNKNOWN)

  /**
   * Constructor to create a ResultSet from a Matrix.
   *
   * @param matrix the matrix containing the data, note that a cloned copy of it will be used
   */
  MatrixResultSet(Matrix matrix) {
    this.matrix = matrix.clone()
  }
  /**
   * Moves the cursor forward one row from its current position.
   * A {@code ResultSet} cursor is initially positioned
   * before the first row; the first call to the method
   * {@code next} makes the first row the current row; the
   * second call makes the second row the current row, and so on.
   * <p>
   * When a call to the {@code next} method returns {@code false},
   * the cursor is positioned after the last row. Any
   * invocation of a {@code ResultSet} method which requires a
   * current row will result in a {@code SQLException} being thrown.
   *  If the result set type is {@code TYPE_FORWARD_ONLY}, it is vendor specified
   * whether their JDBC driver implementation will return {@code false} or
   *  throw an {@code SQLException} on a
   * subsequent call to {@code next}.
   *
   * <P>If an input stream is open for the current row, a call
   * to the method {@code next} will
   * implicitly close it. A {@code ResultSet} object's
   * warning chain is cleared when a new row is read.
   *
   * @return {@code true} if the new current row is valid;
   * {@code false} if there are no more rows
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   */
  @Override
  boolean next() throws SQLException {
    rowIdx++
    if (rowIdx >= matrix.rowCount()) {
      return false
    }
    return true
  }

  /**
   * Releases this {@code ResultSet} object's database and
   * JDBC resources immediately instead of waiting for
   * this to happen when it is automatically closed.
   *
   * <P>The closing of a {@code ResultSet} object does <strong>not</strong> close the {@code Blob},
   * {@code Clob} or {@code NClob} objects created by the {@code ResultSet}. {@code Blob},
   * {@code Clob} or {@code NClob} objects remain valid for at least the duration of the
   * transaction in which they are created, unless their {@code free} method is invoked.
   * <p>
   * When a {@code ResultSet} is closed, any {@code ResultSetMetaData}
   * instances that were created by calling the  {@code getMetaData}
   * method remain accessible.
   *
   * <P><B>Note:</B> A {@code ResultSet} object
   * is automatically closed by the
   * {@code Statement} object that generated it when
   * that {@code Statement} object is closed,
   * re-executed, or is used to retrieve the next result from a
   * sequence of multiple results.
   * <p>
   * Calling the method {@code close} on a {@code ResultSet}
   * object that is already closed is a no-op.
   *
   *
   * @throws SQLException if a database access error occurs
   */
  @Override
  void close() throws SQLException {
    matrix = null
  }

  /**
   * Reports whether
   * the last column read had a value of SQL {@code NULL}.
   * Note that you must first call one of the getter methods
   * on a column to try to read its value and then call
   * the method {@code wasNull} to see if the value read was
   * SQL {@code NULL}.
   *
   * @return {@code true} if the last column value read was SQL
   * {@code NULL} and {@code false} otherwise
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   */
  @Override
  boolean wasNull() throws SQLException {
    return lastReadValue == null
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code String} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  String getString(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex -1, String]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code boolean} in the Java programming language.
   *
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 0, a value of {@code false} is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 1, a value of {@code true} is returned.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code false}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  boolean getBoolean(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Boolean]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code byte} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  byte getByte(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Byte]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code short} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  short getShort(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Short]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * an {@code int} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  int getInt(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Integer]
    lastReadValue ?: 0
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code long} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  long getLong(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Long]
    lastReadValue ?: 0
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code float} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  float getFloat(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Float]
    lastReadValue ?: 0
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code double} in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  double getDouble(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Double]
    lastReadValue ?: 0
  }

  /** @deprecated */
  @Override
  BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, BigDecimal]
    (lastReadValue == null) ? null : lastReadValue.setScale(scale)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code byte} array in the Java programming language.
   * The bytes represent the raw values returned by the driver.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  byte[] getBytes(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, byte[]]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code java.sql.Date} object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Date getDate(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Date]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code java.sql.Time} object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Time getTime(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Time]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code java.sql.Timestamp} object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Timestamp getTimestamp(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1, Timestamp]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a stream of ASCII characters. The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large {@code LONGVARCHAR} values.
   * The JDBC driver will
   * do any necessary conversion from the database format into ASCII.
   *
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream.  Also, a
   * stream may return {@code 0} when the method
   * {@code InputStream.available}
   * is called whether there is data available or not.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a Java input stream that delivers the database column value
   * as a stream of one-byte ASCII characters;
   * if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  InputStream getAsciiStream(int columnIndex) throws SQLException {
    String val = matrix[rowIdx, columnIndex-1, String]
    lastReadValue = ReaderInputStream.builder()
        .setCharset(StandardCharsets.UTF_8)
        .setReader(new CharArrayReader(val.toCharArray()))
        .get()
  }

  /** @deprecated */
  @Override
  InputStream getUnicodeStream(int columnIndex) throws SQLException {
    String val = matrix[rowIdx, columnIndex-1, String]
    lastReadValue = new ByteArrayInputStream(val.getBytes(StandardCharsets.UTF_8))
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a  stream of
   * uninterpreted bytes. The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large {@code LONGVARBINARY} values.
   *
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream.  Also, a
   * stream may return {@code 0} when the method
   * {@code InputStream.available}
   * is called whether there is data available or not.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a Java input stream that delivers the database column value
   *         as a stream of uninterpreted bytes;
   *         if the value is SQL {@code NULL}, the value returned is
   * {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  InputStream getBinaryStream(int columnIndex) throws SQLException {
    byte[] val = matrix[rowIdx, columnIndex-1, byte[]]
    lastReadValue = val == null ? null : new ByteArrayInputStream(val)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code String} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  String getString(String columnLabel) throws SQLException {
    getString(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code boolean} in the Java programming language.
   *
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 0, a value of {@code false} is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 1, a value of {@code true} is returned.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code false}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  boolean getBoolean(String columnLabel) throws SQLException {
    getBoolean(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code byte} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  byte getByte(String columnLabel) throws SQLException {
    getByte(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code short} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  short getShort(String columnLabel) throws SQLException {
    getShort(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * an {@code int} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  int getInt(String columnLabel) throws SQLException {
    getInt(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code long} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  long getLong(String columnLabel) throws SQLException {
    getLong(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code float} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  float getFloat(String columnLabel) throws SQLException {
    getFloat(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code double} in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code 0}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  double getDouble(String columnLabel) throws SQLException {
   getDouble(matrix.columnIndex(columnLabel)+1)
  }

  /** @deprecated */
  @Override
  BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
    getBigDecimal(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code byte} array in the Java programming language.
   * The bytes represent the raw values returned by the driver.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  byte[] getBytes(String columnLabel) throws SQLException {
    getBytes(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code java.sql.Date} object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Date getDate(String columnLabel) throws SQLException {
    getDate(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code java.sql.Time} object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Time getTime(String columnLabel) throws SQLException {
   getTime(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code java.sql.Timestamp} object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Timestamp getTimestamp(String columnLabel) throws SQLException {
    getTimestamp(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a stream of
   * ASCII characters. The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large {@code LONGVARCHAR} values.
   * The JDBC driver will
   * do any necessary conversion from the database format into ASCII.
   *
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream. Also, a
   * stream may return {@code 0} when the method {@code available}
   * is called whether there is data available or not.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a Java input stream that delivers the database column value
   * as a stream of one-byte ASCII characters.
   * If the value is SQL {@code NULL},
   * the value returned is {@code null}.
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  InputStream getAsciiStream(String columnLabel) throws SQLException {
    getAsciiStream(matrix.columnIndex(columnLabel)+1)
  }

  /** @deprecated */
  @Override
  InputStream getUnicodeStream(String columnLabel) throws SQLException {
    getUnicodeStream(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a stream of uninterpreted
   * {@code byte}s.
   * The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large {@code LONGVARBINARY}
   * values.
   *
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream. Also, a
   * stream may return {@code 0} when the method {@code available}
   * is called whether there is data available or not.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a Java input stream that delivers the database column value
   * as a stream of uninterpreted bytes;
   * if the value is SQL {@code NULL}, the result is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  InputStream getBinaryStream(String columnLabel) throws SQLException {
    getBinaryStream(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the first warning reported by calls on this
   * {@code ResultSet} object.
   * Subsequent warnings on this {@code ResultSet} object
   * will be chained to the {@code SQLWarning} object that
   * this method returns.
   *
   * <P>The warning chain is automatically cleared each time a new
   * row is read.  This method may not be called on a {@code ResultSet}
   * object that has been closed; doing so will cause an
   * {@code SQLException} to be thrown.
   * <P>
   * <B>Note:</B> This warning chain only covers warnings caused
   * by {@code ResultSet} methods.  Any warning caused by
   * {@code Statement} methods
   * (such as reading OUT parameters) will be chained on the
   * {@code Statement} object.
   *
   * @return the first {@code SQLWarning} object reported or
   * {@code null} if there are none
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   */
  @Override
  SQLWarning getWarnings() throws SQLException {
    if (matrix == null) {
      throw new SQLException('Result set is closed')
    }
    return null
  }

  /**
   * Clears all warnings reported on this {@code ResultSet} object.
   * After this method is called, the method {@code getWarnings}
   * returns {@code null} until a new warning is
   * reported for this {@code ResultSet} object.
   *
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   */
  @Override
  void clearWarnings() throws SQLException {

  }

  /**
   * Retrieves the name of the SQL cursor used by this {@code ResultSet}
   * object.
   *
   * <P>In SQL, a result table is retrieved through a cursor that is
   * named. The current row of a result set can be updated or deleted
   * using a positioned update/delete statement that references the
   * cursor name. To insure that the cursor has the proper isolation
   * level to support update, the cursor's {@code SELECT} statement
   * should be of the form {@code SELECT FOR UPDATE}. If
   * {@code FOR UPDATE} is omitted, the positioned updates may fail.
   *
   * <P>The JDBC API supports this SQL feature by providing the name of the
   * SQL cursor used by a {@code ResultSet} object.
   * The current row of a {@code ResultSet} object
   * is also the current row of this SQL cursor.
   *
   * @return the SQL name for this {@code ResultSet} object's cursor
   * @throws SQLException if a database access error occurs or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   */
  @Override
  String getCursorName() throws SQLException {
    throw new SQLFeatureNotSupportedException('Cursor name has no meaning in a MatrixResultSet')
  }

  /**
   * Retrieves the  number, types and properties of
   * this {@code ResultSet} object's columns.
   *
   * @return the description of this {@code ResultSet} object's columns
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   */
  @Override
  ResultSetMetaData getMetaData() throws SQLException {
    if (matrix == null) {
      throw new SQLException("Result set is closed")
    }
    return new MatrixResultSetMetaData(matrix)
  }

  /**
   * <p>Gets the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * an {@code Object} in the Java programming language.
   *
   * <p>This method will return the value of the given column as a
   * Java object.  The type of the Java object will be the default
   * Java object type corresponding to the column's SQL type,
   * following the mapping for built-in types specified in the JDBC
   * specification. If the value is an SQL {@code NULL},
   * the driver returns a Java {@code null}.
   *
   * <p>This method may also be used to read database-specific
   * abstract data types.
   *
   * In the JDBC 2.0 API, the behavior of method
   * {@code getObject} is extended to materialize
   * data of SQL user-defined types.
   * <p>
   * If {@code Connection.getTypeMap} does not throw a
   * {@code SQLFeatureNotSupportedException},
   * then when a column contains a structured or distinct value,
   * the behavior of this method is as
   * if it were a call to: {@code getObject(columnIndex,
   * this.getStatement().getConnection().getTypeMap())}.
   *
   * If {@code Connection.getTypeMap} does throw a
   * {@code SQLFeatureNotSupportedException},
   * then structured values are not supported, and distinct values
   * are mapped to the default Java class as determined by the
   * underlying SQL type of the DISTINCT type.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a {@code java.lang.Object} holding the column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Object getObject(int columnIndex) throws SQLException {
    lastReadValue = matrix[rowIdx, columnIndex-1]
  }

  /**
   * <p>Gets the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * an {@code Object} in the Java programming language.
   *
   * <p>This method will return the value of the given column as a
   * Java object.  The type of the Java object will be the default
   * Java object type corresponding to the column's SQL type,
   * following the mapping for built-in types specified in the JDBC
   * specification. If the value is an SQL {@code NULL},
   * the driver returns a Java {@code null}.
   * <P>
   * This method may also be used to read database-specific
   * abstract data types.
   * <P>
   * In the JDBC 2.0 API, the behavior of the method
   * {@code getObject} is extended to materialize
   * data of SQL user-defined types.  When a column contains
   * a structured or distinct value, the behavior of this method is as
   * if it were a call to: {@code getObject(columnIndex,
   * this.getStatement().getConnection().getTypeMap())}.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code java.lang.Object} holding the column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   */
  @Override
  Object getObject(String columnLabel) throws SQLException {
    lastReadValue = matrix[rowIdx, columnLabel]
  }

  /**
   * Maps the given {@code ResultSet} column label to its
   * {@code ResultSet} column index.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column index of the given column name
   * @throws SQLException if the {@code ResultSet} object
   * does not contain a column labeled {@code columnLabel}, a database access error occurs
   *  or this method is called on a closed result set
   */
  @Override
  int findColumn(String columnLabel) throws SQLException {
    return matrix.columnIndex(columnLabel) + 1
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a
   * {@code java.io.Reader} object.
   * @return a {@code java.io.Reader} object that contains the column
   * value; if the value is SQL {@code NULL}, the value returned is
   * {@code null} in the Java programming language.
   * @param columnIndex the first column is 1, the second is 2, ...
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   * @since 1.2
   */
  @Override
  Reader getCharacterStream(int columnIndex) throws SQLException {
    String val = matrix[rowIdx, columnIndex-1, String]
    new CharArrayReader(val.toCharArray())
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a
   * {@code java.io.Reader} object.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code java.io.Reader} object that contains the column
   * value; if the value is SQL {@code NULL}, the value returned is
   * {@code null} in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   * @since 1.2
   */
  @Override
  Reader getCharacterStream(String columnLabel) throws SQLException {
    getCharacterStream(matrix.columnIndex(columnLabel)+1)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a
   * {@code java.math.BigDecimal} with full precision.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value (full precision);
   * if the value is SQL {@code NULL}, the value returned is
   * {@code null} in the Java programming language.
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   * @since 1.2
   */
  @Override
  BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    matrix[rowIdx, columnIndex-1, BigDecimal]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a
   * {@code java.math.BigDecimal} with full precision.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value (full precision);
   * if the value is SQL {@code NULL}, the value returned is
   * {@code null} in the Java programming language.
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs or this method is
   *            called on a closed result set
   * @since 1.2
   *
   */
  @Override
  BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    matrix[rowIdx, columnLabel, BigDecimal]
  }

  /**
   * Retrieves whether the cursor is before the first row in
   * this {@code ResultSet} object.
   * <p>
   * <strong>Note:</strong>Support for the {@code isBeforeFirst} method
   * is optional for {@code ResultSet}s with a result
   * set type of {@code TYPE_FORWARD_ONLY}
   *
   * @return {@code true} if the cursor is before the first row;
   * {@code false} if the cursor is at any other position or the
   * result set contains no rows
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean isBeforeFirst() throws SQLException {
    return rowIdx < 0
  }

  /**
   * Retrieves whether the cursor is after the last row in
   * this {@code ResultSet} object.
   * <p>
   * <strong>Note:</strong>Support for the {@code isAfterLast} method
   * is optional for {@code ResultSet}s with a result
   * set type of {@code TYPE_FORWARD_ONLY}
   *
   * @return {@code true} if the cursor is after the last row;
   * {@code false} if the cursor is at any other position or the
   * result set contains no rows
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean isAfterLast() throws SQLException {
    return rowIdx > matrix.lastRowIndex()
  }

  /**
   * Retrieves whether the cursor is on the first row of
   * this {@code ResultSet} object.
   * <p>
   * <strong>Note:</strong>Support for the {@code isFirst} method
   * is optional for {@code ResultSet}s with a result
   * set type of {@code TYPE_FORWARD_ONLY}
   *
   * @return {@code true} if the cursor is on the first row;
   * {@code false} otherwise
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean isFirst() throws SQLException {
    return rowIdx == 0
  }

  /**
   * Retrieves whether the cursor is on the last row of
   * this {@code ResultSet} object.
   *  <strong>Note:</strong> Calling the method {@code isLast} may be expensive
   * because the JDBC driver
   * might need to fetch ahead one row in order to determine
   * whether the current row is the last row in the result set.
   * <p>
   * <strong>Note:</strong> Support for the {@code isLast} method
   * is optional for {@code ResultSet}s with a result
   * set type of {@code TYPE_FORWARD_ONLY}
   * @return {@code true} if the cursor is on the last row;
   * {@code false} otherwise
   * @throws SQLException if a database access error occurs or this method is
   *         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean isLast() throws SQLException {
    return rowIdx == matrix.lastRowIndex()
  }

  /**
   * Moves the cursor to the front of
   * this {@code ResultSet} object, just before the
   * first row. This method has no effect if the result set contains no rows.
   *
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set or the
   * result set type is {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void beforeFirst() throws SQLException {
    rowIdx = -1
  }

  /**
   * Moves the cursor to the end of
   * this {@code ResultSet} object, just after the
   * last row. This method has no effect if the result set contains no rows.
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set
   * or the result set type is {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void afterLast() throws SQLException {
    rowIdx = matrix.rowCount()
  }

  /**
   * Moves the cursor to the first row in
   * this {@code ResultSet} object.
   *
   * @return {@code true} if the cursor is on a valid row;
   * {@code false} if there are no rows in the result set
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set
   * or the result set type is {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean first() throws SQLException {
    rowIdx = 0
    if (matrix.rowCount() == 0) {
      return false
    }
    return true
  }

  /**
   * Moves the cursor to the last row in
   * this {@code ResultSet} object.
   *
   * @return {@code true} if the cursor is on a valid row;
   * {@code false} if there are no rows in the result set
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set
   * or the result set type is {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean last() throws SQLException {
    rowIdx = matrix.lastRowIndex()
    if (matrix.rowCount() == 0) {
      return false
    }
    return true
  }

  /**
   * Retrieves the current row number.  The first row is number 1, the
   * second number 2, and so on.
   * <p>
   * <strong>Note:</strong>Support for the {@code getRow} method
   * is optional for {@code ResultSet}s with a result
   * set type of {@code TYPE_FORWARD_ONLY}
   *
   * @return the current row number; {@code 0} if there is no current row
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  int getRow() throws SQLException {
    return rowIdx + 1
  }

  /**
   * Moves the cursor to the given row number in
   * this {@code ResultSet} object.
   *
   * <p>If the row number is positive, the cursor moves to
   * the given row number with respect to the
   * beginning of the result set.  The first row is row 1, the second
   * is row 2, and so on.
   *
   * <p>If the given row number is negative, the cursor moves to
   * an absolute row position with respect to
   * the end of the result set.  For example, calling the method
   * {@code absolute(-1)} positions the
   * cursor on the last row; calling the method {@code absolute(-2)}
   * moves the cursor to the next-to-last row, and so on.
   *
   * <p>If the row number specified is zero, the cursor is moved to
   * before the first row.
   *
   * <p>An attempt to position the cursor beyond the first/last row in
   * the result set leaves the cursor before the first row or after
   * the last row.
   *
   * <p><B>Note:</B> Calling {@code absolute(1)} is the same
   * as calling {@code first()}. Calling {@code absolute(-1)}
   * is the same as calling {@code last()}.
   *
   * @param row the number of the row to which the cursor should move.
   *        A value of zero indicates that the cursor will be positioned
   *        before the first row; a positive number indicates the row number
   *        counting from the beginning of the result set; a negative number
   *        indicates the row number counting from the end of the result set
   * @return {@code true} if the cursor is moved to a position in this
   * {@code ResultSet} object;
   * {@code false} if the cursor is before the first row or after the
   * last row
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set
   * or the result set type is {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean absolute(int row) throws SQLException {
    if (row < 0) {
      if (row < -matrix.rowCount()) {
        return false
      }
      rowIdx = matrix.rowCount() - row
    } else {
      if (row > matrix.rowCount()) {
        return false
      }
      rowIdx = row - 1
    }
    return true
  }

  /**
   * Moves the cursor a relative number of rows, either positive or negative.
   * Attempting to move beyond the first/last row in the
   * result set positions the cursor before/after the
   * the first/last row. Calling {@code relative(0)} is valid, but does
   * not change the cursor position.
   *
   * <p>Note: Calling the method {@code relative(1)}
   * is identical to calling the method {@code next()} and
   * calling the method {@code relative(-1)} is identical
   * to calling the method {@code previous()}.
   *
   * @param rows an {@code int} specifying the number of rows to
   *        move from the current row; a positive number moves the cursor
   *        forward; a negative number moves the cursor backward
   * @return {@code true} if the cursor is on a row;
   * {@code false} otherwise
   * @throws SQLException if a database access error occurs;  this method
   * is called on a closed result set or the result set type is
   * {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean relative(int rows) throws SQLException {
    rowIdx = rowIdx + rows
    if (rowIdx < 0) rowIdx = -1
    if (rowIdx > matrix.lastRowIndex()) rowIdx = matrix.columnCount()
    return true
  }

  /**
   * Moves the cursor to the previous row in this
   * {@code ResultSet} object.
   * <p>
   * When a call to the {@code previous} method returns {@code false},
   * the cursor is positioned before the first row.  Any invocation of a
   * {@code ResultSet} method which requires a current row will result in a
   * {@code SQLException} being thrown.
   * <p>
   * If an input stream is open for the current row, a call to the method
   * {@code previous} will implicitly close it.  A {@code ResultSet}
   *  object's warning change is cleared when a new row is read.
   *
   * @return {@code true} if the cursor is now positioned on a valid row;
   * {@code false} if the cursor is positioned before the first row
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set
   * or the result set type is {@code TYPE_FORWARD_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  boolean previous() throws SQLException {
    return relative(-1)
  }

  /**
   * Gives a hint as to the direction in which the rows in this
   * {@code ResultSet} object will be processed.
   * The initial value is determined by the
   * {@code Statement} object
   * that produced this {@code ResultSet} object.
   * The fetch direction may be changed at any time.
   *
   * @param direction an {@code int} specifying the suggested
   *        fetch direction; one of {@code ResultSet.FETCH_FORWARD},
   * {@code ResultSet.FETCH_REVERSE}, or
   * {@code ResultSet.FETCH_UNKNOWN}
   * @throws SQLException if a database access error occurs; this
   * method is called on a closed result set or
   * the result set type is {@code TYPE_FORWARD_ONLY} and the fetch
   * direction is not {@code FETCH_FORWARD}
   * @since 1.2
   * @see Statement#setFetchDirection
   * @see #getFetchDirection
   */
  @Override
  void setFetchDirection(int direction) throws SQLException {
    throw new SQLFeatureNotSupportedException("This result set moves only forward")
  }

  /**
   * Retrieves the fetch direction for this
   * {@code ResultSet} object.
   *
   * @return the current fetch direction for this {@code ResultSet} object
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   * @see #setFetchDirection
   */
  @Override
  int getFetchDirection() throws SQLException {
    ResultSet.FETCH_FORWARD
  }

  /**
   * Gives the JDBC driver a hint as to the number of rows that should
   * be fetched from the database when more rows are needed for this
   * {@code ResultSet} object.
   * If the fetch size specified is zero, the JDBC driver
   * ignores the value and is free to make its own best guess as to what
   * the fetch size should be.  The default value is set by the
   * {@code Statement} object
   * that created the result set.  The fetch size may be changed at any time.
   *
   * @param rows the number of rows to fetch
   * @throws SQLException if a database access error occurs; this method
   * is called on a closed result set or the
   * condition {@code rows >= 0} is not satisfied
   * @since 1.2
   * @see #getFetchSize
   */
  @Override
  void setFetchSize(int rows) throws SQLException {
    // not applicable
  }

  /**
   * Retrieves the fetch size for this
   * {@code ResultSet} object.
   *
   * @return the current fetch size for this {@code ResultSet} object
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   * @see #setFetchSize
   */
  @Override
  int getFetchSize() throws SQLException {
    return 0
  }

  /**
   * Retrieves the type of this {@code ResultSet} object.
   * The type is determined by the {@code Statement} object
   * that created the result set.
   *
   * @return {@code ResultSet.TYPE_FORWARD_ONLY},
   * {@code ResultSet.TYPE_SCROLL_INSENSITIVE},
   *         or {@code ResultSet.TYPE_SCROLL_SENSITIVE}
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  int getType() throws SQLException {
    ResultSet.TYPE_SCROLL_INSENSITIVE
  }

  /**
   * Retrieves the concurrency mode of this {@code ResultSet} object.
   * The concurrency used is determined by the
   * {@code Statement} object that created the result set.
   *
   * @return the concurrency type, either
   * {@code ResultSet.CONCUR_READ_ONLY}
   *         or {@code ResultSet.CONCUR_UPDATABLE}
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  int getConcurrency() throws SQLException {
    ResultSet.CONCUR_UPDATABLE
  }

  /**
   * Retrieves whether the current row has been updated.  The value returned
   * depends on whether or not the result set can detect updates.
   * <p>
   * <strong>Note:</strong> Support for the {@code rowUpdated} method is optional with a result set
   * concurrency of {@code CONCUR_READ_ONLY}
   * @return {@code true} if the current row is detected to
   * have been visibly updated by the owner or another; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @see java.sql.DatabaseMetaData#updatesAreDetected
   * @since 1.2
   */
  @Override
  boolean rowUpdated() throws SQLException {
    throw new SQLFeatureNotSupportedException("rowUpdated is not implemented")
  }

  /**
   * Retrieves whether the current row has had an insertion.
   * The value returned depends on whether or not this
   * {@code ResultSet} object can detect visible inserts.
   * <p>
   * <strong>Note:</strong> Support for the {@code rowInserted} method is optional with a result set
   * concurrency of {@code CONCUR_READ_ONLY}
   * @return {@code true} if the current row is detected to
   * have been inserted; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   *
   * @see java.sql.DatabaseMetaData#insertsAreDetected
   * @since 1.2
   */
  @Override
  boolean rowInserted() throws SQLException {
    throw new SQLFeatureNotSupportedException("rowInserted is not implemented")
  }

  /**
   * Retrieves whether a row has been deleted.  A deleted row may leave
   * a visible "hole" in a result set.  This method can be used to
   * detect holes in a result set.  The value returned depends on whether
   * or not this {@code ResultSet} object can detect deletions.
   * <p>
   * <strong>Note:</strong> Support for the {@code rowDeleted} method is optional with a result set
   * concurrency of {@code CONCUR_READ_ONLY}
   * @return {@code true} if the current row is detected to
   * have been deleted by the owner or another; {@code false} otherwise
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   *
   * @see java.sql.DatabaseMetaData#deletesAreDetected
   * @since 1.2
   */
  @Override
  boolean rowDeleted() throws SQLException {
    throw new SQLFeatureNotSupportedException("rowDeleted is not implemented")
  }

  /**
   * Updates the designated column with a {@code null} value.
   *
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow}
   * or {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateNull(int columnIndex) throws SQLException {
    matrix[rowIdx, columnIndex-1] = null
  }

  /**
   * Updates the designated column with a {@code boolean} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBoolean(int columnIndex, boolean x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code byte} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateByte(int columnIndex, byte x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code short} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateShort(int columnIndex, short x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with an {@code int} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateInt(int columnIndex, int x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code long} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateLong(int columnIndex, long x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code float} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateFloat(int columnIndex, float x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code double} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateDouble(int columnIndex, double x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code java.math.BigDecimal}
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code String} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateString(int columnIndex, String x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code byte} array value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBytes(int columnIndex, byte[] x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.Date} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateDate(int columnIndex, Date x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.Time} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateTime(int columnIndex, Time x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.Timestamp}
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x.getText()
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x.getBytes()
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x.getText()
  }

  /**
   * Updates the designated column with an {@code Object} value.
   *
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   * <p>
   * If the second argument is an {@code InputStream} then the stream must contain
   * the number of bytes specified by scaleOrLength.  If the second argument is a
   * {@code Reader} then the reader must contain the number of characters specified
   * by scaleOrLength. If these conditions are not true the driver will generate a
   * {@code SQLException} when the statement is executed.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param scaleOrLength for an object of {@code java.math.BigDecimal} ,
   *          this is the number of digits after the decimal point. For
   *          Java Object types {@code InputStream} and {@code Reader},
   *          this is the length
   *          of the data in the stream or reader.  For all other types,
   *          this value will be ignored.
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
    if (x instanceof BigDecimal) {
      x = x.setScale(scaleOrLength)
    }
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with an {@code Object} value.
   *
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateObject(int columnIndex, Object x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x
  }

  /**
   * Updates the designated column with a {@code null} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateNull(String columnLabel) throws SQLException {
    matrix[rowIdx, columnLabel] = null
  }

  /**
   * Updates the designated column with a {@code boolean} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBoolean(String columnLabel, boolean x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code byte} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateByte(String columnLabel, byte x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code short} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateShort(String columnLabel, short x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with an {@code int} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateInt(String columnLabel, int x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code long} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateLong(String columnLabel, long x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code float} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateFloat(String columnLabel, float x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code double} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateDouble(String columnLabel, double x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.BigDecimal}
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code String} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateString(String columnLabel, String x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a byte array value.
   *
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow}
   * or {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBytes(String columnLabel, byte[] x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.Date} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateDate(String columnLabel, Date x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.Time} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateTime(String columnLabel, Time x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with a {@code java.sql.Timestamp}
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
    matrix[rowIdx, columnLabel] = x.getText()
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
    matrix[rowIdx, columnLabel] = x.getBytes()
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader the {@code java.io.Reader} object containing
   *        the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
    matrix[rowIdx, columnLabel] = reader.getText()
  }

  /**
   * Updates the designated column with an {@code Object} value.
   *
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   * <p>
   * If the second argument is an {@code InputStream} then the stream must contain
   * the number of bytes specified by scaleOrLength.  If the second argument is a
   * {@code Reader} then the reader must contain the number of characters specified
   * by scaleOrLength. If these conditions are not true the driver will generate a
   * {@code SQLException} when the statement is executed.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @param scaleOrLength for an object of {@code java.math.BigDecimal} ,
   *          this is the number of digits after the decimal point. For
   *          Java Object types {@code InputStream} and {@code Reader},
   *          this is the length
   *          of the data in the stream or reader.  For all other types,
   *          this value will be ignored.
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
    if (x instanceof BigDecimal) {
      x = x.setScale(scaleOrLength)
    }
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Updates the designated column with an {@code Object} value.
   *
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateObject(String columnLabel, Object x) throws SQLException {
    matrix[rowIdx, columnLabel] = x
  }

  /**
   * Inserts the contents of the insert row into this
   * {@code ResultSet} object and into the database.
   * The cursor must be on the insert row when this method is called.
   *
   * @throws SQLException if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY},
   * this method is called on a closed result set,
   * if this method is called when the cursor is not on the insert row,
   * or if not all of non-nullable columns in
   * the insert row have been given a non-null value
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void insertRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("This result set is detached")
  }

  /**
   * Updates the underlying database with the new contents of the
   * current row of this {@code ResultSet} object.
   * This method cannot be called when the cursor is on the insert row.
   *
   * @throws SQLException if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY};
   *  this method is called on a closed result set or
   * if this method is called when the cursor is on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void updateRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("This result set is detached")
  }

  /**
   * Deletes the current row from this {@code ResultSet} object
   * and from the underlying database.  This method cannot be called when
   * the cursor is on the insert row.
   *
   * @throws SQLException if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY};
   * this method is called on a closed result set
   * or if this method is called when the cursor is on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void deleteRow() throws SQLException {
    matrix.removeRows(rowIdx)
  }

  /**
   * Refreshes the current row with its most recent value in
   * the database.  This method cannot be called when
   * the cursor is on the insert row.
   *
   * <P>The {@code refreshRow} method provides a way for an
   * application to
   * explicitly tell the JDBC driver to refetch a row(s) from the
   * database.  An application may want to call {@code refreshRow} when
   * caching or prefetching is being done by the JDBC driver to
   * fetch the latest value of a row from the database.  The JDBC driver
   * may actually refresh multiple rows at once if the fetch size is
   * greater than one.
   *
   * <P> All values are refetched subject to the transaction isolation
   * level and cursor sensitivity.  If {@code refreshRow} is called after
   * calling an updater method, but before calling
   * the method {@code updateRow}, then the
   * updates made to the row are lost.  Calling the method
   * {@code refreshRow} frequently will likely slow performance.
   *
   * @throws SQLException if a database access error
   * occurs; this method is called on a closed result set;
   * the result set type is {@code TYPE_FORWARD_ONLY} or if this
   * method is called when the cursor is on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method or this method is not supported for the specified result
   * set type and result set concurrency.
   * @since 1.2
   */
  @Override
  void refreshRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("This result set is detached")
  }

  /**
   * Cancels the updates made to the current row in this
   * {@code ResultSet} object.
   * This method may be called after calling an
   * updater method(s) and before calling
   * the method {@code updateRow} to roll back
   * the updates made to a row.  If no updates have been made or
   * {@code updateRow} has already been called, this method has no
   * effect.
   *
   * @throws SQLException if a database access error
   *         occurs; this method is called on a closed result set;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or if this method is called when the cursor is
   *            on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void cancelRowUpdates() throws SQLException {
    throw new SQLFeatureNotSupportedException("This result set is detached")
  }

  /**
   * Moves the cursor to the insert row.  The current cursor position is
   * remembered while the cursor is positioned on the insert row.
   *
   * The insert row is a special row associated with an updatable
   * result set.  It is essentially a buffer where a new row may
   * be constructed by calling the updater methods prior to
   * inserting the row into the result set.
   *
   * Only the updater, getter,
   * and {@code insertRow} methods may be
   * called when the cursor is on the insert row.  All of the columns in
   * a result set must be given a value each time this method is
   * called before calling {@code insertRow}.
   * An updater method must be called before a
   * getter method can be called on a column value.
   *
   * @throws SQLException if a database access error occurs; this
   * method is called on a closed result set
   * or the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void moveToInsertRow() throws SQLException {
    throw new SQLFeatureNotSupportedException("This result set is detached")
  }

  /**
   * Moves the cursor to the remembered cursor position, usually the
   * current row.  This method has no effect if the cursor is not on
   * the insert row.
   *
   * @throws SQLException if a database access error occurs; this
   * method is called on a closed result set
   *  or the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  void moveToCurrentRow() throws SQLException {

  }

  /**
   * Retrieves the {@code Statement} object that produced this
   * {@code ResultSet} object.
   * If the result set was generated some other way, such as by a
   * {@code DatabaseMetaData} method, this method  may return
   * {@code null}.
   *
   * @return the {@code Statement} object that produced
   * this {@code ResultSet} object or {@code null}
   * if the result set was produced some other way
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Statement getStatement() throws SQLException {
    return null
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as an {@code Object}
   * in the Java programming language.
   * If the value is an SQL {@code NULL},
   * the driver returns a Java {@code null}.
   * This method uses the given {@code Map} object
   * for the custom mapping of the
   * SQL structured or distinct type that is being retrieved.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param map a {@code java.util.Map} object that contains the mapping
   * from SQL type names to classes in the Java programming language
   * @return an {@code Object} in the Java programming language
   * representing the SQL value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
    Class<?> type = matrix.type(columnIndex-1)
    String sqlType = sqlTypeMapper.sqlType(type)
    if (sqlType.contains('(')) {
      sqlType = sqlType.substring(0, sqlType.indexOf('('))
    }
    Class<?> mapType = map.getOrDefault(sqlType, type)
    matrix[rowIdx, columnIndex-1, mapType]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code Ref} object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a {@code Ref} object representing an SQL {@code REF}
   *         value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Ref getRef(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Ref is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code Blob} object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a {@code Blob} object representing the SQL
   * {@code BLOB} value in the specified column
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Blob getBlob(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Blob is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code Clob} object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a {@code Clob} object representing the SQL
   * {@code CLOB} value in the specified column
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Clob getClob(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Clob is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as an {@code Array} object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return an {@code Array} object representing the SQL
   * {@code ARRAY} value in the specified column
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Array getArray(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Array is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as an {@code Object}
   * in the Java programming language.
   * If the value is an SQL {@code NULL},
   * the driver returns a Java {@code null}.
   * This method uses the specified {@code Map} object for
   * custom mapping if appropriate.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param map a {@code java.util.Map} object that contains the mapping
   * from SQL type names to classes in the Java programming language
   * @return an {@code Object} representing the SQL value in the
   *         specified column
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
    return getObject(matrix.columnIndex(columnLabel)+1, map)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code Ref} object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code Ref} object representing the SQL {@code REF}
   *         value in the specified column
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Ref getRef(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Ref is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code Blob} object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code Blob} object representing the SQL {@code BLOB}
   *         value in the specified column
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Blob getBlob(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Blob is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code Clob} object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code Clob} object representing the SQL {@code CLOB}
   * value in the specified column
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Clob getClob(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Clob is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as an {@code Array} object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return an {@code Array} object representing the SQL {@code ARRAY} value in
   *         the specified column
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  @Override
  Array getArray(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("support for Array is not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.sql.Date} object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the date if the underlying database does not store
   * timezone information.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param cal the {@code java.util.Calendar} object
   * to use in constructing the date
   * @return the column value as a {@code java.sql.Date} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Date getDate(int columnIndex, Calendar cal) throws SQLException {
    def val = matrix[rowIdx, columnIndex-1]
    if (val instanceof Number) {
      // assume we are storing millis
      return new Date(val + cal.getTimeZone().getOffset(0) as long)
    }
    return matrix[rowIdx, columnIndex-1, Date]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.sql.Date} object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the date if the underlying database does not store
   * timezone information.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param cal the {@code java.util.Calendar} object
   * to use in constructing the date
   * @return the column value as a {@code java.sql.Date} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Date getDate(String columnLabel, Calendar cal) throws SQLException {
    getDate(matrix.columnIndex(columnLabel)+1, cal)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.sql.Time} object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the time if the underlying database does not store
   * timezone information.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param cal the {@code java.util.Calendar} object
   * to use in constructing the time
   * @return the column value as a {@code java.sql.Time} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Time getTime(int columnIndex, Calendar cal) throws SQLException {
    def val = matrix[rowIdx, columnIndex-1]
    if (val instanceof Number) {
      // assume we are storing millis
      return new Time(val + cal.getTimeZone().getOffset(0) as long)
    }
    return matrix[rowIdx, columnIndex-1, Time]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.sql.Time} object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the time if the underlying database does not store
   * timezone information.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param cal the {@code java.util.Calendar} object
   * to use in constructing the time
   * @return the column value as a {@code java.sql.Time} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Time getTime(String columnLabel, Calendar cal) throws SQLException {
    getTime(matrix.columnIndex(columnLabel)+1, cal)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.sql.Timestamp} object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the timestamp if the underlying database does not store
   * timezone information.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param cal the {@code java.util.Calendar} object
   * to use in constructing the timestamp
   * @return the column value as a {@code java.sql.Timestamp} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    def val = matrix[rowIdx, columnIndex-1]
    if (val instanceof Number) {
      // assume we are storing millis
      return new Timestamp(val + cal.getTimeZone().getOffset(0) as long)
    }
    return matrix[rowIdx, columnIndex-1, Timestamp]
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.sql.Timestamp} object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the timestamp if the underlying database does not store
   * timezone information.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param cal the {@code java.util.Calendar} object
   * to use in constructing the date
   * @return the column value as a {@code java.sql.Timestamp} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnLabel is not valid or
   * if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
    getTimestamp(matrix.columnIndex(columnLabel) + 1, cal)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.net.URL}
   * object in the Java programming language.
   *
   * @param columnIndex the index of the column 1 is the first, 2 is the second,...
   * @return the column value as a {@code java.net.URL} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs; this method
   * is called on a closed result set or if a URL is malformed
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  URL getURL(int columnIndex) throws SQLException {
    try {
      new URL(matrix[rowIdx, columnIndex - 1, String])
    } catch (MalformedURLException e) {
      throw new SQLException(e.getMessage(), e)
    }
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code java.net.URL}
   * object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value as a {@code java.net.URL} object;
   * if the value is SQL {@code NULL},
   * the value returned is {@code null} in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs; this method
   * is called on a closed result set or if a URL is malformed
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  URL getURL(String columnLabel) throws SQLException {
    getURL(matrix.columnIndex(columnLabel) +1)
  }

  /**
   * Updates the designated column with a {@code java.sql.Ref} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateRef(int columnIndex, Ref x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Ref not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Ref} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateRef(String columnLabel, Ref x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Ref not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Blob} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateBlob(int columnIndex, Blob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Blob not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Blob} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateBlob(String columnLabel, Blob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Blob not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Clob} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateClob(int columnIndex, Clob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Clob} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateClob(String columnLabel, Clob x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Array} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateArray(int columnIndex, Array x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Array not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.Array} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.4
   */
  @Override
  void updateArray(String columnLabel, Array x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Array not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row of this
   * {@code ResultSet} object as a {@code java.sql.RowId} object in the Java
   * programming language.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @return the column value; if the value is a SQL {@code NULL} the
   *     value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  RowId getRowId(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for RowId not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row of this
   * {@code ResultSet} object as a {@code java.sql.RowId} object in the Java
   * programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value ; if the value is a SQL {@code NULL} the
   *     value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  RowId getRowId(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for RowId not implemented")
  }

  /**
   * Updates the designated column with a {@code RowId} value. The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the {@code updateRow} or {@code insertRow} methods are called
   * to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param x the column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateRowId(int columnIndex, RowId x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for RowId not implemented")
  }

  /**
   * Updates the designated column with a {@code RowId} value. The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the {@code updateRow} or {@code insertRow} methods are called
   * to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateRowId(String columnLabel, RowId x) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for RowId not implemented")
  }

  /**
   * Retrieves the holdability of this {@code ResultSet} object
   * @return either {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
   * @throws SQLException if a database access error occurs
   * or this method is called on a closed result set
   * @since 1.6
   */
  @Override
  int getHoldability() throws SQLException {
    ResultSet.CLOSE_CURSORS_AT_COMMIT
  }

  /**
   * Retrieves whether this {@code ResultSet} object has been closed. A {@code ResultSet} is closed if the
   * method close has been called on it, or if it is automatically closed.
   *
   * @return true if this {@code ResultSet} object is closed; false if it is still open
   * @throws SQLException if a database access error occurs
   * @since 1.6
   */
  @Override
  boolean isClosed() throws SQLException {
    return matrix == null
  }

  /**
   * Updates the designated column with a {@code String} value.
   * It is intended for use when updating {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param nString the value for the column to be updated
   * @throws SQLException if the columnIndex is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNString(int columnIndex, String nString) throws SQLException {
    matrix[rowIdx, columnIndex-1] = nString
  }

  /**
   * Updates the designated column with a {@code String} value.
   * It is intended for use when updating {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param nString the value for the column to be updated
   * @throws SQLException if the columnLabel is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   *  or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNString(String columnLabel, String nString) throws SQLException {
    matrix[rowIdx, columnLabel] = nString
  }

  /**
   * Updates the designated column with a {@code java.sql.NClob} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param nClob the value for the column to be updated
   * @throws SQLException if the columnIndex is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set;
   * if a database access error occurs or
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.NClob} value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param nClob the value for the column to be updated
   * @throws SQLException if the columnLabel is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set;
   *  if a database access error occurs or
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code NClob} object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a {@code NClob} object representing the SQL
   * {@code NCLOB} value in the specified column
   * @throws SQLException if the columnIndex is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set
   * or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  NClob getNClob(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a {@code NClob} object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code NClob} object representing the SQL {@code NCLOB}
   * value in the specified column
   * @throws SQLException if the columnLabel is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set
   * or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  NClob getNClob(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Retrieves the value of the designated column in  the current row of
   *  this {@code ResultSet} as a
   * {@code java.sql.SQLXML} object in the Java programming language.
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a {@code SQLXML} object that maps an {@code SQL XML} value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  SQLXML getSQLXML(int columnIndex) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for SQLXML not implemented")
  }

  /**
   * Retrieves the value of the designated column in  the current row of
   *  this {@code ResultSet} as a
   * {@code java.sql.SQLXML} object in the Java programming language.
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code SQLXML} object that maps an {@code SQL XML} value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  SQLXML getSQLXML(String columnLabel) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for SQLXML not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.SQLXML} value.
   * The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the {@code updateRow} or {@code insertRow} methods are called
   * to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param xmlObject the value for the column to be updated
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs; this method
   *  is called on a closed result set;
   * the {@code java.xml.transform.Result},
   * {@code Writer} or {@code OutputStream} has not been closed
   * for the {@code SQLXML} object;
   *  if there is an error processing the XML value or
   * the result set concurrency is {@code CONCUR_READ_ONLY}.  The {@code getCause} method
   *  of the exception may provide a more detailed exception, for example, if the
   *  stream does not contain valid XML.
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for SQLXML not implemented")
  }

  /**
   * Updates the designated column with a {@code java.sql.SQLXML} value.
   * The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the {@code updateRow} or {@code insertRow} methods are called
   * to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param xmlObject the column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs; this method
   *  is called on a closed result set;
   * the {@code java.xml.transform.Result},
   * {@code Writer} or {@code OutputStream} has not been closed
   * for the {@code SQLXML} object;
   *  if there is an error processing the XML value or
   * the result set concurrency is {@code CONCUR_READ_ONLY}.  The {@code getCause} method
   *  of the exception may provide a more detailed exception, for example, if the
   *  stream does not contain valid XML.
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for SQLXML not implemented")
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code String} in the Java programming language.
   * It is intended for use when
   * accessing  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  String getNString(int columnIndex) throws SQLException {
    getString(columnIndex)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as
   * a {@code String} in the Java programming language.
   * It is intended for use when
   * accessing  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL {@code NULL}, the
   * value returned is {@code null}
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  String getNString(String columnLabel) throws SQLException {
    getString(columnLabel)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a
   * {@code java.io.Reader} object.
   * It is intended for use when
   * accessing  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   *
   * @return a {@code java.io.Reader} object that contains the column
   * value; if the value is SQL {@code NULL}, the value returned is
   * {@code null} in the Java programming language.
   * @param columnIndex the first column is 1, the second is 2, ...
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  Reader getNCharacterStream(int columnIndex) throws SQLException {
    getCharacterStream(columnIndex)
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object as a
   * {@code java.io.Reader} object.
   * It is intended for use when
   * accessing  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a {@code java.io.Reader} object that contains the column
   * value; if the value is SQL {@code NULL}, the value returned is
   * {@code null} in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  Reader getNCharacterStream(String columnLabel) throws SQLException {
    getCharacterStream(columnLabel)
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.   The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY} or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    updateCharacterStream(columnIndex, x, length)
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.  The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader the {@code java.io.Reader} object containing
   *        the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY} or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    updateCharacterStream(columnLabel, reader, length)
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    updateAsciiStream(columnIndex, x)
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    updateBinaryStream(columnIndex, x)
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    updateCharacterStream(columnIndex, x)
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    updateAsciiStream(columnLabel, x)
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    updateBinaryStream(columnLabel, x)
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader the {@code java.io.Reader} object containing
   *        the new column value
   * @param length the length of the stream
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    updateCharacterStream(columnLabel, reader)
  }

  /**
   * Updates the designated column using the given input stream, which
   * will have the specified number of bytes.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param inputStream An object that contains the data to set the parameter
   * value to.
   * @param length the number of bytes in the parameter data.
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Blob not implemented")
  }

  /**
   * Updates the designated column using the given input stream, which
   * will have the specified number of bytes.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param inputStream An object that contains the data to set the parameter
   * value to.
   * @param length the number of bytes in the parameter data.
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Blob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a {@code LONGVARCHAR}
   * parameter, it may be more practical to send it via a
   * {@code java.io.Reader} object. The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param reader An object that contains the data to set the parameter value to.
   * @param length the number of characters in the parameter data.
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a {@code LONGVARCHAR}
   * parameter, it may be more practical to send it via a
   * {@code java.io.Reader} object.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader An object that contains the data to set the parameter value to.
   * @param length the number of characters in the parameter data.
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a {@code LONGVARCHAR}
   * parameter, it may be more practical to send it via a
   * {@code java.io.Reader} object. The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param reader An object that contains the data to set the parameter value to.
   * @param length the number of characters in the parameter data.
   * @throws SQLException if the columnIndex is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set,
   * if a database access error occurs or
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for NClob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a {@code LONGVARCHAR}
   * parameter, it may be more practical to send it via a
   * {@code java.io.Reader} object. The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader An object that contains the data to set the parameter value to.
   * @param length the number of characters in the parameter data.
   * @throws SQLException if the columnLabel is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set;
   *  if a database access error occurs or
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for NClob not implemented")
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateNCharacterStream} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY} or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    updateCharacterStream(columnIndex, x)
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  {@code NCHAR},{@code NVARCHAR}
   * and {@code LONGNVARCHAR} columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateNCharacterStream} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader the {@code java.io.Reader} object containing
   *        the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY} or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    updateCharacterStream(columnLabel, reader)
  }

  /**
   * Updates the designated column with an ascii stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateAsciiStream} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x.text
  }

  /**
   * Updates the designated column with a binary stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateBinaryStream} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x.getBytes()
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateCharacterStream} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x the new column value
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    matrix[rowIdx, columnIndex-1] = x.text
  }

  /**
   * Updates the designated column with an ascii stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateAsciiStream} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    matrix[rowIdx, columnLabel] = x.text
  }

  /**
   * Updates the designated column with a binary stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateBinaryStream} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x the new column value
   * @throws SQLException if the columnLabel is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    matrix[rowIdx, columnLabel] = x.getBytes()
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateCharacterStream} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader the {@code java.io.Reader} object containing
   *        the new column value
   * @throws SQLException if the columnLabel is not valid; if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    matrix[rowIdx, columnLabel] = reader.text
  }

  /**
   * Updates the designated column using the given input stream. The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateBlob} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param inputStream An object that contains the data to set the parameter
   * value to.
   * @throws SQLException if the columnIndex is not valid; if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Blob not implemented")
  }

  /**
   * Updates the designated column using the given input stream. The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   *   <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateBlob} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param inputStream An object that contains the data to set the parameter
   * value to.
   * @throws SQLException if the columnLabel is not valid; if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Blob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object.
   *  The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   *   <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateClob} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param reader An object that contains the data to set the parameter value to.
   * @throws SQLException if the columnIndex is not valid;
   * if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateClob(int columnIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object.
   *  The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateClob} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader An object that contains the data to set the parameter value to.
   * @throws SQLException if the columnLabel is not valid; if a database access error occurs;
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateClob(String columnLabel, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for Clob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   *
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateNClob} which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param reader An object that contains the data to set the parameter value to.
   * @throws SQLException if the columnIndex is not valid;
   * if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set,
   * if a database access error occurs or
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNClob(int columnIndex, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for NClob not implemented")
  }

  /**
   * Updates the designated column using the given {@code Reader}
   * object.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the {@code updateRow} or
   * {@code insertRow} methods are called to update the database.
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * {@code updateNClob} which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader An object that contains the data to set the parameter value to.
   * @throws SQLException if the columnLabel is not valid; if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; this method is called on a closed result set;
   *  if a database access error occurs or
   * the result set concurrency is {@code CONCUR_READ_ONLY}
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  @Override
  void updateNClob(String columnLabel, Reader reader) throws SQLException {
    throw new SQLFeatureNotSupportedException("Support for NClob not implemented")
  }

  /**
   * <p>Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object and will convert from the
   * SQL type of the column to the requested Java data type, if the
   * conversion is supported. If the conversion is not
   * supported  or null is specified for the type, a
   * {@code SQLException} is thrown.
   * <p>
   * At a minimum, an implementation must support the conversions defined in
   * Appendix B, Table B-3 and conversion of appropriate user defined SQL
   * types to a Java type which implements {@code SQLData}, or {@code Struct}.
   * Additional conversions may be supported and are vendor defined.
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param type Class representing the Java data type to convert the designated
   * column to.
   * @return an instance of {@code type} holding the column value
   * @throws SQLException if conversion is not supported, type is null or
   *         another error occurs. The getCause() method of the
   * exception may provide a more detailed exception, for example, if
   * a conversion error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.7
   */
  @Override
  <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    matrix[rowIdx, columnIndex-1, type]
  }

  /**
   * <p>Retrieves the value of the designated column in the current row
   * of this {@code ResultSet} object and will convert from the
   * SQL type of the column to the requested Java data type, if the
   * conversion is supported. If the conversion is not
   * supported  or null is specified for the type, a
   * {@code SQLException} is thrown.
   * <p>
   * At a minimum, an implementation must support the conversions defined in
   * Appendix B, Table B-3 and conversion of appropriate user defined SQL
   * types to a Java type which implements {@code SQLData}, or {@code Struct}.
   * Additional conversions may be supported and are vendor defined.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.
   * If the SQL AS clause was not specified, then the label is the name
   * of the column
   * @param type Class representing the Java data type to convert the designated
   * column to.
   * @return an instance of {@code type} holding the column value
   * @throws SQLException if conversion is not supported, type is null or
   *         another error occurs. The getCause() method of the
   * exception may provide a more detailed exception, for example, if
   * a conversion error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.7
   */
  @Override
  <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    matrix[rowIdx, columnLabel, type]
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
  def <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface == Matrix) {
      return matrix as T
    }
    if (iface == List) {
      return matrix.rows() as T
    }
    return null
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
    if (iface == Matrix || iface == List) {
      return true
    }
    return false
  }
}
