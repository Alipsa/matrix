package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleShape
import se.alipsa.matrix.gg.scale.ScaleShapeManual
import se.alipsa.matrix.gg.scale.ScaleShapeBinned

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for shape scales.
 */
class ScaleShapeTest {

  @Test
  void testScaleShapeBasic() {
    ScaleShape scale = new ScaleShape()
    scale.train(['A', 'B', 'C'])

    assertEquals('circle', scale.transform('A'))
    assertEquals('triangle', scale.transform('B'))
    assertEquals('square', scale.transform('C'))
  }

  @Test
  void testScaleShapeCustomShapes() {
    ScaleShape scale = new ScaleShape(shapes: ['square', 'circle'])
    scale.train(['A', 'B', 'C'])

    assertEquals('square', scale.transform('A'))
    assertEquals('circle', scale.transform('B'))
    // Should cycle back to first shape
    assertEquals('square', scale.transform('C'))
  }

  @Test
  void testScaleShapeNullValue() {
    ScaleShape scale = new ScaleShape()
    scale.train(['A', 'B'])

    assertNull(scale.transform(null))
  }

  @Test
  void testScaleShapeUnknownValue() {
    ScaleShape scale = new ScaleShape()
    scale.train(['A', 'B'])

    // Unknown value should return null
    assertNull(scale.transform('C'))
  }

  @Test
  void testScaleShapeWithLimits() {
    ScaleShape scale = new ScaleShape(limits: ['B', 'C'])
    scale.train(['A', 'B', 'C'])

    // Only B and C should be in levels due to limits
    assertEquals('circle', scale.transform('B'))
    assertEquals('triangle', scale.transform('C'))
  }

  @Test
  void testScaleShapeManualListValues() {
    ScaleShapeManual scale = new ScaleShapeManual(values: ['triangle', 'diamond'])
    scale.train(['A', 'B', 'C'])

    assertEquals('triangle', scale.transform('A'))
    assertEquals('diamond', scale.transform('B'))
    // Should cycle
    assertEquals('triangle', scale.transform('C'))
  }

  @Test
  void testScaleShapeManualNamedValues() {
    ScaleShapeManual scale = new ScaleShapeManual(values: [A: 'diamond', B: 'plus', C: 'x'])
    scale.train(['A', 'B', 'C'])

    assertEquals('diamond', scale.transform('A'))
    assertEquals('plus', scale.transform('B'))
    assertEquals('x', scale.transform('C'))
  }

  @Test
  void testScaleShapeManualNullValue() {
    ScaleShapeManual scale = new ScaleShapeManual(values: ['square'], naValue: 'cross')
    scale.train(['A', 'B'])

    assertEquals('cross', scale.transform(null))
  }

  @Test
  void testScaleShapeManualDefaultShapes() {
    ScaleShapeManual scale = new ScaleShapeManual()
    scale.train(['A', 'B', 'C'])

    // Should use default shapes when no values provided
    assertEquals('circle', scale.transform('A'))
    assertEquals('triangle', scale.transform('B'))
    assertEquals('square', scale.transform('C'))
  }

  @Test
  void testScaleShapeManualInverse() {
    ScaleShapeManual scale = new ScaleShapeManual(values: ['triangle', 'diamond'])
    scale.train(['A', 'B'])

    assertEquals('A', scale.inverse('triangle'))
    assertEquals('B', scale.inverse('diamond'))
    assertNull(scale.inverse('circle'))
  }

  @Test
  void testScaleShapeManualInverseNamed() {
    ScaleShapeManual scale = new ScaleShapeManual(values: [cat1: 'diamond', cat2: 'plus'])
    scale.train(['cat1', 'cat2'])

    assertEquals('cat1', scale.inverse('diamond'))
    assertEquals('cat2', scale.inverse('plus'))
  }

  @Test
  void testScaleShapeManualInverseWithCycledShapes() {
    // Test that inverse correctly handles cycled shapes (more levels than shapes)
    ScaleShapeManual scale = new ScaleShapeManual(values: ['triangle', 'diamond'])
    scale.train(['A', 'B', 'C', 'D'])

    // A maps to triangle (index 0 % 2 = 0)
    // B maps to diamond (index 1 % 2 = 1)
    // C maps to triangle (index 2 % 2 = 0) - cycled
    // D maps to diamond (index 3 % 2 = 1) - cycled

    // Verify transformations
    assertEquals('triangle', scale.transform('A'))
    assertEquals('diamond', scale.transform('B'))
    assertEquals('triangle', scale.transform('C'))
    assertEquals('diamond', scale.transform('D'))

    // Inverse should return the FIRST level that maps to each shape
    assertEquals('A', scale.inverse('triangle'))  // Not 'C'
    assertEquals('B', scale.inverse('diamond'))   // Not 'D'
  }

  @Test
  void testScaleShapeManualGetShapeForIndex() {
    ScaleShapeManual scale = new ScaleShapeManual(values: ['square', 'circle'])
    scale.train(['A', 'B', 'C'])

    assertEquals('square', scale.getShapeForIndex(0))
    assertEquals('circle', scale.getShapeForIndex(1))
    assertEquals('square', scale.getShapeForIndex(2))
  }

  @Test
  void testScaleShapeManualGetShapes() {
    ScaleShapeManual scale = new ScaleShapeManual(values: ['triangle', 'diamond'])
    scale.train(['A', 'B', 'C'])

    List<String> shapes = scale.getShapes()
    assertEquals(3, shapes.size())
    assertEquals('triangle', shapes[0])
    assertEquals('diamond', shapes[1])
    assertEquals('triangle', shapes[2])
  }

  @Test
  void testScaleShapeBinnedBasic() {
    ScaleShapeBinned scale = new ScaleShapeBinned(bins: 3)
    scale.train([0, 100])

    // Test that different values map to different bins/shapes
    String shape0 = scale.transform(0) as String
    String shape50 = scale.transform(50) as String
    String shape100 = scale.transform(100) as String

    assertNotNull(shape0)
    assertNotNull(shape50)
    assertNotNull(shape100)

    // First and last should be different
    assertNotEquals(shape0, shape100)
  }

  @Test
  void testScaleShapeBinnedCustomShapes() {
    ScaleShapeBinned scale = new ScaleShapeBinned(
        bins: 3,
        shapes: ['circle', 'square', 'triangle']
    )
    scale.train([0, 100])

    assertEquals('circle', scale.transform(0))
    assertEquals('square', scale.transform(50))
    assertEquals('triangle', scale.transform(100))
  }

  @Test
  void testScaleShapeBinnedNullValue() {
    ScaleShapeBinned scale = new ScaleShapeBinned(naValue: 'cross')
    scale.train([0, 100])

    assertEquals('cross', scale.transform(null))
  }

  @Test
  void testScaleShapeBinnedConstantDomain() {
    ScaleShapeBinned scale = new ScaleShapeBinned(shapes: ['circle', 'square'])
    scale.train([50, 50])

    // All values should map to first shape when domain is constant
    assertEquals('circle', scale.transform(50))
  }

  @Test
  void testScaleShapeBinnedSingleBin() {
    ScaleShapeBinned scale = new ScaleShapeBinned(bins: 1, shapes: ['circle', 'square'])
    scale.train([0, 100])

    // With only one bin, all values map to first shape
    assertEquals('circle', scale.transform(0))
    assertEquals('circle', scale.transform(50))
    assertEquals('circle', scale.transform(100))
  }

  @Test
  void testScaleShapeBinnedMoreBinsThanShapes() {
    ScaleShapeBinned scale = new ScaleShapeBinned(
        bins: 5,
        shapes: ['circle', 'square']
    )
    scale.train([0, 100])

    // Should cycle through shapes
    String shape0 = scale.transform(0) as String
    String shape100 = scale.transform(100) as String

    assertNotNull(shape0)
    assertNotNull(shape100)
    // Both should be one of the two shapes
    assertTrue(shape0 in ['circle', 'square'])
    assertTrue(shape100 in ['circle', 'square'])
  }

  @Test
  void testScaleShapeFactory() {
    def scale = scale_shape()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleShape)
  }

  @Test
  void testScaleShapeDiscreteFactory() {
    def scale = scale_shape_discrete()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleShape)
  }

  @Test
  void testScaleShapeBinnedFactory() {
    def scale = scale_shape_binned()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleShapeBinned)
  }

  @Test
  void testScaleShapeManualFactory() {
    def scale = scale_shape_manual()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleShapeManual)
  }

  @Test
  void testScaleShapeWithGeomPoint() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B'],
            [3, 1, 'A'],
            [4, 4, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', shape: 'category')) +
        geom_point() +
        scale_shape()

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
    // Should contain circles and triangles
    assertTrue(content.contains('<circle') || content.contains('<polygon'))
  }

  @Test
  void testScaleShapeManualWithGeomPoint() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'type')
        .rows([
            [1, 2, 'cat1'],
            [2, 3, 'cat2'],
            [3, 1, 'cat1']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', shape: 'type')) +
        geom_point() +
        scale_shape_manual(values: ['square', 'diamond'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
    // Should render successfully
    assertTrue(content.contains('<rect') || content.contains('<polygon'))
  }

  @Test
  void testScaleShapeBinnedWithGeomPoint() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 2, 10],
            [2, 3, 50],
            [3, 1, 90]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', shape: 'value')) +
        geom_point() +
        scale_shape_binned(bins: 3)

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testScaleShapeWithCustomName() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', shape: 'cat')) +
        geom_point() +
        scale_shape(name: 'Category Type')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain the custom legend title
    assertTrue(content.contains('Category Type'))
  }

  @Test
  void testScaleShapeManualWithNamedMapping() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 2, 'control'],
            [2, 3, 'treatment'],
            [3, 1, 'control']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', shape: 'group')) +
        geom_point() +
        scale_shape_manual(values: [control: 'circle', treatment: 'triangle'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
  }
}
