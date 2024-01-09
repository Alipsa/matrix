package se.alipsa.groovy.charts.swing

import org.knowm.xchart.style.PieStyler
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.Style

class SwingStyler {

    static void style(org.knowm.xchart.PieChart pieChart, PieChart chart) {
        PieStyler styler = pieChart.getStyler()
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
        styler
    }
}
