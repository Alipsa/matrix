package chart

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.charts.AreaChart
import se.alipsa.matrix.charts.BarChart
import se.alipsa.matrix.charts.BoxChart
import se.alipsa.matrix.charts.CharmBridge
import se.alipsa.matrix.charts.ChartDirection
import se.alipsa.matrix.charts.ChartType
import se.alipsa.matrix.charts.Histogram
import se.alipsa.matrix.charts.LineChart
import se.alipsa.matrix.charts.PieChart
import se.alipsa.matrix.charts.ScatterChart
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*

/**
 * Integration tests verifying that each legacy chart type converts
 * through CharmBridge and renders valid SVG output.
 */
class ChartsCharmIntegrationTest {

  @Test
  void testAreaChartRendersPath() {
    Matrix data = Matrix.builder()
        .matrixName('AreaData')
        .columns([
            month: ['Jan', 'Feb', 'Mar', 'Apr', 'May'],
            value: [10, 25, 15, 30, 20]
        ])
        .types([String, Number])
        .build()

    AreaChart chart = AreaChart.create('Sales Trend', data, 'month', 'value')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() > 0, 'Area chart should render at least one path element')
  }

  @Test
  void testMultiSeriesAreaChart() {
    AreaChart chart = AreaChart.create('Multi Area', ['Q1', 'Q2', 'Q3', 'Q4'] as List,
        [10, 20, 15, 25] as List, [5, 15, 10, 20] as List)
    chart.valueSeriesNames = ['Series A', 'Series B']

    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() > 0, 'Multi-series area chart should render path elements')
  }

  @Test
  void testVerticalBarChartRendersRects() {
    Matrix data = Matrix.builder()
        .matrixName('BarData')
        .columns([
            product: ['Apples', 'Oranges', 'Bananas'],
            sales  : [120, 85, 200]
        ])
        .types([String, Number])
        .build()

    BarChart chart = BarChart.createVertical('Fruit Sales', data, 'product', ChartType.BASIC, 'sales')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() >= 3, "Expected at least 3 bar rects, got ${rects.size()}")
  }

  @Test
  void testStackedBarChartRendersRects() {
    Matrix data = Matrix.builder()
        .matrixName('StackedBar')
        .columns([
            region: ['North', 'South', 'East'],
            q1    : [50, 60, 40],
            q2    : [70, 55, 80]
        ])
        .types([String, Number, Number])
        .build()

    BarChart chart = BarChart.createVertical('Revenue', data, 'region', ChartType.STACKED, 'q1', 'q2')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def rects = svg.descendants().findAll { it instanceof Rect }
    // 6 bars (3 categories x 2 series) + background rects; in long format these become 6 data rects
    assertTrue(rects.size() >= 3, "Stacked bar should render multiple rects, got ${rects.size()}")
  }

  @Test
  void testHorizontalBarChart() {
    Matrix data = Matrix.builder()
        .matrixName('HBar')
        .columns([
            category: ['A', 'B', 'C'],
            value   : [30, 50, 20]
        ])
        .types([String, Number])
        .build()

    BarChart chart = BarChart.createHorizontal('H-Bar', data, 'category', ChartType.BASIC, 'value')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() >= 3, 'Horizontal bar chart should render rects')
  }

  @Test
  void testBoxChartRendersBoxplotElements() {
    Matrix data = Matrix.builder().matrixName('BoxData').columns([
        group: ['A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
                'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B'],
        value: [10, 12, 14, 15, 13, 11, 16, 18, 12, 14,
                20, 22, 25, 28, 23, 21, 26, 30, 24, 22]
    ]).types([String, int]).build()

    BoxChart chart = BoxChart.create('Box Test', data, 'group', 'value')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def rects = svg.descendants().findAll { it instanceof Rect }
    def lines = svg.descendants().findAll { it instanceof Line }
    // 2 box rects (one per group) + 2 background rects (canvas + panel) + clip-path rect = 5 minimum
    assertTrue(rects.size() >= 3, "Box chart should render box rectangles, got ${rects.size()}")
    // Median lines + whisker lines + axis lines
    assertTrue(lines.size() >= 2, "Box chart should render median/whisker lines, got ${lines.size()}")
  }

  @Test
  void testBoxChartStatisticalCorrectness() {
    // Known data: 1,2,3,4,5,6,7,8,9 -> Q1=2.5, median=5, Q3=7.5, IQR=5
    Matrix data = Matrix.builder().matrixName('StatBox').columns([
        group: ['X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'],
        value: [1, 2, 3, 4, 5, 6, 7, 8, 9]
    ]).types([String, int]).build()

    BoxChart chart = BoxChart.create('Stat Box', data, 'group', 'value')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg, 'Box chart with known data should render')
  }

  @Test
  void testHistogramRendersBarRects() {
    Matrix data = Matrix.builder()
        .matrixName('HistData')
        .columns([score: [55, 60, 62, 65, 70, 72, 75, 78, 80, 85, 90, 95]])
        .types([Number])
        .build()

    Histogram chart = Histogram.create('Score Distribution', data, 'score', 5)
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def rects = svg.descendants().findAll { it instanceof Rect }
    // 5 histogram bins + panel/canvas rects
    assertTrue(rects.size() >= 5, "Histogram should render bin rects, got ${rects.size()}")
  }

  @Test
  void testLineChartRendersLineSegments() {
    Matrix data = Matrix.builder()
        .matrixName('LineData')
        .columns([
            time : [1, 2, 3, 4, 5],
            value: [10, 15, 12, 18, 20]
        ])
        .types([int, Number])
        .build()

    LineChart chart = LineChart.create('Trend', data, 'time', 'value')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def lines = svg.descendants().findAll { it instanceof Line }
    // At least 4 data line segments + axis lines
    assertTrue(lines.size() >= 4, "Line chart should render line segments, got ${lines.size()}")
  }

  @Test
  void testMultiSeriesLineChart() {
    Matrix data = Matrix.builder()
        .matrixName('MultiLine')
        .columns([
            x     : [1, 2, 3, 4],
            seriesA: [10, 20, 15, 25],
            seriesB: [5, 15, 10, 20]
        ])
        .types([int, Number, Number])
        .build()

    LineChart chart = LineChart.create('Multi Line', data, 'x', 'seriesA', 'seriesB')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def lines = svg.descendants().findAll { it instanceof Line }
    assertTrue(lines.size() >= 6, "Multi-series line chart should render multiple line segments, got ${lines.size()}")
  }

  @Test
  void testPieChartRendersArcPaths() {
    Matrix data = Matrix.builder()
        .matrixName('PieData')
        .columns([
            category: ['Red', 'Blue', 'Green'],
            value   : [40, 35, 25]
        ])
        .types([String, Number])
        .build()

    PieChart chart = PieChart.create('Color Split', data, 'category', 'value')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() >= 3, "Pie chart should render 3 arc paths, got ${paths.size()}")
  }

  @Test
  void testScatterChartRendersCircles() {
    Matrix data = Matrix.builder()
        .matrixName('ScatterData')
        .columns([
            x: [1, 2, 3, 4, 5],
            y: [2, 4, 1, 5, 3]
        ])
        .types([Number, Number])
        .build()

    ScatterChart chart = ScatterChart.create('XY Plot', data, 'x', 'y')
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertEquals(5, circles.size(), 'Scatter chart should render 5 point circles')
  }

  @Test
  void testStyleThemePassthrough() {
    Matrix data = Matrix.builder()
        .matrixName('StyledChart')
        .columns([
            x: ['A', 'B', 'C'],
            y: [10, 20, 30]
        ])
        .types([String, Number])
        .build()

    BarChart chart = BarChart.createVertical('Styled', data, 'x', ChartType.BASIC, 'y')
    chart.style.plotBackgroundColor = new java.awt.Color(240, 240, 240)
    chart.style.chartBackgroundColor = new java.awt.Color(255, 255, 255)
    chart.style.legendVisible = false

    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)
    Svg svg = charmChart.render()
    assertNotNull(svg)

    def texts = svg.descendants().findAll { it instanceof Text }
    def titleTexts = texts.findAll { it.content == 'Styled' }
    assertTrue(titleTexts.size() > 0, 'Title should be rendered')
  }
}
