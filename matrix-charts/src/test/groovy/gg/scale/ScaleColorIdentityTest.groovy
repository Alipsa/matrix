package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleColorIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Type-specific edge-case tests for ScaleColorIdentity.
 * Shared contract (testName, testAesthetic, testNullValue, testCustomNaValue,
 * testFactoryMethod, testFactoryMethodWithParams) is in ScaleIdentityContractTest.
 * Render tests are in ScaleIdentityIntegrationTest.
 */
class ScaleColorIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleColorIdentity()
    assertEquals('red', scale.transform('red'))
    assertEquals('blue', scale.transform('blue'))
    assertEquals('green', scale.transform('green'))
    assertEquals('yellow', scale.transform('yellow'))
  }

  @Test
  void testHexColorTransform() {
    def scale = new ScaleColorIdentity()
    assertEquals('#FF0000', scale.transform('#FF0000'))
    assertEquals('#00FF00', scale.transform('#00FF00'))
  }

  @Test
  void testNullValue() {
    def scale = new ScaleColorIdentity()
    // grey50 may be normalised to a hex value by the color pipeline
    assertNotNull(scale.transform(null))
    assertTrue(scale.transform(null).toString().startsWith('#') ||
               scale.transform(null) == 'grey50')
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleColorIdentity(naValue: 'black')
    assertEquals('black', scale.transform(null))
  }

  @Test
  void testCustomNaValueHex() {
    def scale = new ScaleColorIdentity(naValue: '#CCCCCC')
    assertEquals('#CCCCCC', scale.transform(null))
  }

  @Test
  void testBritishFactoryMethod() {
    def scale = scale_colour_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorIdentity)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testUnknownColor() {
    def scale = new ScaleColorIdentity()
    def result = scale.transform('unknowncolor123')
    assertNotNull(result)
    assertEquals('unknowncolor123', result)
  }

  @Test
  void testGreyShades() {
    def scale = new ScaleColorIdentity()
    def grey50 = scale.transform('grey50')
    assertNotNull(grey50)
    assertTrue(grey50.toString().startsWith('#'))

    def gray75 = scale.transform('gray75')
    assertNotNull(gray75)
    assertTrue(gray75.toString().startsWith('#'))
  }

  @Test
  void testHexColorVariations() {
    def scale = new ScaleColorIdentity()
    assertEquals('#F00', scale.transform('#F00'))        // 3-digit
    assertEquals('#FF0000', scale.transform('#FF0000'))  // 6-digit
    assertEquals('#FF000080', scale.transform('#FF000080'))  // 8-digit with alpha
  }

  @Test
  void testInvalidHexColor() {
    def scale = new ScaleColorIdentity()
    assertEquals('#GGG', scale.transform('#GGG'))
    assertEquals('#12', scale.transform('#12'))
    assertEquals('#ZZZZZ', scale.transform('#ZZZZZ'))
  }

  @Test
  void testEmptyString() {
    def scale = new ScaleColorIdentity()
    def result = scale.transform('')
    assertNotNull(result)
    assertEquals('', result)
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleColorIdentity()
    assertEquals('true', scale.transform(true))
    assertEquals('false', scale.transform(false))
  }

  @Test
  void testNumericValue() {
    def scale = new ScaleColorIdentity()
    assertEquals('123', scale.transform(123))
    assertEquals('42', scale.transform(42))
  }

  @Test
  void testWhitespace() {
    def scale = new ScaleColorIdentity()
    def result = scale.transform(' red ')
    assertNotNull(result)
    assertTrue(result == 'red' || result == ' red ')
  }

  @Test
  void testCaseInsensitivity() {
    def scale = new ScaleColorIdentity()
    assertEquals('RED', scale.transform('RED'))
    assertEquals('Blue', scale.transform('Blue'))
    assertEquals('GrEeN', scale.transform('GrEeN'))
  }

  @Test
  void testRGBFormat() {
    def scale = new ScaleColorIdentity()
    assertEquals('rgb(255,0,0)', scale.transform('rgb(255,0,0)'))
    assertEquals('rgba(0,255,0,0.5)', scale.transform('rgba(0,255,0,0.5)'))
  }

  @Test
  void testHSLFormat() {
    def scale = new ScaleColorIdentity()
    assertEquals('hsl(120,100%,50%)', scale.transform('hsl(120,100%,50%)'))
    assertEquals('hsla(240,100%,50%,0.5)', scale.transform('hsla(240,100%,50%,0.5)'))
  }
}
