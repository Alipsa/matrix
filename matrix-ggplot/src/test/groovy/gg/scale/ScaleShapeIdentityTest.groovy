package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleShapeIdentity

import static org.junit.jupiter.api.Assertions.*

/**
 * Type-specific edge-case tests for ScaleShapeIdentity.
 * Shared contract (testName, testAesthetic, testNullValue, testCustomNaValue,
 * testFactoryMethod, testFactoryMethodWithParams) is in ScaleIdentityContractTest.
 * Render tests are in ScaleIdentityIntegrationTest.
 */
class ScaleShapeIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleShapeIdentity()
    assertEquals('circle', scale.transform('circle'))
    assertEquals('square', scale.transform('square'))
    assertEquals('triangle', scale.transform('triangle'))
    assertEquals('diamond', scale.transform('diamond'))
  }

  @Test
  void testNumericShapeToString() {
    def scale = new ScaleShapeIdentity()
    assertEquals('1', scale.transform(1))
    assertEquals('2', scale.transform(2))
  }

  @Test
  void testAllCommonShapes() {
    def scale = new ScaleShapeIdentity()
    ['circle', 'square', 'triangle', 'diamond', 'plus', 'x', 'cross'].each { shape ->
      assertEquals(shape, scale.transform(shape))
    }
  }

  @Test
  void testCustomShape() {
    def scale = new ScaleShapeIdentity()
    assertEquals('myCustomShape', scale.transform('myCustomShape'))
  }

  @Test
  void testEmptyString() {
    def scale = new ScaleShapeIdentity()
    assertEquals('', scale.transform(''))
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleShapeIdentity()
    assertEquals('true', scale.transform(true))
    assertEquals('false', scale.transform(false))
  }

  @Test
  void testCaseInsensitivity() {
    def scale = new ScaleShapeIdentity()
    assertEquals('CIRCLE', scale.transform('CIRCLE'))
    assertEquals('Circle', scale.transform('Circle'))
    assertEquals('cIrClE', scale.transform('cIrClE'))
  }

  @Test
  void testWhitespace() {
    def scale = new ScaleShapeIdentity()
    assertEquals(' circle ', scale.transform(' circle '))
    assertEquals('  square', scale.transform('  square'))
  }

  @Test
  void testInvalidShape() {
    def scale = new ScaleShapeIdentity()
    assertEquals('not_a_shape', scale.transform('not_a_shape'))
    assertEquals('123abc', scale.transform('123abc'))
  }
}
