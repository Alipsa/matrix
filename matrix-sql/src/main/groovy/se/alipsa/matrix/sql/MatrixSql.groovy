package se.alipsa.matrix.sql

import groovy.transform.CompileStatic

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.sql.config.JaasConfigLoader
import se.alipsa.mavenutils.MavenUtils

import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.Driver
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.ExecutionException
import java.util.stream.IntStream

/**
 * Bridges Matrix and SQL allowing you to go back and forth between the two.
 * Note: use the MatrixSqlFactory for more flexible ways to create a MatrixSql.
 */
@CompileStatic
class MatrixSql implements Closeable {

  private static final Logger log = Logger.getLogger(MatrixSql)

  private ConnectionInfo ci
  private SqlTypeMapper mapper
  private MatrixDbUtil matrixDbUtil
  private Connection con
  private boolean closeConnectionOnClose = true

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

  /**
   * Create a MatrixSql instance that wraps an externally managed connection.
   * The connection will NOT be closed when {@link #close()} is called.
   *
   * @param connection the externally managed database connection
   * @param dbProvider the database provider used for type mapping
   */
  MatrixSql(Connection connection, DataBaseProvider dbProvider) {
    this.con = connection
    this.mapper = SqlTypeMapper.create(dbProvider)
    this.matrixDbUtil = new MatrixDbUtil(mapper)
    this.closeConnectionOnClose = false
  }

  /**
   * Create a MatrixSql instance that wraps an externally managed connection.
   * The connection will NOT be closed when {@link #close()} is called.
   *
   * @param connection the externally managed database connection
   * @param mapper the SQL type mapper to use
   */
  MatrixSql(Connection connection, SqlTypeMapper mapper) {
    this.con = connection
    this.mapper = mapper
    this.matrixDbUtil = new MatrixDbUtil(mapper)
    this.closeConnectionOnClose = false
  }

  @Override
  void close() throws SQLException {
    if (closeConnectionOnClose && con != null) {
      con.close()
    }
    con = null
  }

  Matrix select(String sqlQuery, String matrixName = 'myMatrix') throws SQLException {
    matrixDbUtil.select(connect(), sqlQuery).withMatrixName(matrixName)
  }

  /**
   * Execute a prepared select query and return the result as a Matrix.
   *
   * @param sqlQuery the sql query to execute, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement
   * @param matrixName the name of the resulting Matrix
   * @return a Matrix containing the query results
   * @throws SQLException if a database access error occurs
   */
  Matrix select(String sqlQuery, List<Object> params, String matrixName = 'myMatrix') throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sqlQuery)) {
      int i = 1
      params.each {
        stm.setObject(i++, it)
      }
      try (ResultSet rs = stm.executeQuery()) {
        return Matrix.builder().data(rs).build().withMatrixName(matrixName)
      }
    }
  }

  int update(String sqlQuery) {
    dbUpdate(sqlQuery)
  }

  /**
   * Execute a prepared update query.
   *
   * @param sqlQuery the sql query to execute, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement
   * @return the number of rows affected
   * @throws SQLException if a database access error occurs
   */
  int update(String sqlQuery, List<Object> params) throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sqlQuery)) {
      int i = 1
      params.each {
        stm.setObject(i++, it)
      }
      return stm.executeUpdate()
    }
  }

  private int dbUpdate(String sqlQuery) throws SQLException  {
    try(Statement stm = connect().createStatement()) {
      return dbExecuteUpdate(stm, sqlQuery)
    }
  }

  /**
   * Update a row in the given table. This overload delegates to
   * {@link #update(String, Row, String...)} with an empty matchColumnName array,
   * which will throw {@link IllegalArgumentException} because match columns are required.
   *
   * @param tableName the name of the table to update
   * @param row the row data to update
   * @throws SQLException if a database access error occurs
   * @throws IllegalArgumentException because matchColumnName is required
   */
  int update(String tableName, Row row) throws SQLException {
    update(tableName, row, new String[0])
  }

  int update(String tableName, Row row, String... matchColumnName) throws SQLException {
    SqlGenerator.PreparedUpdate prepared = SqlGenerator.createPreparedUpdate(tableName, row, matchColumnName)
    try(PreparedStatement stm = connect().prepareStatement(prepared.sql)) {
      int i = 1
      prepared.values.each {
        stm.setObject(i++, it)
      }
      return stm.executeUpdate()
    }
  }

  int update(Matrix table, String... matchColumnName) throws SQLException {
    dbExecuteBatchUpdate(table, matchColumnName)
  }

  /**
   * Execute the given sql query. The result can be one or more ResultSets
   * or update counts. The key is the result index (0..n) and the value is either a Matrix
   * (for ResultSets) or an Integer (for update counts). DDL statements (like CREATE TABLE)
   * will yield an update count of -1.
   *
   * @param sqlQuery the sql query to execute
   * @return A Map with the result index as key and either a Matrix or an Integer as value
   * @throws SQLException if a database access error occurs
   */
  Map<Integer, Object> execute(String sqlQuery) throws SQLException {
    try(Statement stm = connect().createStatement()) {
      return dbExecute(stm, sqlQuery)
    }
  }

  /**
   * Execute a prepared sql query. The result can be one or more ResultSets
   * or update counts. The key is the result index (0..n) and the value is either a Matrix
   * (for ResultSets) or an Integer (for update counts). DDL statements (like CREATE TABLE)
   * will yield an update count of -1.
   *
   * @param sqlQuery the sql query to execute, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement
   * @return A Map with the result index as key and either a Matrix or an Integer as value
   * @throws SQLException if a database access error occurs
   */
  Map<Integer, Object> execute(String sqlQuery, List<Object> params) throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sqlQuery)) {
      int i = 1
      params.each {
        stm.setObject(i++, it)
      }
      return dbExecute(stm)
    }
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
  Map create(Matrix table, int scanNumRows, boolean addQuotes = true, String... primaryKey) throws SQLException {
    matrixDbUtil.create(connect(), table, scanNumRows, addQuotes, primaryKey)
  }

  /**
   * create a table corresponding to the Matrix and insert the matrix data.
   *
   * @param tableName the name of the table to create
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
  Map create(String tableName, Matrix table, int scanNumRows, boolean addQuotes = true, String... primaryKey) throws SQLException {
    matrixDbUtil.create(tableName, connect(), table, scanNumRows, addQuotes, primaryKey)
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

  /**
   * create a table corresponding to the Matrix and insert the matrix data.
   *
   * @param tableName the name of the table to create
   * @param table the table to copy to the db
   * @param primaryKey name(s) of the primary key columns
   * @return A Map with the following keys:
   <ul>
   <li>sql - the ddl to create the table</li>
   <li>ddlResult - the result of the ddl query</li>
   <li>inserted - the number of rows inserted</li>
   </ul>
   */
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

  String createDdl(Matrix table, boolean addQuotes = true, int... scanNumrows) {
    Map mappings = matrixDbUtil.createMappings(table, scanNumrows.length > 0 ? scanNumrows[0] : Math.max(100, table.rowCount()))
    matrixDbUtil.createTableDdl(tableName(table), table, mappings, addQuotes)
  }

  static String tableName(Matrix table) {
    MatrixDbUtil.tableName(table)
  }

  Object dropTable(String tableName) {
    dbExecuteSql("drop table ${SqlIdentifier.renderTable(tableName)}")
  }

  Object dropTable(Matrix table) {
    dropTable(tableName(table))
  }

  private int dbInsert(String sqlQuery) throws SQLException, ExecutionException, InterruptedException {
    return (int)dbExecuteSql(sqlQuery)
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

  /**
   * Insert the data from the given Matrix into the given table in the database.
   *
   * @param tableName the name of the table to insert into
   * @param table the Matrix containing the data to insert
   * @return the number of inserted rows
   * @throws SQLException if any sql error occurs
   */
  int insert(String tableName, Matrix table) throws SQLException {
    matrixDbUtil.insert(connect(), tableName, table)
  }

  int insert(Matrix table) throws SQLException {
    return matrixDbUtil.insert(connect(), table)
  }

  int delete(String sql) throws SQLException {
    try(Statement stm = connect().createStatement()) {
      return stm.executeUpdate(sql)
    }
  }

  /**
   * Execute a prepared delete query.
   *
   * @param sql the sql delete statement, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement
   * @return the number of rows deleted
   * @throws SQLException if a database access error occurs
   */
  int delete(String sql, List<Object> params) throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sql)) {
      int i = 1
      params.each {
        stm.setObject(i++, it)
      }
      return stm.executeUpdate()
    }
  }

  static Map<Integer, Object> dbExecute(Statement stm, String sqlQuery) throws SQLException {
    return dbExecuteResults(stm, stm.execute(sqlQuery))
  }

  static Map<Integer, Object> dbExecute(PreparedStatement stm) throws SQLException {
    return dbExecuteResults(stm, stm.execute())
  }

  private static Map<Integer, Object> dbExecuteResults(Statement stm, boolean initialIsResultSet) throws SQLException {
    Map<Integer, Object> allResults = [:]
    int index = 0

    boolean isResultSet = initialIsResultSet
    do {
      if (isResultSet) {
        try (ResultSet rs = stm.getResultSet()) {
          Matrix matrix = Matrix.builder().data(rs).build()
          allResults[index] = matrix
        }
      } else {
        // --- It's an UPDATE, INSERT, DELETE, or DDL result ---
        int updateCount = stm.getUpdateCount()

        // updateCount == -1 means no more results or it was DDL (like CREATE TABLE)
        // updateCount >= 0 means it was an UPDATE/INSERT/DELETE
        allResults[index] = updateCount
      }
      index++
      isResultSet = stm.getMoreResults()

      // 4. Continue looping as long as there is another result (either a ResultSet
      //    or an update count other than -1)
    } while (isResultSet || stm.getUpdateCount() != -1)

    return allResults
  }

  static int dbExecuteUpdate(Statement stm, String sqlQuery) throws SQLException {
    return stm.executeUpdate(sqlQuery)
  }

  private int dbExecuteBatchUpdate(Matrix table, String[] matchColumnName) throws SQLException {
    if (matchColumnName == null || matchColumnName.length == 0) {
      throw new IllegalArgumentException("matchColumnName is required")
    }
    if (table.rowCount() == 0) {
      return 0
    }
    List<String> matchColumns = matchColumnName.toList()
    List<String> updateColumns = SqlGenerator.updateColumnNames(table.columnNames(), matchColumns)
    if (updateColumns.isEmpty()) {
      throw new IllegalArgumentException("No columns left to update after excluding match columns")
    }
    String sql = SqlGenerator.createPreparedUpdateSql(tableName(table), updateColumns, matchColumns)
    try(PreparedStatement stm = connect().prepareStatement(sql)) {
      for (Row row : table) {
        List<Object> values = SqlGenerator.updateValues(row, updateColumns, matchColumns)
        int i = 1
        values.each {
          stm.setObject(i++, it)
        }
        stm.addBatch()
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  private Object dbExecuteSql(String sql) throws SQLException {
      matrixDbUtil.dbExecuteSql(connect(), sql)
  }

  synchronized Connection connect() throws SQLException {
    if (con != null && !con.isClosed()) {
      return con
    }
    if (ci == null) {
      throw new SQLException("Connection is not available and no ConnectionInfo is configured to create one")
    }
    String url = ci.getUrl().toLowerCase()
    if (!url.contains(':h2:') && !url.contains(':derby:')
        && isBlank(ci.getPassword()) && !url.contains("passw")
        && !url.contains("integratedsecurity=true")) {
      log.warn("Password probably required to ${ci.getName()} for ${ci.getUser()}")
    }
    con = dbConnect(ci)
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
        dependencies << (it as String)
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
        log.error("Failed to instantiate the driver: ${ci.getDriver()}, clazz is $clazz: ${e.message}", e)
        throw e
      }
    } catch (ClassCastException | ClassNotFoundException e) {
      log.error("Failed to load driver: ${ci.getDriver()} could not be loaded from dependency ${ci.getDependency()}", e)
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
    // This is needed in newer version of sql server driver
    if (ci.url.startsWith(DataBaseProvider.MSSQL.urlStart)
        && ci.url.toLowerCase().contains('authenticationscheme=javakerberos')) {
      JaasConfigLoader.loadDefaultKerberosConfigIfNeeded()
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
