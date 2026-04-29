import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_PRECISION
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getDECIMAL_SCALE
import static se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper.getVARCHAR_SIZE

import it.AbstractDbTest
import org.junit.jupiter.api.Test

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixDbUtil
import se.alipsa.matrix.sql.SqlIdentifier

class MatrixDbUtilTest {

  @Test
  void testDdl() {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.4.240')
    String dbName = "ddltestdb_${System.nanoTime()}"
    ci.setUrl("jdbc:h2:mem:${dbName};DB_CLOSE_DELAY=-1;MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    Matrix m = AbstractDbTest.getComplexData()
    def mapper = SqlTypeMapper.create(ci)
    def util = new MatrixDbUtil(mapper)
    Map mappings = util.createMappings(m, m.rowCount())
    String ddl = util.createTableDdl(MatrixDbUtil.tableName(m), m, mappings, true)
    // check that LocalDateTime becomes a datetime2 and not a TIMESTAMP
    assertTrue(ddl.contains('"local date time" datetime2'))
  }

  @Test
  void testDdlQuotesUnusualIdentifiers() {
    Matrix m = Matrix.builder('odd table-name*').data([
        'id': [1],
        'first name': ['Alice'],
        'select': ['reserved'],
        'quote " col': ['quoted']
    ])
    .types(int, String, String, String)
    .build()

    def util = new MatrixDbUtil(SqlTypeMapper.create(DataBaseProvider.UNKNOWN))
    Map mappings = util.createMappings(m, m.rowCount())
    String tableName = MatrixDbUtil.tableName(m)
    String ddl = util.createTableDdl(tableName, m, mappings, true, 'id')

    assertTrue(ddl.startsWith("create table ${SqlIdentifier.quote(tableName)}"))
    assertTrue(ddl.contains('"first name"'))
    assertTrue(ddl.contains('"select"'))
    assertTrue(ddl.contains('"quote "" col"'))
    assertTrue(ddl.contains("CONSTRAINT ${SqlIdentifier.quote('pk_odd_table_name')} PRIMARY KEY (\"id\")"))
  }

  @Test
  void testDdlCanRenderUnquotedIdentifiers() {
    Matrix m = Matrix.builder('plain_table').data([
        id: [1],
        name: ['Alice']
    ])
    .types(int, String)
    .build()

    def util = new MatrixDbUtil(SqlTypeMapper.create(DataBaseProvider.UNKNOWN))
    Map mappings = util.createMappings(m, m.rowCount())
    String ddl = util.createTableDdl(MatrixDbUtil.tableName(m), m, mappings, false, 'id')

    assertTrue(ddl.startsWith('create table plain_table'))
    assertTrue(ddl.contains('id '))
    assertTrue(ddl.contains('name '))
    assertTrue(ddl.contains('CONSTRAINT pk_plain_table PRIMARY KEY (id)'))
    assertFalse(ddl.contains('"id"'))
  }

  @Test
  void testTableNameSanitizesUnsafeCharacters() {
    Matrix m = Matrix.builder('fun(data) with \'quotes\' and *stuff*').data([
        id: [1]
    ])
    .types(int)
    .build()

    assertEquals('fun_data_ with _quotes_ and _stuff', MatrixDbUtil.tableName(m))
  }

  @Test
  void testCreateMappingsClampsScanRows() {
    Matrix m = Matrix.builder('small').data([
        name: ['abc'],
        amount: [12.34]
    ])
    .types(String, BigDecimal)
    .build()

    def util = new MatrixDbUtil(SqlTypeMapper.create(DataBaseProvider.UNKNOWN))
    Map mappings = util.createMappings(m, 10)
    assertEquals(2, mappings.size())
    assertEquals(1, mappings['name'].size())
    assertEquals(2, mappings['amount'].size())
  }

  @Test
  void testCreateMappingsUsesDefaultsForNullOnlyColumns() {
    Matrix m = Matrix.builder('nulls').data([
        name: [null, null],
        amount: [null, null]
    ])
    .types(String, BigDecimal)
    .build()

    def util = new MatrixDbUtil(SqlTypeMapper.create(DataBaseProvider.UNKNOWN))
    Map mappings = util.createMappings(m, 2)

    assertEquals(MatrixDbUtil.DEFAULT_VARCHAR_SIZE, mappings['name'][VARCHAR_SIZE])
    assertEquals(MatrixDbUtil.DEFAULT_DECIMAL_PRECISION, mappings['amount'][DECIMAL_PRECISION])
    assertEquals(MatrixDbUtil.DEFAULT_DECIMAL_SCALE, mappings['amount'][DECIMAL_SCALE])
  }

  @Test
  void testCreateMappingsUsesDefaultsForZeroScanRows() {
    Matrix m = Matrix.builder('zeroScan').data([
        name: ['abc'],
        amount: [12.34]
    ])
    .types(String, BigDecimal)
    .build()

    def util = new MatrixDbUtil(SqlTypeMapper.create(DataBaseProvider.UNKNOWN))
    Map mappings = util.createMappings(m, 0)

    assertEquals(MatrixDbUtil.DEFAULT_VARCHAR_SIZE, mappings['name'][VARCHAR_SIZE])
    assertEquals(MatrixDbUtil.DEFAULT_DECIMAL_PRECISION, mappings['amount'][DECIMAL_PRECISION])
    assertEquals(MatrixDbUtil.DEFAULT_DECIMAL_SCALE, mappings['amount'][DECIMAL_SCALE])
  }

  @Test
  void testCreateMappingsUsesDefaultsForEmptyMatrices() {
    Matrix m = Matrix.builder('empty').data([
        name: [],
        amount: []
    ])
    .types(String, BigDecimal)
    .build()

    def util = new MatrixDbUtil(SqlTypeMapper.create(DataBaseProvider.UNKNOWN))
    Map mappings = util.createMappings(m, 10)

    assertEquals(MatrixDbUtil.DEFAULT_VARCHAR_SIZE, mappings['name'][VARCHAR_SIZE])
    assertEquals(MatrixDbUtil.DEFAULT_DECIMAL_PRECISION, mappings['amount'][DECIMAL_PRECISION])
    assertEquals(MatrixDbUtil.DEFAULT_DECIMAL_SCALE, mappings['amount'][DECIMAL_SCALE])
  }
}
