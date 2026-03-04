package chart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.pict.AreaChart
import se.alipsa.matrix.pict.BarChart
import se.alipsa.matrix.pict.BubbleChart
import se.alipsa.matrix.pict.Chart
import se.alipsa.matrix.pict.ChartDirection
import se.alipsa.matrix.pict.ChartType
import se.alipsa.matrix.pict.DataType
import se.alipsa.matrix.pict.Histogram
import se.alipsa.matrix.pict.Legend
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.Style
import se.alipsa.matrix.pict.PieChart
import se.alipsa.matrix.pict.ScatterChart
import se.alipsa.matrix.core.Matrix

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
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
  void testBubbleChartFactory() {
    Matrix data = Matrix.builder()
        .matrixName('BubbleData')
        .columns([x: [1, 2, 3], y: [10, 20, 30], s: [5, 10, 15]])
        .types([Number, Number, Number])
        .build()

    BubbleChart chart = BubbleChart.create('Bubble', data, 'x', 'y', 's')
    assertEquals('Bubble', chart.title)
    assertEquals(data.column('x'), chart.categorySeries)
    assertEquals(1, chart.valueSeries.size())
    assertEquals(3, chart.sizeSeries.size())
    assertEquals('x', chart.xAxisTitle)
    assertEquals('y', chart.yAxisTitle)
  }

  @Test
  void testBubbleChartGroupedFactory() {
    Matrix data = Matrix.builder()
        .matrixName('GroupedBubble')
        .columns([x: [1, 2, 3], y: [10, 20, 30], s: [5, 10, 15], g: ['A', 'A', 'B']])
        .types([Number, Number, Number, String])
        .build()

    BubbleChart chart = BubbleChart.create('Grouped', data, 'x', 'y', 's', 'g')
    assertEquals('Grouped', chart.title)
    assertEquals('g', chart.groupColumn)
    assertEquals(3, chart.groupSeries.size())
  }

  @Test
  void testLegendTitleAndAxisSetters() {
    Matrix data = employeeData()
    LineChart chart = LineChart.create('Line Salaries', data, 'emp_id', 'salary')
    Legend legend = new Legend(visible: false, position: Style.Position.TOP)

    chart.setLegend(legend)
        .setTitle('Updated Title')
        .setXAxisTitle('Employee ID')
        .setYAxisTitle('Salary')

    assertSame(legend, chart.legend)
    assertEquals('Updated Title', chart.title)
    assertEquals('Employee ID', chart.xAxisTitle)
    assertEquals('Salary', chart.yAxisTitle)

    assertFalse(chart.legend.visible)
    assertEquals(Style.Position.TOP, chart.legend.position)
    chart.legend.visible = true
    assertTrue(chart.legend.visible)
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
  void testDataTypeOfClassifiesCorrectly() {
    assertEquals(DataType.NUMERIC, DataType.of(Integer))
    assertEquals(DataType.NUMERIC, DataType.of(Double))
    assertEquals(DataType.NUMERIC, DataType.of(Long))
    assertEquals(DataType.NUMERIC, DataType.of(Short))
    assertEquals(DataType.NUMERIC, DataType.of(Float))
    assertEquals(DataType.NUMERIC, DataType.of(BigDecimal))
    assertEquals(DataType.CHARACTER, DataType.of(String))
    assertEquals(DataType.CHARACTER, DataType.of(Boolean))
    assertEquals(DataType.CHARACTER, DataType.of(LocalDate))
  }

  @Test
  void testDataTypeDiffersAndEquals() {
    assertTrue(DataType.differs(String, Integer))
    assertTrue(DataType.differs(Boolean, Double))
    assertFalse(DataType.differs(Integer, Double))
    assertFalse(DataType.differs(Long, Float))
    assertTrue(DataType.equals(Integer, Double))
    assertFalse(DataType.equals(String, Integer))
  }

  @Test
  void testDataTypeSqlTypeMappings() {
    assertEquals('SMALLINT', DataType.sqlType(Short))
    assertEquals('INTEGER', DataType.sqlType(Integer))
    assertEquals('BIGINT', DataType.sqlType(Long))
    assertEquals('REAL', DataType.sqlType(Float))
    assertEquals('BIT', DataType.sqlType(Boolean))
    assertEquals('DOUBLE', DataType.sqlType(Double))
    assertEquals('VARCHAR(8000)', DataType.sqlType(String))
    assertEquals('VARCHAR(255)', DataType.sqlType(String, 255))
    assertEquals('DATE', DataType.sqlType(LocalDate))
    assertEquals('TIME', DataType.sqlType(LocalTime))
    assertEquals('TIMESTAMP', DataType.sqlType(LocalDateTime))
    assertEquals('TIMESTAMP', DataType.sqlType(Instant))
    assertEquals('BLOB', DataType.sqlType(BigDecimal))
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
