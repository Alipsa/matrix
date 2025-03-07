package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.core.Matrix

abstract class AbstractChart<T extends AbstractChart, C extends Chart, ST extends Styler, S extends Series> {

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

  void setxLabel(String label) {
    xchart.XAxisTitle = label
  }

  void setyLabel(String label) {
    xchart.YAxisTitle = label
  }

  String getxLabel() {
    xchart.XAxisTitle
  }

  String getyLabel() {
    xchart.YAxisTitle
  }

}
