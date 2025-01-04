package se.alipsa.matrix.charts.swing

import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.charts.Style

class SwingStyler {

    static void style(Chart swingChart, se.alipsa.matrix.charts.Chart chart) {
        Style style = chart.getStyle()
        setChartStyles(swingChart, style)
        setTitleStyles(swingChart, style)
        setPlotStyles(swingChart, style)
        setLegendStyles(swingChart, style)
    }

    static void setChartStyles(Chart swingChart, Style style) {
        Styler styler = swingChart.getStyler()

        if (style.chartBackgroundColor != null) {
            styler.setChartBackgroundColor(style.chartBackgroundColor)
        }
    }

    static void setTitleStyles(Chart swingChart, Style style) {
        Styler styler = swingChart.getStyler()

        if (style.titleVisible != null) {
            styler.setChartTitleVisible(style.titleVisible)
        }
    }

    static void setPlotStyles(Chart swingChart, Style style) {
        Styler styler = swingChart.getStyler()

        if (style.plotBackgroundColor != null) {
            styler.setPlotBackgroundColor(style.plotBackgroundColor)
        }
    }

    static void setLegendStyles(Chart swingChart, Style style) {
        Styler styler = swingChart.getStyler()

        if (style.legendVisible != null) {
            styler.setLegendVisible(style.legendVisible)
        }

        if (style.legendPosition != null) {
            switch (style.legendPosition) {
                case TOP -> styler.setLegendPosition(Styler.LegendPosition.InsideN)
                case RIGHT -> styler.setLegendPosition(Styler.LegendPosition.OutsideE)
                case BOTTOM -> styler.setLegendPosition(Styler.LegendPosition.OutsideS)
                case LEFT -> styler.setLegendPosition(Styler.LegendPosition.InsideNW)
            }
        }

        if (style.legendBackgroundColor != null) {
            styler.setLegendBackgroundColor(style.legendBackgroundColor)
        }

        if (style.legendFont != null) {
            styler.setLegendFont(style.legendFont)
        }
    }
}
