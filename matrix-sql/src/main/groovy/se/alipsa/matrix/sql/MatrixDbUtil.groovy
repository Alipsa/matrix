package se.alipsa.matrix.sql

import groovy.transform.CompileStatic
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.util.Logger

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.stream.IntStream

import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_PRECISION
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_SCALE
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getVARCHAR_SIZE

/**
 * Utility class for creating tables and inserting data from Matrix objects into a database.
 */
// dynamic use of getAt extensions prevents this from being compiled statically
// @CompileStatic
class MatrixDbUtil {

  private static final Logger log = Logger.getLogger(MatrixDbUtil)

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
   * @param addQuotes whether to add quotes around column names
   * @param primaryKey name(s) of the primary key columns
   */
  Map create(Connection con, Matrix table, Map<String, Map<String, Integer>> props, boolean addQuotes = true, String... primaryKey) throws SQLException {
    create(tableName(table), con, table, props, addQuotes, primaryKey)
  }

  /**
   * create table and insert the table data.
   *
   * @param table the table to copy to the db
   * @param props a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * constants as key and the size as value
   * @param addQuotes whether to add quotes around column names
   * @param primaryKey name(s) of the primary key columns
   */
  Map create(String tableName, Connection con, Matrix table, Map<String, Map<String, Integer>> props, boolean addQuotes = true, String... primaryKey) throws SQLException {
    Map result = [:]

    String sql = createTableDdl(tableName, table, props, addQuotes, primaryKey)
    result.sql = sql
    try(Statement stm = con.createStatement()) {
      if (tableExists(con, tableName)) {
        throw new SQLException("Table $tableName already exists", "Cannot create $tableName since it already exists, no data copied to db")
      }
      result.ddlResult = stm.execute(sql)

    } catch (SQLException e) {
      log.error("Failed to create table $tableName using ddl: $sql", e)
      throw e
    }
    try {
      result.inserted = insert(con, tableName, table)
    } catch (SQLException e) {
      log.error("Failed to insert data to table $tableName: ${e.message}", e)
      throw e
    }
    result
  }

  /**
   * Create a create table ddl statement for the given table.
   *
   * @param tableName the name of the table to create
   * @param table the table to create
   * @param props a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * @param addQuotes whether to add quotes around column names
   * @param primaryKey name(s) of the primary key columns
   * @return the create table ddl statement
   */
  String createTableDdl(String tableName, Matrix table, Map<String, Map<String, Integer>> props, boolean addQuotes, String... primaryKey) {
    String sql = "create table $tableName (\n"

    List<String> columns = new ArrayList<>()
    int i = 0
    List<Class> types = table.types()
    for (String name : table.columnNames()) {
      Class type = types.get(i++)
      if (addQuotes) {
        columns.add("\"" + name + "\" " + mapper.sqlType(type, props[name]))
      } else {
        columns.add(name + " " + mapper.sqlType(type, props[name]))
      }
    }
    sql += String.join(",\n", columns)
    if (primaryKey.length > 0) {
      sql += "\n , CONSTRAINT pk_" + table.getMatrixName() + " PRIMARY KEY (\"" + String.join("\", \"", primaryKey) + "\")"
    }
    sql += "\n)"
    sql
  }

  /**
   * create table and insert the table data.
   *
   * @param con the db connection
   * @param table the table to copy to the db
   * @param scanNumRows number of rows to scan for sizing information
   * @param addQuotes whether to add quotes around column names
   * @param primaryKey name(s) of the primary key columns
   * @return a map with information about the created table and inserted data
   * @throws SQLException if any sql error occurs
   */
  Map create(Connection con, Matrix table, int scanNumRows, boolean addQuotes = true, String... primaryKey) throws SQLException {
    Map<String,Map<String, Integer>> mappings = createMappings(table, scanNumRows)
    return create(con, table, mappings, addQuotes, primaryKey)
  }

  /**
   * create table and insert the table data.
   *
   * @param tableName The name of the table to create
   * @param con the db connection
   * @param table the table to copy to the db
   * @param scanNumRows number of rows to scan for sizing information
   * @param addQuotes whether to add quotes around column names
   * @param primaryKey name(s) of the primary key columns
   * @return a map with information about the created table and inserted data
   * @throws SQLException if any sql error occurs
   */
  Map create(String tableName, Connection con, Matrix table, int scanNumRows, boolean addQuotes = true, String... primaryKey) throws SQLException {
    Map<String,Map<String, Integer>> mappings = createMappings(table, scanNumRows)
    return create(tableName, con, table, mappings, addQuotes, primaryKey)
  }

  /**
   * Create sizing mappings for the given table by scanning the given number of rows.
   *
   * @param table the table to create mappings for
   * @param scanNumRows number of rows to scan for sizing information
   * @return a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * constants as key and the size as value
   */
  Map<String,Map<String, Integer>> createMappings(Matrix table, int scanNumRows) {
    List<Class<?>> types = table.types()
    Map<String, Map<String, Integer>> mappings = [:]
    int rowCount = table.rowCount()
    int rowsToScan = Math.min(Math.max(scanNumRows, 0), rowCount)
    int i = 0
    for (String name : table.columnNames()) {
      Map<String, Integer> props = [:]
      Class type = types.get(i++)
      if (BigDecimal == type) {
        Integer left = 0
        Integer right = 0
        for (int r = 0; r < rowsToScan; r++) {
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
        for (int r = 0; r < rowsToScan; r++) {
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
   * @param addQuotes whether to add quotes around column names
   * @param primaryKey name(s) of the primary key columns
   */
  Map create(Connection con, Matrix table, boolean addQuotes = true, String... primaryKey) throws SQLException {
    return create(con, table, Math.max(100, table.rowCount()), addQuotes, primaryKey)
  }

  /**
   * Drop the given table from the database.
   *
   * @param con the db connection
   * @param tableName the name of the table to drop
   * @return the result of the drop operation
   */
  Object dropTable(Connection con, String tableName) {
    dbExecuteSql(con, "drop table $tableName")
  }

  /**
   * Drop the given table from the database.
   *
   * @param con the db connection
   * @param table the table to drop
   * @return the result of the drop operation
   */
  Object dropTable(Connection con, Matrix table) {
    dropTable(con,  tableName(table))
  }

  /**
   * Execute the given select query and return the result as a Matrix.
   *
   * @param con the db connection
   * @param sqlQuery the sql select query to execute
   * @return the result as a Matrix
   * @throws SQLException if any sql error occurs
   */
  Matrix select(Connection con, String sqlQuery) throws SQLException {
    try(Statement stm = con.createStatement(); ResultSet rs = stm.executeQuery(sqlQuery)) {
      return Matrix.builder().data(rs).build()
    }
  }


  /**
   * Check if the given table exists in the database.
   *
   * @param con the db connection
   * @param table the table to check for
   * @return true if the table exists, false otherwise
   * @throws SQLException if any sql error occurs
   */
  boolean tableExists(Connection con, Matrix table) throws SQLException {
    tableExists(con, tableName(table))
  }

  /**
   * Check if the given table exists in the database.
   *
   * @param con the db connection
   * @param tableName the name of the table to check for
   * @return true if the table exists, false otherwise
   * @throws SQLException if any sql error occurs
   */
  boolean tableExists(Connection con, String tableName) throws SQLException {
    try (ResultSet rs = con.getMetaData().getTables(null, null, null, null)) {
      while (rs.next()) {
        String name = rs.getString('TABLE_NAME')
        if (name.toUpperCase() == tableName.toUpperCase()) {
          return true
        }
      }
      return false
    }
  }

  /**
   * Get the names of all tables in the database.
   *
   * @param con the db connection
   * @return a set of table names
   * @throws SQLException if any sql error occurs
   */
  Set<String> getTableNames(Connection con) throws SQLException {
    Set<String> names = new HashSet<>()
    try (ResultSet rs = con.getMetaData().getTables(null, null, null, null)) {
      while (rs.next()) {
        names << rs.getString('TABLE_NAME')
      }
    }
    names
  }

  /**
   * Insert the data from the given table into the given table in the database.
   *
   * @param con the db connection
   * @param table the table containing the data to insert
   * @return the number of inserted rows
   * @throws SQLException if any sql error occurs
   */
  int insert(Connection con, Matrix table) throws SQLException {
    insert(con, tableName(table), table)
  }

  /**
   * Insert the data from the given table into the given table in the database.
   *
   * @param con the db connection
   * @param tableName the name of the table to insert into
   * @param table the table containing the data to insert
   * @return the number of inserted rows
   * @throws SQLException if any sql error occurs
   */
  int insert(Connection con, String tableName, Matrix table) throws SQLException {
    String insertSql = SqlGenerator.createPreparedInsertSql(tableName, table)
    try(PreparedStatement stm = con.prepareStatement(insertSql)) {
      for (Row row : table) {
        int i = 1
        row.each {
          // if there are issues we could use the setObject method that also takes a java.sql.Types
          stm.setObject(i++, mapper.convertToDbValue(it))
        }
        stm.addBatch()
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  /**
   * Get a valid table name from the matrix name by replacing invalid characters.
   *
   * @param table the matrix to get the table name for
   * @return a valid table name
   */
  static String tableName(Matrix table) {
    def name = table.getMatrixName()
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Matrix name is required but was '$name'")
    }
    name.replace(".", "_")
        .replace("-", "_")
        .replace("*", "")
  }

  /**
   * Execute the given sql statement.
   *
   * @param con the db connection
   * @param sql the sql statement to execute
   * @return either a Matrix (for select statements) or an Integer (for update counts)
   * @throws SQLException if any sql error occurs
   */
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

  /**
   * Convert the given matrix to a ResultSet.
   *
   * @param matrix the matrix to convert
   * @return a ResultSet representing the matrix data
   */
  static ResultSet asResultSet(Matrix matrix) {
    new MatrixResultSet(matrix)
  }
}
