package se.alipsa.matrix.tablesaw.gtable


import tech.tablesaw.io.DataFrameReader
import tech.tablesaw.io.ReadOptions
import tech.tablesaw.io.ReaderRegistry

import java.sql.ResultSet
import java.sql.SQLException

/**
 * A Groovy-enhanced data frame reader that returns {@link Gtable} instead of standard Tablesaw {@link tech.tablesaw.api.Table}.
 *
 * <p>This class extends {@link DataFrameReader} and wraps all read operations to return Gtable instances,
 * which provide additional Groovy convenience methods and operators.
 *
 * @see Gtable
 * @see DataFrameReader
 */
class GdataFrameReader extends DataFrameReader {

  /**
   * Constructs a new GdataFrameReader with the given registry.
   *
   * @param registry the reader registry for detecting file formats
   */
  GdataFrameReader(ReaderRegistry registry) {
    super(registry)
  }

  /**
   * Read a table from a URL string.
   *
   * @param url the URL string to read from
   * @return a Gtable containing the data
   */
  Gtable url(String url) {
    Gtable.create(super.url(url))
  }

  /**
   * Read a table from a URL.
   *
   * @param url the URL to read from
   * @return a Gtable containing the data
   */
  Gtable url(URL url) {
    Gtable.create(super.url(url))
  }

  /**
   * Read a table from a string with the specified file extension for format detection.
   *
   * @param s the string containing the table data
   * @param fileExtension the file extension (e.g., "csv", "tsv") to determine the format
   * @return a Gtable containing the data
   */
  Gtable string(String s, String fileExtension) {
    Gtable.create(super.string(s, fileExtension))
  }

  /**
   * Read a table from a file path.
   *
   * @param file the file path to read from
   * @return a Gtable containing the data
   */
  Gtable file(String file) {
    Gtable.create(super.file(file))
  }

  /**
   * Read a table from a File object.
   *
   * @param file the File to read from
   * @return a Gtable containing the data
   */
  Gtable file(File file) {
    Gtable.create(super.file(file))
  }

  /**
   * Read a table using the specified read options.
   *
   * @param options the read options specifying how to parse the data
   * @param <T> the type of read options
   * @return a Gtable containing the data
   */
  def <T extends ReadOptions> Gtable usingOptions(T options) {
    Gtable.create(super.usingOptions(options))
  }

  /**
   * Read a table using a read options builder.
   *
   * @param builder the read options builder
   * @return a Gtable containing the data
   */
  Gtable usingOptions(ReadOptions.Builder builder) {
    Gtable.create(super.usingOptions(builder))
  }

  /**
   * Read a table from a SQL ResultSet.
   *
   * @param resultSet the ResultSet to read from
   * @return a Gtable containing the data
   * @throws SQLException if a database access error occurs
   */
  Gtable db(ResultSet resultSet) throws SQLException {
    Gtable.create(super.db(resultSet))
  }

  /**
   * Read a table from a SQL ResultSet with a specified table name.
   *
   * @param resultSet the ResultSet to read from
   * @param tableName the name to assign to the table
   * @return a Gtable containing the data
   * @throws SQLException if a database access error occurs
   */
  Gtable db(ResultSet resultSet, String tableName) throws SQLException {
    Gtable.create(super.db(resultSet, tableName))
  }
}
