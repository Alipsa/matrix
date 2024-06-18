package se.alipsa.groovy.matrix.sql

import javafx.application.Platform
import javafx.scene.control.Alert
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row
import se.alipsa.mavenutils.MavenUtils

import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.Driver
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ExecutionException
import java.util.stream.IntStream

class MatrixSql {

  static Matrix dbSelect(ConnectionInfo ci, String sqlQuery) throws SQLException {
    if (!sqlQuery.trim().toLowerCase().startsWith("select ")) {
      sqlQuery = "select $sqlQuery"
    }
    try(Connection con = dbConnect(ci); Statement stm = con.createStatement(); ResultSet rs = stm.executeQuery(sqlQuery)) {
      return Matrix.create(rs);
    }
  }

  static int dbUpdate(ConnectionInfo ci, String sqlQuery) throws SQLException  {
    try(Connection con = dbConnect(ci); Statement stm = con.createStatement()) {
      return dbExecuteUpdate(stm, sqlQuery);
    }
  }

  static int dbUpdate(ConnectionInfo ci, String tableName, Row row, String... matchColumnName) throws SQLException {
    String sql = dbCreateUpdateSql(tableName, row, matchColumnName);
    return dbUpdate(ci, sql);
  }

  static int dbUpdate(ConnectionInfo ci, Matrix table, String... matchColumnName) throws SQLException {
    return dbExecuteBatchUpdate(table, dbConnect(ci), matchColumnName);
  }

  static boolean dbTableExists(ConnectionInfo connectionInfo, String tableName) throws SQLException {
    try(Connection con = dbConnect(connectionInfo)) {
      return dbTableExists(con, tableName);
    }
  }

  static boolean dbTableExists(Connection con, String tableName) throws SQLException {
    var rs = con.getMetaData().getTables(null, null, tableName.toUpperCase(), null);
    return rs.next();
  }

  /**
   * create table and insert the table data.
   *
   * @param connectionInfo the connection info defined in the Connections tab
   * @param table the table to copy to the db
   * @param primaryKey name(s) of the primary key columns
   */
  static void dbCreate(ConnectionInfo connectionInfo, Matrix table, String... primaryKey) throws SQLException {

    var tableName = table.getName()
        .replaceAll("\\.", "_")
        .replaceAll("-", "_")
        .replaceAll("\\*", "");


    String sql = "create table $tableName (\n";

    List<String> columns = new ArrayList<>();
    int i = 0;
    List<Class<?>> types = table.columnTypes();
    for (String name : table.columnNames()) {
      String column = "\"" + name + "\" " + sqlType(types.get(i++), connectionInfo)
      columns.add(column);
    }
    sql += String.join(",\n", columns);
    if (primaryKey.length > 0) {
      sql += "\n , CONSTRAINT pk_" + table.getName() + " PRIMARY KEY (\"" + String.join("\", \"", primaryKey) + "\")";
    }
    sql += "\n);";

    try(Connection con = dbConnect(connectionInfo);
        Statement stm = con.createStatement()) {
      if (dbTableExists(con, tableName)) {
        throw new SQLException("Table " + tableName + " already exists", "Cannot create " + tableName + " since it already exists, no data copied to db")
      }
      println("Creating table using DDL: ${sql}");
      stm.execute(sql);
      dbInsert(con, table);
    }
  }

  static dbDropTable(ConnectionInfo ci, String tableName) {
    println "Dropping $tableName..."
    dbExecuteSql(ci, "drop table $tableName")
  }

  static int dbInsert(ConnectionInfo ci, String sqlQuery) throws SQLException, ExecutionException, InterruptedException {
    if (sqlQuery.trim().toLowerCase().startsWith("insert into ")) {
      return (int)dbExecuteSql(ci, sqlQuery);
    } else {
      return (int)dbExecuteSql(ci, "insert into " + sqlQuery);
    }
  }

  static int dbInsert(ConnectionInfo ci, String tableName, Row row) throws SQLException, ExecutionException, InterruptedException {
    String sql = createInsertSql(tableName, row);
    println("Executing insert query: ${sql}")
    return dbInsert(ci, sql);
  }

  static int dbInsert(ConnectionInfo ci, Matrix table) throws SQLException {
    try(Connection con = dbConnect(ci)) {
      return dbInsert(con, table);
    }
  }

  static int dbInsert(Connection con, Matrix table) throws SQLException {
    try(Statement stm = con.createStatement()) {
      for (Row row : table) {
        String insertSql = createInsertSql(table.getName(), row)
        //println insertSql
        stm.addBatch(insertSql)
      }
      int[] results = stm.executeBatch();
      return IntStream.of(results).sum();
    }
  }

  private static int dbExecuteUpdate(Statement stm, String sqlQuery) throws SQLException {
    if (sqlQuery.trim().toLowerCase().startsWith("update ")) {
      return stm.executeUpdate(sqlQuery);
    } else {
      return stm.executeUpdate("update $sqlQuery");
    }
  }

  private static String dbCreateUpdateSql(String tableName, Row row, String[] matchColumnName) {
    String sql = "update " + tableName + " set ";
    List<String> columnNames = new ArrayList<>(row.columnNames());
    columnNames.removeAll(List.of(matchColumnName));
    List<String> setValues = new ArrayList<>();
    columnNames.forEach(n -> setValues.add(n + " = " + quoteIfString(row, n)));
    sql += String.join(", ", setValues);
    sql += " where ";
    List<String> conditions = new ArrayList<>();
    for (String condition : matchColumnName) {
      conditions.add(condition + " = " + quoteIfString(row, condition));
    }
    sql += String.join(" and ", conditions);
    println("Executing update query: ${sql}");
    return sql;
  }

  private static String quoteIfString(Row row, String columnName) {
    def value = row[columnName];
    if (value instanceof CharSequence || value instanceof Character) {
      return "'" + value + "'";
    }
    return String.valueOf(value);
  }

  private static int dbExecuteBatchUpdate(Matrix table, Connection connect, String[] matchColumnName) throws SQLException {
    try(Connection con = connect;
        Statement stm = con.createStatement()) {
      for (Row row : table) {
        stm.addBatch(dbCreateUpdateSql(table.getName(), row, matchColumnName));
      }
      int[] results = stm.executeBatch();
      return IntStream.of(results).sum();
    }
  }

  /**
   *
   * @param connectionName the name of the connection defined in the connection tab
   * @param sql the sql string to execute
   * @return if the sql returns a result set, a Table containing the data is returned, else the number of rows affected is returned
   * @throws SQLException if there is something wrong with the sql
   */
  private static Object dbExecuteSql(String connectionName, String sql) throws SQLException {
    try(Connection con = dbConnect(connectionName);
        Statement stm = con.createStatement()) {
      boolean hasResultSet = stm.execute(sql);
      if (hasResultSet) {
        return Matrix.create(stm.getResultSet());
      } else {
        return stm.getUpdateCount();
      }
    }
  }

  private static Object dbExecuteSql(ConnectionInfo ci, String sql) throws SQLException {
    try(Connection con = dbConnect(ci);
        Statement stm = con.createStatement()) {
      boolean hasResultSet = stm.execute(sql);
      if (hasResultSet) {
        return Matrix.create(stm.getResultSet());
      } else {
        return stm.getUpdateCount();
      }
    }
  }

  private static Connection dbConnect(ConnectionInfo ci) throws SQLException {
    String url = ci.getUrl().toLowerCase();
    if (isBlank(ci.getPassword()) && !url.contains("passw") && !url.contains("integratedsecurity=true")) {
      println("Password required to " + ci.getName() + " for " + ci.getUser());
    }
    return connect(ci);
  }

  static boolean isBlank(String str) {
    if (str == null) {
      return true
    }
    return str.isBlank()
  }

  static Connection connect(ConnectionInfo ci) throws SQLException, IOException {
    println("Connecting to ${ci.getUrl()} using ${ci.getDependency()}")

    Driver driver

    MavenUtils mvnUtils = new MavenUtils()
    String[] dep = ci.getDependency().split(':')
    println("Resolving dependency ${ci.getDependency()}");
    File jar = mvnUtils.resolveArtifact(dep[0], dep[1], null, 'jar', dep[2])
    URL url = jar.toURI().toURL();
    URL[] urls = new URL[]{url};
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
      Class<Driver> clazz = (Class<Driver>) cl.loadClass(ci.getDriver());
      println("Loaded driver from session classloader, instating the driver ${ci.getDriver()}");
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

  static boolean urlContainsLogin(String url) {
    String safeLcUrl = url.toLowerCase();
    return ( safeLcUrl.contains("user") && safeLcUrl.contains("pass") ) || safeLcUrl.contains("@");
  }

  private static String createInsertSql(String tableName, Row row) {
    String sql = "insert into " + tableName + " ( ";
    List<String> columnNames = row.columnNames();

    sql += "\"" + String.join("\", \"", columnNames) + "\"";
    sql += " ) values ( ";

    List<String> values = new ArrayList<>();
    columnNames.forEach(n -> {
      values.add(quoteIfString(row, n));
    });
    sql += String.join(", ", values);
    sql += " ); ";
    return sql;
  }

  static String sqlType(Class columnType, ConnectionInfo ci, int... varcharSize) {
    if (BigDecimal == columnType) {
      DataBase db = DataBase.fromUrl(ci.urlSafe)
      if (db == DataBase.H2) {
        return "NUMBER"
      }
      // TODO: need precision here
      return "DECIMAL"
    }
    if (Boolean == columnType) {
      return "BIT"
    }
    if (Byte == columnType) {
      return "TINYINT"
    }
    if (Character == columnType) {
      return "CHAR"
    }
    if (Double == columnType) {
      return "DOUBLE"
    }
    if (Float == columnType) {
      return "REAL"
    }
    if (Instant == columnType) {
      return "TIMESTAMP"
    }
    if (Integer == columnType) {
      return "INTEGER"
    }
    if (LocalDate == columnType) {
      return "DATE"
    }
    if(LocalTime == columnType) {
      return "TIME"
    }
    if (LocalDateTime == columnType) {
      return "TIMESTAMP"
    }
    if (Long == columnType) {
      return "BIGINT"
    }
    if (Short == columnType) {
      return "SMALLINT"
    }
    if (String == columnType) {
      return "VARCHAR(" + (varcharSize.length > 0 ? varcharSize[0] : 8000) + ")"
    }
    if (Time == columnType) {
      return "TIME"
    }
    if (Timestamp == columnType) {
      return "TIMESTAMP"
    }
    println("No match for $columnType, returning blob")
    return "BLOB"
  }
}
