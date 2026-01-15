package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleColorIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleColorIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleColorIdentity()
    // Named colors are passed through as-is (SVG supports named colors)
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
    // grey50 should be normalized to a hex value
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
  void testName() {
    def scale = new ScaleColorIdentity(name: 'Point Colors')
    assertEquals('Point Colors', scale.name)
  }

  @Test
  void testWithChart() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'color'])
        .rows([
            [1, 2, 'red'],
            [2, 4, 'blue'],
            [3, 6, 'green']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'color')) +
        geom_point() +
        scale_color_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testWithChartBritishSpelling() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'colour'])
        .rows([
            [1, 2, 'red'],
            [2, 4, 'blue']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'colour')) +
        geom_point() +
        scale_colour_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_color_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorIdentity)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testFactoryMethodWithParams() {
    def scale = scale_color_identity(naValue: 'purple', name: 'My Colors')
    assertNotNull(scale)
    assertEquals('My Colors', scale.name)
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
    // Unknown colors should be returned as-is
    def result = scale.transform('unknowncolor123')
    assertNotNull(result)
    assertEquals('unknowncolor123', result)
  }
}
