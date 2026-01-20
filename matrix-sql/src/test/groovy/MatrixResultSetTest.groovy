import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixResultSet
import java.sql.SQLException
import java.sql.ResultSet
import java.sql.Types

import static org.junit.jupiter.api.Assertions.*

class MatrixResultSetTest {

  @Test
  void testSimple() {
    Matrix mtcars = Dataset.mtcars()
    ResultSet rs = new MatrixResultSet(mtcars)
    int i = 0
    while (rs.next()) {
      assertEquals(mtcars[i, 0, String], rs.getString(1))
      assertEquals(mtcars[i, 1, BigDecimal], rs.getBigDecimal(2))
      assertEquals(mtcars[i, 2, int, 0], rs.getInt(3))
      assertEquals(mtcars[i, 0], rs.getObject('model'), "model, row $i")
      assertEquals(mtcars[i, 0, String], rs.getObject('model', String), "model, row $i")
      assertEquals(mtcars[i, 1, BigDecimal], rs.getBigDecimal('mpg'))
      assertEquals(mtcars[i, 2, int, 0], rs.getInt('cyl'), mtcars.row(i).toString())
      i++
    }
    rs.first()
    rs.updateString(1, "Foo")
    rs.updateRow()
    Matrix m = rs.unwrap(Matrix)
    assertEquals("Foo", m[0,0])

    def rsmd = rs.getMetaData()
    assertEquals(String.class.getName(), rsmd.getColumnClassName(1), "First column should be String")
    assertEquals(mtcars.columnCount(), rsmd.columnCount)
    assertEquals('VARCHAR', rsmd.getColumnTypeName(1))
    assertEquals(Types.VARCHAR, rsmd.getColumnType(1))
  }

  @Test
  void testNullHandlingForPrimitiveGettersAndStreams() {
    Matrix matrix = Matrix.builder('nulls').data([
        flag: [null],
        b: [null],
        s: [null],
        i: [null],
        l: [null],
        f: [null],
        d: [null],
        text: [null]
    ])
    .types(Boolean, Byte, Short, Integer, Long, Float, Double, String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.next())

    assertFalse(rs.getBoolean(1))
    assertTrue(rs.wasNull())

    assertEquals(0 as byte, rs.getByte(2))
    assertTrue(rs.wasNull())

    assertEquals(0 as short, rs.getShort(3))
    assertTrue(rs.wasNull())

    assertEquals(0, rs.getInt(4))
    assertTrue(rs.wasNull())

    assertEquals(0L, rs.getLong(5))
    assertTrue(rs.wasNull())

    assertEquals(0.0f, rs.getFloat(6))
    assertTrue(rs.wasNull())

    assertEquals(0.0d, rs.getDouble(7))
    assertTrue(rs.wasNull())

    assertNull(rs.getAsciiStream(8))
    assertTrue(rs.wasNull())

    assertNull(rs.getUnicodeStream(8))
    assertTrue(rs.wasNull())
  }

  @Test
  void testWrapperContracts() {
    Matrix matrix = Matrix.builder('wrap').data([
        name: ['x']
    ])
    .types(String)
    .build()

    ResultSet rs = new MatrixResultSet(matrix)
    assertTrue(rs.isWrapperFor(Matrix))
    assertTrue(rs.isWrapperFor(List))
    assertFalse(rs.isWrapperFor(String))
    assertNotNull(rs.unwrap(Matrix))
    assertNotNull(rs.unwrap(List))
    assertThrows(SQLException) { rs.unwrap(String) }

    def rsmd = rs.getMetaData()
    assertTrue(rsmd.isWrapperFor(Matrix))
    assertTrue(rsmd.isWrapperFor(List))
    assertFalse(rsmd.isWrapperFor(String))
    assertNotNull(rsmd.unwrap(Matrix))
    assertNotNull(rsmd.unwrap(List))
    assertThrows(SQLException) { rsmd.unwrap(String) }
  }
}
