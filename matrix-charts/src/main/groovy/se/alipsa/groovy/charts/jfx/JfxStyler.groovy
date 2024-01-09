package se.alipsa.groovy.charts.jfx

import javafx.scene.Node
import se.alipsa.groovy.charts.PieChart
import se.alipsa.groovy.charts.Style

import java.awt.Color

class JfxStyler {

    static void style(javafx.scene.chart.PieChart pieChart, PieChart chart) {
        Style style = chart.getStyle()

        if (style.plotBackgroundColor != null) {
            addStyle(pieChart.lookup('.chart-content'), "-fx-background-color: ${asHexString(style.plotBackgroundColor)};")
        }

        if (style.legendVisible != null) {
            pieChart.setLegendVisible(style.legendVisible)
        }

        if (style.chartBackgroundColor != null) {
            addStyle(pieChart.lookup('.chart'),"-fx-background-color: ${asHexString(style.chartBackgroundColor)};")
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
