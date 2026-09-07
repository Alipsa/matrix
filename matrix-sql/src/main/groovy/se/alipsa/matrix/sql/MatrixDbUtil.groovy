package se.alipsa.matrix.sql

import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_PRECISION
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_SCALE
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getVARCHAR_SIZE

import groovy.transform.CompileDynamic

import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.util.Logger

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * Utility class for creating tables and inserting data from Matrix objects into a database.
 */
class MatrixDbUtil {

  static final int DEFAULT_VARCHAR_SIZE = 255
  static final int DEFAULT_DECIMAL_PRECISION = 38
  static final int DEFAULT_DECIMAL_SCALE = 10

  private static final String COL_TABLE_NAME = 'TABLE_NAME'
  private static final String COL_TABLE_SCHEMA = 'TABLE_SCHEM'
  private static final String COL_TABLE_CATALOG = 'TABLE_CAT'
  private static final String COL_COLUMN_NAME = 'COLUMN_NAME'
  private static final String COMMA_SEPARATOR = ', '
  private static final String UNDERSCORE = '_'
  private static final String[] TABLE_TYPES = ['TABLE', 'BASE TABLE'] as String[]

  private static final Logger log = Logger.getLogger(MatrixDbUtil)

  private static final Map<Connection, ConnectionMetadataCache> TABLE_METADATA_CACHE = new WeakHashMap<>()

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
    if (tableExists(con, tableName)) {
      throw new SQLException("Table $tableName already exists")
    }
    try(Statement stm = con.createStatement()) {
      result.ddlResult = stm.execute(sql)
    } catch (SQLException e) {
      log.error("Failed to create table $tableName using ddl: $sql", e)
      throw e
    } finally {
      clearTableMetadataCache(con)
    }
    try {
      result.inserted = insert(con, tableName, table, addQuotes)
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
  @CompileDynamic
  String createTableDdl(String tableName, Matrix table, Map<String, Map<String, Integer>> props, boolean addQuotes, String... primaryKey) {
    String sql = "create table ${SqlIdentifier.renderTable(tableName, addQuotes)} (\n"

    List<String> columns = []
    int i = 0
    List<Class> types = table.types()
    for (String name : table.columnNames()) {
      Class type = types.get(i++)
      columns.add("${SqlIdentifier.render(name, addQuotes)} ${mapper.sqlType(type, props[name])}")
    }
    sql += String.join(',\n', columns)
    if (primaryKey.length > 0) {
      sql += "\n , CONSTRAINT ${SqlIdentifier.constraintName('pk', tableName, addQuotes)} PRIMARY KEY ("
      sql += SqlIdentifier.renderAll(primaryKey.toList(), addQuotes).join(COMMA_SEPARATOR)
      sql += ')'
    }
    sql += '\n)'
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
  @CompileDynamic
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
        (0..<rowsToScan).each { int r ->
          BigDecimal val = table[r, name]
          if (val != null) {
            left = Math.max(left, val.precision() - val.scale())
            right = Math.max(right, val.scale())
          }
        }
        Integer precision = left + right
        props.put(DECIMAL_PRECISION, precision > 0 ? precision : DEFAULT_DECIMAL_PRECISION)
        props.put(DECIMAL_SCALE, precision > 0 ? right : DEFAULT_DECIMAL_SCALE)
      } else if (type == String) {
        Integer maxLength = 0
        (0..<rowsToScan).each { int r ->
          String val = table[r, name]
          if (val != null) {
            maxLength = Math.max(maxLength, val.length())
          }
        }
        props.put(VARCHAR_SIZE, DEFAULT_VARCHAR_SIZE.max(maxLength))
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
    dbExecuteSql(con, "drop table ${SqlIdentifier.renderTable(tableName)}")
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
    !findTables(con.getMetaData(), con.catalog, con.schema, tableName).isEmpty()
  }

  /**
   * Get the names of all tables in the connection's current catalog and schema.
   *
   * @param con the db connection
   * @return a set of table names
   * @throws SQLException if any sql error occurs
   */
  Set<String> getTableNames(Connection con) throws SQLException {
    findTables(con.getMetaData(), con.catalog, con.schema, null)*.name as Set<String>
  }

  /**
   * Get the primary key column names of the given table, ordered by key sequence.
   *
   * @param con the db connection
   * @param tableName the name of the table to look up
   * @return the primary key column names in key order, or an empty array if the table has no primary key
   * @throws SQLException if any sql error occurs
   */
  String[] primaryKeyColumns(Connection con, String tableName) throws SQLException {
    TableMetadata table = tableMetadata(con, tableName)
    if (table == null) {
      return new String[0]
    }
    table.primaryKeyColumns as String[]
  }

  /**
   * Create an update statement whose match columns come from the current-schema table's primary key.
   * Row column names are resolved case-insensitively to their stored database spellings.
   *
   * @param con the database connection
   * @param tableName the table to update in the connection's current catalog and schema
   * @param row the row containing update and primary-key values
   * @return the prepared SQL and ordered values
   * @throws SQLException if metadata cannot be read
   * @throws IllegalArgumentException if no primary key exists or required columns cannot be resolved
   */
  SqlGenerator.PreparedUpdate createPreparedUpdate(Connection con, String tableName, Row row) throws SQLException {
    TableMetadata table = tableMetadata(con, tableName)
    if (table == null || table.primaryKeyColumns.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot derive match columns for $tableName: no primary key. " +
          'Use update(tableName, row, matchColumnName...) instead')
    }

    Map<String, String> storedColumnNames = resolveStoredColumnNames(tableName, row.columnNames(), table.columnNames)
    List<String> matchColumns = []
    List<String> missingPrimaryKeyColumns = []
    table.primaryKeyColumns.each { String primaryKeyColumn ->
      String matchColumn = storedColumnNames.find { String rowColumn, String storedColumn ->
        storedColumn == primaryKeyColumn
      }?.key
      if (matchColumn == null) {
        missingPrimaryKeyColumns << primaryKeyColumn
      } else {
        matchColumns << matchColumn
      }
    }
    if (!missingPrimaryKeyColumns.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot update $tableName: row is missing primary key column(s): ${missingPrimaryKeyColumns.join(COMMA_SEPARATOR)}")
    }
    SqlGenerator.createPreparedUpdate(table.name, row, matchColumns as String[], storedColumnNames)
  }

  /**
   * Clear resolved table metadata for a connection after schema changes.
   *
   * @param con the connection whose cached metadata should be discarded
   */
  static void clearTableMetadataCache(Connection con) {
    synchronized (TABLE_METADATA_CACHE) {
      ConnectionMetadataCache cache = TABLE_METADATA_CACHE[con]
      if (cache != null) {
        cache.tableMetadata.clear()
        cache.generation++
      }
    }
  }

  private TableMetadata tableMetadata(Connection con, String tableName) throws SQLException {
    String catalog = con.catalog
    String schema = con.schema
    String cacheKey = [catalog, schema, tableName].join('\u0000')
    while (true) {
      long generation
      synchronized (TABLE_METADATA_CACHE) {
        ConnectionMetadataCache cache = TABLE_METADATA_CACHE[con]
        if (cache == null) {
          cache = new ConnectionMetadataCache()
          TABLE_METADATA_CACHE[con] = cache
        }
        TableMetadata cached = cache.tableMetadata[cacheKey]
        if (cached != null) {
          return cached
        }
        generation = cache.generation
      }

      TableMetadata resolved = loadTableMetadata(con.getMetaData(), catalog, schema, tableName)

      synchronized (TABLE_METADATA_CACHE) {
        ConnectionMetadataCache cache = TABLE_METADATA_CACHE[con]
        if (cache == null || cache.generation != generation) {
          continue
        }
        if (resolved != null) {
          cache.tableMetadata[cacheKey] = resolved
        }
        return resolved
      }
    }
  }

  private static TableMetadata loadTableMetadata(
      DatabaseMetaData metadata,
      String catalog,
      String schema,
      String tableName
  ) throws SQLException {
    TableReference table = findTable(metadata, catalog, schema, tableName)
    if (table == null) {
      return null
    }
    List<String> columnNames = []
    try (ResultSet rs = metadata.getColumns(table.catalog, table.schema, table.name, null)) {
      while (rs.next()) {
        if (sameTable(rs, table)) {
          columnNames << rs.getString(COL_COLUMN_NAME)
        }
      }
    }
    SortedMap<Short, String> columnsBySeq = new TreeMap<>()
    try (ResultSet rs = metadata.getPrimaryKeys(table.catalog, table.schema, table.name)) {
      while (rs.next()) {
        if (sameTable(rs, table)) {
          columnsBySeq[rs.getShort('KEY_SEQ')] = rs.getString(COL_COLUMN_NAME)
        }
      }
    }
    new TableMetadata(table, columnNames, columnsBySeq.values() as List<String>)
  }

  private static boolean sameTable(ResultSet rs, TableReference table) throws SQLException {
    rs.getString(COL_TABLE_NAME) == table.name
        && rs.getString(COL_TABLE_SCHEMA) == table.schema
        && rs.getString(COL_TABLE_CATALOG) == table.catalog
  }

  private static Map<String, String> resolveStoredColumnNames(
      String tableName,
      List<String> rowColumnNames,
      List<String> storedColumnNames
  ) {
    Map<String, String> resolved = [:]
    List<String> missing = []
    rowColumnNames.each { String rowColumn ->
      String storedColumn = storedColumnNames.find { it == rowColumn }
      if (storedColumn == null) {
        List<String> caseInsensitiveMatches = storedColumnNames.findAll { it.equalsIgnoreCase(rowColumn) }
        if (caseInsensitiveMatches.size() > 1) {
          throw new IllegalArgumentException(
              "Cannot update $tableName: row column $rowColumn is ambiguous; matches: ${caseInsensitiveMatches.join(COMMA_SEPARATOR)}")
        }
        storedColumn = caseInsensitiveMatches.find()
      }
      if (storedColumn == null) {
        missing << rowColumn
      } else {
        resolved[rowColumn] = storedColumn
      }
    }
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot update $tableName: row column(s) not found in table: ${missing.join(COMMA_SEPARATOR)}")
    }
    resolved
  }

  private static TableReference findTable(
      DatabaseMetaData metadata,
      String catalog,
      String schema,
      String tableName
  ) throws SQLException {
    List<TableReference> matches = findTables(metadata, catalog, schema, tableName)
    if (matches.isEmpty()) {
      return null
    }
    List<TableReference> exactMatches = matches.findAll { it.name == tableName }
    if (exactMatches.size() == 1) {
      return exactMatches.first()
    }
    if (matches.size() == 1) {
      return matches.first()
    }
    String locations = matches*.qualifiedName().join(COMMA_SEPARATOR)
    throw new SQLException("Ambiguous table name $tableName; matches: $locations")
  }

  private static List<TableReference> findTables(
      DatabaseMetaData metadata,
      String catalog,
      String schema,
      String tableName
  ) throws SQLException {
    List<TableReference> matches = []
    try (ResultSet rs = metadata.getTables(catalog, schema, null, TABLE_TYPES)) {
      while (rs.next()) {
        String name = rs.getString(COL_TABLE_NAME)
        if (tableName == null || name.equalsIgnoreCase(tableName)) {
          matches << new TableReference(
              rs.getString(COL_TABLE_CATALOG),
              rs.getString(COL_TABLE_SCHEMA),
              name
          )
        }
      }
    }
    matches
  }

  private static class TableReference {

    final String catalog
    final String schema
    final String name

    TableReference(String catalog, String schema, String name) {
      this.catalog = catalog
      this.schema = schema
      this.name = name
    }

    String qualifiedName() {
      [catalog, schema, name].findAll { it != null }.join('.')
    }
  }

  private static class TableMetadata {

    final String name
    final List<String> columnNames
    final List<String> primaryKeyColumns

    TableMetadata(TableReference table, List<String> columnNames, List<String> primaryKeyColumns) {
      this.name = table.name
      this.columnNames = columnNames.asImmutable()
      this.primaryKeyColumns = primaryKeyColumns.asImmutable()
    }
  }

  private static class ConnectionMetadataCache {

    final Map<String, TableMetadata> tableMetadata = [:]
    long generation
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
    insert(con, tableName, table, true)
  }

  /**
   * Insert the data from the given table into the given table in the database.
   *
   * @param con the db connection
   * @param tableName the name of the table to insert into
   * @param table the table containing the data to insert
   * @param addQuotes whether to quote identifiers
   * @return the number of inserted rows
   * @throws SQLException if any sql error occurs
   */
  int insert(Connection con, String tableName, Matrix table, boolean addQuotes) throws SQLException {
    String insertSql = SqlGenerator.createPreparedInsertSql(tableName, table, addQuotes)
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
      return batchResultCount(results)
    }
  }

  /**
   * Convert JDBC batch update counts into a non-negative affected-row count.
   * Drivers reporting {@link Statement#SUCCESS_NO_INFO} are counted as one successful row.
   * {@link Statement#EXECUTE_FAILED} entries are excluded from the count and logged as warnings.
   *
   * @param results the update counts returned by {@link Statement#executeBatch()}
   * @return the non-negative affected-row count
   */
  static int batchResultCount(int[] results) {
    results.inject(0) { int total, int result ->
      if (result == Statement.EXECUTE_FAILED) {
        log.warn('JDBC batch result contains EXECUTE_FAILED; the affected-row count excludes that statement')
        return total
      }
      total + (result == Statement.SUCCESS_NO_INFO ? 1 : result > 0 ? result : 0)
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
    name.replaceAll(/[^A-Za-z0-9_ ]/, UNDERSCORE)
        .replaceAll(/_+/, UNDERSCORE)
        .replaceAll(/^_+|_+$/, '')
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
    try (Statement stm = con.createStatement()) {
      boolean hasResultSet = stm.execute(sql)
      if (hasResultSet) {
        return Matrix.builder().data(stm.getResultSet()).build()
      }
      stm.getUpdateCount()
    } finally {
      clearTableMetadataCache(con)
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
