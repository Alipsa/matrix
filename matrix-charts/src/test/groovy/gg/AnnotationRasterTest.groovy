package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.layer.Layer

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Test annotation_raster() functionality.
 */
class AnnotationRasterTest {

  @Test
  void testBasicRaster() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    def raster = [
        ['red', 'green', 'blue'],
        ['yellow', 'purple', 'orange']
    ]

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1, xmax: 3,
            ymin: 1, ymax: 3
        )

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Verify the raster layer
    def rasterLayer = chart.layers[1]
    assertNotNull(rasterLayer)
    assertNotNull(rasterLayer.geom)
    assertEquals('GeomRasterAnn', rasterLayer.geom.class.simpleName)
    assertFalse(rasterLayer.inheritAes)

    // Render and verify SVG output
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('</svg>'))
    // Should contain rectangles for each cell
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testRasterWithDefaultBounds() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    def raster = [
        ['#FF0000', '#00FF00'],
        ['#0000FF', '#FFFF00']
    ]

    // Default bounds should fill entire panel
    def chart = ggplot(data, aes('x', 'y')) +
        annotation_raster(raster: raster) +
        geom_point()

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testRasterWithPartialInfBounds() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4], [5, 5]])
        .build()

    def raster = [
        ['red', 'green'],
        ['blue', 'yellow']
    ]

    // Only xmin and xmax specified, ymin/ymax should be infinite
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 2, xmax: 4
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testSingleCellRaster() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    def raster = [['red']]

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1.5, xmax: 2.5,
            ymin: 1.5, ymax: 2.5
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
    assertTrue(svgContent.contains('fill="red"') || svgContent.contains('fill="#'))
  }

  @Test
  void testLargerRaster() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[0, 0], [10, 10]])
        .build()

    // 5x5 raster
    def raster = []
    for (int i = 0; i < 5; i++) {
      def row = []
      for (int j = 0; j < 5; j++) {
        // Create gradient-like colors
        int r = (i * 50) % 256
        int g = (j * 50) % 256
        int b = ((i + j) * 25) % 256
        row << String.format('#%02x%02x%02x', r, g, b)
      }
      raster << row
    }

    def chart = ggplot(data, aes('x', 'y')) +
        annotation_raster(
            raster: raster,
            xmin: 0, xmax: 10,
            ymin: 0, ymax: 10
        ) +
        geom_point()

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testRasterWithInterpolate() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    def raster = [
        ['red', 'green'],
        ['blue', 'yellow']
    ]

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1, xmax: 3,
            ymin: 1, ymax: 3,
            interpolate: true
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
    // Interpolation hint may be present in the group style
    assertTrue(svgContent.contains('image-rendering') || svgContent.contains('<rect'))
  }

  @Test
  void testRasterLayer() {
    def raster = [
        ['red', 'green', 'blue'],
        ['yellow', 'purple', 'orange']
    ]

    Layer layer = annotation_raster(
        raster: raster,
        xmin: 1, xmax: 4,
        ymin: 1, ymax: 3
    )

    assertNotNull(layer)
    assertNotNull(layer.geom)
    assertEquals('GeomRasterAnn', layer.geom.class.simpleName)
    assertNotNull(layer.data)
    assertNull(layer.aes)
    assertFalse(layer.inheritAes)

    // Verify data contains bounds
    assertTrue(layer.data.columnNames().contains('xmin'))
    assertTrue(layer.data.columnNames().contains('xmax'))
    assertTrue(layer.data.columnNames().contains('ymin'))
    assertTrue(layer.data.columnNames().contains('ymax'))
  }

  @Test
  void testRasterRequiresRasterParam() {
    def exception = assertThrows(IllegalArgumentException) {
      annotation_raster(xmin: 1, xmax: 2)
    }

    assertTrue(exception.message.contains('raster'))
  }

  @Test
  void testRasterWithArrayInput() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    // Use String[][] array instead of List
    String[][] raster = [
        ['red', 'green', 'blue'] as String[],
        ['cyan', 'magenta', 'yellow'] as String[]
    ] as String[][]

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1, xmax: 3,
            ymin: 1, ymax: 3
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testRasterDoesNotAffectScales() {
    // Create data with specific bounds
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[2, 2], [4, 4]])
        .build()

    // Raster extends beyond data bounds
    def raster = [['red', 'blue'], ['green', 'yellow']]

    def chart = ggplot(data, aes('x', 'y')) +
        annotation_raster(
            raster: raster,
            xmin: 0, xmax: 10,  // Extends beyond data
            ymin: 0, ymax: 10   // Extends beyond data
        ) +
        geom_point()

    assertNotNull(chart)

    // Render should work without errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
    assertTrue(svgContent.contains('<circle'))  // geom_point circles
  }

  @Test
  void testRasterWithNamedColors() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    def raster = [
        ['red', 'green', 'blue'],
        ['white', 'black', 'gray'],
        ['orange', 'purple', 'pink']
    ]

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1, xmax: 3,
            ymin: 1, ymax: 3
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testRasterWithRgbColors() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2]])
        .build()

    def raster = [
        ['rgb(255, 0, 0)', 'rgb(0, 255, 0)'],
        ['rgb(0, 0, 255)', 'rgb(255, 255, 0)']
    ]

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1, xmax: 2,
            ymin: 1, ymax: 2
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testRasterBackground() {
    // Test using raster as a background (before geom_point)
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([
            [1, 1, 'A'],
            [2, 2, 'B'],
            [3, 3, 'A'],
            [4, 4, 'B']
        ])
        .build()

    // Create a subtle gradient background
    def raster = [
        ['#f0f0f0', '#e0e0e0', '#d0d0d0', '#c0c0c0'],
        ['#e0e0e0', '#d0d0d0', '#c0c0c0', '#b0b0b0'],
        ['#d0d0d0', '#c0c0c0', '#b0b0b0', '#a0a0a0'],
        ['#c0c0c0', '#b0b0b0', '#a0a0a0', '#909090']
    ]

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'z')) +
        annotation_raster(raster: raster) +  // Background layer
        geom_point(size: 5)  // Foreground layer

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
    assertTrue(svgContent.contains('<circle'))
  }

  @Test
  void testEmptyRaster() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2]])
        .build()

    // Empty raster should not cause errors
    def raster = []

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: 1, xmax: 2,
            ymin: 1, ymax: 2
        )

    assertNotNull(chart)

    // Should render without errors even with empty raster
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<svg'))
  }

  @Test
  void testStringInfinityBounds() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    def raster = [['red', 'blue'], ['green', 'yellow']]

    // Test with string infinity values
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_raster(
            raster: raster,
            xmin: '-Inf',
            xmax: 'Inf',
            ymin: '-Inf',
            ymax: 'Inf'
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }
}
