package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleSizeIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleSizeIdentityTest {

  @Test
  void testBasicTransform() {
    def scale = new ScaleSizeIdentity()
    assertEquals(5 as BigDecimal, scale.transform(5))
    assertEquals(10 as BigDecimal, scale.transform(10))
    assertEquals(2.5 as BigDecimal, scale.transform(2.5))
  }

  @Test
  void testNullValue() {
    def scale = new ScaleSizeIdentity()
    assertEquals(3.0 as BigDecimal, scale.transform(null))
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleSizeIdentity(naValue: 5.0)
    assertEquals(5.0 as BigDecimal, scale.transform(null))
  }

  @Test
  void testName() {
    def scale = new ScaleSizeIdentity(name: 'Point Sizes')
    assertEquals('Point Sizes', scale.name)
  }

  @Test
  void testWithChart() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'pointSize'])
        .rows([
            [1, 2, 3],
            [2, 4, 5],
            [3, 6, 8]
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', size: 'pointSize')) +
        geom_point() +
        scale_size_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_size_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleSizeIdentity)
    assertEquals('size', scale.aesthetic)
  }

  @Test
  void testFactoryMethodWithParams() {
    def scale = scale_size_identity(naValue: 10.0, name: 'My Sizes')
    assertNotNull(scale)
    assertEquals('My Sizes', scale.name)
    assertEquals(10.0 as BigDecimal, scale.naValue)
  }

  @Test
  void testAesthetic() {
    def scale = new ScaleSizeIdentity()
    assertEquals('size', scale.aesthetic)
  }

  @Test
  void testNumericStringTransform() {
    def scale = new ScaleSizeIdentity()
    // Should handle numeric strings
    def result = scale.transform('7.5')
    assertNotNull(result)
    assertEquals(7.5 as BigDecimal, result)
  }

  @Test
  void testIntegerTransform() {
    def scale = new ScaleSizeIdentity()
    assertEquals(4 as BigDecimal, scale.transform(4))
  }

  @Test
  void testBigDecimalTransform() {
    def scale = new ScaleSizeIdentity()
    assertEquals(12.75 as BigDecimal, scale.transform(12.75G))
  }
}
