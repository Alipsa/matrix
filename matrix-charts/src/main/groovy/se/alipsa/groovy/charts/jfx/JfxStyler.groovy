package se.alipsa.groovy.charts.jfx

import javafx.scene.Node
import javafx.scene.chart.Chart
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.Style

import java.awt.Color

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

    static void style(Chart jfxChart, PieChart chart) {
        Style style = chart.getStyle()

        if (style.plotBackgroundColor != null) {
            addStyle(jfxChart.lookup('.chart-content'), "-fx-background-color: ${asHexString(style.plotBackgroundColor)};")
        }

        if (style.legendVisible != null) {
            jfxChart.setLegendVisible(style.legendVisible)
        }

        if (style.chartBackgroundColor != null) {
            addStyle(jfxChart.lookup('.chart'),"-fx-background-color: ${asHexString(style.chartBackgroundColor)};")
        }

        if (style.titleVisible != null && style.titleVisible == false) {
            //addStyle(jfxChart.lookup('.chart-title'), 'visibility: hidden;') //does not work
            jfxChart.setTitle(null)
        }
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
}
