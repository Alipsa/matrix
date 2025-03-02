package se.alipsa.matrix.xchart

import org.knowm.xchart.CategorySeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractCategoryChart

class StickChart extends AbstractCategoryChart<StickChart> {

  private StickChart(Matrix matrix, Integer width, Integer height) {
    super(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Stick)
  }

  static create(Matrix matrix, Integer width, Integer height) {
    new StickChart(matrix, width, height)
  }
}
