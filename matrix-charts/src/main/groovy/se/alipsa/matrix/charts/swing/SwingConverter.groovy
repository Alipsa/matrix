package se.alipsa.matrix.charts.swing

import org.knowm.xchart.CategoryChart
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.XYChart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.charts.AreaChart
import se.alipsa.matrix.charts.BarChart
import se.alipsa.matrix.charts.BoxChart
import se.alipsa.matrix.charts.Histogram
import se.alipsa.matrix.charts.LineChart
import se.alipsa.matrix.charts.PieChart
import se.alipsa.matrix.charts.ScatterChart
import se.alipsa.matrix.charts.Chart

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
    } else if (chart instanceof LineChart) {
      return convert((LineChart)chart)
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

  static XChartPanel<XYChart> convert(LineChart chart) {
    return new XChartPanel<>(SwingLineChartConverter.convert(chart))
  }
}
