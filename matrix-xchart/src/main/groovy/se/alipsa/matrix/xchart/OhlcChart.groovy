package se.alipsa.matrix.xchart

import org.knowm.xchart.OHLCChart
import org.knowm.xchart.OHLCChartBuilder
import org.knowm.xchart.OHLCSeries
import org.knowm.xchart.style.OHLCStyler

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart
import se.alipsa.matrix.xchart.abstractions.ChartBuilder

/**
 * Also known as a Candle Stick chart.
 * The Open-high-low-close Charts (or OHLC Charts) are often used as a trading tool to visualise and analyse
 * the price changes over time for securities, currencies, stocks, bonds, commodities, etc.
 * OHLC Charts are useful for interpreting the day-to-day sentiment of the market and forecasting any future
 * price changes through the patterns produced. The y-axis on an OHLC Chart is used for the price scale,
 * while the x-axis is the timescale. On each single time period, an OHLC Chart plots a symbol that represents
 * two ranges: the highest and lowest prices traded, and also the opening and closing price on that single time
 * period (for example in a day). On the range symbol, the high and low price ranges are represented by the
 * length of the main vertical line. The open and close prices are represented by the vertical positioning
 * of tick-marks that appear on the left (representing the open price) and on right (representing the close
 * price) sides of the high-low vertical line.
 * Sample usage:
 * <pre><code>
 * def url = this.getClass().getResource('/gspc.csv')
 * CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
 * Matrix gspc = CsvImporter.importCsv(url, format)
 *   .convert([
 *     Date: LocalDate,
 *     Open: Number,
 *     High: Number,
 *     Low: Number,
 *     Close: Number,
 *     Volume: Number,
 *     Adjusted: Number,
 *   ])
 * def ohlcChart = OhlcChart.create(gspc)
 *   .addSeries('GSPC', gspc.Date, gspc.Open, gspc.High, gspc.Low, gspc.Close)
 * def file2 = new File('OhlcChart2.png')
 * ohlcChart.exportPng(file2)
 * </code></pre>
 */
class OhlcChart extends AbstractChart<OhlcChart, OHLCChart, OHLCStyler, OHLCSeries> {

  private OhlcChart(Matrix matrix, Integer width = null, Integer height = null) {
    def builder = new OHLCChartBuilder()
    if (width != null) {
      builder.width(width)
    }
    if (height != null) {
      builder.height(height)
    }
    initChart(builder.build(), matrix)
  }

  /**
   * Create a new OHLC chart with optional dimensions.
   *
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new OhlcChart instance
   */
  static OhlcChart create(Matrix matrix, Integer width = null, Integer height = null) {
    new OhlcChart(matrix, width, height)
  }

  /**
   * Create a new OHLC chart with a title and optional dimensions.
   *
   * @param title the chart title
   * @param matrix the source Matrix data
   * @param width optional chart width in pixels
   * @param height optional chart height in pixels
   * @return a new OhlcChart instance
   */
  static OhlcChart create(String title, Matrix matrix, Integer width = null, Integer height = null) {
    def chart = new OhlcChart(matrix, width, height)
    chart.title = title
    chart
  }

  /**
   * Add an OHLC series using column names from the source Matrix.
   *
   * @param name the name for this series (displayed in legend)
   * @param xData the name of the column containing date/time values
   * @param open the name of the column containing opening prices
   * @param high the name of the column containing high prices
   * @param low the name of the column containing low prices
   * @param close the name of the column containing closing prices
   * @return this chart for method chaining
   */
  OhlcChart addSeries(String name, String xData, String open, String high, String low, String close) {
    addSeries(name, matrix[xData] as List<?>, matrix[open] as List<Number>, matrix[high] as List<Number>, matrix[low] as List<Number>, matrix[close] as List<Number>)
  }

  /**
   * Add an OHLC series using lists of values.
   *
   * @param name the name for this series (displayed in legend)
   * @param xData the X-axis values: {@link Number}, {@link Date}, {@link java.time.Instant},
   *        {@link java.time.ZonedDateTime}, {@link java.time.OffsetDateTime}, {@link java.time.LocalDateTime},
   *        {@link java.time.LocalDate} or {@link java.time.LocalTime}; all values must be of the same kind
   *        (numeric or date/time) and none may be null
   * @param open the opening price values
   * @param high the high price values
   * @param low the low price values
   * @param close the closing price values
   * @return this chart for method chaining
   * @throws IllegalArgumentException if any list is null, lists have unequal lengths, or xData contains
   *         a null, an unsupported type, or mixes numeric and date/time values
   */
  OhlcChart addSeries(String name, List<?> xData, List<Number> open, List<Number> high, List<Number> low, List<Number> close) {
    validateEqualLengths(xData, open, high, low, close)
    validateXValues(xData)
    xchart.addSeries(name, xData, open, high, low, close)
    this
  }

  private static void validateEqualLengths(List<?> xData, List<Number> open, List<Number> high, List<Number> low, List<Number> close) {
    Map<String, List> data = [
        xData: xData,
        open: open,
        high: high,
        low: low,
        close: close
    ]
    data.each { String columnName, List values ->
      if (values == null) {
        throw new IllegalArgumentException("OHLC $columnName data must not be null")
      }
      if (values.size() != xData.size()) {
        throw new IllegalArgumentException("OHLC series lists must have equal lengths; expected ${xData.size()} values but $columnName has ${values.size()}")
      }
    }
  }

  private static void validateXValues(List<?> xData) {
    Boolean numeric = null
    xData.eachWithIndex { Object value, int i ->
      if (value == null) {
        throw new IllegalArgumentException("OHLC xData must not contain null values (index $i)")
      }
      if (!isSupportedXValue(value.class)) {
        throw new IllegalArgumentException("OHLC xData value at index $i has unsupported type ${value.class.simpleName}; ${ChartBuilder.AXIS_VALUE_TYPES_MESSAGE}")
      }
      boolean isNumber = value instanceof Number
      if (numeric == null) {
        numeric = isNumber
      } else if (numeric != isNumber) {
        throw new IllegalArgumentException("OHLC xData must not mix numeric and date/time values (index $i is ${value.class.simpleName})")
      }
    }
  }

  /**
   * Whether values of the given type can be used on the OHLC X-axis.
   *
   * @param type the column/value type
   * @return true for types that XChart can convert to an X-axis value
   */
  static boolean isSupportedXValue(Class type) {
    ChartBuilder.isAxisValueType(type)
  }

  /** Creates a deferred convenience builder. */
  static Builder builder(Matrix data) { new Builder(data) }

  static class Builder extends ChartBuilder<Builder> {
    private String dateColumn
    private String openColumn
    private String highColumn
    private String lowColumn
    private String closeColumn
    private String seriesName = 'OHLC'
    Builder(Matrix data) { super(data) }
    Builder seriesName(String name) { seriesName = name; this }
    Builder date(String name) {
      requireColumn(name)
      Class type = data.type(name)
      if (!isSupportedXValue(type)) {
        throw new IllegalArgumentException("Column '$name' has type ${type?.simpleName}; ${ChartBuilder.AXIS_VALUE_TYPES_MESSAGE}")
      }
      dateColumn = name
      this
    }
    Builder open(String name) { requireNumeric(name); openColumn = name; this }
    Builder high(String name) { requireNumeric(name); highColumn = name; this }
    Builder low(String name) { requireNumeric(name); lowColumn = name; this }
    Builder close(String name) { requireNumeric(name); closeColumn = name; this }
    OhlcChart build() {
      if ([dateColumn, openColumn, highColumn, lowColumn, closeColumn].any { it == null }) {
        throw new IllegalStateException('date, open, high, low, and close must be set before build()')
      }
      OhlcChart chart = OhlcChart.create(data, chartWidth, chartHeight)
      applyTo(chart)
      chart.addSeries(seriesName, dateColumn, openColumn, highColumn, lowColumn, closeColumn)
      chart
    }
  }

}
