import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Stat
import se.alipsa.groovy.matrix.sql.MatrixResultSet
import java.sql.ResultSet
import java.sql.Types

import static org.junit.jupiter.api.Assertions.*

class MatrixResultSetTest {

  @Test
  void testSimple() {
    Matrix mtcars = Dataset.mtcars()
    ResultSet rs = new MatrixResultSet(mtcars)
    int i = 0
    println mtcars.columnNames()
    while (rs.next()) {
      assertEquals(mtcars[i, 0, String], rs.getString(1))
      assertEquals(mtcars[i, 1, BigDecimal], rs.getBigDecimal(2))
      assertEquals(mtcars[i, 2, int, 0], rs.getInt(3))
      assertEquals(mtcars[i, 0], rs.getObject('model'))
      assertEquals(mtcars[i, 0, String], rs.getObject('model', String))
      assertEquals(mtcars[i, 1, BigDecimal], rs.getBigDecimal('mpg'))
      assertEquals(mtcars[i, 2, int, 0], rs.getInt('cyl'), mtcars.row(i).toString())
      i++
    }
    rs.first()
    rs.updateString(1, "Foo")
    Matrix m = rs.unwrap(Matrix)
    assertEquals("Foo", m[0,0])
  }
}
