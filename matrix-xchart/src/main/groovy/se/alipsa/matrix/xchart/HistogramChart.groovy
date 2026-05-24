package se.alipsa.matrix.xchart

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
 * File file = new File('exampleHistogram.png')
 * hc.exportPng(file)
 * </code></pre>
 */
class HistogramChart extends AbstractChart<HistogramChart, CategoryChart, CategoryStyler, CategorySeries> {

  private static final BigDecimal SCOTT_FACTOR = 3.49
  private static final int CUBE_ROOT_DENOMINATOR = 3
  private static final int FREEDMAN_DIACONIS_FACTOR = 2

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
    List<Number> values = validatedValues(c)
    BigDecimal range = validateRange(values)
    addSeries(c.name, values, bucketCount(range, scottsRule(values)), false)
  }

  HistogramChart addSeries(String columnName, int numBuckets) {
    addSeries(matrix.column(columnName), numBuckets)
  }

  HistogramChart addSeries(Column column, int numBuckets) {
    addSeries(column.name, validatedValues(column), numBuckets)
  }

  private HistogramChart addSeries(String seriesName, List<Number> values, int numBuckets, boolean validateDataRange = true) {
    if (numBuckets <= 0) {
      throw new IllegalArgumentException("Histogram bucket count must be positive, got $numBuckets")
    }
    if (validateDataRange) {
      validateRange(values)
    }
    Histogram h = new Histogram(values, numBuckets)
    xchart.addSeries(seriesName, h.xAxisData, h.yAxisData)
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
    SCOTT_FACTOR * s / n ** (1 / CUBE_ROOT_DENOMINATOR)
  }

  private static int bucketCount(BigDecimal range, BigDecimal binWidth) {
    if (binWidth == null || binWidth <= 0) {
      throw new IllegalArgumentException("Histogram bin width must be positive, got $binWidth")
    }
    (range / binWidth).ceil() as int
  }

  private static List<Number> validatedValues(Column column) {
    if (column == null || column.isEmpty()) {
      throw new IllegalArgumentException('Histogram column must contain at least one value')
    }
    List<Number> values = []
    values.addAll(column.findAll { Number.isInstance(it) } as List<Number>)
    if (values.isEmpty()) {
      throw new IllegalArgumentException("Histogram column '$column.name' must contain at least one non-null numeric value")
    }
    values
  }

  private static BigDecimal validateRange(List<Number> values) {
    BigDecimal min = values[0] as BigDecimal
    BigDecimal max = values[0] as BigDecimal
    values.each { Number value ->
      BigDecimal number = value as BigDecimal
      if (number < min) {
        min = number
      }
      if (number > max) {
        max = number
      }
    }
    BigDecimal range = max - min
    if (range <= 0) {
      throw new IllegalArgumentException('Histogram data must span more than one distinct value')
    }
    range
  }

  /**
   * Calculate the bin width using Freedman-Diaconis rule
   * The formula is:
   * <code>2 * IQR / n^(1/3)</code>
   * where IQR is a measure of the spread of the data (1st quartile - 3:rd quartile)
   * and n is the number of data points.
   * @param data the data values to inspect
   * @return the suggested bin width as per the Freedman-Diaconis rule
   */
  static BigDecimal freedmanDiaconisRule(List data) {
    def iqr = Stat.iqr(data)
    BigDecimal r = (FREEDMAN_DIACONIS_FACTOR * iqr / data.size() ** (1 / CUBE_ROOT_DENOMINATOR))
    r.abs()
  }

}
