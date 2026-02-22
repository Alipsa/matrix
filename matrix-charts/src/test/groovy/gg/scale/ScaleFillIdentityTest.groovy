package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleFillIdentity

import static org.junit.jupiter.api.Assertions.*

/**
 * Type-specific edge-case tests for ScaleFillIdentity.
 * Shared contract (testName, testAesthetic, testNullValue, testCustomNaValue,
 * testFactoryMethod, testFactoryMethodWithParams) is in ScaleIdentityContractTest.
 * Render tests are in ScaleIdentityIntegrationTest.
 */
class ScaleFillIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleFillIdentity()
    assertEquals('red', scale.transform('red'))
    assertEquals('blue', scale.transform('blue'))
    assertEquals('green', scale.transform('green'))
    assertEquals('yellow', scale.transform('yellow'))
  }

  @Test
  void testHexColorTransform() {
    def scale = new ScaleFillIdentity()
    assertEquals('#FF0000', scale.transform('#FF0000'))
    assertEquals('#00FF00', scale.transform('#00FF00'))
  }

  @Test
  void testNullValue() {
    def scale = new ScaleFillIdentity()
    // grey50 may be normalised to a hex value by the color pipeline
    assertNotNull(scale.transform(null))
    assertTrue(scale.transform(null).toString().startsWith('#') ||
               scale.transform(null) == 'grey50')
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleFillIdentity(naValue: 'white')
    assertEquals('white', scale.transform(null))
  }

  @Test
  void testCustomNaValueHex() {
    def scale = new ScaleFillIdentity(naValue: '#EEEEEE')
    assertEquals('#EEEEEE', scale.transform(null))
  }

  @Test
  void testUnknownColor() {
    def scale = new ScaleFillIdentity()
    def result = scale.transform('unknowncolor456')
    assertNotNull(result)
    assertEquals('unknowncolor456', result)
  }
}
