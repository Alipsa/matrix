package se.alipsa.matrix.xchart.abstractions

import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.VectorGraphicsEncoder
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.internal.series.Series
import org.knowm.xchart.style.Styler
import se.alipsa.matrix.core.Matrix

abstract class AbstractChart<T extends AbstractChart> {

  protected Chart xchart
  protected Matrix matrix

  Matrix getMatrix() {
    return matrix
  }

  T setTitle(String title) {
    xchart.setTitle(title)
    return this as T
  }

  abstract <ST extends Styler> ST getStyle();

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

  <C extends Chart> XChartPanel<C> exportSwing() {
    new XChartPanel<>(getXchart())
  }

  abstract <C extends Chart> C getXchart();

  abstract <S extends Series> S getSeries(String name);

  abstract <S extends Series> Map<String, S> getSeries();

}
