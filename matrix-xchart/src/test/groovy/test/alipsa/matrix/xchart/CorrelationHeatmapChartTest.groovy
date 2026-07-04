package test.alipsa.matrix.xchart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.xchart.CorrelationHeatmapChart

class CorrelationHeatmapChartTest {

  @Test
  void testCorrelationHeatmap() {
    Matrix whisky = Matrix.builder().data(this.class.getResource('/ScotchWhisky01.csv')).build()
    assertNotNull(whisky, 'Failed to load csv file')
    List<String> numericColumns = whisky.columnNames() - 'Distillery' as List<String>
    whisky = whisky.convert(numericColumns.collectEntries { String columnName -> [(columnName): Number] })
    File chartFile = new File('build/correlationHeatmap.svg')
    def chart = CorrelationHeatmapChart.create(whisky, 800, 600)
        .setTitle('Correlation Heatmap of Scotch Whisky Data')
        .addSeries('Correlation', numericColumns)
    chart.exportSvg(chartFile)
    assertTrue(chartFile.exists(), 'SVG file should be created')
  }

  @Test
  void testCorrelationHeatmapRejectsStringColumn() {
    Matrix matrix = Matrix.builder()
        .data(name: ['a', 'b', 'c'], value: [1, 2, 3])
        .types([String, Number])
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['name', 'value'])
    }

    assertTrue(exception.message.contains("'name' must be numeric"))
  }

  @Test
  void testCorrelationHeatmapRejectsUnknownColumn() {
    Matrix matrix = Matrix.builder()
        .data(x: [1, 2, 3], y: [1, 4, 9])
        .types([Number, Number])
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['x', 'missing'])
    }

    assertTrue(exception.message.contains("'missing' does not exist"))
  }

  @Test
  void testCorrelationHeatmapRejectsUntypedColumn() {
    Matrix matrix = Matrix.builder()
        .data(x: [1, 2, 3], y: [1, 4, 9])
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['x', 'y'])
    }

    assertTrue(exception.message.contains("'x' must be numeric"))
  }

  @Test
  void testCorrelationHeatmapAcceptsNumericColumns() {
    Matrix matrix = Matrix.builder()
        .data(x: [1, 2, 3], y: [1, 4, 9])
        .types([Number, Number])
        .build()

    def chart = CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['x', 'y'])

    assertNotNull(chart.getSeries('Correlation'))
  }

  @Test
  void testCorrelationHeatmapAcceptsConstantNumericColumn() {
    Matrix matrix = Matrix.builder()
        .data(x: [1, 2, 3], constant: [5, 5, 5])
        .types([Number, Number])
        .build()

    def chart = CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['x', 'constant'])

    def series = chart.getSeries('Correlation')
    assertNotNull(series)

    Map<String, Number> heatData = series.heatData.collectEntries { Number[] point ->
      [("${point[0]},${point[1]}".toString()): point[2]]
    }
    // (0,0) = x vs x (self), (1,1) = constant vs constant (self)
    assertEquals(1G, heatData['0,0'])
    assertEquals(0G, heatData['1,0'])
    assertEquals(0G, heatData['0,1'])
    assertEquals(1G, heatData['1,1'])
  }

  @Test
  void testCorrelationHeatmapCellsMatchColumnLabelsForThreeColumns() {
    Matrix matrix = Matrix.builder().data(
        A: [1, 2, 3, 4, 5],
        B: [5, 3, 4, 1, 2],
        C: [2, 1, 5, 3, 4]
    ).types([Number] * 3).build()

    def chart = CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['A', 'B', 'C'])
    def series = chart.getSeries('Correlation')

    BigDecimal corrAB = Correlation.cor(matrix['A'] as List, matrix['B'] as List).round(2)
    BigDecimal corrAC = Correlation.cor(matrix['A'] as List, matrix['C'] as List).round(2)
    BigDecimal corrBC = Correlation.cor(matrix['B'] as List, matrix['C'] as List).round(2)

    List<?> xData = series.xData
    List<?> yData = series.yData
    Map<String, Number> cellByLabels = series.heatData.collectEntries { Number[] point ->
      String x = xData[point[0].intValue()]
      String y = yData[point[1].intValue()]
      [("$x,$y".toString()): point[2]]
    }

    assertEquals(corrAB, cellByLabels['A,B'])
    assertEquals(corrAB, cellByLabels['B,A'])
    assertEquals(corrAC, cellByLabels['A,C'])
    assertEquals(corrAC, cellByLabels['C,A'])
    assertEquals(corrBC, cellByLabels['B,C'])
    assertEquals(corrBC, cellByLabels['C,B'])
    assertEquals(1G, cellByLabels['A,A'])
    assertEquals(1G, cellByLabels['B,B'])
    assertEquals(1G, cellByLabels['C,C'])
  }

}
