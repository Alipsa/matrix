package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomBin2d
import se.alipsa.matrix.gg.geom.GeomCount

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomBin2dCountTest {

  // ==================== GeomBin2d Tests ====================

  @Test
  void testGeomBin2dDefaults() {
    GeomBin2d geom = new GeomBin2d()

    assertEquals(30, geom.bins)
    assertNull(geom.binwidth)
    assertEquals('steelblue', geom.fill)
    assertEquals('white', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(1.0, geom.alpha)
    assertTrue(geom.drop)
    assertEquals(9, geom.fillColors.size())
  }

  @Test
  void testGeomBin2dWithParams() {
    GeomBin2d geom = new GeomBin2d(
        bins: 20,
        alpha: 0.8,
        color: 'black',
        drop: false
    )

    assertEquals(20, geom.bins)
    assertEquals(0.8, geom.alpha)
    assertEquals('black', geom.color)
    assertFalse(geom.drop)
  }

  @Test
  void testGeomBin2dWithBinwidth() {
    GeomBin2d geom = new GeomBin2d(
        binwidth: [0.5, 0.5]
    )

    assertNotNull(geom.binwidth)
    assertEquals(2, geom.binwidth.size())
    assertEquals(0.5, geom.binwidth[0])
    assertEquals(0.5, geom.binwidth[1])
  }

  @Test
  void testSimpleBin2dPlot() {
    // Generate some random 2D data
    def random = new Random(42)
    def rows = (1..500).collect {
      [random.nextGaussian() * 2 + 5, random.nextGaussian() * 2 + 5]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(bins: 15) +
        labs(title: '2D Binning Heatmap')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rectangle elements")

    File outputFile = new File('build/bin2d_simple.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBin2dWithCustomColors() {
    def random = new Random(123)
    def rows = (1..300).collect {
      // Create two clusters
      double cx = random.nextBoolean() ? 2 : 8
      double cy = random.nextBoolean() ? 2 : 8
      [cx + random.nextGaussian(), cy + random.nextGaussian()]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Double, Double)
        .build()

    def customColors = ['#ffffcc', '#ffeda0', '#fed976', '#feb24c', '#fd8d3c',
                        '#fc4e2a', '#e31a1c', '#bd0026', '#800026']

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(bins: 20, fillColors: customColors) +
        labs(title: 'Bin2d with Custom Colors')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/bin2d_custom_colors.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBin2dWithBinwidth() {
    def random = new Random(456)
    def rows = (1..200).collect {
      [random.nextDouble() * 10, random.nextDouble() * 10]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(binwidth: [1.0, 1.0], alpha: 0.8) +
        labs(title: 'Bin2d with Fixed Bin Width')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/bin2d_binwidth.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBin2dNoBorders() {
    def random = new Random(789)
    def rows = (1..400).collect {
      [random.nextGaussian() * 3, random.nextGaussian() * 3]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(bins: 25, linewidth: 0) +
        labs(title: 'Bin2d without Borders')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/bin2d_no_border.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomCount Tests ====================

  @Test
  void testGeomCountDefaults() {
    GeomCount geom = new GeomCount()

    assertEquals('black', geom.color)
    assertEquals('steelblue', geom.fill)
    assertEquals(0.7, geom.alpha)
    assertEquals(3, geom.sizeMin)
    assertEquals(15, geom.sizeMax)
    assertEquals('circle', geom.shape)
    assertEquals(1, geom.stroke)
  }

  @Test
  void testGeomCountWithParams() {
    GeomCount geom = new GeomCount(
        fill: 'red',
        alpha: 0.5,
        sizeMin: 5,
        sizeMax: 20,
        shape: 'square'
    )

    assertEquals('red', geom.fill)
    assertEquals(0.5, geom.alpha)
    assertEquals(5, geom.sizeMin)
    assertEquals(20, geom.sizeMax)
    assertEquals('square', geom.shape)
  }

  @Test
  void testSimpleCountPlot() {
    // Create data with overlapping points
    def rows = [
        [1, 1], [1, 1], [1, 1], [1, 1], [1, 1],  // 5 at (1,1)
        [2, 2], [2, 2], [2, 2],                   // 3 at (2,2)
        [3, 3], [3, 3], [3, 3], [3, 3], [3, 3], [3, 3], [3, 3],  // 7 at (3,3)
        [4, 4], [4, 4],                           // 2 at (4,4)
        [5, 5]                                     // 1 at (5,5)
    ]

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_count() +
        labs(title: 'Count Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain circle elements")

    File outputFile = new File('build/count_simple.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCountPlotWithSquares() {
    def rows = [
        [1, 1], [1, 1], [1, 1],
        [2, 1], [2, 1], [2, 1], [2, 1], [2, 1],
        [1, 2], [1, 2],
        [2, 2], [2, 2], [2, 2], [2, 2]
    ]

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_count(shape: 'square', fill: 'coral', alpha: 0.6) +
        labs(title: 'Count Plot with Squares')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rectangle elements")

    File outputFile = new File('build/count_squares.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCountPlotWithTriangles() {
    def rows = [
        ['A', 10], ['A', 10], ['A', 10], ['A', 10],
        ['B', 20], ['B', 20],
        ['C', 15], ['C', 15], ['C', 15], ['C', 15], ['C', 15], ['C', 15]
    ]

    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows(rows)
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_count(shape: 'triangle', fill: 'green', sizeMax: 25) +
        labs(title: 'Count Plot with Triangles')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain path elements for triangles")

    File outputFile = new File('build/count_triangles.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCountPlotWithRandomData() {
    // Generate data with some clustering
    def random = new Random(42)
    def rows = (1..100).collect {
      // Round to integers to create overlapping points
      [Math.round(random.nextGaussian() * 2 + 5) as int,
       Math.round(random.nextGaussian() * 2 + 5) as int]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_count(fill: 'purple', alpha: 0.6, sizeMin: 4, sizeMax: 20) +
        labs(title: 'Count Plot with Random Data')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/count_random.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Factory Method Tests ====================

  @Test
  void testFactoryMethods() {
    def bin2d = geom_bin_2d()
    assertNotNull(bin2d)
    assertTrue(bin2d instanceof GeomBin2d)

    def bin2dParams = geom_bin_2d(bins: 25, alpha: 0.7)
    assertEquals(25, bin2dParams.bins)
    assertEquals(0.7, bin2dParams.alpha)

    def count = geom_count()
    assertNotNull(count)
    assertTrue(count instanceof GeomCount)

    def countParams = geom_count(fill: 'red', shape: 'square')
    assertEquals('red', countParams.fill)
    assertEquals('square', countParams.shape)
  }

  // ==================== Combination Tests ====================

  @Test
  void testBin2dWithPointsOverlay() {
    def random = new Random(999)
    def rows = (1..200).collect {
      [random.nextGaussian() * 2, random.nextGaussian() * 2]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(bins: 10, alpha: 0.5) +
        geom_point(color: 'white', size: 2) +
        labs(title: 'Bin2d with Points Overlay')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain bin rectangles")
    assertTrue(content.contains('<circle'), "Should contain point circles")

    File outputFile = new File('build/bin2d_with_points.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Edge Cases ====================

  @Test
  void testBin2dWithSinglePoint() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[5, 5]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(bins: 10)

    // Should not throw exception
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testCountWithSingleLocation() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [1, 1], [1, 1]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_count()

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testBin2dKeepEmptyBins() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[0, 0], [10, 10]])
        .types(Integer, Integer)
        .build()

    // With drop=false, empty bins should still be rendered
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_bin_2d(bins: 5, drop: false)

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/bin2d_keep_empty.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }
}
