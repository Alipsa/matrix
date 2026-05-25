package se.alipsa.matrix.xchart

import org.knowm.xchart.OHLCChart
import org.knowm.xchart.OHLCChartBuilder
import org.knowm.xchart.OHLCSeries
import org.knowm.xchart.style.OHLCStyler

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractChart

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
 *     Date: Date,
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
    addSeries(name, matrix[xData] as List<Date>, matrix[open] as List<Number>, matrix[high] as List<Number>, matrix[low] as List<Number>, matrix[close] as List<Number>)
  }

  /**
   * Add an OHLC series using lists of values.
   *
   * @param name the name for this series (displayed in legend)
   * @param xData the date/time values for the X-axis
   * @param open the opening price values
   * @param high the high price values
   * @param low the low price values
   * @param close the closing price values
   * @return this chart for method chaining
   * @throws IllegalArgumentException if any list is null or lists have unequal lengths
   */
  OhlcChart addSeries(String name, List<Date> xData, List<Number> open, List<Number> high, List<Number> low, List<Number> close) {
    validateEqualLengths(xData, open, high, low, close)
    xchart.addSeries(name, xData, open, high, low, close)
    this
  }

  private static void validateEqualLengths(List<Date> xData, List<Number> open, List<Number> high, List<Number> low, List<Number> close) {
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

}
