package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleLinetypeIdentity

import static org.junit.jupiter.api.Assertions.*

/**
 * Type-specific edge-case tests for ScaleLinetypeIdentity.
 * Shared contract (testName, testAesthetic, testNullValue, testCustomNaValue,
 * testFactoryMethod, testFactoryMethodWithParams) is in ScaleIdentityContractTest.
 * Render tests are in ScaleIdentityIntegrationTest.
 */
class ScaleLinetypeIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('solid', scale.transform('solid'))
    assertEquals('dashed', scale.transform('dashed'))
    assertEquals('dotted', scale.transform('dotted'))
    assertEquals('dotdash', scale.transform('dotdash'))
  }

  @Test
  void testAllCommonLinetypes() {
    def scale = new ScaleLinetypeIdentity()
    ['solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash'].each { linetype ->
      assertEquals(linetype, scale.transform(linetype))
    }
  }

  @Test
  void testCustomLinetype() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('myCustomLinetype', scale.transform('myCustomLinetype'))
  }

  @Test
  void testNumericLinetypeToString() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('1', scale.transform(1))
    assertEquals('2', scale.transform(2))
  }

  @Test
  void testCustomDashPattern() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('5,5', scale.transform('5,5'))
    assertEquals('10,5,2,5', scale.transform('10,5,2,5'))
    assertEquals('2,2,8,2', scale.transform('2,2,8,2'))
  }

  @Test
  void testEmptyString() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('', scale.transform(''))
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('true', scale.transform(true))
    assertEquals('false', scale.transform(false))
  }

  @Test
  void testCaseVariations() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('SOLID', scale.transform('SOLID'))
    assertEquals('Dashed', scale.transform('Dashed'))
    assertEquals('DotTed', scale.transform('DotTed'))
  }

  @Test
  void testWhitespace() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals(' solid ', scale.transform(' solid '))
    assertEquals('  dashed', scale.transform('  dashed'))
  }

  @Test
  void testInvalidLinetype() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('invalid_type', scale.transform('invalid_type'))
    assertEquals('xyz123', scale.transform('xyz123'))
  }

  @Test
  void testBlankLinetype() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('blank', scale.transform('blank'))
  }
}
