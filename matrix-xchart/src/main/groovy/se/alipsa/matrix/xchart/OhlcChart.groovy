package se.alipsa.matrix.xchart

import groovy.transform.CompileStatic

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
 *   .addSeries("GSPC", gspc.Date, gspc.Open, gspc.High, gspc.Low, gspc.Close)
 * def file2 = new File("OhlcChart2.png")
 * ohlcChart.exportPng(file2)
 * </code></pre>
 */
@CompileStatic
class OhlcChart extends AbstractChart<OhlcChart, OHLCChart, OHLCStyler, OHLCSeries> {

  private OhlcChart(Matrix matrix, Integer width = null, Integer height = null) {
    super.matrix = matrix
    def builder = new OHLCChartBuilder()
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


  static OhlcChart create(Matrix table, Integer width = null, Integer height = null) {
    new OhlcChart(table, width, height)
  }

  OhlcChart addSeries(String name, String xData, String open, String high, String low, String close) {
    addSeries(name, matrix[xData], matrix[open], matrix[high], matrix[low], matrix[close])
  }

  OhlcChart addSeries(String name, List<Number> xData, List<Number> open, List<Number> high, List<Number> low, List<Number> close) {
    xchart.addSeries(name, xData, open, high, low, close)
    this
  }
}
