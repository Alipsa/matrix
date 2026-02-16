package chart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charts.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

import static org.junit.jupiter.api.Assertions.*

class ChartBuilderTest {

  @Test
  void testAreaChartBuilder() {
    def data = Matrix.builder()
        .columnNames(['month', 'robberies'])
        .rows([
            ['Jan', 41], ['Feb', 30], ['Mar', 50]
        ])
        .types([String, int])
        .build()

    def factory = AreaChart.create('Crime Stats', data, 'month', 'robberies')
    def builder = AreaChart.builder(data)
        .title('Crime Stats')
        .x('month')
        .y('robberies')
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.categorySeries, builder.categorySeries)
    assertEquals(factory.valueSeries, builder.valueSeries)
    assertEquals(factory.valueSeriesNames, builder.valueSeriesNames)
  }

  @Test
  void testAreaChartBuilderFluentChaining() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [3, 4]])
        .types([int, int])
        .build()

    def result = AreaChart.builder(data)
        .title('Test')
        .x('x')
        .y('y')
        .xAxisTitle('X Label')
        .yAxisTitle('Y Label')

    assertNotNull(result)
    assertTrue(result instanceof AreaChart.Builder)

    def chart = result.build()
    assertEquals('Test', chart.title)
    assertEquals('X Label', chart.xAxisTitle)
    assertEquals('Y Label', chart.yAxisTitle)
  }

  @Test
  void testBarChartBuilderVertical() {
    def data = Matrix.builder()
        .columnNames(['region', 'sales'])
        .rows([
            ['North', 100], ['South', 200], ['East', 150]
        ])
        .types([String, int])
        .build()

    def factory = BarChart.createVertical('Sales', data, 'region', ChartType.BASIC, 'sales')
    def builder = BarChart.builder(data)
        .title('Sales')
        .x('region')
        .y('sales')
        .vertical()
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.categorySeries, builder.categorySeries)
    assertEquals(factory.valueSeries, builder.valueSeries)
    assertEquals(factory.direction, builder.direction)
    assertEquals(factory.chartType, builder.chartType)
  }

  @Test
  void testBarChartBuilderHorizontalStacked() {
    def data = Matrix.builder()
        .columnNames(['region', 'q1', 'q2'])
        .rows([
            ['North', 100, 120], ['South', 200, 180]
        ])
        .types([String, int, int])
        .build()

    def chart = BarChart.builder(data)
        .title('Quarterly Sales')
        .x('region')
        .y('q1', 'q2')
        .horizontal()
        .stacked()
        .build()

    assertEquals('Quarterly Sales', chart.title)
    assertEquals(ChartDirection.HORIZONTAL, chart.direction)
    assertEquals(ChartType.STACKED, chart.chartType)
    assertEquals(['North', 'South'], chart.categorySeries)
    assertEquals(2, chart.valueSeries.size())
    assertEquals(['q1', 'q2'], chart.valueSeriesNames)
  }

  @Test
  void testLineChartBuilder() {
    def data = Matrix.builder()
        .columnNames(['year', 'sales', 'profit'])
        .rows([
            [2020, 100, 20], [2021, 150, 35], [2022, 200, 50]
        ])
        .types([int, int, int])
        .build()

    def chart = LineChart.builder(data)
        .title('Trends')
        .x('year')
        .y('sales', 'profit')
        .build()

    assertEquals('Trends', chart.title)
    assertEquals([2020, 2021, 2022], chart.categorySeries)
    assertEquals(2, chart.valueSeries.size())
    assertEquals(['sales', 'profit'], chart.valueSeriesNames)
  }

  @Test
  void testScatterChartBuilder() {
    def mtcars = Dataset.mtcars()

    def factory = ScatterChart.create('MPG vs Weight', mtcars, 'wt', 'mpg')
    def builder = ScatterChart.builder(mtcars)
        .title('MPG vs Weight')
        .x('wt')
        .y('mpg')
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.categorySeries, builder.categorySeries)
    assertEquals(factory.valueSeries, builder.valueSeries)
  }

  @Test
  void testPieChartBuilder() {
    def data = Matrix.builder()
        .columnNames(['company', 'revenue'])
        .rows([
            ['Apple', 300], ['Google', 250], ['Microsoft', 200]
        ])
        .types([String, int])
        .build()

    def factory = PieChart.create('Market Share', data, 'company', 'revenue')
    def builder = PieChart.builder(data)
        .title('Market Share')
        .x('company')
        .y('revenue')
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.categorySeries, builder.categorySeries)
    assertEquals(factory.valueSeries, builder.valueSeries)
  }

  @Test
  void testBoxChartBuilderWithCategoryAndValue() {
    def data = Matrix.builder()
        .columnNames(['dept', 'salary'])
        .rows([
            ['HR', 50000], ['HR', 55000], ['HR', 60000],
            ['IT', 70000], ['IT', 75000], ['IT', 80000]
        ])
        .types([String, int])
        .build()

    def factory = BoxChart.create('Salary Distribution', data, 'dept', 'salary')
    def builder = BoxChart.builder(data)
        .title('Salary Distribution')
        .x('dept')
        .y('salary')
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.categorySeries, builder.categorySeries)
    assertEquals(factory.valueSeries, builder.valueSeries)
  }

  @Test
  void testBoxChartBuilderWithColumns() {
    def mtcars = Dataset.mtcars()

    def factory = BoxChart.create('Features', mtcars, ['mpg', 'hp'])
    def builder = BoxChart.builder(mtcars)
        .title('Features')
        .columns(['mpg', 'hp'])
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.categorySeries, builder.categorySeries)
    assertEquals(factory.valueSeries.size(), builder.valueSeries.size())
  }

  @Test
  void testHistogramBuilder() {
    def mtcars = Dataset.mtcars()

    def factory = Histogram.create('mtcars.mpg', mtcars, 'mpg', 5)
    def builder = Histogram.builder(mtcars)
        .title('mtcars.mpg')
        .x('mpg')
        .bins(5)
        .build()

    assertEquals(factory.title, builder.title)
    assertEquals(factory.numberOfBins, builder.numberOfBins)
    assertEquals(factory.ranges.size(), builder.ranges.size())

    def expectedCounts = [6, 12, 8, 2, 4]
    int i = 0
    builder.ranges.each {
      assertEquals(expectedCounts[i], it.value)
      i++
    }
  }

  @Test
  void testHistogramBuilderWithBinDecimals() {
    def mtcars = Dataset.mtcars()

    def factory = Histogram.create('mpg', mtcars, 'mpg', 9, 2)
    def builder = Histogram.builder(mtcars)
        .title('mpg')
        .x('mpg')
        .bins(9)
        .binDecimals(2)
        .build()

    assertEquals(factory.ranges.size(), builder.ranges.size())
    assertEquals(factory.ranges.keySet().first().toString(), builder.ranges.keySet().first().toString())
  }

  @Test
  void testBuilderWithAxisScale() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 10], [2, 20], [3, 30]])
        .types([int, int])
        .build()

    def chart = AreaChart.builder(data)
        .title('Scaled')
        .x('x')
        .y('y')
        .xAxisScale(0.0, 5.0, 1.0)
        .yAxisScale(0.0, 50.0, 10.0)
        .build()

    assertNotNull(chart.xAxisScale)
    assertNotNull(chart.yAxisScale)
    assertEquals(0.0, chart.xAxisScale.start)
    assertEquals(5.0, chart.xAxisScale.end)
    assertEquals(1.0, chart.xAxisScale.step)
    assertEquals(0.0, chart.yAxisScale.start)
    assertEquals(50.0, chart.yAxisScale.end)
    assertEquals(10.0, chart.yAxisScale.step)
  }

  @Test
  void testBuilderWithStyle() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 10], [2, 20]])
        .types([int, int])
        .build()

    def style = new Style()
    style.legendVisible = true

    def chart = LineChart.builder(data)
        .title('Styled')
        .x('x')
        .y('y')
        .style(style)
        .build()

    assertTrue(chart.style.legendVisible)
  }

  @Test
  void testBuilderWithLegend() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 10], [2, 20]])
        .types([int, int])
        .build()

    def legend = new Legend()

    def chart = ScatterChart.builder(data)
        .title('With Legend')
        .x('x')
        .y('y')
        .legend(legend)
        .build()

    assertNotNull(chart.legend)
  }
}
