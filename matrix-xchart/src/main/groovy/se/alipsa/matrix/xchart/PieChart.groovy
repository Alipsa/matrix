package se.alipsa.matrix.xchart

import groovy.transform.CompileStatic
import groovy.transform.CompileDynamic

import org.knowm.xchart.PieChartBuilder
import org.knowm.xchart.PieSeries
import org.knowm.xchart.style.PieStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A PieChart is a circular statistical graphic that is divided into slices to illustrate numerical proportions.
 * Each slice of the pie represents a category's contribution to the whole.
 * Sample usage:
 * <pre><code>
 * Matrix matrix = Matrix.builder().data(
 *   'Category': ['A', 'B', 'C', 'D'],
 *   'Value': [30, 20, 40, 10]
 * ).types([String, Number]).matrixName("Pie Chart Example").build()
 *
 * def pieChart = PieChart.create(matrix)
 *   .addSeries('Category', 'Value')
 *
 * File file = new File('pieChart.png')
 * pieChart.exportPng(file)
 * </code></pre>
 */
@CompileStatic
class PieChart extends AbstractChart<PieChart, org.knowm.xchart.PieChart, PieStyler, PieSeries> {

  private PieChart(Matrix matrix, Integer width = null, Integer height = null) {
    super()
    super.matrix = matrix
    PieChartBuilder builder = new PieChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    xchart = builder.build()
    getStyle().setTheme(new MatrixTheme())
    def matrixName = matrix.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      title = matrix.matrixName
    }
  }

  static PieChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new PieChart(matrix, width, height)
  }

  static PieChart createDonut(Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new PieChart(matrix, width, height)
    chart.style.setDefaultSeriesRenderStyle(PieSeries.PieSeriesRenderStyle.Donut)
    chart.style.setLabelType(PieStyler.LabelType.NameAndValue)
    chart.style.setLabelsDistance(.82d)
    chart.style.setPlotContentSize(.9d)
    chart.style.setSumVisible(true)
    chart
  }

  org.knowm.xchart.PieChart getXchart() {
    return super.xchart as org.knowm.xchart.PieChart
  }

  PieChart addSeries(String xValueCol, String yValueCol) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol))
  }

  PieChart addSeries(Column xCol, Column yCol) {
    if (xCol == null) {
      throw new IllegalArgumentException("The xCol is null, cannot add series")
    }
    if (yCol == null) {
      throw new IllegalArgumentException("The yCol is null, cannot add series")
    }
    if (xCol.size() != yCol.size()) {
      throw new IllegalArgumentException("xCol and yCol must be of equal length but xCol has ${xCol.size()} elements wheras yCol has ${yCol.size()} elements.")
    }
    xCol.eachWithIndex { Object name, int i ->
      xchart.addSeries(ValueConverter.asString(name), yCol[i] as Number)
    }
    this
  }
}
