package se.alipsa.matrix.sql

import groovy.transform.CompileStatic
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.mavenutils.MavenUtils

import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.Driver
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.ExecutionException
import java.util.stream.IntStream

@CompileStatic
class MatrixSql implements Closeable {

  private ConnectionInfo ci
  private SqlTypeMapper mapper
  private MatrixDbUtil matrixDbUtil
  private Connection con

  MatrixSql(ConnectionInfo ci) {
    check(ci)
    this.ci = ci
    mapper = SqlTypeMapper.create(ci)
    matrixDbUtil = new MatrixDbUtil(mapper)
  }

  MatrixSql(ConnectionInfo ci, DataBaseProvider dbProvider) {
    check(ci)
    this.ci = ci
    mapper = SqlTypeMapper.create(dbProvider)
    matrixDbUtil = new MatrixDbUtil(mapper)
  }

  @Override
  void close() throws SQLException {
    if (con != null) {
      con.close()
    }
    con == null
  }

  Matrix select(String sqlQuery, String matrixName = 'myMatrix') throws SQLException {
    matrixDbUtil.select(connect(), sqlQuery).withMatrixName(matrixName)
  }

  int update(String sqlQuery) {
    if (!sqlQuery.trim().toLowerCase().startsWith('update ')) {
      sqlQuery = "update $sqlQuery"
    }
    dbUpdate(sqlQuery)
  }

  private int dbUpdate(String sqlQuery) throws SQLException  {
    try(Statement stm = connect().createStatement()) {
      return dbExecuteUpdate(stm, sqlQuery)
    }
  }

  int update(String tableName, Row row, String... matchColumnName) throws SQLException {
    String sql = SqlGenerator.createUpdateSql(tableName, row, matchColumnName)
    dbUpdate(sql)
  }

  int update(Matrix table, String... matchColumnName) throws SQLException {
    dbExecuteBatchUpdate(table, matchColumnName)
  }

  boolean tableExists(String tableName) throws SQLException {
    return matrixDbUtil.tableExists(connect(), tableName)
  }

  boolean tableExists(Matrix table) throws SQLException {
    tableExists(tableName(table))
  }

  Set<String> getTableNames() throws SQLException {
    matrixDbUtil.getTableNames(connect())
  }

  Object executeQuery(String sql) throws SQLException {
    matrixDbUtil.dbExecuteSql(connect(), sql)
  }


  /**
   * create a table corresponding to the Matrix and insert the matrix data.
   *
   * @param table the Matrix to copy to the db
   * @param scanNumRows the number of rows to scan to obtain sizing info
   * @param primaryKey the primary keys (if any)
   * @return A Map with the following keys:
   <ul>
   <li>sql - the ddl to create the table</li>
   <li>ddlResult - the result of the ddl query</li>
   <li>inserted - the number of rows inserted</li>
   </ul>
   * @throws SQLException
   */
  Map create(Matrix table, int scanNumRows, String... primaryKey) throws SQLException {
    matrixDbUtil.create(connect(), table, scanNumRows, primaryKey)
  }

  Map create(String tableName, Matrix table, int scanNumRows, String... primaryKey) throws SQLException {
    matrixDbUtil.create(tableName, connect(), table, scanNumRows, primaryKey)
  }

  /**
   * create a table corresponding to the Matrix and insert the matrix data.
   *
   * @param connectionInfo the connection info defined in the Connections tab
   * @param table the table to copy to the db
   * @param primaryKey name(s) of the primary key columns
   * @return A Map with the following keys:
   <ul>
   <li>sql - the ddl to create the table</li>
   <li>ddlResult - the result of the ddl query</li>
   <li>inserted - the number of rows inserted</li>
   </ul>
   */
  Map create(Matrix table, String... primaryKey) throws SQLException {
    create(table, Math.max(100, table.rowCount()), primaryKey)
  }

  Map create(String tableName, Matrix table, String... primaryKey) throws SQLException {
    create(tableName, table, Math.max(100, table.rowCount()), primaryKey)
  }

  /**
   * create table and insert the table data.
   *
   * @param table the table to copy to the db
   * @param props a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * constants as key and the size as value
   * @param primaryKey name(s) of the primary key columns
   * @returns A Map with the following keys:
   <ul>
      <li>sql - the ddl to create the table</li>
      <li>ddlResult - the result of the ddl query</li>
      <li>inserted - the number of rows inserted</li>
   </ul>
   */
  Map create(Matrix table, Map<String, Map<String, Integer>> props, String... primaryKey) throws SQLException {
    matrixDbUtil.create(connect(), table, props, primaryKey)
  }

  String createDdl(Matrix table, int... scanNumrows) {
    Map mappings = matrixDbUtil.createMappings(table, scanNumrows.length > 0 ? scanNumrows[0] : Math.max(100, table.rowCount()))
    matrixDbUtil.createTableDdl(tableName(table), table, mappings)
  }

  static String tableName(Matrix table) {
    MatrixDbUtil.tableName(table)
  }

  Object dropTable(String tableName) {
    dbExecuteSql("drop table $tableName")
  }

  Object dropTable(Matrix table) {
    dropTable(tableName(table))
  }

  private int dbInsert(String sqlQuery) throws SQLException, ExecutionException, InterruptedException {
    if (sqlQuery.trim().toLowerCase().startsWith("insert into ")) {
      return (int)dbExecuteSql(sqlQuery)
    } else {
      return (int)dbExecuteSql("insert into " + sqlQuery)
    }
  }

  int insert(String tableName, Row row) throws SQLException, ExecutionException, InterruptedException {
    String sql = SqlGenerator.createPreparedInsertSql(tableName, row)
    try(PreparedStatement stm = connect().prepareStatement(sql)) {
      int i = 1
      row.each {
        stm.setObject(i++, it)
      }
      return stm.executeUpdate()
    }
  }

  int insert(Matrix table, Row row) throws SQLException, ExecutionException, InterruptedException {
    insert(tableName(table), row)
  }

  int insert(Matrix table) throws SQLException {
    return matrixDbUtil.insert(connect(), table)
  }

  int delete(String sql) throws SQLException {
    if (sql.trim().toLowerCase().startsWith("delete ")) {
      sql = "delete $sql"
    }
    if (!sql.toLowerCase().startsWith("delete from ")) {
      sql = "from $sql"
    }
    try(Statement stm = connect().createStatement()) {
      return stm.executeUpdate(sql)
    }
  }

  static int dbExecuteUpdate(Statement stm, String sqlQuery) throws SQLException {
    if (sqlQuery.trim().toLowerCase().startsWith("update ")) {
      return stm.executeUpdate(sqlQuery)
    } else {
      return stm.executeUpdate("update $sqlQuery")
    }
  }

  private int dbExecuteBatchUpdate(Matrix table, String[] matchColumnName) throws SQLException {
    try(Statement stm = connect().createStatement()) {
      for (Row row : table) {
        stm.addBatch(SqlGenerator.createUpdateSql(table.getMatrixName(), row, matchColumnName))
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  private Object dbExecuteSql(String sql) throws SQLException {
      matrixDbUtil.dbExecuteSql(connect(), sql)
  }

  synchronized Connection connect() throws SQLException {
    if (con == null) {
      String url = ci.getUrl().toLowerCase()
      if (!url.contains(':h2:') && !url.contains(':derby:')
          && isBlank(ci.getPassword()) && !url.contains("passw")
          && !url.contains("integratedsecurity=true")) {
        System.err.println("Password probably required to " + ci.getName() + " for " + ci.getUser())
      }
      con = dbConnect(ci)
    }
    con
  }

  static boolean isBlank(String str) {
    if (str == null) {
      return true
    }
    return str.isBlank()
  }

  Connection dbConnect(ConnectionInfo ci) throws SQLException, IOException {
    Driver driver
    MavenUtils mvnUtils = new MavenUtils()
    GroovyClassLoader cl
    if (this.class.getClassLoader() instanceof GroovyClassLoader) {
      cl = this.class.getClassLoader() as GroovyClassLoader
    } else {
      cl = new GroovyClassLoader()
    }
    String dependency = ci.getDependency()
    List<String> dependencies = []
    if (dependency.contains(';')) {
      dependency.split(';').each {
        dependencies << it
      }
    } else {
      dependencies << dependency
    }
    dependencies.each { String d ->
      String[] dep = d.split(':')
      File jar = mvnUtils.resolveArtifact(dep[0], dep[1], null, 'jar', dep[2])
      URL url = jar.toURI().toURL()
      addToClassloader(cl, url)
    }

    try {
      Class<Driver> clazz = (Class<Driver>) cl.loadClass(ci.getDriver())
      try {
        driver = clazz.getDeclaredConstructor().newInstance()
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NullPointerException e) {
        System.err.println("Failed to instantiate the driver: ${ci.getDriver()}, clazz is ${clazz}: " + e)
        throw e
      }
    } catch (ClassCastException | ClassNotFoundException e) {
      System.err.println("Failed to load driver; ${ci.getDriver()} could not be loaded from dependency ${ci.getDependency()}")
      throw e
    }
    Properties props = new Properties()
    if ( urlContainsLogin(ci.getUrlSafe()) ) {
    } else {
      if (ci.getUser() != null) {
        props.put("user", ci.getUser())
        if (ci.getPassword() != null) {
          props.put("password", ci.getPassword())
        }
      }
    }
    return driver.connect(ci.getUrl(), props)
  }

  private static void addToClassloader(GroovyClassLoader cl, URL url) {
    if (Arrays.stream(cl.getURLs()).noneMatch(p -> p == url)) {
      cl.addURL(url)
    }
  }

  private static boolean urlContainsLogin(String url) {
    String safeLcUrl = url.toLowerCase()
    return ( safeLcUrl.contains("user") && safeLcUrl.contains("pass") ) || safeLcUrl.contains("@")
  }

  static void check(ConnectionInfo connectionInfo) {
    if (connectionInfo.getDependency() == null || connectionInfo.getDependency().isBlank()) {
      throw new IllegalArgumentException("Dependency is required")
    }
  }

  ConnectionInfo getConnectionInfo() {
    return ci
  }

  SqlTypeMapper getSqlTypeMapper() {
    return mapper
  }

  MatrixDbUtil getMatrixDbUtil() {
    return matrixDbUtil
  }

  Connection getConnection() {
    return con
  }
}
