import static org.junit.jupiter.api.Assertions.*

import groovy.sql.Sql

import it.AbstractDbTest
import org.junit.jupiter.api.Test

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.SqlUtil
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.sql.MatrixDbUtil
import se.alipsa.mavenutils.ArtifactLookup
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.sql.MatrixSqlFactory
import se.alipsa.matrix.sql.SqlIdentifier

import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate

class MatrixSqlTest {

  private static String h2MemUrl(String name, String additionalProperties = null) {
    String uniqueName = "${name}_${System.nanoTime()}"
    String url = "jdbc:h2:mem:${uniqueName};DB_CLOSE_DELAY=-1"
    if (additionalProperties != null && !additionalProperties.isBlank()) {
      url += ";${additionalProperties}"
    }
    return url
  }


  @Test
  void testH2TableCreation() {
    String url = h2MemUrl('h2testdb')
    Matrix airq = Dataset.airquality()
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {

      String tableName = matrixSql.tableName(airq)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(airq)

      // For h2 we MUST piggyback on the existing connection as there can only be one
      // for another db we could have done Sql sql = new Sql(matrixSql.dbConnect(ci))
      // But since this is surrounded in a "try with resources", this is actually better
      Sql sql = new Sql(matrixSql.connect())
      int i = 0
      // Explicitly call toString to force interpolation in a closure
      sql.query("select * from $tableName".toString()) { rs ->
        while (rs.next()) {
          assertEquals(ps(airq[i, 0]), ps(rs.getBigDecimal("Ozone")), "ozone on row $i differs")
          assertEquals(ps(airq[i, 1]), ps(rs.getBigDecimal("Solar.R")), "Solar.R on row $i differs")
          assertEquals(ps(airq[i, 2]), ps(rs.getBigDecimal("Wind")), "Wind on row $i differs")
          assertEquals(ps(airq[i, 3]), ps(rs.getBigDecimal("Temp")), "Temp on row $i differs")
          assertEquals(airq[i, 4], rs.getShort("Month"), "Month on row $i differs")
          assertEquals(airq[i, 5], rs.getShort("Day"), "Day on row $i differs")
          i++
        }
      }

      Matrix m2 = matrixSql.select("select * from $tableName")
      airq.eachWithIndex { Row row, int r ->
        row.eachWithIndex { BigDecimal expected, int c ->
          def actual = (m2[r, c] as BigDecimal)
          if (expected != null && actual != null) {
            actual = actual.setScale(expected.scale())
          }
          assertEquals(expected, actual, "Diff detected on row $r, column ${airq.columnName(c)}")
        }
      }
    }
  }

  static Double ps(BigDecimal bd) {
    bd == null ? null : bd.doubleValue()
  }

  @Test
  void testExample() {

    Matrix complexData = Matrix.builder('complexData').data([
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ])
    .types(int, String, LocalDate)
    .build()

    String url = h2MemUrl('testdb', 'MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE')
    // Test that deriving the dependency and driver from the url works
    try(MatrixSql matrixSql = MatrixSqlFactory.create(url, 'sa', '123')) {

      String tableName = matrixSql.tableName(complexData)

      if (matrixSql.tableExists(complexData)) {
        matrixSql.dropTable(complexData)
      }

      matrixSql.create(complexData)

      Matrix stored = matrixSql.select("select * from $tableName")
      //println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
      assertEquals(java.sql.Date, stored.type('start'))
      assertEquals(ListConverter.toSqlDates('2021-12-01', '2022-07-10', '2023-05-27'), stored.column('start'))

      stored = stored.convert('start', LocalDate)
      assertEquals(LocalDate, stored.type('start'))
      assertEquals(ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'), stored.column('start'))
    }
  }

  @Test
  void testFactorySetsDriverWhenMissing() {
    String url = h2MemUrl('driver_testdb')
    String expectedDriver = SqlUtil.getDriverClassName(url)
    assertNotNull(expectedDriver, "Expected SqlUtil to resolve a driver for $url")
    try (MatrixSql matrixSql = MatrixSqlFactory.create(url, 'sa', '123', '2.4.240')) {
      assertEquals(expectedDriver, matrixSql.connectionInfo.driver)
    }
  }

  @Test
  void testReconnectAfterClose() {
    String url = h2MemUrl('reconnect_testdb')
    MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')
    try {
      Connection first = matrixSql.connect()
      assertFalse(first.isClosed(), 'Expected initial connection to be open')
      matrixSql.close()
      assertTrue(first.isClosed(), 'Expected initial connection to be closed after MatrixSql.close()')
      Connection second = matrixSql.connect()
      assertFalse(second.isClosed(), 'Expected new connection after close')
      assertNotSame(first, second, 'Expected a new connection instance after close')
    } finally {
      matrixSql.close()
    }
  }

  @Test
  void testUpdateUsesPreparedStatement() {
    Matrix data = Matrix.builder('people').data([
        id: [1],
        name: ["O'Neil"]
    ])
    .types(int, String)
    .build()

    String props = "DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
    String url = h2MemUrl('update_testdb', props)
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(data)

      Row row = data.row(0)
      row['name'] = "D'Arcy"
      assertEquals(1, matrixSql.update(tableName, row, 'id'))

      Matrix stored = matrixSql.select("select * from $tableName")
      assertEquals("D'Arcy", stored[0, 'name'])
    }
  }

  @Test
  void testUpdateRequiresMatchColumn() {
    Matrix data = Matrix.builder('people2').data([
        id: [1],
        name: ['Alice']
    ])
    .types(int, String)
    .build()

    String url = h2MemUrl('update_match_testdb')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(data)

      Row row = data.row(0)
      row['name'] = 'Bob'
      assertThrows(IllegalArgumentException) { matrixSql.update(tableName, row) }
    }
  }

  @Test
  void testPrimaryKey() {
    Matrix pkdata = Matrix.builder('pkdata').data([
        'id': [1,2,3],
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ])
    .types(int, int, String, LocalDate)
    .build()

    String url = h2MemUrl('pktestdb')

    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(pkdata)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }

      matrixSql.create(pkdata, 'id')

      try (Connection con = matrixSql.connect(); ResultSet rs = con.getMetaData().getPrimaryKeys(null, null, tableName.toUpperCase())) {
        assertTrue(rs.next(), 'No pk results found')
        assertEquals(
            'id',
            rs.getString("COLUMN_NAME"),
            "Expected to find 'id' as the primary key"
        )
      }
    }
  }

  @Test
  void testDdl() {
    String h2version = "2.4.240"
    Matrix m = AbstractDbTest.getComplexData()
    String props = "MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
    String url = h2MemUrl('ddltestdb', props)

    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency("com.h2database:h2:$h2version")
    ci.setUrl(url)
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    String ddl1
    String ddl2
    try (MatrixSql sql = new MatrixSql(ci)) {
      ddl1 = sql.createDdl(m)
      //check that DB detection works properly
      assertTrue(ddl1.contains('"local date time" datetime2'), "Expected to find datetime2 but was $ddl1")
    }

    // Check that the factory creates the same MatrixSql as the explicit creation does
    try (MatrixSql h2 = MatrixSqlFactory.createH2(url, 'sa', '123', null, h2version)) {
      //println "using $h2.connectionInfo.dependency with url ${h2.connectionInfo.url}"
      ddl2 = h2.createDdl(m)
      String latestVersion = h2.connectionInfo.dependencyVersion
      if (latestVersion != h2version) {
        System.err.println("We are using version $h2version when explicitly specifying it but latest version is $latestVersion")
        System.err.println("Consider updating it!")
      }
      //check that DB detection works properly
      assertTrue(ddl2.contains('"local date time" datetime2'), "Expected to find datetime2 but was $ddl2")
      assertEquals(ci.url, h2.connectionInfo.url, "Urls does not match")
    }
    assertEquals(ddl1, ddl2)
  }

  @Test
  void testPreparedParameterSelectUpdateDeleteExecute() {
    Matrix data = Matrix.builder('people').data([
        id: [1, 2, 3],
        name: ['Alice', 'Bob', 'Charlie']
    ])
    .types(int, String)
    .build()

    String url = h2MemUrl('prep_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(data)

      String tbl = SqlIdentifier.renderTable(tableName)

      // Prepared select
      Matrix result = matrixSql.select("select * from $tbl where id > ?", [1], 'result')
      assertEquals('result', result.matrixName)
      assertEquals(2, result.rowCount())
      assertEquals('Bob', result[0, 'name'])

      // Prepared update
      int updated = matrixSql.update("update $tbl set name = ? where id = ?", ['Robert', 2])
      assertEquals(1, updated)

      // Verify update
      Matrix afterUpdate = matrixSql.select("select * from $tbl where id = ?", [2])
      assertEquals('Robert', afterUpdate[0, 'name'])

      // Prepared delete
      int deleted = matrixSql.delete("delete from $tbl where id = ?", [3])
      assertEquals(1, deleted)

      // Verify delete
      Matrix afterDelete = matrixSql.select("select * from $tbl")
      assertEquals(2, afterDelete.rowCount())

      // Prepared execute (update)
      Map<Integer, Object> execResult = matrixSql.execute("update $tbl set name = ? where id = ?", ['Bobby', 2])
      assertEquals(1, execResult[0])

      // Prepared execute (select)
      Map<Integer, Object> execSelect = matrixSql.execute("select * from $tbl where id = ?", [1])
      assertTrue(execSelect[0] instanceof Matrix)
      assertEquals(1, ((Matrix) execSelect[0]).rowCount())
    }
  }

  @Test
  void testInsertMatrixWithExplicitTableName() {
    Matrix data = Matrix.builder('people').data([
        id: [1],
        name: ['Alice']
    ])
    .types(int, String)
    .build()

    String url = h2MemUrl('insert_matrix_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      if (matrixSql.tableExists(tableName)) {
        matrixSql.dropTable(tableName)
      }
      matrixSql.create(data)

      Matrix extra = Matrix.builder('extra').data([
          id: [2],
          name: ['Bob']
      ])
      .types(int, String)
      .build()

      assertEquals(1, matrixSql.insert(tableName, extra))

      Matrix stored = matrixSql.select("select * from $tableName order by id")
      assertEquals(2, stored.rowCount())
      assertEquals('Alice', stored[0, 'name'])
      assertEquals('Bob', stored[1, 'name'])
    }
  }

  @Test
  void testManagedConnectionDoesNotCloseExternallyOwnedConnection() {
    String url = h2MemUrl('managed_con_testdb')
    MatrixSql owner = MatrixSqlFactory.createH2(url, 'sa', '123')
    try {
      Connection con = owner.connect()
      MatrixSql managed = new MatrixSql(con, DataBaseProvider.H2)
      assertFalse(con.isClosed(), 'Expected external connection to be open')
      managed.close()
      assertFalse(con.isClosed(), 'Expected external connection to remain open after MatrixSql.close()')
    } finally {
      owner.close()
    }
  }

  @Test
  void testGeneratedSqlHandlesQuotedIdentifiers() {
    Matrix data = Matrix.builder('odd table-name*').data([
        'id': [1],
        'first name': ['Alice'],
        'select': ['reserved'],
        'MiXeD': ['Case'],
        'quote " col': ['quoted']
    ])
    .types(int, String, String, String, String)
    .build()

    String url = h2MemUrl('quoted_identifiers', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      String tableName = matrixSql.tableName(data)
      matrixSql.create(data, 'id')
      assertTrue(matrixSql.tableExists(tableName))

      Matrix extra = Matrix.builder('extra').data([
          'id': [2],
          'first name': ['Beatrice'],
          'select': ['second'],
          'MiXeD': ['Camel'],
          'quote " col': ['embedded']
      ])
      .types(int, String, String, String, String)
      .build()
      assertEquals(1, matrixSql.insert(tableName, extra.row(0)))

      Row changed = extra.row(0)
      changed['first name'] = 'Bob'
      changed['select'] = 'updated'
      changed['MiXeD'] = 'StillMixed'
      changed['quote " col'] = 'still " quoted'
      assertEquals(1, matrixSql.update(tableName, changed, 'id'))

      Matrix stored = matrixSql.select("""
        select ${SqlIdentifier.quote('id')},
               ${SqlIdentifier.quote('first name')},
               ${SqlIdentifier.quote('select')},
               ${SqlIdentifier.quote('MiXeD')},
               ${SqlIdentifier.quote('quote " col')}
        from ${SqlIdentifier.quote(tableName)}
        order by ${SqlIdentifier.quote('id')}
      """)
      assertEquals(2, stored.rowCount())
      assertEquals('Alice', stored[0, 'first name'])
      assertEquals('Bob', stored[1, 'first name'])
      assertEquals('updated', stored[1, 'select'])
      assertEquals('StillMixed', stored[1, 'MiXeD'])
      assertEquals('still " quoted', stored[1, 'quote " col'])

      matrixSql.dropTable(tableName)
      assertFalse(matrixSql.tableExists(tableName))
    }
  }

  @Test
  void testConstructorWithConnectionInfoAndDbProvider() {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.4.240')
    ci.setUrl(h2MemUrl('ci_dbprovider_testdb'))
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver('org.h2.Driver')

    Matrix data = Matrix.builder('ci_prov').data([id: [1], val: ['x']]).types(int, String).build()
    try (MatrixSql matrixSql = new MatrixSql(ci, DataBaseProvider.H2)) {
      matrixSql.create(data)
      Matrix stored = matrixSql.select("select * from ${SqlIdentifier.renderTable(matrixSql.tableName(data))}")
      assertEquals(1, stored.rowCount())
      assertEquals('x', stored[0, 'val'])
    }
  }

  @Test
  void testConstructorWithConnectionAndSqlTypeMapper() {
    String url = h2MemUrl('con_mapper_testdb')
    MatrixSql owner = MatrixSqlFactory.createH2(url, 'sa', '123')
    try {
      Connection con = owner.connect()
      SqlTypeMapper mapper = SqlTypeMapper.create(DataBaseProvider.H2)
      Matrix data = Matrix.builder('mapper_tbl').data([id: [1], val: ['y']]).types(int, String).build()
      try (MatrixSql managed = new MatrixSql(con, mapper)) {
        managed.create(data)
        Matrix stored = managed.select("select * from ${SqlIdentifier.renderTable(managed.tableName(data))}")
        assertEquals(1, stored.rowCount())
        assertEquals('y', stored[0, 'val'])
      }
      assertFalse(con.isClosed(), 'External connection must stay open after managed MatrixSql close')
    } finally {
      owner.close()
    }
  }

  @Test
  void testUpdateUnprepared() {
    Matrix data = Matrix.builder('upd_plain').data([id: [1, 2], name: ['Alice', 'Bob']]).types(int, String).build()
    String url = h2MemUrl('upd_plain_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(data)
      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(data))
      int affected = matrixSql.update("UPDATE $tbl SET name = 'Charlie' WHERE id = 1")
      assertEquals(1, affected)
      Matrix stored = matrixSql.select("SELECT * FROM $tbl WHERE id = 1")
      assertEquals('Charlie', stored[0, 'name'])
    }
  }

  @Test
  void testBatchMatrixUpdate() {
    Matrix original = Matrix.builder('batch_upd').data([
        id: [1, 2, 3],
        name: ['Alice', 'Bob', 'Charlie']
    ]).types(int, String).build()

    String url = h2MemUrl('batch_upd_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(original)

      Matrix updates = Matrix.builder('batch_upd').data([
          id: [1, 2, 3],
          name: ['Alicia', 'Bobby', 'Chuck']
      ]).types(int, String).build()

      int total = matrixSql.update(updates, 'id')
      assertEquals(3, total)

      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(original))
      Matrix stored = matrixSql.select("SELECT * FROM $tbl ORDER BY id")
      assertEquals('Alicia', stored[0, 'name'])
      assertEquals('Bobby', stored[1, 'name'])
      assertEquals('Chuck', stored[2, 'name'])
    }
  }

  @Test
  void testExecuteUnprepared() {
    String url = h2MemUrl('exec_plain_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      Map<Integer, Object> ddlResult = matrixSql.execute('CREATE TABLE exec_t (id INT, val VARCHAR(50))')
      assertEquals(0, ddlResult[0], 'DDL should return 0 update count')

      matrixSql.execute("INSERT INTO exec_t VALUES (1, 'hello')")
      Map<Integer, Object> selResult = matrixSql.execute('SELECT * FROM exec_t')
      assertTrue(selResult[0] instanceof Matrix)
      assertEquals(1, ((Matrix) selResult[0]).rowCount())
    }
  }

  @Test
  void testGetTableNames() {
    Matrix a = Matrix.builder('tbl_a').data([id: [1]]).types(int).build()
    Matrix b = Matrix.builder('tbl_b').data([id: [2]]).types(int).build()
    String url = h2MemUrl('tablenames_testdb')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(a)
      matrixSql.create(b)
      Set<String> names = matrixSql.getTableNames()
      assertTrue(names.any { it.equalsIgnoreCase('tbl_a') }, "Expected tbl_a in $names")
      assertTrue(names.any { it.equalsIgnoreCase('tbl_b') }, "Expected tbl_b in $names")
    }
  }

  @Test
  void testExecuteQuery() {
    Matrix data = Matrix.builder('exec_q').data([id: [1], val: ['foo']]).types(int, String).build()
    String url = h2MemUrl('executequery_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(data)
      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(data))
      Object result = matrixSql.executeQuery("SELECT * FROM $tbl")
      assertTrue(result instanceof Matrix, "Expected Matrix, got ${result?.class}")
      assertEquals(1, ((Matrix) result).rowCount())
    }
  }

  @Test
  void testCreateWithExplicitTableName() {
    Matrix data = Matrix.builder('original_name').data([id: [1], val: ['a']]).types(int, String).build()
    String url = h2MemUrl('explicit_name_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create('custom_table', data)
      assertTrue(matrixSql.tableExists('custom_table'))
      assertFalse(matrixSql.tableExists('original_name'))
      Matrix stored = matrixSql.select("SELECT * FROM ${SqlIdentifier.renderTable('custom_table')}")
      assertEquals(1, stored.rowCount())
      assertEquals('a', stored[0, 'val'])
    }
  }

  @Test
  void testCreateWithScanRows() {
    Matrix data = Matrix.builder('scan_rows').data([
        id: [1, 2, 3, 4, 5],
        name: ['a', 'b', 'c', 'd', 'e']
    ]).types(int, String).build()
    String url = h2MemUrl('scanrows_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(data, 2, true)
      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(data))
      Matrix stored = matrixSql.select("SELECT * FROM $tbl ORDER BY id")
      assertEquals(5, stored.rowCount())
    }
  }

  @Test
  void testCreateWithProps() {
    Matrix data = Matrix.builder('props_tbl').data([
        id: [1],
        name: ['hello world']
    ]).types(int, String).build()
    String url = h2MemUrl('props_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      Map<String, Map<String, Integer>> props = [
          'id': [:],
          'name': [(SqlTypeMapper.VARCHAR_SIZE): 200]
      ]
      matrixSql.create(data, props)
      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(data))
      Matrix stored = matrixSql.select("SELECT * FROM $tbl")
      assertEquals(1, stored.rowCount())
      assertEquals('hello world', stored[0, 'name'])
    }
  }

  @Test
  void testInsertMatrixAndInsertMatrixRow() {
    Matrix schema = Matrix.builder('insert_tbl').data([id: [1], name: ['Alice']]).types(int, String).build()
    String url = h2MemUrl('insert_ref_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(schema)

      // insert(Matrix) uses the Matrix name to derive the table name
      Matrix bulk = Matrix.builder('insert_tbl').data([id: [2, 3], name: ['Bob', 'Charlie']]).types(int, String).build()
      assertEquals(2, matrixSql.insert(bulk))

      // insert(Matrix, Row) uses the Matrix name for the table, Row carries column values
      Matrix ref = Matrix.builder('insert_tbl').data([id: [0], name: ['x']]).types(int, String).build()
      Row newRow = Matrix.builder('insert_tbl').data([id: [4], name: ['Diana']]).types(int, String).build().row(0)
      assertEquals(1, matrixSql.insert(ref, newRow))

      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(schema))
      Matrix stored = matrixSql.select("SELECT * FROM $tbl ORDER BY id")
      assertEquals(4, stored.rowCount())
      assertEquals('Alice', stored[0, 'name'])
      assertEquals('Bob', stored[1, 'name'])
      assertEquals('Charlie', stored[2, 'name'])
      assertEquals('Diana', stored[3, 'name'])
    }
  }

  @Test
  void testDeleteUnprepared() {
    Matrix data = Matrix.builder('del_plain').data([id: [1, 2, 3], name: ['a', 'b', 'c']]).types(int, String).build()
    String url = h2MemUrl('del_plain_testdb', 'DATABASE_TO_UPPER=FALSE')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      matrixSql.create(data)
      String tbl = SqlIdentifier.renderTable(matrixSql.tableName(data))
      int deleted = matrixSql.delete("DELETE FROM $tbl WHERE id = 2")
      assertEquals(1, deleted)
      Matrix stored = matrixSql.select("SELECT * FROM $tbl ORDER BY id")
      assertEquals(2, stored.rowCount())
      assertEquals(1, stored[0, 'id'])
      assertEquals(3, stored[1, 'id'])
    }
  }

  @Test
  void testStaticHelpersAndAccessors() {
    assertTrue(MatrixSql.isBlank(null))
    assertTrue(MatrixSql.isBlank(''))
    assertTrue(MatrixSql.isBlank('   '))
    assertFalse(MatrixSql.isBlank('x'))

    ConnectionInfo valid = new ConnectionInfo()
    valid.setDependency('com.h2database:h2:2.4.240')
    MatrixSql.check(valid)  // must not throw

    ConnectionInfo invalid = new ConnectionInfo()
    assertThrows(IllegalArgumentException) { MatrixSql.check(invalid) }

    String url = h2MemUrl('accessors_testdb')
    try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
      assertNotNull(matrixSql.getSqlTypeMapper())
      assertNotNull(matrixSql.getMatrixDbUtil())
      assertNull(matrixSql.getConnection(), 'Connection should be null before first use')
      matrixSql.connect()
      assertNotNull(matrixSql.getConnection(), 'Connection should be non-null after connect()')
      assertTrue(matrixSql.getMatrixDbUtil() instanceof MatrixDbUtil)
      assertTrue(matrixSql.getSqlTypeMapper() instanceof SqlTypeMapper)
    }
  }

  @Test
  void testCreateH2FallsBackOnNetworkFailure() {
    String url = h2MemUrl('fallback_h2_testdb')
    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
      MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
        @Override
        String fetchLatestVersion(String g, String a) throws Exception {
          throw new IOException("Simulated network failure")
        }
      }
      try (MatrixSql ms = MatrixSqlFactory.createH2(url, 'sa', '123')) {
        assertTrue(ms.connectionInfo.dependency.contains(MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]),
            "Expected dependency to contain H2 fallback version ${MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]}")
      }
    } finally {
      MatrixSqlFactory.artifactLookup = original
    }
  }

  @Test
  void testCreateDerbyFallsBackOnNetworkFailure() {
    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
      MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
        @Override
        String fetchLatestVersion(String g, String a) throws Exception {
          throw new IOException("Simulated network failure")
        }
      }
      try (MatrixSql ms = MatrixSqlFactory.createDerby("memory:fallback_derby_${System.nanoTime()}")) {
        assertTrue(ms.connectionInfo.dependency.contains(MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.DERBY]),
            "Expected dependency to contain Derby fallback version ${MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.DERBY]}")
      }
    } finally {
      MatrixSqlFactory.artifactLookup = original
    }
  }

  @Test
  void testGenericCreateFallsBackOnNetworkFailure() {
    String url = h2MemUrl('fallback_generic_testdb')
    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
      MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
        @Override
        String fetchLatestVersion(String g, String a) throws Exception {
          throw new IOException("Simulated network failure")
        }
      }
      try (MatrixSql ms = MatrixSqlFactory.create(url, 'sa', '123')) {
        assertTrue(ms.connectionInfo.dependency.contains(MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]),
            "Expected dependency to contain H2 fallback version ${MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]}")
      }
    } finally {
      MatrixSqlFactory.artifactLookup = original
    }
  }

  @Test
  void testGenericCreateThrowsWithCoordinatesWhenNoFallback() {
    String pgUrl = 'jdbc:postgresql://localhost:5432/testdb'
    Map<String, String> pgDependency = MatrixSqlFactory.getDependencyName(pgUrl)
    assertNotNull(pgDependency, 'Expected PostgreSQL to be a known provider in DataBaseProvider')

    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
      MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
        @Override
        String fetchLatestVersion(String g, String a) throws Exception {
          throw new IOException("Simulated network failure")
        }
      }
      RuntimeException ex = assertThrows(RuntimeException) {
        MatrixSqlFactory.create(pgUrl, 'user', 'pass')
      }
      assertTrue(ex.message.contains(pgDependency.groupId),
          "Expected message to contain groupId '${pgDependency.groupId}', was: ${ex.message}")
      assertTrue(ex.message.contains(pgDependency.artifactId),
          "Expected message to contain artifactId '${pgDependency.artifactId}', was: ${ex.message}")
      assertTrue(ex.message.contains('no fallback version is configured'),
          "Expected message to mention missing fallback, was: ${ex.message}")
    } finally {
      MatrixSqlFactory.artifactLookup = original
    }
  }

  @Test
  void testManagedConnectionRemainsUsableAfterClose() {
    String url = h2MemUrl('managed_usable_testdb', 'DATABASE_TO_UPPER=FALSE')
    MatrixSql owner = MatrixSqlFactory.createH2(url, 'sa', '123')
    try {
      Matrix data = Matrix.builder('usable').data([id: [1]]).types(int).build()
      owner.create(data)

      Connection con = owner.connect()
      MatrixSql managed = new MatrixSql(con, DataBaseProvider.H2)
      managed.close()

      String tbl = SqlIdentifier.renderTable('usable')
      Matrix stored = managed.select("SELECT * FROM $tbl")
      assertEquals(1, stored.rowCount(), 'Managed MatrixSql must remain usable after close()')
    } finally {
      owner.close()
    }
  }
}
