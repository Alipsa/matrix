package se.alipsa.groovy.charts.swing

import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.style.PieStyler
import org.knowm.xchart.style.Styler
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.Style

class SwingStyler {

    static void style(Chart pieChart, PieChart chart) {
        Styler styler = pieChart.getStyler()
        Style style = chart.getStyle()

        if (style.plotBackgroundColor != null) {
            styler.setPlotBackgroundColor(style.plotBackgroundColor)
        }

        if (style.legendVisible != null) {
            styler.setLegendVisible(style.legendVisible)
        }

        if (style.chartBackgroundColor != null) {
            styler.setChartBackgroundColor(style.chartBackgroundColor)
        }

        if (style.titleVisible != null) {
            styler.setChartTitleVisible(style.titleVisible)
        }

        styler
    }

}
