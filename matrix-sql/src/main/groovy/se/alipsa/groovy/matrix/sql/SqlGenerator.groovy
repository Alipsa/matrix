package se.alipsa.groovy.matrix.sql

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row

class SqlGenerator {

  private static final Logger LOG = LogManager.getLogger(SqlGenerator.class)

  static String createUpdateSql(String tableName, Row row, String[] matchColumnName) {
    String sql = "update " + tableName + " set "
    List<String> columnNames = new ArrayList<>(row.columnNames())
    columnNames.removeAll(List.of(matchColumnName))
    List<String> setValues = new ArrayList<>();
    columnNames.forEach(n -> setValues.add(n + " = " + quoteIfString(row, n)))
    sql += String.join(", ", setValues)
    sql += " where "
    List<String> conditions = new ArrayList<>()
    for (String condition : matchColumnName) {
      conditions.add(condition + " = " + quoteIfString(row, condition))
    }
    sql += String.join(" and ", conditions)
    LOG.debug("Executing update query: ${sql}")
    return sql
  }

  static String createPreparedInsertSql(Matrix table) {
    StringBuilder sql = new StringBuilder("insert into " + table.name + " ( ")
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

  // Not used, using createPreparedInsertSql instead as it handles various data much better
  private static String createInsertSql(String tableName, Row row) {
    // TODO values for things like byte[] must be converted to hex format
    //  https://www.postgresql.org/docs/current/datatype-binary.html#AEN5318
    //  https://techcommunity.microsoft.com/t5/sql-server-blog/sql-server-2008-new-binary-8211-hex-string-conversion/ba-p/383490
    String sql = "insert into " + tableName + " ( "
    List<String> columnNames = row.columnNames()

    sql += "\"" + String.join("\", \"", columnNames) + "\""
    sql += " ) values ( "

    List<String> values = new ArrayList<>()
    columnNames.forEach(n -> {
      values.add(quoteIfString(row, n))
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
