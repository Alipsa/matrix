package se.alipsa.matrix.charts.jfx

import javafx.geometry.Insets
import javafx.scene.chart.Chart
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

/**
 * A custom JavaFX Chart that renders box-and-whisker plots.
 *
 * <p>Each category is drawn as a box from Q1 to Q3 with a median line,
 * whiskers extending to the lower/upper fences, and circles for outliers.</p>
 */
class JfxBoxChart extends Chart {

  private static final double MARGIN_LEFT = 60
  private static final double MARGIN_RIGHT = 20
  private static final double MARGIN_TOP = 40
  private static final double MARGIN_BOTTOM = 50
  private static final double TICK_LENGTH = 5
  private static final int NUM_Y_TICKS = 8
  private static final double OUTLIER_RADIUS = 3
  private static final Color DEFAULT_BOX_FILL = Color.STEELBLUE
  private static final Color DEFAULT_BOX_STROKE = Color.BLACK
  private static final Color MEDIAN_COLOR = Color.DARKRED
  private static final Color OUTLIER_COLOR = Color.FIREBRICK

  private final List<BoxStats> stats = []
  private String xAxisLabel = 'X'
  private String yAxisLabel = 'Y'

  /**
   * Creates a JfxBoxChart from category names and corresponding raw value lists.
   *
   * @param categories the category labels
   * @param valueSeries a list of number lists, one per category
   * @param title the chart title
   * @param xLabel the x-axis label
   * @param yLabel the y-axis label
   */
  JfxBoxChart(List<String> categories, List<List> valueSeries, String title, String xLabel, String yLabel) {
    setTitle(title ?: '')
    this.xAxisLabel = xLabel ?: 'X'
    this.yAxisLabel = yLabel ?: 'Y'

    categories.eachWithIndex { String category, int idx ->
      List<Number> values = valueSeries[idx].findAll { it instanceof Number } as List<Number>
      if (values) {
        stats << computeStats(category, values)
      }
    }
  }

  /**
   * Computes box plot statistics for a single category.
   */
  private static BoxStats computeStats(String category, List<Number> values) {
    List<Number> quartiles = Stat.quartiles(values)
    BigDecimal q1 = quartiles[0] as BigDecimal
    BigDecimal q3 = quartiles[1] as BigDecimal
    BigDecimal median = Stat.median(values)
    BigDecimal iqr = Stat.iqr(values) as BigDecimal
    BigDecimal dataMin = Stat.min(values) as BigDecimal
    BigDecimal dataMax = Stat.max(values) as BigDecimal

    BigDecimal lowerFence = [dataMin, q1 - 1.5 * iqr].max() as BigDecimal
    BigDecimal upperFence = [dataMax, q3 + 1.5 * iqr].min() as BigDecimal

    List<Number> outliers = values.findAll { Number v ->
      (v as BigDecimal) < lowerFence || (v as BigDecimal) > upperFence
    }

    new BoxStats(
        category: category,
        q1: q1,
        median: median,
        q3: q3,
        iqr: iqr,
        lowerWhisker: lowerFence,
        upperWhisker: upperFence,
        outliers: outliers
    )
  }

  @Override
  protected void layoutChartChildren(double top, double left, double width, double height) {
    getChartChildren().clear()

    if (stats.isEmpty()) {
      return
    }

    double plotLeft = left + MARGIN_LEFT
    double plotRight = left + width - MARGIN_RIGHT
    double plotTop = top + MARGIN_TOP
    double plotBottom = top + height - MARGIN_BOTTOM
    double plotWidth = plotRight - plotLeft
    double plotHeight = plotBottom - plotTop

    if (plotWidth <= 0 || plotHeight <= 0) {
      return
    }

    // Compute global Y range
    BigDecimal yMin = stats.collect { [it.lowerWhisker, it.outliers.min { it as BigDecimal } ?: it.lowerWhisker].min() as BigDecimal }.min()
    BigDecimal yMax = stats.collect { [it.upperWhisker, it.outliers.max { it as BigDecimal } ?: it.upperWhisker].max() as BigDecimal }.max()

    // Add padding to Y range
    BigDecimal yRange = yMax - yMin
    if (yRange == 0) {
      yRange = 1
    }
    BigDecimal yPadding = yRange * 0.05
    yMin = yMin - yPadding
    yMax = yMax + yPadding

    // Draw axes
    drawAxes(plotLeft, plotTop, plotRight, plotBottom, yMin, yMax)

    // Draw boxes
    int n = stats.size()
    double categoryWidth = plotWidth / n
    double boxWidth = categoryWidth * 0.6

    stats.eachWithIndex { BoxStats bs, int idx ->
      double cx = plotLeft + categoryWidth * idx + categoryWidth / 2
      drawBox(bs, cx, boxWidth, plotTop, plotBottom, yMin, yMax)
    }
  }

  /**
   * Draws the X and Y axes with tick marks and labels.
   */
  private void drawAxes(double plotLeft, double plotTop, double plotRight, double plotBottom,
                        BigDecimal yMin, BigDecimal yMax) {
    // Y axis
    Line yAxis = new Line(plotLeft, plotTop, plotLeft, plotBottom)
    yAxis.setStroke(Color.BLACK)
    getChartChildren().add(yAxis)

    // X axis
    Line xAxis = new Line(plotLeft, plotBottom, plotRight, plotBottom)
    xAxis.setStroke(Color.BLACK)
    getChartChildren().add(xAxis)

    // Y axis ticks and labels
    BigDecimal yRange = yMax - yMin
    for (int i = 0; i <= NUM_Y_TICKS; i++) {
      BigDecimal value = yMin + yRange * i / NUM_Y_TICKS
      double y = valueToY(value, plotTop, plotBottom, yMin, yMax)

      Line tick = new Line(plotLeft - TICK_LENGTH, y, plotLeft, y)
      tick.setStroke(Color.BLACK)
      getChartChildren().add(tick)

      Text label = new Text(formatNumber(value))
      label.setFont(Font.font(10))
      label.setTextAlignment(TextAlignment.RIGHT)
      double textWidth = label.getLayoutBounds().getWidth()
      label.setX(plotLeft - TICK_LENGTH - textWidth - 2)
      label.setY(y + label.getLayoutBounds().getHeight() / 4)
      getChartChildren().add(label)
    }

    // Y axis label
    Text yLabel = new Text(yAxisLabel)
    yLabel.setFont(Font.font(12))
    yLabel.setRotate(-90)
    double yLabelX = plotLeft - MARGIN_LEFT + 12
    double yLabelY = (plotTop + plotBottom) / 2
    yLabel.setX(yLabelX)
    yLabel.setY(yLabelY)
    getChartChildren().add(yLabel)

    // X axis category labels
    double categoryWidth = (plotRight - plotLeft) / stats.size()
    stats.eachWithIndex { BoxStats bs, int idx ->
      double cx = plotLeft + categoryWidth * idx + categoryWidth / 2
      Text label = new Text(bs.category)
      label.setFont(Font.font(11))
      double textWidth = label.getLayoutBounds().getWidth()
      label.setX(cx - textWidth / 2)
      label.setY(plotBottom + 18)
      getChartChildren().add(label)
    }

    // X axis label
    Text xLabel = new Text(xAxisLabel)
    xLabel.setFont(Font.font(12))
    double xLabelWidth = xLabel.getLayoutBounds().getWidth()
    xLabel.setX((plotLeft + plotRight) / 2 - xLabelWidth / 2)
    xLabel.setY(plotBottom + 38)
    getChartChildren().add(xLabel)
  }

  /**
   * Draws a single box plot (box, median line, whiskers, caps, outliers).
   */
  private void drawBox(BoxStats bs, double cx, double boxWidth, double plotTop, double plotBottom,
                       BigDecimal yMin, BigDecimal yMax) {
    double halfBox = boxWidth / 2

    double yQ1 = valueToY(bs.q1, plotTop, plotBottom, yMin, yMax)
    double yQ3 = valueToY(bs.q3, plotTop, plotBottom, yMin, yMax)
    double yMedian = valueToY(bs.median, plotTop, plotBottom, yMin, yMax)
    double yLower = valueToY(bs.lowerWhisker, plotTop, plotBottom, yMin, yMax)
    double yUpper = valueToY(bs.upperWhisker, plotTop, plotBottom, yMin, yMax)

    // Box (Q1 to Q3) — note: yQ3 < yQ1 because Y is inverted in screen coords
    double boxTop = Math.min(yQ1, yQ3)
    double boxHeight = Math.abs(yQ1 - yQ3)
    Rectangle box = new Rectangle(cx - halfBox, boxTop, boxWidth, boxHeight)
    box.setFill(DEFAULT_BOX_FILL)
    box.setStroke(DEFAULT_BOX_STROKE)
    box.setStrokeWidth(1)
    box.setOpacity(0.7)
    getChartChildren().add(box)

    // Median line
    Line medianLine = new Line(cx - halfBox, yMedian, cx + halfBox, yMedian)
    medianLine.setStroke(MEDIAN_COLOR)
    medianLine.setStrokeWidth(2)
    getChartChildren().add(medianLine)

    // Lower whisker (vertical line from Q1 down to lower fence)
    Line lowerWhisker = new Line(cx, yQ1, cx, yLower)
    lowerWhisker.setStroke(Color.BLACK)
    getChartChildren().add(lowerWhisker)

    // Upper whisker (vertical line from Q3 up to upper fence)
    Line upperWhisker = new Line(cx, yQ3, cx, yUpper)
    upperWhisker.setStroke(Color.BLACK)
    getChartChildren().add(upperWhisker)

    // Lower cap
    double capWidth = boxWidth * 0.3
    Line lowerCap = new Line(cx - capWidth, yLower, cx + capWidth, yLower)
    lowerCap.setStroke(Color.BLACK)
    getChartChildren().add(lowerCap)

    // Upper cap
    Line upperCap = new Line(cx - capWidth, yUpper, cx + capWidth, yUpper)
    upperCap.setStroke(Color.BLACK)
    getChartChildren().add(upperCap)

    // Outliers
    bs.outliers.each { Number outlier ->
      double yOutlier = valueToY(outlier as BigDecimal, plotTop, plotBottom, yMin, yMax)
      Circle circle = new Circle(cx, yOutlier, OUTLIER_RADIUS)
      circle.setFill(Color.TRANSPARENT)
      circle.setStroke(OUTLIER_COLOR)
      circle.setStrokeWidth(1.5)
      getChartChildren().add(circle)
    }
  }

  /**
   * Maps a data value to a Y pixel coordinate (inverted: higher values are nearer the top).
   */
  private static double valueToY(BigDecimal value, double plotTop, double plotBottom,
                                  BigDecimal yMin, BigDecimal yMax) {
    double ratio = ((value - yMin) / (yMax - yMin)) as double
    plotBottom - ratio * (plotBottom - plotTop)
  }

  /**
   * Formats a number for axis tick labels.
   */
  private static String formatNumber(BigDecimal value) {
    if (value == value.setScale(0, RoundingMode.HALF_UP)) {
      return value.toBigInteger().toString()
    }
    value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
  }

  /**
   * Internal data class holding computed statistics for one box plot category.
   */
  private static class BoxStats {
    String category
    BigDecimal q1
    BigDecimal median
    BigDecimal q3
    BigDecimal iqr
    BigDecimal lowerWhisker
    BigDecimal upperWhisker
    List<Number> outliers = []
  }
}
