package chart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charts.AreaChart
import se.alipsa.matrix.charts.BarChart
import se.alipsa.matrix.charts.BubbleChart
import se.alipsa.matrix.charts.Chart
import se.alipsa.matrix.charts.ChartDirection
import se.alipsa.matrix.charts.ChartType
import se.alipsa.matrix.charts.Histogram
import se.alipsa.matrix.charts.Legend
import se.alipsa.matrix.charts.LineChart
import se.alipsa.matrix.charts.PieChart
import se.alipsa.matrix.charts.ScatterChart
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class ChartFactoryBaselineTest {

  private static Matrix employeeData() {
    Matrix.builder()
        .matrixName('Employees')
        .columns([
            emp_id  : [1, 2, 3, 4],
            emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan'],
            salary  : [623.3, 515.2, 611.0, 729.0]
        ])
        .types([int, String, Number])
        .build()
  }

  @Test
  void testAreaChartFactoryFromMatrix() {
    Matrix data = Matrix.builder()
        .matrixName('AreaData')
        .columns([
            category: ['Rick', 'Dan', 'Michelle', 'Ryan'],
            value   : [623.3, 515.2, 611.0, 729.0]
        ])
        .types([String, Number])
        .build()
    AreaChart chart = AreaChart.create(data)
    assertEquals('AreaData', chart.title)
    assertEquals(data.column('category'), chart.categorySeries)
    assertEquals(['value'], chart.valueSeriesNames)
    assertEquals(1, chart.valueSeries.size())
  }

  @Test
  void testAreaChartRejectsMatrixWithWrongColumnCount() {
    Matrix invalid = Matrix.builder()
        .matrixName('invalid')
        .columns([a: [1, 2], b: [3, 4], c: [5, 6]])
        .types([int, int, int])
        .build()
    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      AreaChart.create(invalid)
    }
    assertTrue(ex.message.contains('does not contain 2 columns.'))
  }

  @Test
  void testBarChartFactoriesAndDirectionBehavior() {
    Matrix data = employeeData()
    BarChart vertical = BarChart.createVertical('Salaries', data, 'emp_name', ChartType.STACKED, 'salary')
    assertEquals(ChartDirection.VERTICAL, vertical.direction)
    assertTrue(vertical.isStacked())
    assertEquals(['salary'], vertical.valueSeriesNames)

    BarChart horizontal = BarChart.createHorizontal('Salaries', data, 'emp_name', ChartType.BASIC, 'salary')
    assertEquals(ChartDirection.HORIZONTAL, horizontal.direction)
    assertFalse(horizontal.isStacked())
  }

  @Test
  void testPieAndScatterFactories() {
    Matrix data = employeeData()
    PieChart pie = PieChart.create('Pie Salaries', data, 'emp_name', 'salary')
    assertEquals('Pie Salaries', pie.title)
    assertEquals(data.column('emp_name'), pie.categorySeries)
    assertEquals(1, pie.valueSeries.size())

    ScatterChart scatter = ScatterChart.create('Salary Scatter', data, 'emp_id', 'salary')
    assertEquals('emp_id', scatter.xAxisTitle)
    assertEquals('salary', scatter.yAxisTitle)
    assertEquals(data.column('emp_id'), scatter.categorySeries)
    assertEquals(1, scatter.valueSeries.size())
  }

  @Test
  void testBubbleChartIsStillStubbed() {
    RuntimeException ex = assertThrows(RuntimeException) {
      BubbleChart.create('Bubble', employeeData(), 'emp_id', 'salary', 'salary', 'emp_name')
    }
    assertEquals('Not yet implemented', ex.message)
  }

  @Test
  void testStyleLegendTitleAndAxisSetters() {
    Matrix data = employeeData()
    LineChart chart = LineChart.create('Line Salaries', data, 'emp_id', 'salary')
    Legend legend = new Legend()

    chart.setLegend(legend)
        .setTitle('Updated Title')
        .setXAxisTitle('Employee ID')
        .setYAxisTitle('Salary')

    assertSame(legend, chart.legend)
    assertEquals('Updated Title', chart.title)
    assertEquals('Employee ID', chart.xAxisTitle)
    assertEquals('Salary', chart.yAxisTitle)

    assertNotNull(chart.style)
    assertFalse(chart.style.legendVisible)
    chart.style.legendVisible = true
    assertTrue(chart.style.legendVisible)
  }

  @Test
  void testHistogramRejectsNonNumericColumns() {
    Matrix nonNumeric = Matrix.builder()
        .matrixName('people')
        .columns([name: ['Rick', 'Dan', 'Michelle']])
        .types([String])
        .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      Histogram.create('Names', nonNumeric, 'name', 5)
    }
    assertTrue(ex.message.contains('Column must be numeric in a histogram'))
  }

  @Test
  void testValidateSeriesRejectsInvalidInputs() {
    Matrix valid = Matrix.builder()
        .matrixName('valid')
        .columns([x: ['a', 'b'], y: [1, 2]])
        .types([String, int])
        .build()
    Matrix invalidColumnCount = Matrix.builder()
        .matrixName('invalidCols')
        .columns([x: ['a', 'b'], y: [1, 2], z: [3, 4]])
        .types([String, int, int])
        .build()
    Matrix invalidType = Matrix.builder()
        .matrixName('invalidType')
        .columns([x: [1, 2], y: [1, 2]])
        .types([int, int])
        .build()

    IllegalArgumentException noData = assertThrows(IllegalArgumentException) {
      Chart.validateSeries([] as Matrix[])
    }
    assertEquals('The series contains no data', noData.message)

    IllegalArgumentException badCols = assertThrows(IllegalArgumentException) {
      Chart.validateSeries([valid, invalidColumnCount] as Matrix[])
    }
    assertTrue(badCols.message.contains('does not contain 2 columns.'))

    IllegalArgumentException typeMismatch = assertThrows(IllegalArgumentException) {
      Chart.validateSeries([valid, invalidType] as Matrix[])
    }
    assertTrue(typeMismatch.message.contains('Column mismatch in series'))
  }
}
