package se.alipsa.groovy.charts.swing

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYSeries
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.groovy.charts.AreaChart
import se.alipsa.groovy.charts.BarChart
import se.alipsa.groovy.charts.BoxChart
import se.alipsa.groovy.charts.Chart
import se.alipsa.groovy.charts.Histogram
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.ScatterChart

class SwingConverter {

  static XChartPanel<? extends org.knowm.xchart.internal.chartpart.Chart<? extends Styler,? extends Series>> convert(Chart chart) {
    if (chart instanceof AreaChart) {
      return convert((AreaChart) chart)
    } else if (chart instanceof BarChart) {
      return convert((BarChart) chart)
    } else if (chart instanceof PieChart) {
      return convert((PieChart) chart)
    } else if (chart instanceof Histogram) {
      return convert((Histogram)chart)
    } else if (chart instanceof BoxChart) {
      return convert((BoxChart)chart)
    } else if (chart instanceof ScatterChart) {
      return convert((ScatterChart)chart)
    }
    throw new RuntimeException(chart.getClass().getSimpleName() + " conversion is not yet implemented")
  }

  static XChartPanel<XYChart> convert(AreaChart chart) {
    return new XChartPanel<>(SwingAreaChartConverter.convert(chart))
  }

  static XChartPanel<CategoryChart> convert(BarChart chart) {
    return new XChartPanel<>(SwingBarChartConverter.convert(chart))
  }

  static XChartPanel<org.knowm.xchart.PieChart> convert(PieChart chart) {
    return new XChartPanel<>(SwingPieChartConverter.convert(chart))
  }

  static XChartPanel<CategoryChart> convert(Histogram chart) {
    return new XChartPanel<>(SwingHistogramConverter.convert(chart))
  }

  static XChartPanel<org.knowm.xchart.BoxChart> convert(BoxChart chart) {
    return new XChartPanel<>(SwingBoxChartConverter.convert(chart))
  }

  static XChartPanel<XYChart> convert(ScatterChart chart) {
    return new XChartPanel<>(SwingScatterChartConverter.convert(chart))
  }
}
