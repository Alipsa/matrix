package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleShapeIdentity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

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
  void testNullValue() {
    def scale = new ScaleShapeIdentity()
    assertEquals('circle', scale.transform(null))
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleShapeIdentity(naValue: 'square')
    assertEquals('square', scale.transform(null))
  }

  @Test
  void testName() {
    def scale = new ScaleShapeIdentity(name: 'Point Shapes')
    assertEquals('Point Shapes', scale.name)
  }

  @Test
  void testWithChart() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'shape'])
        .rows([
            [1, 2, 'circle'],
            [2, 4, 'square'],
            [3, 6, 'triangle']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', shape: 'shape')) +
        geom_point() +
        scale_shape_identity()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_shape_identity()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleShapeIdentity)
    assertEquals('shape', scale.aesthetic)
  }

  @Test
  void testFactoryMethodWithParams() {
    def scale = scale_shape_identity(naValue: 'diamond', name: 'My Shapes')
    assertNotNull(scale)
    assertEquals('My Shapes', scale.name)
    assertEquals('diamond', scale.naValue)
  }

  @Test
  void testAesthetic() {
    def scale = new ScaleShapeIdentity()
    assertEquals('shape', scale.aesthetic)
  }

  @Test
  void testNumericShapeToString() {
    def scale = new ScaleShapeIdentity()
    // Numbers should be converted to strings
    assertEquals('1', scale.transform(1))
    assertEquals('2', scale.transform(2))
  }

  @Test
  void testAllCommonShapes() {
    def scale = new ScaleShapeIdentity()
    def shapes = ['circle', 'square', 'triangle', 'diamond', 'plus', 'x', 'cross']
    shapes.each { shape ->
      assertEquals(shape, scale.transform(shape))
    }
  }

  @Test
  void testCustomShape() {
    def scale = new ScaleShapeIdentity()
    // Custom/unknown shapes should pass through
    assertEquals('myCustomShape', scale.transform('myCustomShape'))
  }

  // Edge case tests

  @Test
  void testEmptyString() {
    def scale = new ScaleShapeIdentity()
    // Empty string should convert to string
    assertEquals('', scale.transform(''))
  }

  @Test
  void testBooleanValue() {
    def scale = new ScaleShapeIdentity()
    // Boolean values should convert to string
    assertEquals('true', scale.transform(true))
    assertEquals('false', scale.transform(false))
  }

  @Test
  void testCaseInsensitivity() {
    def scale = new ScaleShapeIdentity()
    // Shape names should pass through with original case
    assertEquals('CIRCLE', scale.transform('CIRCLE'))
    assertEquals('Circle', scale.transform('Circle'))
    assertEquals('cIrClE', scale.transform('cIrClE'))
  }

  @Test
  void testWhitespace() {
    def scale = new ScaleShapeIdentity()
    // Whitespace should be preserved
    assertEquals(' circle ', scale.transform(' circle '))
    assertEquals('  square', scale.transform('  square'))
  }

  @Test
  void testInvalidShape() {
    def scale = new ScaleShapeIdentity()
    // Invalid/unknown shape names should pass through
    // (rendering layer will handle fallback to circle)
    assertEquals('not_a_shape', scale.transform('not_a_shape'))
    assertEquals('123abc', scale.transform('123abc'))
  }
}
