package se.alipsa.matrix.sql

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

/**
 * Generates prepared SQL statements (INSERT, UPDATE) for {@link Matrix} and {@link Row} data.
 */
class SqlGenerator {

  private static final String COMMA_SEP = ', '
  private static final String PLACEHOLDER = '?'
  private static final String VALUES_CLAUSE = ' ) values ( '
  private static final String CLOSE_PAREN = ' ) '
  private static final String WHERE_CLAUSE = ' where '
  private static final String AND_SEPARATOR = ' and '

  /**
   * Prepared update statement details.
   */
  static class PreparedUpdate {

    final String sql
    final List<Object> values

    PreparedUpdate(String sql, List<Object> values) {
      this.sql = sql
      this.values = values
    }

  }

  /**
   * Create a prepared update statement (with placeholders) and parameter values.
   *
   * @param tableName the table name
   * @param row the row containing the values to update and match on
   * @param matchColumnName the column(s) to match in the WHERE clause
   * @return a PreparedUpdate with sql and values
   */
  static PreparedUpdate createPreparedUpdate(String tableName, Row row, String[] matchColumnName) {
    Map<String, String> columnNames = row.columnNames().collectEntries { String column -> [(column): column] }
    createPreparedUpdate(tableName, row, matchColumnName, columnNames, false)
  }

  /**
   * Create a prepared update statement using database-resolved identifier spellings.
   *
   * @param tableName the stored table name
   * @param row the row containing the values to update and match on
   * @param matchColumnName the row column name(s) to match in the WHERE clause
   * @param storedColumnNames row column names mapped to their stored database spellings
   * @return a PreparedUpdate with sql and values
   */
  static PreparedUpdate createPreparedUpdate(
      String tableName,
      Row row,
      String[] matchColumnName,
      Map<String, String> storedColumnNames
  ) {
    createPreparedUpdate(tableName, row, matchColumnName, storedColumnNames, true)
  }

  private static PreparedUpdate createPreparedUpdate(
      String tableName,
      Row row,
      String[] matchColumnName,
      Map<String, String> storedColumnNames,
      boolean storedTableName
  ) {
    if (matchColumnName == null || matchColumnName.length == 0) {
      throw new IllegalArgumentException('matchColumnName is required')
    }
    if (storedColumnNames == null) {
      throw new IllegalArgumentException('storedColumnNames is required')
    }
    List<String> matchColumns = matchColumnName.toList()
    List<String> updateColumns = updateColumnNames(row.columnNames(), matchColumns)
    if (updateColumns.isEmpty()) {
      throw new IllegalArgumentException('No columns left to update after excluding match columns')
    }
    row.columnNames().each { String column ->
      if (!storedColumnNames.containsKey(column) || storedColumnNames[column] == null || storedColumnNames[column].isBlank()) {
        throw new IllegalArgumentException("No stored column name mapping for row column: $column")
      }
    }
    List<String> storedUpdateColumns = updateColumns.collect { storedColumnNames[it] }
    List<String> storedMatchColumns = matchColumns.collect { storedColumnNames[it] }
    String sql = createPreparedUpdateSqlWithTableName(tableName, storedUpdateColumns, storedMatchColumns, storedTableName)
    List<Object> values = updateValues(row, updateColumns, matchColumns)
    new PreparedUpdate(sql, values)
  }

  /**
   * Create a prepared update statement (with placeholders).
   *
   * @param tableName the table name
   * @param updateColumns columns to update in the SET clause
   * @param matchColumns columns to match in the WHERE clause
   * @return the SQL update statement with placeholders
   */
  static String createPreparedUpdateSql(String tableName, List<String> updateColumns, List<String> matchColumns) {
    createPreparedUpdateSqlWithTableName(tableName, updateColumns, matchColumns, false)
  }

  private static String createPreparedUpdateSqlWithTableName(
      String tableName,
      List<String> updateColumns,
      List<String> matchColumns,
      boolean storedTableName
  ) {
    String renderedTableName = storedTableName ? SqlIdentifier.quote(tableName) : SqlIdentifier.renderTable(tableName)
    String sql = "update $renderedTableName set "
    sql += updateColumns.collect { String column -> "${SqlIdentifier.render(column)} = $PLACEHOLDER" }.join(COMMA_SEP)
    sql += WHERE_CLAUSE
    sql += matchColumns.collect { String column -> "${SqlIdentifier.render(column)} = $PLACEHOLDER" }.join(AND_SEPARATOR)
    sql
  }

  /**
   * Create a prepared update statement (with placeholders), optionally quoting identifiers.
   *
   * @param tableName the table name
   * @param updateColumns columns to update in the SET clause
   * @param matchColumns columns to match in the WHERE clause
   * @param addQuotes whether to quote identifiers
   * @return the SQL update statement with placeholders
   * @deprecated Prefer {@link #createPreparedUpdateSql(String, List, List)}, which always quotes identifiers
   */
  @Deprecated
  static String createPreparedUpdateSql(
      String tableName,
      List<String> updateColumns,
      List<String> matchColumns,
      boolean addQuotes
  ) {
    String sql = "update ${SqlIdentifier.renderTable(tableName, addQuotes)} set "
    sql += updateColumns.collect { String column -> "${SqlIdentifier.render(column, addQuotes)} = $PLACEHOLDER" }.join(COMMA_SEP)
    sql += WHERE_CLAUSE
    sql += matchColumns.collect { String column -> "${SqlIdentifier.render(column, addQuotes)} = $PLACEHOLDER" }.join(AND_SEPARATOR)
    sql
  }

  /**
   * Determine the columns to update, excluding match columns.
   *
   * @param columnNames all column names
   * @param matchColumns columns to exclude from updates
   * @return update column names
   */
  static List<String> updateColumnNames(List<String> columnNames, List<String> matchColumns) {
    List<String> updateColumns = [] + columnNames
    updateColumns.removeAll(matchColumns)
    updateColumns
  }

  /**
   * Build the ordered parameter values for an update statement.
   *
   * @param row the row containing values
   * @param updateColumns columns to update
   * @param matchColumns columns to match
   * @return ordered list of parameter values
   */
  static List<Object> updateValues(Row row, List<String> updateColumns, List<String> matchColumns) {
    List<Object> values = []
    updateColumns.each { values.add(row[it]) }
    matchColumns.each { values.add(row[it]) }
    values
  }

  static String createPreparedInsertSql(String tableName, Matrix table) {
    createPreparedInsertSql(tableName, table, true)
  }

  static String createPreparedInsertSql(String tableName, Matrix table, boolean addQuotes) {
    StringBuilder sql = new StringBuilder("insert into ${SqlIdentifier.renderTable(tableName, addQuotes)} ( ")
    List<String> columnNames = table.columnNames()
    String placeholders = ([PLACEHOLDER] * columnNames.size()).join(COMMA_SEP)

    sql.append(SqlIdentifier.renderAll(columnNames, addQuotes).join(COMMA_SEP))
    sql.append(VALUES_CLAUSE)
    sql.append(placeholders)
    sql.append(CLOSE_PAREN)
    sql.toString()
  }

  static String createPreparedInsertSql(String tableName, Row row) {
    createPreparedInsertSql(tableName, row, true)
  }

  static String createPreparedInsertSql(String tableName, Row row, boolean addQuotes) {
    String sql = "insert into ${SqlIdentifier.renderTable(tableName, addQuotes)} ( "
    List<String> columnNames = row.columnNames()
    String placeholders = ([PLACEHOLDER] * columnNames.size()).join(COMMA_SEP)

    sql += SqlIdentifier.renderAll(columnNames, addQuotes).join(COMMA_SEP)
    sql += VALUES_CLAUSE
    sql += placeholders
    sql += CLOSE_PAREN
    sql
  }

}
