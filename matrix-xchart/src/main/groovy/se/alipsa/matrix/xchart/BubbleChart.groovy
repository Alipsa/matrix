package se.alipsa.matrix.xchart

import org.knowm.xchart.BubbleChartBuilder
import org.knowm.xchart.BubbleSeries
import org.knowm.xchart.style.BubbleStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

import java.awt.Color

class BubbleChart extends AbstractChart<BubbleChart, org.knowm.xchart.BubbleChart, BubbleStyler, BubbleSeries> {

  int numSeries = 0

  private BubbleChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    def builder = new BubbleChartBuilder()
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

  static BubbleChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BubbleChart(matrix, width, height)
  }

  BubbleChart addSeries(String xCol, String yCol,String valueCol, Integer transparency = 185) {
    addSeries(matrix[xCol], matrix[yCol],matrix[valueCol], transparency)
  }

  BubbleChart addSeries(String seriesName, String xCol, String yCol,String valueCol, Integer transparency = 185) {
    addSeries(seriesName, matrix[xCol], matrix[yCol],matrix[valueCol], transparency)
  }

  BubbleChart addSeries(Column xCol, Column yCol,Column valueCol, Integer transparency = 185) {
    addSeries(valueCol.name, xCol, yCol, valueCol, transparency)
  }

  BubbleChart addSeries(String seriesName, Column xCol, Column yCol, Column valueCol, Integer transparency = 185) {
    if (valueCol == null) {
      throw new IllegalArgumentException("The valueCol is null, cannot add series")
    }
    def s = xchart.addSeries(seriesName, xCol, yCol, valueCol)
    // Make the fill transparent so that overlaps are visible
    def colors = style.theme.seriesColors
    if (numSeries > colors.size() - 1) {
      def multiple = Math.ceil(style.theme.seriesColors.size() / numSeries).intValue()
      colors = style.theme.seriesColors*multiple
    }
    def color = colors[numSeries]
    //s.lineColor = color.darker()
    s.fillColor = new Color(color.red, color.green, color.blue, transparency)
    numSeries++
    this
  }
}
