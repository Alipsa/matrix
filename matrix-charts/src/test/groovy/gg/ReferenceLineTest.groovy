package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomHline
import se.alipsa.matrix.gg.geom.GeomVline
import se.alipsa.matrix.gg.geom.GeomAbline
import se.alipsa.matrix.gg.geom.GeomSegment

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ReferenceLineTest {

  // ==================== GeomHline Tests ====================

  @Test
  void testGeomHlineDefaults() {
    GeomHline geom = new GeomHline()

    assertNull(geom.yintercept)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
  }

  @Test
  void testGeomHlineWithParams() {
    GeomHline geom = new GeomHline(
        yintercept: 5,
        color: 'red',
        linewidth: 2,
        linetype: 'dashed',
        alpha: 0.5
    )

    assertEquals(5, geom.yintercept)
    assertEquals('red', geom.color)
    assertEquals(2, geom.linewidth)
    assertEquals('dashed', geom.linetype)
    assertEquals(0.5, geom.alpha)
  }

  @Test
  void testGeomHlineWithBritishSpelling() {
    GeomHline geom = new GeomHline(colour: 'blue')
    assertEquals('blue', geom.color)
  }

  @Test
  void testGeomHlineMultipleIntercepts() {
    GeomHline geom = new GeomHline(yintercept: [10, 20, 30])
    assertEquals([10, 20, 30], geom.yintercept)
  }

  @Test
  void testHlineInChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 30],
            [3, 20],
            [4, 40],
            [5, 25]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_hline(yintercept: 25, color: 'red', linetype: 'dashed') +
        labs(title: 'Scatter with Horizontal Reference Line')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain point elements")
    assertTrue(content.contains('<line'), "Should contain line elements")

    File outputFile = new File('build/hline_test.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testMultipleHlines() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 50],
            [3, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_hline(yintercept: [20, 40], color: 'blue', linetype: 'dotted') +
        labs(title: 'Multiple Horizontal Lines')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/multiple_hlines.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomVline Tests ====================

  @Test
  void testGeomVlineDefaults() {
    GeomVline geom = new GeomVline()

    assertNull(geom.xintercept)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
  }

  @Test
  void testGeomVlineWithParams() {
    GeomVline geom = new GeomVline(
        xintercept: 3,
        color: 'green',
        linewidth: 1.5,
        linetype: 'longdash',
        alpha: 0.7
    )

    assertEquals(3, geom.xintercept)
    assertEquals('green', geom.color)
    assertEquals(1.5, geom.linewidth)
    assertEquals('longdash', geom.linetype)
    assertEquals(0.7, geom.alpha)
  }

  @Test
  void testVlineInChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 30],
            [3, 20],
            [4, 40],
            [5, 25]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_vline(xintercept: 3, color: 'orange', linewidth: 2) +
        labs(title: 'Scatter with Vertical Reference Line')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")

    File outputFile = new File('build/vline_test.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testMultipleVlines() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [5, 50],
            [10, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_vline(xintercept: [3, 7], color: 'purple', linetype: 'dashed') +
        labs(title: 'Multiple Vertical Lines')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/multiple_vlines.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomAbline Tests ====================

  @Test
  void testGeomAblineDefaults() {
    GeomAbline geom = new GeomAbline()

    assertEquals(1, geom.slope)
    assertEquals(0, geom.intercept)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
  }

  @Test
  void testGeomAblineWithParams() {
    GeomAbline geom = new GeomAbline(
        slope: 2,
        intercept: 5,
        color: 'red',
        linewidth: 1.5,
        linetype: 'dashed'
    )

    assertEquals(2, geom.slope)
    assertEquals(5, geom.intercept)
    assertEquals('red', geom.color)
    assertEquals(1.5, geom.linewidth)
    assertEquals('dashed', geom.linetype)
  }

  @Test
  void testAblineInChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 3],
            [2, 5],
            [3, 4],
            [4, 8],
            [5, 9]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_abline(slope: 1.5, intercept: 1, color: 'blue', linetype: 'dashed') +
        labs(title: 'Scatter with Trend Line')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")

    File outputFile = new File('build/abline_test.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testAblineDiagonal() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 0],
            [5, 5],
            [10, 10]
        ])
        .types(Integer, Integer)
        .build()

    // y = x line (45 degree diagonal)
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_abline(slope: 1, intercept: 0, color: 'gray', linetype: 'dotted') +
        labs(title: 'Diagonal Reference Line (y=x)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/abline_diagonal.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomSegment Tests ====================

  @Test
  void testGeomSegmentDefaults() {
    GeomSegment geom = new GeomSegment()

    assertNull(geom.x)
    assertNull(geom.y)
    assertNull(geom.xend)
    assertNull(geom.yend)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
    assertFalse(geom.arrow)
  }

  @Test
  void testGeomSegmentWithParams() {
    GeomSegment geom = new GeomSegment(
        x: 1, y: 1,
        xend: 5, yend: 5,
        color: 'red',
        linewidth: 2,
        arrow: true
    )

    assertEquals(1, geom.x)
    assertEquals(1, geom.y)
    assertEquals(5, geom.xend)
    assertEquals(5, geom.yend)
    assertEquals('red', geom.color)
    assertEquals(2, geom.linewidth)
    assertTrue(geom.arrow)
  }

  @Test
  void testSegmentInChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 30],
            [3, 20],
            [4, 40],
            [5, 25]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_segment(x: 1, y: 10, xend: 5, yend: 40, color: 'green', linewidth: 2) +
        labs(title: 'Scatter with Segment')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")

    File outputFile = new File('build/segment_test.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testSegmentWithArrow() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 0],
            [10, 10]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_segment(x: 2, y: 2, xend: 8, yend: 8, color: 'blue', arrow: true) +
        labs(title: 'Segment with Arrow')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/segment_arrow.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testSegmentFromData() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend')
        .rows([
            [1, 1, 3, 5],
            [2, 2, 4, 8],
            [3, 3, 5, 7]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_segment(color: 'purple') +
        labs(title: 'Segments from Data')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")

    File outputFile = new File('build/segment_from_data.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Combined Tests ====================

  @Test
  void testCombinedReferenceLines() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 15],
            [4, 25],
            [5, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 4) +
        geom_hline(yintercept: 20, color: 'red', linetype: 'dashed') +
        geom_vline(xintercept: 3, color: 'blue', linetype: 'dotted') +
        geom_abline(slope: 4, intercept: 5, color: 'green', linetype: 'longdash') +
        labs(title: 'Combined Reference Lines')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/combined_reference_lines.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLineTypes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 0],
            [10, 50]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_hline(yintercept: 10, linetype: 'solid', color: 'black') +
        geom_hline(yintercept: 20, linetype: 'dashed', color: 'red') +
        geom_hline(yintercept: 30, linetype: 'dotted', color: 'blue') +
        geom_hline(yintercept: 40, linetype: 'longdash', color: 'green') +
        labs(title: 'Line Type Comparison')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('stroke-dasharray'), "Should have dashed lines")

    File outputFile = new File('build/line_types_comparison.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFactoryMethods() {
    def hline = geom_hline(yintercept: 5)
    assertNotNull(hline)
    assertTrue(hline instanceof GeomHline)
    assertEquals(5, hline.yintercept)

    def vline = geom_vline(xintercept: 3)
    assertNotNull(vline)
    assertTrue(vline instanceof GeomVline)
    assertEquals(3, vline.xintercept)

    def abline = geom_abline(slope: 2, intercept: 1)
    assertNotNull(abline)
    assertTrue(abline instanceof GeomAbline)
    assertEquals(2, abline.slope)
    assertEquals(1, abline.intercept)

    def segment = geom_segment(x: 0, y: 0, xend: 1, yend: 1)
    assertNotNull(segment)
    assertTrue(segment instanceof GeomSegment)
    assertEquals(0, segment.x)
    assertEquals(1, segment.xend)
  }
}
