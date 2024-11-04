import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.ListConverter

import static org.junit.jupiter.api.Assertions.*

class ListConverterTest {

  @Test
  void testConvertStringsToDoubles() {
    assertIterableEquals([5.0d, 1.0d, 25.5d], ListConverter.toDoubles(['5.0', '1.0', '25.5']))
  }
}
