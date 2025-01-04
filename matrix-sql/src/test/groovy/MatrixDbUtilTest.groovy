import it.AbstractDbTest
import org.junit.jupiter.api.Test
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixDbUtil
import static org.junit.jupiter.api.Assertions.*

class MatrixDbUtilTest {

  @Test
  void testDdl() {
    ConnectionInfo ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.3.232')
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'ddltestdb').getAbsolutePath()
    ci.setUrl("jdbc:h2:file:${tmpDb};MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    Matrix m = AbstractDbTest.getComplexData()
    def mapper = SqlTypeMapper.create(ci)
    def util = new MatrixDbUtil(mapper)
    Map mappings = util.createMappings(m, m.rowCount())
    String ddl = util.createTableDdl(MatrixDbUtil.tableName(m), m, mappings)
    // check that LocalDateTime becomes a datetime2 and not a TIMESTAMP
    assertTrue(ddl.contains('"local date time" datetime2'))
  }
}
