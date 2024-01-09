package se.alipsa.groovy.charts.swing

import org.knowm.xchart.style.PieStyler
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.Style

class SwingStyleConverter {

    static PieStyler convert(PieChart chart) {
        PieStyler styler = new PieStyler()
        Style style = chart.getStyle()

        if (style.backgroundColor != null) {
            styler.setPlotBackgroundColor(style.backgroundColor)
        }

        if (style.legendVisible != null) {
            styler.setLegendVisible(style.legendVisible)
        }
        styler
    }
}
