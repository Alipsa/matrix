package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleSizeIdentity

import static org.junit.jupiter.api.Assertions.*

/**
 * Type-specific edge-case tests for ScaleSizeIdentity.
 * Shared contract (testName, testAesthetic, testNullValue, testCustomNaValue,
 * testFactoryMethod, testFactoryMethodWithParams) is in ScaleIdentityContractTest.
 * Render tests are in ScaleIdentityIntegrationTest.
 */
class ScaleSizeIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleSizeIdentity()
    assertEquals(5 as BigDecimal, scale.transform(5))
    assertEquals(10 as BigDecimal, scale.transform(10))
    assertEquals(2.5 as BigDecimal, scale.transform(2.5))
  }

  @Test
  void testNumericStringTransform() {
    def scale = new ScaleSizeIdentity()
    def result = scale.transform('7.5')
    assertNotNull(result)
    assertEquals(7.5 as BigDecimal, result)
  }

  @Test
  void testIntegerTransform() {
    def scale = new ScaleSizeIdentity()
    assertEquals(4 as BigDecimal, scale.transform(4))
  }

  @Test
  void testBigDecimalTransform() {
    def scale = new ScaleSizeIdentity()
    assertEquals(12.75 as BigDecimal, scale.transform(12.75G))
  }

  @Test
  void testNegativeValue() {
    def scale = new ScaleSizeIdentity()
    assertEquals(0.1 as BigDecimal, scale.transform(-5))
    assertEquals(0.1 as BigDecimal, scale.transform(-0.5))
  }

  @Test
  void testZeroValue() {
    def scale = new ScaleSizeIdentity()
    assertEquals(0.1 as BigDecimal, scale.transform(0))
    assertEquals(0.1 as BigDecimal, scale.transform(0.0))
  }

  @Test
  void testVerySmallPositiveValue() {
    def scale = new ScaleSizeIdentity()
    assertEquals(0.1 as BigDecimal, scale.transform(0.05))
    assertEquals(0.1 as BigDecimal, scale.transform(0.001))
  }

  @Test
  void testNonNumericString() {
    def scale = new ScaleSizeIdentity()
    assertEquals(3.0 as BigDecimal, scale.transform('abc'))
    assertEquals(3.0 as BigDecimal, scale.transform('not a number'))
  }

  @Test
  void testEmptyString() {
    def scale = new ScaleSizeIdentity()
    assertEquals(3.0 as BigDecimal, scale.transform(''))
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleSizeIdentity()
    assertEquals(3.0 as BigDecimal, scale.transform(true))
    assertEquals(3.0 as BigDecimal, scale.transform(false))
  }

  @Test
  void testVeryLargeValue() {
    def scale = new ScaleSizeIdentity()
    assertEquals(1000 as BigDecimal, scale.transform(1000))
  }
}
