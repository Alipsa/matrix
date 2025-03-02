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
 * A chart Theme based on the MathLab theme
 */
class MatrixTheme extends AbstractBaseTheme {

  static final List MARKERS = [SeriesMarkers.CIRCLE, SeriesMarkers.CROSS, SeriesMarkers.DIAMOND,
                         SeriesMarkers.SQUARE, SeriesMarkers.TRIANGLE_UP]
  // Chart Style ///////////////////////////////
  @Override
  Color getChartFontColor() {
    ChartColor.DARK_GREY.getColor()
  }

// SeriesMarkers, SeriesLines, SeriesColors ///////////////////////////////

  @Override
  Marker[] getSeriesMarkers() {
    return [SeriesMarkers.NONE]
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
    return ChartColor.DARK_GREY.getColor()
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
    return ChartColor.DARK_GREY.getColor()
  }

  @Override
  BasicStroke getAxisTickMarksStroke() {
    return new BasicStroke(.5f)
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
    return ChartColor.DARK_GREY.getColor()
  }

  @Override
  boolean isPlotBorderVisible() {
    return false
  }

  @Override
  Color getPlotGridLinesColor() {
    return ChartColor.DARK_GREY.getColor()
  }

  @Override
  BasicStroke getPlotGridLinesStroke() {
    return new BasicStroke(
        .5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[]{1f, 3.0f}, 0.0f)
  }

  @Override
  int getPlotMargin() {
    return 3
  }

  // Tool Tips ///////////////////////////////

  @Override
  Color getToolTipBackgroundColor() {
    return new Color(255, 255, 220)
  }

  @Override
  Color getToolTipBorderColor() {
    return ChartColor.DARK_GREY.getColor()
  }

  @Override
  Color getToolTipHighlightColor() {
    return ChartColor.DARK_GREY.getColor()
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