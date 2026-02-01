import it.AbstractDbTest
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixDbUtil
import static org.junit.jupiter.api.Assertions.*

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
}
