package se.alipsa.groovy.matrix.sql

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row
import se.alipsa.mavenutils.MavenUtils

import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.Driver
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.ExecutionException
import java.util.stream.IntStream

class MatrixSql implements Closeable {

  private static final Logger LOG = LogManager.getLogger(MatrixSql.class)

  ConnectionInfo ci
  SqlTypeMapper mapper
  MatrixDbUtil matrixDbUtil
  Connection con

  MatrixSql(ConnectionInfo ci) {
    this.ci = ci
    mapper = SqlTypeMapper.create(ci)
    matrixDbUtil = new MatrixDbUtil(mapper)
  }

  MatrixSql(ConnectionInfo ci, DataBaseProvider dbProvider) {
    this.ci = ci
    mapper = SqlTypeMapper.create(dbProvider)
    matrixDbUtil = new MatrixDbUtil(mapper)
  }

  @Override
  void close() throws SQLException {
    con.close()
    con == null
  }

  Matrix select(String sqlQuery) throws SQLException {
    matrixDbUtil.select(connect(), sqlQuery)
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


  void create(Matrix table, int scanNumRows, String... primaryKey) throws SQLException {
    matrixDbUtil.create(connect(), table, scanNumRows, primaryKey)
  }

  /**
   * create table and insert the table data.
   *
   * @param connectionInfo the connection info defined in the Connections tab
   * @param table the table to copy to the db
   * @param primaryKey name(s) of the primary key columns
   */
  void create(Matrix table, String... primaryKey) throws SQLException {
    create(table, Math.max(100, table.rowCount()), primaryKey)
  }

  /**
   * create table and insert the table data.
   *
   * @param table the table to copy to the db
   * @param props a map containing the column name and a map containing sizing information using the SqlTypeMapper
   * constants as key and the size as value
   * @param primaryKey name(s) of the primary key columns
   */
  void create(Matrix table, Map<String, Map<String, Integer>> props, String... primaryKey) throws SQLException {
    matrixDbUtil.create(connect(), table, props, primaryKey)
  }

  static String tableName(Matrix table) {
    MatrixDbUtil.tableName(table)
  }

  Object dropTable(String tableName) {
    LOG.debug("Dropping {}...", tableName)
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
    LOG.debug("Executing insert query: {}", sql)
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
        stm.addBatch(SqlGenerator.createUpdateSql(table.getName(), row, matchColumnName))
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
      if (isBlank(ci.getPassword()) && !url.contains("passw") && !url.contains("integratedsecurity=true")) {
        LOG.warn("Password probably required to " + ci.getName() + " for " + ci.getUser())
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
    LOG.debug("Connecting to ${ci.getUrl()} using ${ci.getDependency()}")

    Driver driver

    MavenUtils mvnUtils = new MavenUtils()
    String[] dep = ci.getDependency().split(':')
    LOG.trace("Resolving dependency ${ci.getDependency()}")
    File jar = mvnUtils.resolveArtifact(dep[0], dep[1], null, 'jar', dep[2])
    URL url = jar.toURI().toURL()

    GroovyClassLoader cl
    if (this.class.getClassLoader() instanceof GroovyClassLoader) {
      cl = this.class.getClassLoader() as GroovyClassLoader
    } else {
      cl = new GroovyClassLoader()
    }

    if (Arrays.stream(cl.getURLs()).noneMatch(p -> p == url)) {
      cl.addURL(url)
    }

    try {
      LOG.trace("Attempting to load the class ${ci.getDriver()}")
      Class<Driver> clazz = (Class<Driver>) cl.loadClass(ci.getDriver())
      LOG.trace("Loaded driver from session classloader, instating the driver ${ci.getDriver()}")
      try {
        driver = clazz.getDeclaredConstructor().newInstance()
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NullPointerException e) {
        LOG.trace("Failed to instantiate the driver: ${ci.getDriver()}, clazz is ${clazz}: " + e)
        throw e
      }
    } catch (ClassCastException | ClassNotFoundException e) {
      LOG.warn("Failed to load driver; ${ci.getDriver()} could not be loaded from dependency ${ci.getDependency()}")
      throw e
    }
    Properties props = new Properties()
    if ( urlContainsLogin(ci.getUrlSafe()) ) {
      LOG.debug("Skipping specified user/password since it is part of the url")
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

  private static boolean urlContainsLogin(String url) {
    String safeLcUrl = url.toLowerCase()
    return ( safeLcUrl.contains("user") && safeLcUrl.contains("pass") ) || safeLcUrl.contains("@")
  }


}
