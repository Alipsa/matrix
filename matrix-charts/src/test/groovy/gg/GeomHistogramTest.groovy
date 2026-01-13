package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.stat.GgStat

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomHistogramTest {

  // ============== GeomHistogram Defaults ==============

  @Test
  void testGeomHistogramDefaults() {
    GeomHistogram geom = new GeomHistogram()

    assertEquals('#595959', geom.fill)
    assertEquals('white', geom.color)
    assertEquals(1.0, geom.alpha)
    assertEquals(0.5, geom.linewidth)
    assertEquals(30, geom.bins)
    assertNull(geom.binwidth)
    assertEquals(StatType.BIN, geom.defaultStat)
    assertEquals(['x'], geom.requiredAes)
  }

  @Test
  void testGeomHistogramWithParams() {
    GeomHistogram geom = new GeomHistogram(
        fill: 'steelblue',
        color: 'black',
        bins: 20,
        alpha: 0.7
    )

    assertEquals('steelblue', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(20, geom.bins)
    assertEquals(0.7, geom.alpha)
  }

  @Test
  void testGeomHistogramWithBinwidth() {
    GeomHistogram geom = new GeomHistogram(binwidth: 5)
    assertEquals(5, geom.binwidth)
  }

  // ============== GgStat.bin() Tests ==============

  @Test
  void testStatBinOutput() {
    def data = Matrix.builder()
        .columnNames('value')
        .rows([
            [1], [2], [3], [4], [5],
            [6], [7], [8], [9], [10]
        ])
        .types(Integer)
        .build()

    def aes = new Aes(x: 'value')
    def binned = GgStat.bin(data, aes, [bins: 5])

    //println "Binned data:"
    //println "Columns: ${binned.columnNames()}"
    //binned.each { row ->
    //  println "  x=${row['x']}, xmin=${row['xmin']}, xmax=${row['xmax']}, count=${row['count']}"
    //}

    assertTrue(binned.columnNames().contains('x'), "Should have x column")
    assertTrue(binned.columnNames().contains('xmin'), "Should have xmin column")
    assertTrue(binned.columnNames().contains('xmax'), "Should have xmax column")
    assertTrue(binned.columnNames().contains('count'), "Should have count column")
    assertTrue(binned.columnNames().contains('density'), "Should have density column")
    assertEquals(5, binned.rowCount(), "Should have 5 bins")
  }

  @Test
  void testStatBinWithBinwidth() {
    def data = Matrix.builder()
        .columnNames('value')
        .rows([
            [0], [1], [2], [3], [4],
            [5], [6], [7], [8], [9]
        ])
        .types(Integer)
        .build()

    def aes = new Aes(x: 'value')
    def binned = GgStat.bin(data, aes, [binwidth: 2])

    /*println "Binned with binwidth=2:"
    binned.each { row ->
      println "  xmin=${row['xmin']}, xmax=${row['xmax']}, count=${row['count']}"
    }*/

    // With binwidth=2 over range 0-9, we expect 5 bins
    assertEquals(5, binned.rowCount(), "Should have 5 bins with binwidth=2")
  }

  // ============== Full Chart Tests ==============

  @Test
  void testSimpleHistogram() {
    // Generate some normally-distributed-like data
    def values = []
    def random = new Random(42)
    100.times {
      // Simple uniform distribution for testing
      values << [random.nextDouble() * 100]
    }

    def data = Matrix.builder()
        .columnNames('value')
        .rows(values)
        .types(Double)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        geom_histogram() +
        labs(title: 'Simple Histogram')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements")
    assertTrue(content.contains('Simple Histogram'), "Should contain title")

    // Write for visual inspection
    File outputFile = new File('build/simple_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testHistogramWithBins() {
    def rows = (1..100).collect { [it] }
    def data = Matrix.builder()
        .columnNames('x')
        .rows(rows)
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x')) +
        geom_histogram(bins: 10, fill: 'steelblue', color: 'white') +
        labs(title: 'Histogram with 10 Bins')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill=\"steelblue\"'), "Should have steelblue fill")

    File outputFile = new File('build/histogram_10bins.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testHistogramWithBinwidth() {
    def rows = (0..99).collect { [it] }
    def data = Matrix.builder()
        .columnNames('score')
        .rows(rows)
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'score')) +
        geom_histogram(binwidth: 10, fill: 'coral', color: 'darkred') +
        labs(title: 'Histogram with Binwidth 10', x: 'Score', y: 'Count')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/histogram_binwidth.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testHistogramWithAlpha() {
    def rows = (1..50).collect { [it] }
    def data = Matrix.builder()
        .columnNames('value')
        .rows(rows)
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        geom_histogram(alpha: 0.5, fill: 'purple')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill-opacity'), "Should have fill-opacity attribute")
  }

  @Test
  void testHistogramWithTheme() {
    def random = new Random(123)
    def rows = []
    200.times { rows << [50 + random.nextGaussian() * 15] }
    def data = Matrix.builder()
        .columnNames('measurement')
        .rows(rows)
        .types(Double)
        .build()

    def chart = ggplot(data, aes(x: 'measurement')) +
        geom_histogram(bins: 20, fill: '#3498db', color: 'white') +
        theme_minimal() +
        labs(title: 'Distribution of Measurements', x: 'Value', y: 'Frequency')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/themed_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testHistogramClassicTheme() {
    def random = new Random(456)
    def rows = []
    100.times { rows << [random.nextDouble() * 50 + 25] }
    def data = Matrix.builder()
        .columnNames('data')
        .rows(rows)
        .types(Double)
        .build()

    def chart = ggplot(data, aes(x: 'data')) +
        geom_histogram(bins: 15, fill: '#2ecc71') +
        theme_classic() +
        labs(title: 'Classic Theme Histogram')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/classic_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Factory Method Tests ==============

  @Test
  void testFactoryMethods() {
    def hist1 = geom_histogram()
    assertNotNull(hist1)
    assertTrue(hist1 instanceof GeomHistogram)

    def hist2 = geom_histogram(fill: 'red', bins: 25)
    assertEquals('red', hist2.fill)
    assertEquals(25, hist2.bins)
  }

  // ============== Edge Cases ==============

  @Test
  void testHistogramSmallDataset() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [1], [2], [3], [4], [5]
        ])
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x')) +
        geom_histogram(bins: 3)

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/small_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testHistogramSingleValue() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [5], [5], [5], [5], [5]
        ])
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x')) +
        geom_histogram(bins: 5)

    // Should not throw
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testHistogramDecimalValues() {
    def data = Matrix.builder()
        .columnNames('value')
        .rows([
            [0.1], [0.5], [0.9], [1.2], [1.8],
            [2.3], [2.7], [3.1], [3.5], [4.0]
        ])
        .types(BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        geom_histogram(bins: 4, fill: 'teal') +
        labs(title: 'Decimal Values Histogram')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/decimal_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testHistogramNegativeValues() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [-10], [-5], [-3], [0], [2],
            [5], [8], [10], [15], [20]
        ])
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x')) +
        geom_histogram(bins: 5, fill: 'orange') +
        labs(title: 'Histogram with Negative Values')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/negative_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }
}
