package se.alipsa.matrix.xchart

import org.knowm.xchart.CategorySeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractCategoryChart

class BarChart extends AbstractCategoryChart<BarChart> {


  private BarChart(Matrix matrix, Integer width = null, Integer height = null, CategorySeries.CategorySeriesRenderStyle type) {
    super(matrix, width, height, type)
  }

  static BarChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new BarChart(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Bar)
  }

  static BarChart createStacked(Matrix matrix, Integer width = null, Integer height = null) {
    def bc = new BarChart(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Bar)
    bc.style.setStacked(true)
    bc
  }
}
