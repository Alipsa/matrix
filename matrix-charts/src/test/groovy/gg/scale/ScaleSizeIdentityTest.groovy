package gg.scale

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

  // Edge case tests

  @Test
  void testNegativeValue() {
    def scale = new ScaleSizeIdentity()
    // Negative values should be clamped to 0.1
    assertEquals(0.1 as BigDecimal, scale.transform(-5))
    assertEquals(0.1 as BigDecimal, scale.transform(-0.5))
  }

  @Test
  void testZeroValue() {
    def scale = new ScaleSizeIdentity()
    // Zero should be clamped to 0.1 for visibility
    assertEquals(0.1 as BigDecimal, scale.transform(0))
    assertEquals(0.1 as BigDecimal, scale.transform(0.0))
  }

  @Test
  void testVerySmallPositiveValue() {
    def scale = new ScaleSizeIdentity()
    // Values less than 0.1 should be clamped
    assertEquals(0.1 as BigDecimal, scale.transform(0.05))
    assertEquals(0.1 as BigDecimal, scale.transform(0.001))
  }

  @Test
  void testNonNumericString() {
    def scale = new ScaleSizeIdentity()
    // Non-numeric strings should fall back to naValue
    assertEquals(3.0 as BigDecimal, scale.transform('abc'))
    assertEquals(3.0 as BigDecimal, scale.transform('not a number'))
  }

  @Test
  void testEmptyString() {
    def scale = new ScaleSizeIdentity()
    // Empty string should fall back to naValue
    assertEquals(3.0 as BigDecimal, scale.transform(''))
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleSizeIdentity()
    // Boolean values should fall back to naValue
    assertEquals(3.0 as BigDecimal, scale.transform(true))
    assertEquals(3.0 as BigDecimal, scale.transform(false))
  }

  @Test
  void testVeryLargeValue() {
    def scale = new ScaleSizeIdentity()
    // Very large values should be accepted as-is
    def result = scale.transform(1000)
    assertEquals(1000 as BigDecimal, result)
  }
}
