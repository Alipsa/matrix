package test.alipsa.matrix.xchart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.CorrelationHeatmapChart

class CorrelationHeatmapChartTest {

  @Test
  void testCorrelationHeatmap() {
    Matrix whisky = Matrix.builder().data(this.class.getResource('/ScotchWhisky01.csv')).build()
    assertNotNull(whisky, "Failed to load csv file")
    List<String> numericColumns = whisky.columnNames() - 'Distillery' as List<String>
    whisky = whisky.convert(numericColumns.collectEntries { String columnName -> [(columnName): Number] })
    File chartFile = new File("build/correlationHeatmap.svg")
    def chart = CorrelationHeatmapChart.create(whisky, 800, 600)
        .setTitle("Correlation Heatmap of Scotch Whisky Data")
        .addSeries("Correlation", numericColumns)
    chart.exportSvg(chartFile)
    assertTrue(chartFile.exists(), "SVG file should be created")
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
  void testCorrelationHeatmapAcceptsNumericColumns() {
    Matrix matrix = Matrix.builder()
        .data(x: [1, 2, 3], y: [1, 4, 9])
        .types([Number, Number])
        .build()

    def chart = CorrelationHeatmapChart.create(matrix).addSeries('Correlation', ['x', 'y'])

    assertNotNull(chart.getSeries('Correlation'))
  }
}
