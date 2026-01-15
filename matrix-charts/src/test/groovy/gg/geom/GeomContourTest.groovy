package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomContour
import se.alipsa.matrix.gg.geom.GeomContourFilled

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomContourTest {

  // ==================== GeomContour Tests ====================

  @Test
  void testGeomContourDefaults() {
    GeomContour geom = new GeomContour()

    assertEquals('black', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(1.0, geom.alpha)
    assertEquals('solid', geom.linetype)
    assertEquals(10, geom.bins)
    assertNull(geom.binwidth)
  }

  @Test
  void testGeomContourWithParams() {
    GeomContour geom = new GeomContour(
        color: 'blue',
        linewidth: 1.5,
        alpha: 0.7,
        bins: 15
    )

    assertEquals('blue', geom.color)
    assertEquals(1.5, geom.linewidth)
    assertEquals(0.7, geom.alpha)
    assertEquals(15, geom.bins)
  }

  @Test
  void testGeomContourWithBinwidth() {
    GeomContour geom = new GeomContour(
        binwidth: 0.25
    )

    assertEquals(0.25, geom.binwidth)
  }

  @Test
  void testSimpleContourPlot() {
    // Create grid data for a simple hill (z = -(x^2 + y^2))
    def rows = []
    for (double x = -2; x <= 2; x += 0.5) {
      for (double y = -2; y <= 2; y += 0.5) {
        double z = -(x * x + y * y)
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour(bins: 8, color: 'blue') +
        labs(title: 'Simple Contour Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain contour line elements")

    File outputFile = new File('build/simple_contour.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testContourPlotWithPeaks() {
    // Create grid data with two peaks
    def rows = []
    for (double x = -3; x <= 3; x += 0.4) {
      for (double y = -3; y <= 3; y += 0.4) {
        // Two Gaussian peaks
        double z = Math.exp(-((x - 1) * (x - 1) + (y - 1) * (y - 1))) +
                   0.7 * Math.exp(-((x + 1) * (x + 1) + (y + 1) * (y + 1)))
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour(bins: 10, color: 'darkgreen', linewidth: 1) +
        labs(title: 'Contour Plot with Two Peaks')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/contour_peaks.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testContourWithDashedLines() {
    def rows = []
    for (double x = -2; x <= 2; x += 0.5) {
      for (double y = -2; y <= 2; y += 0.5) {
        double z = Math.sin(x) * Math.cos(y)
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour(linetype: 'dashed', color: 'red') +
        labs(title: 'Contour with Dashed Lines')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('stroke-dasharray'), "Should contain dashed lines")

    File outputFile = new File('build/contour_dashed.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomContourFilled Tests ====================

  @Test
  void testGeomContourFilledDefaults() {
    GeomContourFilled geom = new GeomContourFilled()

    assertEquals('black', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(0.8, geom.fillAlpha)
    assertEquals(10, geom.bins)
    assertNotNull(geom.fillColors)
    assertEquals(10, geom.fillColors.size())
  }

  @Test
  void testGeomContourFilledWithParams() {
    GeomContourFilled geom = new GeomContourFilled(
        bins: 8,
        fillAlpha: 0.6,
        linewidth: 0
    )

    assertEquals(8, geom.bins)
    assertEquals(0.6, geom.fillAlpha)
    assertEquals(0, geom.linewidth)
  }

  @Test
  void testSimpleFilledContourPlot() {
    // Create grid data for a simple hill
    def rows = []
    for (double x = -2; x <= 2; x += 0.4) {
      for (double y = -2; y <= 2; y += 0.4) {
        double z = -(x * x + y * y)
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour_filled(bins: 6) +
        labs(title: 'Filled Contour Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain filled path elements")
    assertTrue(content.contains('fill='), "Paths should have fill attribute")

    File outputFile = new File('build/filled_contour.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFilledContourWithCustomColors() {
    def rows = []
    for (double x = -2; x <= 2; x += 0.5) {
      for (double y = -2; y <= 2; y += 0.5) {
        double z = x * x - y * y  // Saddle surface
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def customColors = ['#f7fbff', '#deebf7', '#c6dbef', '#9ecae1', '#6baed6',
                        '#4292c6', '#2171b5', '#08519c', '#08306b']

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour_filled(bins: 8, fillColors: customColors, fillAlpha: 0.9) +
        labs(title: 'Saddle Surface with Custom Colors')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/filled_contour_custom.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFilledContourNoOutlines() {
    def rows = []
    for (double x = -2; x <= 2; x += 0.4) {
      for (double y = -2; y <= 2; y += 0.4) {
        double z = Math.sin(Math.sqrt(x * x + y * y))
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour_filled(linewidth: 0) +
        labs(title: 'Filled Contour without Outlines')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/filled_contour_no_outline.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFilledContourWithGaussianPeak() {
    def rows = []
    for (double x = -3; x <= 3; x += 0.3) {
      for (double y = -3; y <= 3; y += 0.3) {
        double z = Math.exp(-(x * x + y * y) / 2)
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour_filled(bins: 10, fillAlpha: 0.7) +
        labs(title: 'Gaussian Peak')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/filled_contour_gaussian.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Factory Method Tests ====================

  @Test
  void testFactoryMethods() {
    def contour = geom_contour()
    assertNotNull(contour)
    assertTrue(contour instanceof GeomContour)

    def contourParams = geom_contour(bins: 15, color: 'red')
    assertEquals(15, contourParams.bins)
    assertEquals('red', contourParams.color)

    def filled = geom_contour_filled()
    assertNotNull(filled)
    assertTrue(filled instanceof GeomContourFilled)

    def filledParams = geom_contour_filled(bins: 8, fillAlpha: 0.5)
    assertEquals(8, filledParams.bins)
    assertEquals(0.5, filledParams.fillAlpha)
  }

  @Test
  void testGeomContourfAlias() {
    // geom_contourf should be an alias for geom_contour_filled
    def contourf1 = geom_contourf()
    assertNotNull(contourf1)
    assertTrue(contourf1 instanceof GeomContourFilled)

    def contourf2 = geom_contourf(bins: 12, fillAlpha: 0.6)
    assertEquals(12, contourf2.bins)
    assertEquals(0.6, contourf2.fillAlpha)

    // Test with aesthetic mapping
    def contourf3 = geom_contourf(aes(x: 'x', y: 'y'))
    assertNotNull(contourf3)
    assertTrue(contourf3 instanceof GeomContourFilled)
  }

  // ==================== Edge Cases ====================

  @Test
  void testContourWithSparseData() {
    // Very sparse grid
    def rows = []
    for (double x = 0; x <= 2; x += 1) {
      for (double y = 0; y <= 2; y += 1) {
        double z = x + y
        rows << [x, y, z]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour(bins: 4) +
        labs(title: 'Sparse Data Contour')

    // Should not throw exception
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testContourWithDifferentColumnNames() {
    def rows = []
    for (double a = -1; a <= 1; a += 0.5) {
      for (double b = -1; b <= 1; b += 0.5) {
        double height = a * a + b * b
        rows << [a, b, height]
      }
    }

    def data = Matrix.builder()
        .columnNames('a', 'b', 'height')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    // The z column should be found by name 'height'
    def chart = ggplot(data, aes(x: 'a', y: 'b')) +
        geom_contour(bins: 5) +
        labs(title: 'Contour with Custom Column Names')

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testContourWithValue() {
    def rows = []
    for (double x = -1; x <= 1; x += 0.5) {
      for (double y = -1; y <= 1; y += 0.5) {
        double value = x * y
        rows << [x, y, value]
      }
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    // The z column should be found by name 'value'
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_contour_filled(bins: 4) +
        labs(title: 'Contour with Value Column')

    Svg svg = chart.render()
    assertNotNull(svg)
  }
}
