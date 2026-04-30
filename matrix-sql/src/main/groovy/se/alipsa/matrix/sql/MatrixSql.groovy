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

  /**
   * Create a MatrixSql instance using the given ConnectionInfo. A new connection will be
   * created on first use and closed when {@link #close()} is called.
   *
   * @param ci the connection info describing the database driver, URL, and credentials
   */
  MatrixSql(ConnectionInfo ci) {
    check(ci)
    this.ci = ci
    mapper = SqlTypeMapper.create(ci)
    matrixDbUtil = new MatrixDbUtil(mapper)
  }

  /**
   * Create a MatrixSql instance using the given ConnectionInfo and explicit database provider.
   * A new connection will be created on first use and closed when {@link #close()} is called.
   *
   * @param ci the connection info describing the database driver, URL, and credentials
   * @param dbProvider the database provider used for SQL type mapping
   */
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

  /**
   * Close this MatrixSql instance. If the underlying connection is managed internally
   * (i.e. created from a {@link ConnectionInfo}), the connection is closed. If the connection
   * was supplied externally, this is a no-op for the connection.
   *
   * @throws SQLException if a database access error occurs while closing the connection
   */
  @Override
  void close() throws SQLException {
    if (closeConnectionOnClose && con != null) {
      con.close()
      con = null
    }
  }

  /**
   * Execute a select query and return the result as a Matrix.
   *
   * @param sqlQuery the sql query to execute
   * @param matrixName the name of the resulting Matrix
   * @return a Matrix containing the query results
   * @throws SQLException if a database access error occurs
   */
  Matrix select(String sqlQuery, String matrixName = 'myMatrix') throws SQLException {
    matrixDbUtil.select(connect(), sqlQuery).withMatrixName(matrixName)
  }

  /**
   * Execute a prepared select query and return the result as a Matrix.
   *
   * @param sqlQuery the sql query to execute, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement (raw List for Groovy generic compatibility)
   * @param matrixName the name of the resulting Matrix
   * @return a Matrix containing the query results
   * @throws SQLException if a database access error occurs
   */
  Matrix select(String sqlQuery, List params, String matrixName = 'myMatrix') throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sqlQuery)) {
      bindParams(stm, params)
      try (ResultSet rs = stm.executeQuery()) {
        Matrix.builder().data(rs).build().withMatrixName(matrixName)
      }
    }
  }

  /**
   * Execute an update query (UPDATE, INSERT, DELETE, or DDL).
   *
   * @param sqlQuery the sql query to execute
   * @return the number of rows affected, or 0 for DDL statements
   * @throws SQLException if a database access error occurs
   */
  int update(String sqlQuery) {
    dbUpdate(sqlQuery)
  }

  /**
   * Execute a prepared update query.
   *
   * @param sqlQuery the sql query to execute, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement (raw List for Groovy generic compatibility)
   * @return the number of rows affected
   * @throws SQLException if a database access error occurs
   */
  int update(String sqlQuery, List params) throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sqlQuery)) {
      bindParams(stm, params)
      stm.executeUpdate()
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

  /**
   * Update a single row in the given table, matching by the specified columns.
   *
   * @param tableName the name of the table to update
   * @param row the row data containing both update values and match values
   * @param matchColumnName the column(s) to match in the WHERE clause (required)
   * @return the number of rows affected
   * @throws SQLException if a database access error occurs
   * @throws IllegalArgumentException if matchColumnName is empty
   */
  int update(String tableName, Row row, String... matchColumnName) throws SQLException {
    SqlGenerator.PreparedUpdate prepared = SqlGenerator.createPreparedUpdate(tableName, row, matchColumnName)
    try(PreparedStatement stm = connect().prepareStatement(prepared.sql)) {
      bindParams(stm, prepared.values)
      stm.executeUpdate()
    }
  }

  /**
   * Update all rows in the database table corresponding to the Matrix, matching each row
   * by the given columns.
   *
   * @param table the Matrix containing the rows to update; the table name is derived from the Matrix name
   * @param matchColumnName the column(s) to match in the WHERE clause (required)
   * @return the total number of rows affected
   * @throws SQLException if a database access error occurs
   * @throws IllegalArgumentException if matchColumnName is empty
   */
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
   * @param params the parameters to bind to the prepared statement (raw List for Groovy generic compatibility)
   * @return A Map with the result index as key and either a Matrix or an Integer as value
   * @throws SQLException if a database access error occurs
   */
  Map<Integer, Object> execute(String sqlQuery, List params) throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sqlQuery)) {
      bindParams(stm, params)
      dbExecute(stm)
    }
  }

  /**
   * Check whether a table with the given name exists in the database.
   *
   * @param tableName the name of the table to check
   * @return true if the table exists, false otherwise
   * @throws SQLException if a database access error occurs
   */
  boolean tableExists(String tableName) throws SQLException {
    return matrixDbUtil.tableExists(connect(), tableName)
  }

  /**
   * Check whether the database table corresponding to the Matrix exists.
   * The table name is derived from the Matrix name.
   *
   * @param table the Matrix whose name is used to look up the table
   * @return true if the table exists, false otherwise
   * @throws SQLException if a database access error occurs
   */
  boolean tableExists(Matrix table) throws SQLException {
    tableExists(tableName(table))
  }

  /**
   * Return the names of all tables visible in the current database connection.
   *
   * @return a Set of table names
   * @throws SQLException if a database access error occurs
   */
  Set<String> getTableNames() throws SQLException {
    matrixDbUtil.getTableNames(connect())
  }

  /**
   * Execute an arbitrary SQL statement and return the raw result.
   * Prefer {@link #select(String)}, {@link #update(String)}, or {@link #execute(String)}
   * for typed results.
   *
   * @param sql the SQL statement to execute
   * @return the result as returned by the underlying database utility
   * @throws SQLException if a database access error occurs
   */
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

  /**
   * Generate the CREATE TABLE DDL for the given Matrix without executing it.
   *
   * @param table the Matrix to generate DDL for
   * @param addQuotes whether to quote identifiers in the DDL
   * @param scanNumrows optional number of rows to scan for type inference; defaults to max(100, rowCount)
   * @return the DDL string
   */
  String createDdl(Matrix table, boolean addQuotes = true, int... scanNumrows) {
    Map mappings = matrixDbUtil.createMappings(table, scanNumrows.length > 0 ? scanNumrows[0] : Math.max(100, table.rowCount()))
    matrixDbUtil.createTableDdl(tableName(table), table, mappings, addQuotes)
  }

  /**
   * Derive the database table name from the Matrix name.
   *
   * @param table the Matrix
   * @return the table name
   */
  static String tableName(Matrix table) {
    MatrixDbUtil.tableName(table)
  }

  /**
   * Drop the named table from the database.
   *
   * @param tableName the name of the table to drop
   * @return the result of the DDL execution
   * @throws SQLException if a database access error occurs
   */
  Object dropTable(String tableName) {
    dbExecuteSql("drop table ${SqlIdentifier.renderTable(tableName)}")
  }

  /**
   * Drop the database table corresponding to the Matrix.
   * The table name is derived from the Matrix name.
   *
   * @param table the Matrix whose name identifies the table to drop
   * @return the result of the DDL execution
   * @throws SQLException if a database access error occurs
   */
  Object dropTable(Matrix table) {
    dropTable(tableName(table))
  }

  private int dbInsert(String sqlQuery) throws SQLException, ExecutionException, InterruptedException {
    return (int)dbExecuteSql(sqlQuery)
  }

  /**
   * Insert a single row into the named table using a prepared statement.
   *
   * @param tableName the name of the table to insert into
   * @param row the row to insert; column names are taken from the Row
   * @return the number of rows inserted
   * @throws SQLException if a database access error occurs
   */
  int insert(String tableName, Row row) throws SQLException, ExecutionException, InterruptedException {
    String sql = SqlGenerator.createPreparedInsertSql(tableName, row)
    try(PreparedStatement stm = connect().prepareStatement(sql)) {
      bindParams(stm, row)
      stm.executeUpdate()
    }
  }

  /**
   * Insert a single row into the table corresponding to the Matrix.
   * The table name is derived from the Matrix name.
   *
   * @param table the Matrix identifying the target table
   * @param row the row to insert
   * @return the number of rows inserted
   * @throws SQLException if a database access error occurs
   */
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

  /**
   * Insert all rows from the Matrix into the corresponding database table.
   * The table name is derived from the Matrix name.
   *
   * @param table the Matrix containing the rows to insert
   * @return the number of rows inserted
   * @throws SQLException if a database access error occurs
   */
  int insert(Matrix table) throws SQLException {
    matrixDbUtil.insert(connect(), table)
  }

  /**
   * Execute a delete (or any DML) query.
   *
   * @param sql the sql delete statement to execute
   * @return the number of rows deleted
   * @throws SQLException if a database access error occurs
   */
  int delete(String sql) throws SQLException {
    try(Statement stm = connect().createStatement()) {
      stm.executeUpdate(sql)
    }
  }

  /**
   * Execute a prepared delete query.
   *
   * @param sql the sql delete statement, with '?' placeholders for parameters
   * @param params the parameters to bind to the prepared statement (raw List for Groovy generic compatibility)
   * @return the number of rows deleted
   * @throws SQLException if a database access error occurs
   */
  int delete(String sql, List params) throws SQLException {
    try(PreparedStatement stm = connect().prepareStatement(sql)) {
      bindParams(stm, params)
      stm.executeUpdate()
    }
  }

  private static Map<Integer, Object> dbExecute(Statement stm, String sqlQuery) throws SQLException {
    dbExecuteResults(stm, stm.execute(sqlQuery))
  }

  private static Map<Integer, Object> dbExecute(PreparedStatement stm) throws SQLException {
    dbExecuteResults(stm, stm.execute())
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

  private static int dbExecuteUpdate(Statement stm, String sqlQuery) throws SQLException {
    stm.executeUpdate(sqlQuery)
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
        bindParams(stm, values)
        stm.addBatch()
      }
      int[] results = stm.executeBatch()
      return IntStream.of(results).sum()
    }
  }

  private Object dbExecuteSql(String sql) throws SQLException {
      matrixDbUtil.dbExecuteSql(connect(), sql)
  }

  /**
   * Return an open connection, creating one from the configured {@link ConnectionInfo} if needed.
   * Reuses an existing open connection if one is already held.
   *
   * @return an open database connection
   * @throws SQLException if no connection is available and no ConnectionInfo is configured,
   *                      or if creating a new connection fails
   */
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

  /**
   * Return true if the string is null or blank.
   *
   * @param str the string to test
   * @return true if null or blank
   */
  static boolean isBlank(String str) {
    if (str == null) {
      return true
    }
    return str.isBlank()
  }

  /**
   * Establish a physical database connection using the given ConnectionInfo.
   * Downloads the JDBC driver via Maven if it is not already on the classpath.
   *
   * @param ci the connection info describing the driver dependency, URL, and credentials
   * @return an open Connection
   * @throws SQLException if a database access error occurs
   * @throws IOException if the driver artifact cannot be resolved
   */
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

  private static void bindParams(PreparedStatement stm, List params) throws SQLException {
    int i = 1
    params.each {
      stm.setObject(i++, it)
    }
  }

  private static boolean urlContainsLogin(String url) {
    String safeLcUrl = url.toLowerCase()
    return ( safeLcUrl.contains("user") && safeLcUrl.contains("pass") ) || safeLcUrl.contains("@")
  }

  /**
   * Validate that the ConnectionInfo has a non-blank dependency declaration.
   *
   * @param connectionInfo the connection info to validate
   * @throws IllegalArgumentException if the dependency is null or blank
   */
  static void check(ConnectionInfo connectionInfo) {
    if (connectionInfo.getDependency() == null || connectionInfo.getDependency().isBlank()) {
      throw new IllegalArgumentException("Dependency is required")
    }
  }

  /**
   * Return the ConnectionInfo used to create managed connections, or null for external connections.
   *
   * @return the ConnectionInfo, or null
   */
  ConnectionInfo getConnectionInfo() {
    return ci
  }

  /**
   * Return the SqlTypeMapper used for SQL/Java type conversions.
   *
   * @return the SqlTypeMapper
   */
  SqlTypeMapper getSqlTypeMapper() {
    return mapper
  }

  /**
   * Return the MatrixDbUtil used for low-level database operations.
   *
   * @return the MatrixDbUtil
   */
  MatrixDbUtil getMatrixDbUtil() {
    return matrixDbUtil
  }

  /**
   * Return the current underlying connection, or null if none has been opened yet.
   *
   * @return the Connection, or null
   */
  Connection getConnection() {
    return con
  }
}
