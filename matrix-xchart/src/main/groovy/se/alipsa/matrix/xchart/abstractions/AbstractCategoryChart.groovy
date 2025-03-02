package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import org.knowm.xchart.CategorySeries
import org.knowm.xchart.style.CategoryStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.MatrixTheme

class AbstractCategoryChart<T extends AbstractCategoryChart> extends AbstractChart<T> {

  AbstractCategoryChart(Matrix matrix, Integer width = null, Integer height = null, CategorySeries.CategorySeriesRenderStyle chartType) {
    this.matrix = matrix
    CategoryChartBuilder builder = new CategoryChartBuilder()
    if (width != null) {
      builder.width = width
    }
    if (height != null) {
      builder.height = height
    }
    xchart = builder.build()
    style.theme = new MatrixTheme()
    def matrixName = matrix.matrixName
    if (matrixName != null && !matrixName.isBlank()) {
      title = matrix.matrixName
    }
    style.defaultSeriesRenderStyle = chartType
  }


  T addSeries(String xValueCol, String yValueCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(String seriesName, String xValueCol, String yValueCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(seriesName, matrix.column(xValueCol), matrix.column(yValueCol), renderStyle)
  }

  T addSeries(Column xCol, Column yCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    addSeries(yCol.name, xCol, yCol, renderStyle)
  }

  T addSeries(String name, Column xCol, Column yCol, CategorySeries.CategorySeriesRenderStyle renderStyle = null) {
    CategorySeries series = xchart.addSeries(name, xCol, yCol)
    if (renderStyle != null) {
      series.setChartCategorySeriesRenderStyle(renderStyle)
    }
    this as T
  }

  @Override
  CategoryChart getXchart() {
    super.xchart as CategoryChart
  }

  @Override
  CategoryStyler getStyle() {
    xchart.styler
  }

  @Override
  CategorySeries getSeries(String name) {
    xchart.seriesMap.get(name)
  }

  @Override
  Map<String, CategorySeries> getSeries() {
    xchart.seriesMap
  }
}
