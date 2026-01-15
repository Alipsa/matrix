package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleFillIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleFillIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleFillIdentity()
    // Named colors are passed through as-is (SVG supports named colors)
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
    // grey50 should be normalized to a hex value or kept as grey50
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
  void testName() {
    def scale = new ScaleFillIdentity(name: 'Bar Fills')
    assertEquals('Bar Fills', scale.name)
  }

  @Test
  void testWithChart() {
    def data = Matrix.builder()
        .columnNames(['category', 'value', 'fillColor'])
        .rows([
            ['A', 10, 'red'],
            ['B', 20, 'blue'],
            ['C', 15, 'green']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value', fill: 'fillColor')) +
        geom_col() +
        scale_fill_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_fill_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleFillIdentity)
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testFactoryMethodWithParams() {
    def scale = scale_fill_identity(naValue: 'lightgray', name: 'My Fills')
    assertNotNull(scale)
    assertEquals('My Fills', scale.name)
  }

  @Test
  void testAesthetic() {
    def scale = new ScaleFillIdentity()
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testUnknownColor() {
    def scale = new ScaleFillIdentity()
    // Unknown colors should be returned as-is
    def result = scale.transform('unknowncolor456')
    assertNotNull(result)
    assertEquals('unknowncolor456', result)
  }
}
