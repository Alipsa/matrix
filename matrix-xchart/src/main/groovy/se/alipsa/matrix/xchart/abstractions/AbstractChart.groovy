package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.core.Matrix

import java.awt.Color

abstract class AbstractChart<T extends AbstractChart, C extends Chart, ST extends Styler, S extends Series> implements MatrixXChart {

  protected C xchart
  protected Matrix matrix

  Matrix getMatrix() {
    return matrix
  }

  T setTitle(String title) {
    xchart.setTitle(title)
    return this as T
  }

  ST getStyle() {
    return xchart.getStyler() as ST
  }

  void exportPng(OutputStream os) {
    BitmapEncoder.saveBitmap(xchart, os, BitmapEncoder.BitmapFormat.PNG)
  }

  void exportPng(File file) {
    BitmapEncoder.saveBitmap(xchart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
  }

  void exportSvg(OutputStream os) {
    VectorGraphicsEncoder.saveVectorGraphic(xchart, os, VectorGraphicsEncoder.VectorGraphicsFormat.SVG)
  }

  void exportSvg(File file) {
    VectorGraphicsEncoder.saveVectorGraphic(xchart, file.absolutePath, VectorGraphicsEncoder.VectorGraphicsFormat.SVG)
  }

  XChartPanel<C> exportSwing() {
    new XChartPanel<>(getXchart())
  }

  C getXchart() {
    return this.xchart
  }

  S getSeries(String name) {
    xchart.getSeriesMap().get(name)
  }

  Map<String, S> getSeries() {
    xchart.getSeriesMap()
  }

  T setXLabel(String label) {
    xchart.XAxisTitle = label
    return this as T
  }

  T setYLabel(String label) {
    xchart.YAxisTitle = label
    return this as T
  }

  String getXLabel() {
    xchart.XAxisTitle
  }

  String getYLabel() {
    xchart.YAxisTitle
  }

  void makeFillTransparent(Series s, int numSeries, Integer transparency = 185) {
    // Make the fill transparent so that overlaps are visible
    def colors = style.theme.seriesColors
    if (numSeries > colors.size() - 1) {
      def multiple = Math.ceil(style.theme.seriesColors.size() / numSeries).intValue()
      colors = style.theme.seriesColors*multiple
    }
    def color = colors[numSeries]
    //s.lineColor = color.darker()
    s.fillColor = new Color(color.red, color.green, color.blue, transparency)
  }

}
