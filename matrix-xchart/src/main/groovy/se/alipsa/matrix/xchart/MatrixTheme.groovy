package se.alipsa.matrix.xchart

import org.knowm.xchart.style.PieStyler
import org.knowm.xchart.style.colors.ChartColor
import org.knowm.xchart.style.colors.MatlabSeriesColors
import org.knowm.xchart.style.lines.MatlabSeriesLines
import org.knowm.xchart.style.markers.Marker
import org.knowm.xchart.style.markers.SeriesMarkers
import org.knowm.xchart.style.theme.AbstractBaseTheme

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font

/**
 * A chart Theme based on the Matlab theme
 */
@SuppressWarnings('GetterMethodCouldBeProperty')
class MatrixTheme extends AbstractBaseTheme {

  static final Color TEXT_COLOR = ChartColor.DARK_GREY.getColor().darker().darker()
  private static final float HALF_POINT = 0.5f
  private static final float DASH_PHASE = 0.0f
  private static final float DASH_WIDTH = 1.0f
  private static final float DASH_GAP = 3.0f
  private static final float MITER_LIMIT = 10.0f
  private static final int TOOLTIP_COLOR_RED = 255
  private static final int TOOLTIP_COLOR_GREEN = 255
  private static final int TOOLTIP_COLOR_BLUE = 220
  static final List MARKERS = [SeriesMarkers.CIRCLE, SeriesMarkers.CROSS, SeriesMarkers.DIAMOND,
                         SeriesMarkers.SQUARE, SeriesMarkers.TRIANGLE_UP]
  // Chart Style ///////////////////////////////
  @Override
  Color getChartFontColor() {
    TEXT_COLOR
  }

// SeriesMarkers, SeriesLines, SeriesColors ///////////////////////////////

  @Override
  Marker[] getSeriesMarkers() {
    return [SeriesMarkers.NONE] as Marker[]
  }

  @Override
  BasicStroke[] getSeriesLines() {
    return new MatlabSeriesLines().getSeriesLines()
  }

  @Override
  Color[] getSeriesColors() {
    return new MatlabSeriesColors().getSeriesColors()
  }

  // Chart Title ///////////////////////////////

  @Override
  boolean isChartTitleBoxVisible() {
    return false
  }

// Chart Legend ///////////////////////////////

  @Override
  Color getLegendBorderColor() {
    return TEXT_COLOR
  }

  // Chart Axes ///////////////////////////////

  @Override
  Font getAxisTitleFont() {
    return getBaseFont().deriveFont(12f)
  }

  @Override
  int getAxisTickMarkLength() {
    return 5
  }

  @Override
  Color getAxisTickMarksColor() {
    return TEXT_COLOR
  }

  @Override
  BasicStroke getAxisTickMarksStroke() {
    return new BasicStroke(HALF_POINT)
  }

  @Override
  boolean isAxisTicksLineVisible() {
    return false
  }

  @Override
  boolean isAxisTicksMarksVisible() {
    return false
  }

  // Chart Plot Area ///////////////////////////////

  @Override
  Color getPlotBorderColor() {
    return TEXT_COLOR
  }

  @Override
  boolean isPlotBorderVisible() {
    return false
  }

  @Override
  Color getPlotGridLinesColor() {
    return TEXT_COLOR
  }

  @Override
  BasicStroke getPlotGridLinesStroke() {
    return new BasicStroke(
        HALF_POINT,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_ROUND,
        MITER_LIMIT,
        new float[]{DASH_WIDTH, DASH_GAP},
        DASH_PHASE
    )
  }

  @Override
  int getPlotMargin() {
    return 3
  }

  // Tool Tips ///////////////////////////////

  @Override
  Color getToolTipBackgroundColor() {
    return new Color(TOOLTIP_COLOR_RED, TOOLTIP_COLOR_GREEN, TOOLTIP_COLOR_BLUE)
  }

  @Override
  Color getToolTipBorderColor() {
    return TEXT_COLOR
  }

  @Override
  Color getToolTipHighlightColor() {
    return TEXT_COLOR
  }

  // Category Charts ///////////////////////////////

  // Pie Charts ///////////////////////////////

  @Override
  PieStyler.LabelType getLabelType() {
    return PieStyler.LabelType.Name
  }

  // Line, Scatter, Area Charts ///////////////////////////////

  // Error Bars ///////////////////////////////

  // Chart Annotations ///////////////////////////////

}
