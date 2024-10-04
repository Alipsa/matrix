
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix

import static org.junit.jupiter.api.Assertions.*

class GinqTest {

  @Test
  void testSimple() {
    Matrix m = Matrix.builder().data(
        name: ['Orange', 'Apple', 'Banana', 'Mango', 'Durian'],
        price: [11,6,4,29,32])
    .types(String, int)
    .build()

    def expected = [['Mango', 29], ['Orange', 11], ['Apple', 6], ['Banana', 4]]

    def result = GQ {
      from f in m.rows()
      where f.price < 32
      orderby f.price in desc
      select f.name, f.price
    }.toList()
    assertIterableEquals(expected, result)
  }
}
