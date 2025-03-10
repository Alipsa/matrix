package se.alipsa.matrix.xchart

import org.knowm.xchart.PieChartBuilder
import org.knowm.xchart.PieSeries
import org.knowm.xchart.style.PieStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.xchart.abstractions.AbstractChart

class PieChart extends AbstractChart<PieChart, org.knowm.xchart.PieChart, PieStyler, PieSeries> {

  private PieChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    PieChartBuilder builder = new PieChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    xchart = builder.build()
    style.theme = new MatrixTheme()
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
    chart.style.setLabelsDistance(.82);
    chart.style.setPlotContentSize(.9);
    chart.style.setSumVisible(true);
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
      xchart.addSeries(ValueConverter.asString(name), yCol[i, Number])
    }
    this
  }
}
