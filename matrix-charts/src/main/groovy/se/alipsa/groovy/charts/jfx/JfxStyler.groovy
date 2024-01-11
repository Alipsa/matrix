package se.alipsa.groovy.charts.jfx

import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.chart.Chart
import se.alipsa.groovy.charts.Style

import java.awt.Color

import static se.alipsa.groovy.charts.Style.Position.*

/**
 * Javafx style classes for charts are as follows:
 * <pre>
 * |------------------------------------------|
 * |           .chart                         |
 * |   ------------------------------------   |
 * | . |       .chart-title               |   |
 * | c |----------------------------------| . |
 * | h |                                  | c |
 * | a |       .chart-content             | h |
 * | r |                                  | a |
 * | t |                                  | r |
 * |   |----------------------------------| t |
 * |   |       .chart-legend              |   |
 * |   |----------------------------------|   |
 * |           .chart                         |
 * |------------------------------------------|
 * </pre>
 */
class JfxStyler {

    static void style(Chart jfxChart, se.alipsa.groovy.charts.Chart chart) {
        Style style = chart.getStyle()
        setChartStyles(jfxChart, style)
        setTitleStyles(jfxChart, style)
        setPlotStyles(jfxChart, style)
        setLegendStyles(jfxChart, style)
    }

    static void addStyle(Node node, String style) {
        String existingStyle = node.getStyle().trim()
        String newStyle = style.endsWith(';') ? style : style + ';'
        if(!existingStyle.isEmpty()) {
            newStyle = (existingStyle.endsWith(';') ? existingStyle : existingStyle + ';') + newStyle
        }
        node.setStyle(newStyle)
    }

    private static String asHexString(Color color) {
        final String red = pad(Integer.toHexString(color.getRed()))
        final String green = pad(Integer.toHexString(color.getGreen()))
        final String blue = pad(Integer.toHexString(color.getBlue()))
        final String alpha = pad(Integer.toHexString(color.getAlpha()))
        return '#' + red + green + blue + alpha
    }

    private static String pad(final String hex) {
        return (hex.length() == 1) ? "0" + hex : hex;
    }

    static void setChartStyles(Chart jfxChart, Style style) {
        Node chartNode = jfxChart.lookup('.chart')
        if (style.chartBackgroundColor != null) {
            addStyle(chartNode,"-fx-background-color: ${asHexString(style.chartBackgroundColor)};")
        }
    }

    static void setTitleStyles(Chart jfxChart, Style style) {
        //Node titleNode = jfxChart.lookup('.chart-title')
        if (style.titleVisible != null && style.titleVisible == false) {
            //addStyle(jfxChart.lookup('.chart-title'), 'visibility: hidden;') //does not work
            jfxChart.setTitle(null)
        }
    }

    static void setPlotStyles(Chart jfxChart, Style style) {
        Node plotNode = jfxChart.lookup('.chart-content')
        if (style.plotBackgroundColor != null) {
            addStyle(plotNode, "-fx-background-color: ${asHexString(style.plotBackgroundColor)};")
        }
    }

    static void setLegendStyles(Chart jfxChart, Style style) {
        Node legendNode = jfxChart.lookup('.chart-legend')

        if (style.legendVisible != null) {
            jfxChart.setLegendVisible(style.legendVisible)
        }

        if (style.legendPosition != null) {
            switch (style.legendPosition) {
                case TOP -> jfxChart.setLegendSide(Side.TOP)
                case RIGHT -> jfxChart.setLegendSide(Side.RIGHT)
                case BOTTOM -> jfxChart.setLegendSide(Side.BOTTOM)
                case LEFT -> jfxChart.setLegendSide(Side.LEFT)
            }

        }

        if (style.legendBackgroundColor != null) {
            addStyle(legendNode, "-fx-background-color: ${asHexString(style.legendBackgroundColor)};")
        }

        if (style.legendFont != null) {
            addStyle(legendNode, "-fx-font-family:: ${style.legendFont.getFamily()};")
        }
    }
}
