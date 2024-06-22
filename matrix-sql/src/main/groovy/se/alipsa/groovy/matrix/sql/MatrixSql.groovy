package se.alipsa.groovy.matrix.sql

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row
import se.alipsa.mavenutils.MavenUtils

import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.Driver
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.ExecutionException
import java.util.stream.IntStream

import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.*

class MatrixSql {

  ConnectionInfo ci
  SqlTypeMapper mapper

  MatrixSql(ConnectionInfo ci) {
    this.ci = ci
    mapper = create(ci)
  }

  Matrix dbSelect(String sqlQuery) throws SQLException {
    if (!sqlQuery.trim().toLowerCase().startsWith("select ")) {
      sqlQuery = "select $sqlQuery"
    }
    try(Connection con = dbConnect(); Statement stm = con.createStatement(); ResultSet rs = stm.executeQuery(sqlQuery)) {
      return Matrix.create(rs);
    }
  }

  int dbUpdate(String sqlQuery) throws SQLException  {
    try(Connection con = dbConnect(); Statement stm = con.createStatement()) {
      return dbExecuteUpdate(stm, sqlQuery)
    }
  }

  int dbUpdate(String tableName, Row row, String... matchColumnName) throws SQLException {
    String sql = dbCreateUpdateSql(tableName, row, matchColumnName)
    return dbUpdate(sql);
  }

  int dbUpdate(Matrix table, String... matchColumnName) throws SQLException {
    return dbExecuteBatchUpdate(table, dbConnect(), matchColumnName)
  }

  boolean dbTableExists(String tableName) throws SQLException {
    try(Connection con = dbConnect()) {
      return dbTableExists(con, tableName)
    }
  }

  boolean dbTableExists(Connection con, String tableName) throws SQLException {
    var rs = con.getMetaData().getTables(null, null, tableName.toUpperCase(), null);
    return rs.next()
  }

  /**
   * create table and insert the table data.
   *
   * @param connectionInfo the connection info defined in the Connections tab
   * @param table the table to copy to the db
   * @param primaryKey name(s) of the primary key columns
   */
  void dbCreate(Matrix table, String... primaryKey) throws SQLException {
    int i = 0
    List<Class<?>> types = table.columnTypes()
    Map<String,Map<String, Integer>> mappings = [:]
    int nrows = Math.max(100, table.rowCount());
    for (String name : table.columnNames()) {
      Map<String, Integer> props = [:]
      Class type = types.get(i++)
      if (BigDecimal == type) {
        Integer precision = 0
        Integer scale = 0
        for (int r = 0; r < nrows; r++) {
          BigDecimal val = table[r, name]
          if (val == null) {
            continue
          }
          if (val.precision() > precision) {
            precision = val.precision()
          }
          if (val.scale() > scale) {
            scale = val.scale()
          }
        }
        props.put(DECIMAL_PRECISION, precision)
        props.put(DECIMAL_SCALE, scale)
      } else if (type == String) {
        Integer maxLength = 0
        for (int r = 0; r < nrows; r++) {
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
    dbCreate(table, mappings, primaryKey)
  }

  /**
   * create table and insert the table data.
   *
   * @param table the table to copy to the db
   * @param props a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * constants as key and the size as value
   * @param primaryKey name(s) of the primary key columns
   */
  void dbCreate(Matrix table, Map<String, Map<String, Integer>> props, String... primaryKey) throws SQLException {

    var tableName = table.getName()
        .replaceAll("\\.", "_")
        .replaceAll("-", "_")
        .replaceAll("\\*", "")

    String sql = "create table $tableName (\n"

    List<String> columns = new ArrayList<>()
    int i = 0;
    List<Class<?>> types = table.columnTypes()
    for (String name : table.columnNames()) {
      Class type = types.get(i++)
      String column = "\"" + name + "\" " + mapper.sqlType(type, props[name])
      columns.add(column);
    }
    sql += String.join(",\n", columns)
    if (primaryKey.length > 0) {
      sql += "\n , CONSTRAINT pk_" + table.getName() + " PRIMARY KEY (\"" + String.join("\", \"", primaryKey) + "\")"
    }
    sql += "\n);"

    try(Connection con = dbConnect()
        Statement stm = con.createStatement()) {
      if (dbTableExists(con, tableName)) {
        throw new SQLException("Table $tableName already exists", "Cannot create $tableName since it already exists, no data copied to db")
      }
      println("Creating table using DDL: ${sql}");
      stm.execute(sql);
      dbInsert(con, table);
    }
  }

  Object dbDropTable(String tableName) {
    println "Dropping $tableName..."
    dbExecuteSql("drop table $tableName")
  }

  int dbInsert(String sqlQuery) throws SQLException, ExecutionException, InterruptedException {
    if (sqlQuery.trim().toLowerCase().startsWith("insert into ")) {
      return (int)dbExecuteSql(sqlQuery)
    } else {
      return (int)dbExecuteSql("insert into " + sqlQuery)
    }
  }

  int dbInsert(String tableName, Row row) throws SQLException, ExecutionException, InterruptedException {
    String sql = createInsertSql(tableName, row)
    println("Executing insert query: ${sql}")
    return dbInsert(ci, sql);
  }

  int dbInsert(Matrix table) throws SQLException {
    try(Connection con = dbConnect()) {
      return dbInsert(con, table)
    }
  }

  int dbInsert(Connection con, Matrix table) throws SQLException {
    try(Statement stm = con.createStatement()) {
      for (Row row : table) {
        String insertSql = createInsertSql(table.getName(), row)
        //println insertSql
        stm.addBatch(insertSql)
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  int dbExecuteUpdate(Statement stm, String sqlQuery) throws SQLException {
    if (sqlQuery.trim().toLowerCase().startsWith("update ")) {
      return stm.executeUpdate(sqlQuery)
    } else {
      return stm.executeUpdate("update $sqlQuery")
    }
  }

  String dbCreateUpdateSql(String tableName, Row row, String[] matchColumnName) {
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
    println("Executing update query: ${sql}")
    return sql;
  }

  private static String quoteIfString(Row row, String columnName) {
    def value = row[columnName]
    if (value instanceof CharSequence || value instanceof Character) {
      return "'" + value + "'"
    }
    return String.valueOf(value)
  }

  private int dbExecuteBatchUpdate(Matrix table, Connection connect, String[] matchColumnName) throws SQLException {
    try(Connection con = connect
        Statement stm = con.createStatement()) {
      for (Row row : table) {
        stm.addBatch(dbCreateUpdateSql(table.getName(), row, matchColumnName))
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  private Object dbExecuteSql(String sql) throws SQLException {
    try(Connection con = dbConnect()
        Statement stm = con.createStatement()) {
      boolean hasResultSet = stm.execute(sql)
      if (hasResultSet) {
        return Matrix.create(stm.getResultSet())
      } else {
        return stm.getUpdateCount()
      }
    }
  }

  Connection dbConnect() throws SQLException {
    String url = ci.getUrl().toLowerCase()
    if (isBlank(ci.getPassword()) && !url.contains("passw") && !url.contains("integratedsecurity=true")) {
      println("Password required to " + ci.getName() + " for " + ci.getUser())
    }
    return connect(ci)
  }

  boolean isBlank(String str) {
    if (str == null) {
      return true
    }
    return str.isBlank()
  }

  Connection connect(ConnectionInfo ci) throws SQLException, IOException {
    println("Connecting to ${ci.getUrl()} using ${ci.getDependency()}")

    Driver driver

    MavenUtils mvnUtils = new MavenUtils()
    String[] dep = ci.getDependency().split(':')
    println("Resolving dependency ${ci.getDependency()}")
    File jar = mvnUtils.resolveArtifact(dep[0], dep[1], null, 'jar', dep[2])
    URL url = jar.toURI().toURL()
    URL[] urls = new URL[]{url}
    println("Dependency url is ${urls[0]}")

    GroovyClassLoader cl
    if (this.class.getClassLoader() instanceof GroovyClassLoader) {
      cl = this.class.getClassLoader() as GroovyClassLoader
    } else {
      cl = new GroovyClassLoader()
    }

    if (Arrays.stream(cl.getURLs()).noneMatch(p -> p.equals(url))) {
      cl.addURL(url)
    }

    try {
      println("Attempting to load the class ${ci.getDriver()}")
      Class<Driver> clazz = (Class<Driver>) cl.loadClass(ci.getDriver())
      println("Loaded driver from session classloader, instating the driver ${ci.getDriver()}")
      try {
        driver = clazz.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NullPointerException e) {
        println("Failed to instantiate the driver: ${ci.getDriver()}, clazz is ${clazz}: " + e)
        throw e
      }
    } catch (ClassCastException | ClassNotFoundException e) {
      println("Failed to load driver ${ci.getDriver()} could not be loaded from dependency ${ci.getDependency()}")
      throw e
    }
    Properties props = new Properties()
    if ( urlContainsLogin(ci.getUrlSafe()) ) {
      println("Skipping specified user/password since it is part of the url")
    } else {
      if (ci.getUser() != null) {
        props.put("user", ci.getUser())
        if (ci.getPassword() != null) {
          props.put("password", ci.getPassword())
        }
      }
    }
    return driver.connect(ci.getUrl(), props);
  }

  boolean urlContainsLogin(String url) {
    String safeLcUrl = url.toLowerCase()
    return ( safeLcUrl.contains("user") && safeLcUrl.contains("pass") ) || safeLcUrl.contains("@")
  }

  private String createInsertSql(String tableName, Row row) {
    String sql = "insert into " + tableName + " ( "
    List<String> columnNames = row.columnNames()

    sql += "\"" + String.join("\", \"", columnNames) + "\""
    sql += " ) values ( "

    List<String> values = new ArrayList<>()
    columnNames.forEach(n -> {
      values.add(quoteIfString(row, n))
    });
    sql += String.join(", ", values)
    sql += " ); "
    return sql
  }
}
