package se.alipsa.matrix.sql

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

@CompileStatic
class SqlGenerator {

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
    String sql = "update " + tableName + " set "
    sql += updateColumns.collect { "${it} = ?" }.join(", ")
    sql += " where "
    sql += matchColumns.collect { "${it} = ?" }.join(" and ")
    return sql
  }

  /**
   * Determine the columns to update, excluding match columns.
   *
   * @param columnNames all column names
   * @param matchColumns columns to exclude from updates
   * @return update column names
   */
  static List<String> updateColumnNames(List<String> columnNames, List<String> matchColumns) {
    List<String> updateColumns = new ArrayList<>(columnNames)
    updateColumns.removeAll(matchColumns)
    return updateColumns
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
    List<Object> values = new ArrayList<>(updateColumns.size() + matchColumns.size())
    updateColumns.each { values.add(row[it]) }
    matchColumns.each { values.add(row[it]) }
    return values
  }

  /**
   * Create a prepared update statement (with placeholders).
   *
   * @deprecated use {@link #createPreparedUpdate(String, Row, String[])}
   */
  static String createUpdateSql(String tableName, Row row, String[] matchColumnName) {
    return createPreparedUpdate(tableName, row, matchColumnName).sql
  }

  static String createPreparedInsertSql(String tableName, Matrix table) {
    StringBuilder sql = new StringBuilder("insert into " + tableName + " ( ")
    List<String> columnNames = table.columnNames()

    sql.append('"').append(String.join('", "', columnNames)).append('"')
    sql.append(' ) values ( ')
    List<String> values = new ArrayList<>()
    columnNames.forEach(n -> {
      values.add('?')
    })
    sql.append(String.join(", ", values))
    sql.append(' ) ')
    return sql.toString()
  }

  static String createPreparedInsertSql(String tableName, Row row) {
    String sql = "insert into " + tableName + " ( "
    List<String> columnNames = row.columnNames()

    sql += "\"" + String.join("\", \"", columnNames) + "\""
    sql += " ) values ( "

    List<String> values = new ArrayList<>()
    columnNames.forEach(n -> {
      values.add('?')
    })
    sql += String.join(", ", values)
    sql += " ); "
    return sql
  }

}
