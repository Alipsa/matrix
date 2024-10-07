import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Stat
import se.alipsa.groovy.matrix.sql.MatrixResultSet
import java.sql.ResultSet
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
      i++
    }
  }
}
