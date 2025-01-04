package se.alipsa.matrix.charts.svg

import se.alipsa.matrix.charts.BarChart
import se.alipsa.matrix.charts.Legend
import se.alipsa.matrix.charts.Style
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charts.util.ColorUtil

import java.text.DecimalFormat

class SvgBarChart extends SvgChart {
  BarChart barChart

  SvgBarChart(BarChart barChart, int width, int height) {
    this.barChart = barChart

    svg = new Svg()
    int titleXOffset = 10
    int titleYOffset = 15
    int titleHeight = 30

    int plotXOffset = 50
    int plotYOffset = 30
    int xaxisLabelHeight = 20
    int graphHeight = height - titleYOffset - titleHeight -xaxisLabelHeight
    int graphWidth = width - plotXOffset

    svg.height(height)
    svg.width(width)
    svg.viewBox("0 0 $width $height")
    if (barChart.style.css != null) {
      svg.addStyle()
          .type('text/css')
          .addContent(barChart.style.css)
    }
    addCanvas(width, height)
    addTitle(barChart.title, barChart.style, titleXOffset, titleYOffset)
    addGraph(graphWidth, graphHeight, plotXOffset, plotYOffset)
    addLegend(barChart.legend, barChart.style)
  }

  void addCanvas(int width, int height) {
    svg.addRect(width, height)
        .fill(ColorUtil.asHexString(barChart.style.chartBackgroundColor, 'white'))
        .styleClass("chart")
  }

  void addTitle(String title, Style style, int xOffset, int yOffset) {
    //style.titlePosition todo add this
    svg.addText(title).x(xOffset).y(yOffset).fill('black').styleClass('chart-title')
  }

  void addLegend(Legend legend, Style style) {
    G g = svg.addG()
    g.styleClass('chart-legend')
    g.fill("white")
    Map<String, Double> pos = calculatePosition(style.legendPosition)
    g.transform("translate(${pos.x}, ${pos.y})")
  }

  void addGraph(double graphWidth, double graphHeight, double xOffset, double yOffset) {
    G graph = svg.addG()
    graph.styleClass('chart-content')
    def barSpace = 5 // todo check if defined in style
    Map boundaries = calculateValues(barChart.valueSeries)
    double orgMaxHeight = boundaries.maxHeight
    println "orgMaxHeight = $orgMaxHeight"
    double proportion = graphHeight / orgMaxHeight
    println("proportion = graphHeight / orgMaxHeight = $proportion = $graphHeight / $orgMaxHeight")
    List<List> valueSeries = []
    barChart.valueSeries.each {series ->
      def serie = []
      series.each { val ->
        serie << val * proportion
      }
      valueSeries << serie
    }
    boundaries = calculateValues(valueSeries)
    double maxHeight = boundaries.maxHeight + yOffset
    println "maxHeight = $maxHeight"
    double barWidth = graphWidth / boundaries.numBars - barSpace * 2

    double width = calculateWidth(valueSeries, barWidth, barSpace) + xOffset
    // plot the axis
    plotXAxis(graph, maxHeight, width, xOffset, 'black')
    plotYAxis(graph, maxHeight, xOffset, yOffset, 'black')
    // plot the ticks
    List<Double> xTicks = plotXTicks(graph, maxHeight, valueSeries, xOffset, barWidth, barSpace, 'black')
    List<Double> yTicks = plotYTicks(graph, maxHeight, xOffset, yOffset, 5, 'black')
    // plot the tick values/labels
    plotXLabels(graph, xTicks, maxHeight, 20,'black')
    plotYLabels(graph, yTicks, xOffset, orgMaxHeight, 10, 'black', barChart.style.yLabels.format as DecimalFormat)
    // plot the data
    plotData(graph, valueSeries, barWidth, barSpace, maxHeight, xOffset, yOffset)
  }

  void plotYAxis(G graph, double maxHeight, double xOffset, double yOffset, String color) {
    graph.addLine(xOffset, maxHeight, xOffset, yOffset)
        .stroke(color)
        .styleClass('axis')
        .id('yaxis')
  }

  List<Double> plotYTicks(G graph, double maxHeight, double xOffset, double yOffset, int nofTicks, String color) {
    double tickSpace = (maxHeight - yOffset) /nofTicks
    List<Double> yTicks = []
    double y = maxHeight - tickSpace
    for ( tick in 1..nofTicks) {
      //println "adding y tick at $y"
      graph.addLine(xOffset - 3, y, xOffset +3, y)
          .stroke(color)
          .styleClass('tick ytick')
          .id("ytick-$tick")
      yTicks << y
      y -= tickSpace
    }
    //println "yTicks = $yTicks"
    yTicks
  }

  void plotXAxis(G graph, double maxHeight, double width, double xOffset, String color) {
    graph.addLine(xOffset, maxHeight, width, maxHeight)
        .stroke(color)
        .styleClass('axis')
        .id('xaxis')
    //println "Added x axis with width $width"
  }

  List<Double> plotXTicks(G graph, double maxHeight, List valueSeries , double xOffset, double barWidth, double barSpace, String color) {
    List<Double> xTicks = []
    double x = xOffset + barWidth / 2 + barSpace
    valueSeries.eachWithIndex { series, seriesIdx -> {
        series.eachWithIndex{ bar, valueIdx ->
          //println "add x axis tick at $x"
          graph.addLine(x, maxHeight - 5, x, maxHeight + 5)
              .stroke(color)
              .styleClass('tick xtick')
              .id("xtick-$seriesIdx-$valueIdx")
          xTicks << x
          x += barSpace + barWidth
        }
      }
    }
    //println "xTicks = $xTicks"
    xTicks
  }

  static Map<String, Number> calculateValues(List<List> values) {
    int maxHeight = 0
    int numBars = 0
    values.each {
      Number maxVal = it.max()
      if (maxVal > maxHeight) {
        maxHeight = maxVal
      }
      numBars += it.size()
    }
    [maxHeight: maxHeight, numBars: numBars]
  }

  static double calculateWidth(List<List> values, double barWidth, double barSpace) {
    double totWidth = 0
    values.each {
      it.each {
        totWidth += barWidth + barSpace
      }
    }
    totWidth
  }

  void plotData(G graph, List valueSeries, double barWidth, double barSpace, double maxHeight, double xOffset, double yOffset) {
    int startX = xOffset + barSpace

    valueSeries.eachWithIndex { series, seriesIdx ->
      series.eachWithIndex { value, valueIdx ->
        Number val = value as Number
        graph.addRect(barWidth, val)
            .styleClass("bar")
            .id("bar-$seriesIdx-$valueIdx")
            .fill('navy')
            .x(startX)
            .y(maxHeight - val)
            .rx(5)
        startX = startX + barWidth + barSpace
      }
    }
  }


  void plotXLabels(G graph, List<Double> xTicks, double maxHeight, double margin, String color) {

    xTicks.eachWithIndex { xTick, idx ->
      graph.addText(String.valueOf(barChart.categorySeries.get(idx)))
      .y(maxHeight + margin)
      .x(xTick)
      .fill(color)
      .textAnchor('middle')
    }
  }

  static void plotYLabels(G graph, List<Double> yTicks, double xOffset, double maxValue, double margin, String color, DecimalFormat format = null) {
    if (format ==null) {
      format = getDecimalFormat(maxValue, 3)
    }
    yTicks.sort().eachWithIndex { double yTick, int idx ->
      def val = maxValue /(idx + 1)
      graph.addText(format.format(val))
      .x(xOffset - margin)
      .y(yTick + 5)
      .fill(color)
      .textAnchor('end')
    }
  }

  static DecimalFormat getDecimalFormat(double maxValue, int defaultNumberOfDecimals) {
    int numDigits = defaultNumberOfDecimals
    if (maxValue > 999) {
      numDigits = 0
    } else if (maxValue > 99) {
      numDigits = 1
    } else if (maxValue > 9 ) {
      numDigits = 2
    } else if (maxValue <= 0.001) {
      numDigits = 4
    } else if (numDigits <= 0.01) {
      numDigits = 3
    } else if (numDigits <= 0.1) {
      numDigits = 2
    }
    DecimalFormat format = DecimalFormat.instance as DecimalFormat
    format.setMaximumFractionDigits(numDigits)
    format.setMinimumFractionDigits(numDigits)
    format
  }

  Map<String, Double> calculatePosition(Style.Position position) {
    [:]
  }
}
