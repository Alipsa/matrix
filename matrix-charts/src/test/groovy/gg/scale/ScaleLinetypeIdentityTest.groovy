package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleLinetypeIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

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
  void testNullValue() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('solid', scale.transform(null))
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleLinetypeIdentity(naValue: 'dashed')
    assertEquals('dashed', scale.transform(null))
  }

  @Test
  void testName() {
    def scale = new ScaleLinetypeIdentity(name: 'Line Styles')
    assertEquals('Line Styles', scale.name)
  }

  @Test
  void testWithChart() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'lineStyle'])
        .rows([
            [1, 2, 'solid'],
            [2, 4, 'dashed'],
            [3, 6, 'dotted']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', linetype: 'lineStyle')) +
        geom_line() +
        scale_linetype_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_linetype_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleLinetypeIdentity)
    assertEquals('linetype', scale.aesthetic)
  }

  @Test
  void testFactoryMethodWithParams() {
    def scale = scale_linetype_identity(naValue: 'dotted', name: 'My Linetypes')
    assertNotNull(scale)
    assertEquals('My Linetypes', scale.name)
    assertEquals('dotted', scale.naValue)
  }

  @Test
  void testAesthetic() {
    def scale = new ScaleLinetypeIdentity()
    assertEquals('linetype', scale.aesthetic)
  }

  @Test
  void testAllCommonLinetypes() {
    def scale = new ScaleLinetypeIdentity()
    def linetypes = ['solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash']
    linetypes.each { linetype ->
      assertEquals(linetype, scale.transform(linetype))
    }
  }

  @Test
  void testCustomLinetype() {
    def scale = new ScaleLinetypeIdentity()
    // Custom/unknown linetypes should pass through
    assertEquals('myCustomLinetype', scale.transform('myCustomLinetype'))
  }

  @Test
  void testNumericLinetypeToString() {
    def scale = new ScaleLinetypeIdentity()
    // Numbers should be converted to strings
    assertEquals('1', scale.transform(1))
    assertEquals('2', scale.transform(2))
  }

  // Edge case tests

  @Test
  void testCustomDashPattern() {
    def scale = new ScaleLinetypeIdentity()
    // Custom SVG dash patterns should pass through
    assertEquals('5,5', scale.transform('5,5'))
    assertEquals('10,5,2,5', scale.transform('10,5,2,5'))
    assertEquals('2,2,8,2', scale.transform('2,2,8,2'))
  }

  @Test
  void testEmptyString() {
    def scale = new ScaleLinetypeIdentity()
    // Empty string should convert to string
    assertEquals('', scale.transform(''))
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleLinetypeIdentity()
    // Boolean values should convert to string
    assertEquals('true', scale.transform(true))
    assertEquals('false', scale.transform(false))
  }

  @Test
  void testCaseVariations() {
    def scale = new ScaleLinetypeIdentity()
    // Linetype names should pass through with original case
    assertEquals('SOLID', scale.transform('SOLID'))
    assertEquals('Dashed', scale.transform('Dashed'))
    assertEquals('DotTed', scale.transform('DotTed'))
  }

  @Test
  void testWhitespace() {
    def scale = new ScaleLinetypeIdentity()
    // Whitespace should be preserved
    assertEquals(' solid ', scale.transform(' solid '))
    assertEquals('  dashed', scale.transform('  dashed'))
  }

  @Test
  void testInvalidLinetype() {
    def scale = new ScaleLinetypeIdentity()
    // Invalid/unknown linetype names should pass through
    // (rendering layer will handle fallback to solid)
    assertEquals('invalid_type', scale.transform('invalid_type'))
    assertEquals('xyz123', scale.transform('xyz123'))
  }

  @Test
  void testBlankLinetype() {
    def scale = new ScaleLinetypeIdentity()
    // 'blank' is a valid linetype (no line drawn)
    assertEquals('blank', scale.transform('blank'))
  }
}
