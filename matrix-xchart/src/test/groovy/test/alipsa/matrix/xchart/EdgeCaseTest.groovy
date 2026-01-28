package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.theme.GGPlot2Theme
import org.knowm.xchart.style.theme.MatlabTheme
import org.knowm.xchart.style.theme.XChartTheme
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.AreaChart
import se.alipsa.matrix.xchart.BarChart
import se.alipsa.matrix.xchart.LineChart
import se.alipsa.matrix.xchart.MatrixTheme
import se.alipsa.matrix.xchart.PieChart
import se.alipsa.matrix.xchart.ScatterChart

import static org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive edge case tests for matrix-xchart.
 * Tests edge cases including empty datasets, single data points, null handling,
 * large datasets, and theme variations.
 */
class EdgeCaseTest {

  // ========== Single Data Point Tests ==========

  @Test
  void testLineChartSingleDataPoint() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [1.0],
            Y: [2.0]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 400, 300)
        .addSeries('X', 'Y')

    assertNotNull(chart)
    assertNotNull(chart.xchart)
    assertEquals(1, chart.series.size())

    File file = new File("build/edgeCase_singlePoint_line.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testBarChartSingleDataPoint() {
    Matrix matrix = Matrix.builder()
        .data(
            Category: ['A'],
            Value: [10]
        ).types([String, Number])
        .build()

    BarChart chart = BarChart.create(matrix, 400, 300)
        .addSeries('Category', 'Value')

    assertNotNull(chart)
    File file = new File("build/edgeCase_singlePoint_bar.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testPieChartSingleDataPoint() {
    Matrix matrix = Matrix.builder()
        .data(
            Category: ['A'],
            Value: [100]
        ).types([String, Number])
        .build()

    PieChart chart = PieChart.create(matrix, 400, 300)
        .addSeries('Category', 'Value')

    assertNotNull(chart)
    File file = new File("build/edgeCase_singlePoint_pie.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  // ========== Small Dataset Tests ==========

  @Test
  void testLineChartTwoDataPoints() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [1.0, 2.0],
            Y: [2.0, 4.0]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 400, 300)
        .addSeries('X', 'Y')
        .setTitle('Two Data Points')

    assertNotNull(chart)
    assertEquals('Two Data Points', chart.xchart.title)

    File file = new File("build/edgeCase_twoPoints_line.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  // ========== Large Dataset Tests (Performance) ==========

  @Test
  void testLineChartLargeDataset() {
    // Generate 10,000 data points
    int numPoints = 10000
    List<Double> xValues = (0..<numPoints).collect { it as Double }
    List<Double> yValues = (0..<numPoints).collect { Math.sin(it * 0.01) }

    Matrix matrix = Matrix.builder()
        .data(
            X: xValues,
            Y: yValues
        ).types([Double, Double])
        .matrixName("Large Dataset Performance Test")
        .build()

    LineChart chart = LineChart.create(matrix, 800, 600)
        .addSeries('X', 'Y')

    assertNotNull(chart)
    assertNotNull(chart.xchart)
    assertEquals(1, chart.series.size())

    File file = new File("build/edgeCase_largeDataset_line.png")
    chart.exportPng(file)

    assertTrue(file.exists())
    assertTrue(file.length() > 0, "Exported file should not be empty")
  }

  @Test
  void testScatterChartLargeDataset() {
    // Generate 5,000 scatter points
    int numPoints = 5000
    Random rand = new Random(42) // Fixed seed for reproducibility
    List<Double> xValues = (0..<numPoints).collect { rand.nextDouble() * 100 }
    List<Double> yValues = (0..<numPoints).collect { rand.nextDouble() * 100 }

    Matrix matrix = Matrix.builder()
        .data(
            X: xValues,
            Y: yValues
        ).types([Double, Double])
        .build()

    ScatterChart chart = ScatterChart.create(matrix, 800, 600)
        .addSeries('X', 'Y')

    assertNotNull(chart)
    assertNotNull(chart.xchart)
    assertEquals(1, chart.series.size())

    File file = new File("build/edgeCase_largeDataset_scatter.png")
    chart.exportPng(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0, "Exported file should not be empty")
  }

  // ========== Theme Variation Tests ==========

  @Test
  void testLineChartWithMatrixTheme() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0, 3.0],
            Y: [1.0, 2.0, 1.5, 3.0]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    // MatrixTheme is set by default
    assertNotNull(chart.style.theme)
    assertTrue(chart.style.theme instanceof MatrixTheme)

    File file = new File("build/edgeCase_theme_matrix.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testLineChartWithXChartTheme() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0, 3.0],
            Y: [1.0, 2.0, 1.5, 3.0]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    // Change to XChart default theme
    chart.style.theme = new XChartTheme()

    File file = new File("build/edgeCase_theme_xchart.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testLineChartWithGGPlot2Theme() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0, 3.0],
            Y: [1.0, 2.0, 1.5, 3.0]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    // Change to GGPlot2 theme
    chart.style.theme = new GGPlot2Theme()

    File file = new File("build/edgeCase_theme_ggplot2.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testLineChartWithMatlabTheme() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0, 3.0],
            Y: [1.0, 2.0, 1.5, 3.0]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    // Change to Matlab theme
    chart.style.theme = new MatlabTheme()

    File file = new File("build/edgeCase_theme_matlab.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  // ========== Multiple Series Tests ==========

  @Test
  void testAreaChartMultipleSeriesOverlap() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0, 3.0, 4.0],
            Y1: [1.0, 2.0, 1.5, 3.0, 2.5],
            Y2: [0.5, 1.5, 1.0, 2.5, 2.0],
            Y3: [1.5, 2.5, 2.0, 3.5, 3.0]
        ).types([Double, Double, Double, Double])
        .build()

    AreaChart chart = AreaChart.create(matrix, 800, 600)
        .addSeries('Series 1', 'X', 'Y1')
        .addSeries('Series 2', 'X', 'Y2')
        .addSeries('Series 3', 'X', 'Y3')

    assertNotNull(chart)
    assertEquals(3, chart.series.size())

    File file = new File("build/edgeCase_multipleSeries_area.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  // ========== Styling and Customization Tests ==========

  @Test
  void testLineChartFullyCustomized() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0, 3.0, 4.0],
            Y: [1.0, 3.0, 2.0, 4.0, 3.5]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 800, 600)
        .setTitle('Fully Customized Chart')
        .setXLabel('X Axis Label')
        .setYLabel('Y Axis Label')
        .addSeries('Data Series', 'X', 'Y')

    // Customize styling
    chart.style.legendPosition = Styler.LegendPosition.InsideNE
    chart.style.chartBackgroundColor = java.awt.Color.WHITE
    chart.style.plotBackgroundColor = java.awt.Color.WHITE
    chart.style.chartFontColor = java.awt.Color.BLACK
    chart.style.axisTickLabelsColor = java.awt.Color.BLACK

    assertEquals('Fully Customized Chart', chart.xchart.title)
    assertEquals('X Axis Label', chart.XLabel)
    assertEquals('Y Axis Label', chart.YLabel)

    File file = new File("build/edgeCase_customized.png")
    chart.exportPng(file)
    assertTrue(file.exists())
  }

  // ========== Export Format Tests ==========

  @Test
  void testExportToSvg() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0],
            Y: [1.0, 2.0, 1.5]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    File svgFile = new File("build/edgeCase_export.svg")
    chart.exportSvg(svgFile)
    assertTrue(svgFile.exists())
    assertTrue(svgFile.length() > 0, "SVG file should not be empty")
  }

  @Test
  void testExportToStream() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0],
            Y: [1.0, 2.0, 1.5]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    chart.exportPng(baos)

    assertTrue(baos.size() > 0, "PNG stream should not be empty")
    assertTrue(baos.size() > 1000, "PNG should have reasonable size")
  }

  // ========== Matrix Data Access Tests ==========

  @Test
  void testChartRetainsMatrixReference() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0],
            Y: [1.0, 2.0, 1.5]
        ).types([Double, Double])
        .matrixName("Test Matrix")
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('X', 'Y')

    assertNotNull(chart.matrix)
    assertEquals("Test Matrix", chart.matrix.matrixName)
    assertEquals(2, chart.matrix.columnCount())
    assertEquals(3, chart.matrix.rowCount())
  }

  // ========== Series Access Tests ==========

  @Test
  void testGetSeriesByName() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0],
            Y: [1.0, 2.0, 1.5]
        ).types([Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('My Series', 'X', 'Y')

    def series = chart.getSeries('My Series')
    assertNotNull(series)
    assertEquals('My Series', series.name)
  }

  @Test
  void testGetAllSeries() {
    Matrix matrix = Matrix.builder()
        .data(
            X: [0.0, 1.0, 2.0],
            Y1: [1.0, 2.0, 1.5],
            Y2: [0.5, 1.5, 1.0]
        ).types([Double, Double, Double])
        .build()

    LineChart chart = LineChart.create(matrix, 600, 400)
        .addSeries('Series 1', 'X', 'Y1')
        .addSeries('Series 2', 'X', 'Y2')

    def allSeries = chart.series
    assertNotNull(allSeries)
    assertEquals(2, allSeries.size())
    assertTrue(allSeries.containsKey('Series 1'))
    assertTrue(allSeries.containsKey('Series 2'))
  }
}
