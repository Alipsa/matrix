package se.alipsa.groovy.matrix.sql

import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row

class SqlGenerator {


  static String createUpdateSql(String tableName, Row row, String[] matchColumnName) {
    String sql = "update " + tableName + " set "
    List<String> columnNames = new ArrayList<>(row.columnNames())
    columnNames.removeAll(List.of(matchColumnName))
    List<String> setValues = new ArrayList<>()
    columnNames.forEach(n -> setValues.add(n + " = " + quoteIfString(row, n)))
    sql += String.join(", ", setValues)
    sql += " where "
    List<String> conditions = new ArrayList<>()
    for (String condition : matchColumnName) {
      conditions.add(condition + " = " + quoteIfString(row, condition))
    }
    sql += String.join(" and ", conditions)
    return sql
  }

  static String createPreparedInsertSql(Matrix table) {
    StringBuilder sql = new StringBuilder("insert into " + MatrixDbUtil.tableName(table) + " ( ")
    List<String> columnNames = table.columnNames()

    sql.append('"').append(String.join('", "', columnNames)).append('"')
    sql.append(' ) values ( ')
    List<String> values = new ArrayList<>()
    columnNames.forEach(n -> {
      values.add('?')
    })
    sql.append(String.join(", ", values))
    sql.append(' ); ')
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

  private static String quoteIfString(Row row, String columnName) {
    def value = row[columnName]
    if (value instanceof CharSequence || value instanceof Character) {
      return "'" + value + "'"
    }
    return String.valueOf(value)
  }
}
