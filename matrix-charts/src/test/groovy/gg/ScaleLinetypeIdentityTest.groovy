package gg

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
}
