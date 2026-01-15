package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomRug
import se.alipsa.matrix.gg.geom.GeomViolin
import se.alipsa.matrix.gg.geom.GeomDensity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomRugViolinDensityTest {

  // ==================== GeomRug Tests ====================

  @Test
  void testGeomRugDefaults() {
    GeomRug geom = new GeomRug()

    assertEquals('bl', geom.sides)
    assertEquals('black', geom.color)
    assertEquals(10, geom.length)
    assertEquals(0.5, geom.linewidth)
    assertEquals(0.5, geom.alpha)
    assertFalse(geom.outside)
  }

  @Test
  void testGeomRugWithParams() {
    GeomRug geom = new GeomRug(
        sides: 'tblr',
        color: 'red',
        length: 15,
        alpha: 0.3
    )

    assertEquals('tblr', geom.sides)
    assertEquals('red', geom.color)
    assertEquals(15, geom.length)
    assertEquals(0.3, geom.alpha)
  }

  @Test
  void testRugPlotWithScatter() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 25],
            [3, 15],
            [4, 30],
            [5, 20],
            [6, 35]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 4) +
        geom_rug(color: 'blue', alpha: 0.5) +
        labs(title: 'Scatter with Rug Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain point elements")
    assertTrue(content.contains('<line'), "Should contain rug lines")

    File outputFile = new File('build/rug_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testRugPlotSidesSelection() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 30]
        ])
        .types(Integer, Integer)
        .build()

    // Test bottom only
    def chartBottom = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_rug(sides: 'b')

    Svg svgBottom = chartBottom.render()
    assertNotNull(svgBottom)

    // Test all sides
    def chartAll = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_rug(sides: 'tblr', color: 'red')

    Svg svgAll = chartAll.render()
    assertNotNull(svgAll)

    File outputFile = new File('build/rug_all_sides.svg')
    write(svgAll, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomViolin Tests ====================

  @Test
  void testGeomViolinDefaults() {
    GeomViolin geom = new GeomViolin()

    assertEquals('gray', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(0.7, geom.alpha)
    assertEquals(0.9, geom.width)
    assertTrue(geom.trim)
    assertEquals('area', geom.scale)
    assertTrue(geom.draw_quantiles.isEmpty())
  }

  @Test
  void testGeomViolinWithParams() {
    GeomViolin geom = new GeomViolin(
        fill: 'lightblue',
        color: 'darkblue',
        alpha: 0.5,
        draw_quantiles: [0.25, 0.5, 0.75]
    )

    assertEquals('lightblue', geom.fill)
    assertEquals('darkblue', geom.color)
    assertEquals(0.5, geom.alpha)
    assertEquals([0.25, 0.5, 0.75], geom.draw_quantiles)
  }

  @Test
  void testSimpleViolinPlot() {
    // Create grouped data
    def rows = []
    def random = new Random(42)
    ['A', 'B', 'C'].each { group ->
      double mean = group == 'A' ? 10 : (group == 'B' ? 20 : 15)
      (1..30).each {
        rows << [group, mean + random.nextGaussian() * 5]
      }
    }

    def data = Matrix.builder()
        .columnNames('group', 'value')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'value')) +
        geom_violin(fill: 'lightblue', alpha: 0.7) +
        labs(title: 'Simple Violin Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain violin path elements")

    File outputFile = new File('build/simple_violin.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testViolinWithQuantiles() {
    def rows = []
    def random = new Random(123)
    ['X', 'Y'].each { group ->
      double mean = group == 'X' ? 50 : 70
      (1..40).each {
        rows << [group, mean + random.nextGaussian() * 10]
      }
    }

    def data = Matrix.builder()
        .columnNames('category', 'measurement')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'measurement')) +
        geom_violin(fill: 'lightgreen', draw_quantiles: [0.25, 0.5, 0.75]) +
        labs(title: 'Violin with Quantiles')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/violin_quantiles.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomDensity Tests ====================

  @Test
  void testGeomDensityDefaults() {
    GeomDensity geom = new GeomDensity()

    assertNull(geom.fill)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals(1.0, geom.alpha)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.adjust)
    assertEquals('gaussian', geom.kernel)
    assertEquals(512, geom.n)
    assertFalse(geom.trim)
  }

  @Test
  void testGeomDensityWithParams() {
    GeomDensity geom = new GeomDensity(
        fill: 'lightblue',
        color: 'blue',
        alpha: 0.5,
        adjust: 0.5
    )

    assertEquals('lightblue', geom.fill)
    assertEquals('blue', geom.color)
    assertEquals(0.5, geom.alpha)
    assertEquals(0.5, geom.adjust)
  }

  @Test
  void testSimpleDensityPlot() {
    // Generate some random data
    def random = new Random(42)
    def rows = (1..100).collect {
      [random.nextGaussian() * 10 + 50]
    }

    def data = Matrix.builder()
        .columnNames('value')
        .rows(rows)
        .types(Double)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        geom_density(color: 'blue', fill: 'lightblue', alpha: 0.5) +
        labs(title: 'Simple Density Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line') || content.contains('<path'), "Should contain density curve")

    File outputFile = new File('build/simple_density.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testDensityWithHistogram() {
    def random = new Random(123)
    def rows = (1..200).collect {
      [random.nextGaussian() * 15 + 100]
    }

    def data = Matrix.builder()
        .columnNames('x')
        .rows(rows)
        .types(Double)
        .build()

    def chart = ggplot(data, aes(x: 'x')) +
        geom_histogram(fill: 'gray', alpha: 0.5, bins: 20) +
        geom_density(color: 'red', linewidth: 2) +
        labs(title: 'Histogram with Density Overlay')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/histogram_density.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testDensityBandwidthAdjustment() {
    def random = new Random(456)
    def rows = (1..80).collect {
      [random.nextGaussian() * 5 + 20]
    }

    def data = Matrix.builder()
        .columnNames('x')
        .rows(rows)
        .types(Double)
        .build()

    // Narrow bandwidth (more detail)
    def chartNarrow = ggplot(data, aes(x: 'x')) +
        geom_density(adjust: 0.3, color: 'blue') +
        labs(title: 'Density (adjust=0.3)')

    Svg svgNarrow = chartNarrow.render()
    assertNotNull(svgNarrow)

    // Wide bandwidth (smoother)
    def chartWide = ggplot(data, aes(x: 'x')) +
        geom_density(adjust: 2.0, color: 'red') +
        labs(title: 'Density (adjust=2.0)')

    Svg svgWide = chartWide.render()
    assertNotNull(svgWide)

    File outputFile = new File('build/density_bandwidth.svg')
    write(svgWide, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Factory Method Tests ====================

  @Test
  void testFactoryMethods() {
    def rug = geom_rug()
    assertNotNull(rug)
    assertTrue(rug instanceof GeomRug)

    def rugParams = geom_rug(sides: 'b', color: 'red')
    assertEquals('b', rugParams.sides)
    assertEquals('red', rugParams.color)

    def violin = geom_violin()
    assertNotNull(violin)
    assertTrue(violin instanceof GeomViolin)

    def violinParams = geom_violin(fill: 'blue', alpha: 0.5)
    assertEquals('blue', violinParams.fill)
    assertEquals(0.5, violinParams.alpha)

    def density = geom_density()
    assertNotNull(density)
    assertTrue(density instanceof GeomDensity)

    def densityParams = geom_density(fill: 'green', adjust: 0.5)
    assertEquals('green', densityParams.fill)
    assertEquals(0.5, densityParams.adjust)
  }
}
