package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleAlphaIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

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
    // Values > 1.0 should be clamped to 1.0
    assertEquals(1.0 as BigDecimal, scale.transform(1.5))
    assertEquals(1.0 as BigDecimal, scale.transform(2.0))
    assertEquals(1.0 as BigDecimal, scale.transform(100))
  }

  @Test
  void testClampingLowerBound() {
    def scale = new ScaleAlphaIdentity()
    // Values < 0.0 should be clamped to 0.0
    assertEquals(0.0 as BigDecimal, scale.transform(-0.5))
    assertEquals(0.0 as BigDecimal, scale.transform(-1.0))
    assertEquals(0.0 as BigDecimal, scale.transform(-100))
  }

  @Test
  void testNullValue() {
    def scale = new ScaleAlphaIdentity()
    assertEquals(1.0 as BigDecimal, scale.transform(null))
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleAlphaIdentity(naValue: 0.5)
    assertEquals(0.5 as BigDecimal, scale.transform(null))
  }

  @Test
  void testName() {
    def scale = new ScaleAlphaIdentity(name: 'Transparency')
    assertEquals('Transparency', scale.name)
  }

  @Test
  void testWithChart() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'transparency'])
        .rows([
            [1, 2, 0.3],
            [2, 4, 0.7],
            [3, 6, 1.0]
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', alpha: 'transparency')) +
        geom_point() +
        scale_alpha_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_alpha_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleAlphaIdentity)
    assertEquals('alpha', scale.aesthetic)
  }

  @Test
  void testFactoryMethodWithParams() {
    def scale = scale_alpha_identity(naValue: 0.8, name: 'My Alpha')
    assertNotNull(scale)
    assertEquals('My Alpha', scale.name)
    assertEquals(0.8 as BigDecimal, scale.naValue)
  }

  @Test
  void testAesthetic() {
    def scale = new ScaleAlphaIdentity()
    assertEquals('alpha', scale.aesthetic)
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
    // Should handle numeric strings
    def result = scale.transform('0.6')
    assertNotNull(result)
    assertEquals(0.6 as BigDecimal, result)
  }
}
