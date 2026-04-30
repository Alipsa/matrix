package se.alipsa.matrix.sql

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

/**
 * Generates prepared SQL statements (INSERT, UPDATE) for {@link Matrix} and {@link Row} data.
 */
@SuppressWarnings('SpaceInsideParentheses')
class SqlGenerator {

  private static final String COMMA_SEP = ', '
  private static final String PLACEHOLDER = '?'
  private static final String VALUES_CLAUSE = ' ) values ( '
  private static final String CLOSE_PAREN = ' ) '

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
    if (matchColumnName == null || matchColumnName.length == 0) {
      throw new IllegalArgumentException('matchColumnName is required')
    }
    List<String> matchColumns = matchColumnName.toList()
    List<String> updateColumns = updateColumnNames(row.columnNames(), matchColumns)
    if (updateColumns.isEmpty()) {
      throw new IllegalArgumentException('No columns left to update after excluding match columns')
    }
    String sql = createPreparedUpdateSql(tableName, updateColumns, matchColumns)
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
    createPreparedUpdateSql(tableName, updateColumns, matchColumns, true)
  }

  /**
   * Create a prepared update statement (with placeholders).
   *
   * @param tableName the table name
   * @param updateColumns columns to update in the SET clause
   * @param matchColumns columns to match in the WHERE clause
   * @param addQuotes whether to quote identifiers
   * @return the SQL update statement with placeholders
   */
  static String createPreparedUpdateSql(
      String tableName,
      List<String> updateColumns,
      List<String> matchColumns,
      boolean addQuotes
  ) {
    String sql = "update ${SqlIdentifier.renderTable(tableName, addQuotes)} set "
    sql += updateColumns.collect { String column -> "${SqlIdentifier.render(column, addQuotes)} = $PLACEHOLDER" }.join(COMMA_SEP)
    sql += ' where '
    sql += matchColumns.collect { String column -> "${SqlIdentifier.render(column, addQuotes)} = $PLACEHOLDER" }.join(' and ')
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
