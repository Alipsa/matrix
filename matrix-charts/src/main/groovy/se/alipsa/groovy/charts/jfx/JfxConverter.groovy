package se.alipsa.groovy.charts.jfx

import javafx.scene.chart.XYChart
import se.alipsa.groovy.charts.AreaChart
import se.alipsa.groovy.charts.BarChart
import se.alipsa.groovy.charts.BoxChart
import se.alipsa.groovy.charts.Chart
import se.alipsa.groovy.charts.Histogram
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.ScatterChart

class JfxConverter {

    static javafx.scene.chart.Chart convert(Chart chart) {
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

    static javafx.scene.chart.AreaChart<?,?> convert(AreaChart chart) {
        return JfxAreaChartConverter.convert(chart)
    }

    static XYChart<?,?> convert(BarChart chart) {
        return JfxBarChartConverter.convert(chart)
    }

    static javafx.scene.chart.PieChart convert(PieChart chart) {
        return JfxPieChartConverter.convert(chart)
    }

    static javafx.scene.chart.BarChart convert(Histogram chart) {
        return JfxHistogramConverter.convert(chart)
    }

    static javafx.scene.chart.Chart convert(BoxChart chart) {
        return JfxBoxChartConverter.convert(chart)
    }

    static javafx.scene.chart.ScatterChart convert(ScatterChart chart) {
        return JfXScatterChartConverter.convert(chart)
    }
}
