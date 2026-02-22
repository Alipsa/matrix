package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleAlphaIdentity

import static org.junit.jupiter.api.Assertions.*

/**
 * Type-specific edge-case tests for ScaleAlphaIdentity.
 * Shared contract (testName, testAesthetic, testNullValue, testCustomNaValue,
 * testFactoryMethod, testFactoryMethodWithParams) is in ScaleIdentityContractTest.
 * Render tests are in ScaleIdentityIntegrationTest.
 */
class ScaleAlphaIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleAlphaIdentity()
    assertEquals(0.5 as BigDecimal, scale.transform(0.5))
    assertEquals(0.8 as BigDecimal, scale.transform(0.8))
    assertEquals(1.0 as BigDecimal, scale.transform(1.0))
  }

  @Test
  void testClampingUpperBound() {
    def scale = new ScaleAlphaIdentity()
    assertEquals(1.0 as BigDecimal, scale.transform(1.5))
    assertEquals(1.0 as BigDecimal, scale.transform(2.0))
    assertEquals(1.0 as BigDecimal, scale.transform(100))
  }

  @Test
  void testClampingLowerBound() {
    def scale = new ScaleAlphaIdentity()
    assertEquals(0.0 as BigDecimal, scale.transform(-0.5))
    assertEquals(0.0 as BigDecimal, scale.transform(-1.0))
    assertEquals(0.0 as BigDecimal, scale.transform(-100))
  }

  @Test
  void testZeroAlpha() {
    def scale = new ScaleAlphaIdentity()
    assertEquals(0.0 as BigDecimal, scale.transform(0.0))
  }

  @Test
  void testOneAlpha() {
    def scale = new ScaleAlphaIdentity()
    assertEquals(1.0 as BigDecimal, scale.transform(1.0))
  }

  @Test
  void testNumericStringTransform() {
    def scale = new ScaleAlphaIdentity()
    def result = scale.transform('0.6')
    assertNotNull(result)
    assertEquals(0.6 as BigDecimal, result)
  }
}
