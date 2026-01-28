package se.alipsa.matrix.xchart

import groovy.transform.CompileStatic

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.CategoryChartBuilder
import org.knowm.xchart.CategorySeries
import org.knowm.xchart.Histogram
import org.knowm.xchart.style.CategoryStyler
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.xchart.abstractions.AbstractChart

/**
 * A HistogramChart is a visual representation of the distribution of quantitative data.
 * It is a type of bar chart that groups data into bins or intervals.
 * Sample usage:
 * <pre><code>
 * def hc = HistogramChart.create(Dataset.airquality())
 *   hc.addSeries('Temp', 9) // 9 is the number of bins
 * File file = new File("exampleHistogram.png")
 * hc.exportPng(file)
 * </code></pre>
 */
@CompileStatic
class HistogramChart extends AbstractChart<HistogramChart, CategoryChart, CategoryStyler, CategorySeries> {

  private HistogramChart(Matrix matrix, Integer width = null, Integer height = null, CategorySeries.CategorySeriesRenderStyle type) {
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
    style.defaultSeriesRenderStyle = type
  }

  static HistogramChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new HistogramChart(matrix, width, height, CategorySeries.CategorySeriesRenderStyle.Bar)
  }

  HistogramChart addSeries(String columnName) {
    Column c = matrix.column(columnName)
    addSeries(c, scottsRule(c).round() as int)
  }

  HistogramChart addSeries(String columnName, int numBuckets) {
    addSeries(matrix.column(columnName), numBuckets)
  }

  HistogramChart addSeries(Column column, int numBuckets) {
    Histogram h = new Histogram(column, numBuckets)
    xchart.addSeries(column.name, h.xAxisData, h.yAxisData)
    this
  }

  /**
   * Calculate the bin width using Scott's rule
   * The formula is:
   * <code>3.49 * s / n^(1/3)</code>
   * where s is the standard deviation and n the number of data points
   * @return the suggested number of bins as per the Scott's rule
   */
  static BigDecimal scottsRule(List data) {
    def s = Stat.sd(data)
    def n = data.size()
    3.49 * s / n**(1/3)
  }

  /**
   * Calculate the bin width using Freedman-Diaconis rule
   * The formula is:
   * <code>2 * IQR / n^(1/3)</code>
   * where IQR is a measure of the spread of the data (1st quartile - 3:rd quartile)
   * and n is the number of data points.
   * @param data
   * @return
   */
  static BigDecimal freedmanDiaconisRule(List data) {
    def iqr = Stat.iqr(data)
    BigDecimal r = (2 * iqr / data.size()**(1/3))
    r.abs()
  }


}
