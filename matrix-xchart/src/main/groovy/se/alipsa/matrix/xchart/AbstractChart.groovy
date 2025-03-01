package se.alipsa.matrix.xchart

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.XYChart
import org.knowm.xchart.internal.chartpart.Chart
import se.alipsa.matrix.core.Matrix

abstract class AbstractChart {

  protected Chart xchart
  protected Matrix matrix

  Matrix getMatrix() {
    return matrix
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

  XChartPanel<XYChart> exportSwing() {
    new XChartPanel<XYChart>(xchart as XYChart)
  }
}
