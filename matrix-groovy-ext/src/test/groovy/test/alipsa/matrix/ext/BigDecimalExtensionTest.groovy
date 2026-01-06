package test.alipsa.matrix.ext

import org.junit.jupiter.api.Test
import se.alipsa.matrix.ext.BigDecimalExtension

class BigDecimalExtensionTest {

  @Test
  void testFloor() {
    BigDecimal value = 3.7G
    BigDecimal floored = BigDecimalExtension.floor(value)
    assert floored == 3G
  }

  @Test
  void testCeil() {
    BigDecimal value = 3.2G
    BigDecimal ceiled = BigDecimalExtension.ceil(value)
    assert ceiled == 4G
  }

  @Test
  void testLog10() {
    BigDecimal value = 1000G
    BigDecimal logValue = BigDecimalExtension.log10(value)
    assert logValue == 3G
  }
}
