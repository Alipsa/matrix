package se.alipsa.matrix.charts.jfx

import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.chart.Chart
import javafx.scene.chart.XYChart
import se.alipsa.matrix.charts.Style

import static se.alipsa.matrix.charts.util.ColorUtil.*
import static se.alipsa.matrix.charts.util.StyleUtil.*

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

  static void style(Chart jfxChart, se.alipsa.matrix.charts.Chart chart) {
    Style style = chart.getStyle()
    setChartStyles(jfxChart, style)
    setTitleStyles(jfxChart, style)
    setPlotStyles(jfxChart, style)
    setLegendStyles(jfxChart, style)
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
    if (jfxChart instanceof XYChart) {
      def xyChart = jfxChart as XYChart
      if (Boolean.FALSE.equals(style.XAxisVisible)) {
        xyChart.getXAxis().setTickLabelsVisible(false)
        xyChart.getXAxis().setOpacity(0);
      }
      if (Boolean.FALSE == style.YAxisVisible) {
        xyChart.getYAxis().setTickLabelsVisible(false)
        xyChart.getYAxis().setOpacity(0);
      }
    }
  }

  static void setLegendStyles(Chart jfxChart, Style style) {
    Node legendNode = jfxChart.lookup('.chart-legend')

    if (style.legendVisible != null) {
      jfxChart.setLegendVisible(style.legendVisible)
    }

    if (style.legendPosition != null) {
      Map<Style.Position, Side> sideMapping = [
        (Style.Position.TOP)   : Side.TOP,
        (Style.Position.RIGHT) : Side.RIGHT,
        (Style.Position.BOTTOM): Side.BOTTOM,
        (Style.Position.LEFT)  : Side.LEFT
      ]
      Side mappedSide = sideMapping[style.legendPosition]
      if (mappedSide != null) {
        jfxChart.setLegendSide(mappedSide)
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
