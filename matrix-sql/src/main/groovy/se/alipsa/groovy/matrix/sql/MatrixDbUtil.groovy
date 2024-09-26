package se.alipsa.groovy.matrix.sql

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.stream.IntStream

import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_PRECISION
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_SCALE
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getVARCHAR_SIZE

class MatrixDbUtil {

  private static final Logger LOG = LogManager.getLogger(MatrixDbUtil.class)

  SqlTypeMapper mapper

  MatrixDbUtil(SqlTypeMapper mapper) {
    this.mapper = mapper
  }

  MatrixDbUtil(DataBaseProvider db) {
    this.mapper = SqlTypeMapper.create(db)
  }


  /**
   * create table and insert the table data.
   *
   * @param table the table to copy to the db
   * @param props a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * constants as key and the size as value
   * @param primaryKey name(s) of the primary key columns
   */
  Map create(Connection con, Matrix table, Map<String, Map<String, Integer>> props, String... primaryKey) throws SQLException {
    Map result = [:]
    String tableName = tableName(table)

    String sql = createTableDdl(tableName, table, props, primaryKey)
    result.sql = sql
    try(Statement stm = con.createStatement()) {
      if (tableExists(con, tableName)) {
        throw new SQLException("Table $tableName already exists", "Cannot create $tableName since it already exists, no data copied to db")
      }
      LOG.debug("Creating table using DDL: {}", sql)
      result.ddlResult = stm.execute(sql)
      result.inserted = insert(con, table)
    }
    result
  }

  String createTableDdl(String tableName, Matrix table, Map<String, Map<String, Integer>> props, String... primaryKey) {
    String sql = "create table $tableName (\n"

    List<String> columns = new ArrayList<>()
    int i = 0
    List<Class<?>> types = table.types()
    for (String name : table.columnNames()) {
      Class type = types.get(i++)
      String column = "\"" + name + "\" " + mapper.sqlType(type, props[name])
      columns.add(column)
    }
    sql += String.join(",\n", columns)
    if (primaryKey.length > 0) {
      sql += "\n , CONSTRAINT pk_" + table.getName() + " PRIMARY KEY (\"" + String.join("\", \"", primaryKey) + "\")"
    }
    sql += "\n);"
    sql
  }

  Map create(Connection con, Matrix table, int scanNumRows, String... primaryKey) throws SQLException {
    Map<String,Map<String, Integer>> mappings = createMappings(table, scanNumRows)
    return create(con, table, mappings, primaryKey)
  }

  Map<String,Map<String, Integer>> createMappings(Matrix table, int scanNumRows) {
    List<Class<?>> types = table.types()
    Map<String, Map<String, Integer>> mappings = [:]
    int i = 0
    for (String name : table.columnNames()) {
      Map<String, Integer> props = [:]
      Class type = types.get(i++)
      if (BigDecimal == type) {
        Integer left = 0
        Integer right = 0
        for (int r = 0; r < scanNumRows; r++) {
          BigDecimal val = table[r, name]
          if (val == null) {
            continue
          }
          if (val.precision() - val.scale() > left) {
            left = val.precision() - val.scale()
          }
          if (val.scale() > right) {
            right = val.scale()
          }
        }
        props.put(DECIMAL_PRECISION, left + right)
        props.put(DECIMAL_SCALE, right)
      } else if (type == String) {
        Integer maxLength = 0
        for (int r = 0; r < scanNumRows; r++) {
          String val = table[r, name]
          if (val == null) {
            continue
          }
          if (val.length() > maxLength) {
            maxLength = val.length()
          }
        }
        props.put(VARCHAR_SIZE, maxLength)
      }
      mappings.put(name, props)
    }
    mappings
  }

  /**
   * create table and insert the table data.
   *
   * @param connectionInfo the connection info defined in the Connections tab
   * @param table the table to copy to the db
   * @param primaryKey name(s) of the primary key columns
   */
  Map create(Connection con, Matrix table, String... primaryKey) throws SQLException {
    return create(con, table, Math.max(100, table.rowCount()), primaryKey)
  }

  Object dropTable(Connection con, String tableName) {
    LOG.debug("Dropping {}...", tableName)
    dbExecuteSql(con, "drop table $tableName")
  }

  Object dropTable(Connection con, Matrix table) {
    dropTable(con,  tableName(table))
  }

  Matrix select(Connection con, String sqlQuery) throws SQLException {
    if (!sqlQuery.trim().toLowerCase().startsWith("select ")) {
      sqlQuery = "select $sqlQuery"
    }
    try(Statement stm = con.createStatement(); ResultSet rs = stm.executeQuery(sqlQuery)) {
      return Matrix.builder().data(rs).build()
    }
  }


  boolean tableExists(Connection con, Matrix table) throws SQLException {
    tableExists(con, tableName(table))
  }

  boolean tableExists(Connection con, String tableName) throws SQLException {
    var rs = con.getMetaData().getTables(null, null, null, null)
    while (rs.next()) {
      String name = rs.getString('TABLE_NAME')
      if (name.toUpperCase() == tableName.toUpperCase()) {
        return true
      }
    }
    return false
  }

  int insert(Connection con, Matrix table) throws SQLException {
    String insertSql = SqlGenerator.createPreparedInsertSql(table)
    LOG.trace(insertSql)
    try(PreparedStatement stm = con.prepareStatement(insertSql)) {
      for (Row row : table) {
        int i = 1
        row.each {
          // if there are issues we could use the setObject method that also takes a java.sql.Types
          stm.setObject(i++, it)
        }
        stm.addBatch()
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  static String tableName(Matrix table) {
    table.getName()
        .replace(".", "_")
        .replace("-", "_")
        .replace("*", "")
  }

  Object dbExecuteSql(Connection con, String sql) throws SQLException {
    try(Statement stm = con.createStatement()) {
      boolean hasResultSet = stm.execute(sql)
      if (hasResultSet) {
        return Matrix.builder().data(stm.getResultSet()).build()
      } else {
        return stm.getUpdateCount()
      }
    }
  }
}
