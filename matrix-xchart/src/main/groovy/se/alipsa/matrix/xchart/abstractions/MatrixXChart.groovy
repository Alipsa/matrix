package se.alipsa.matrix.xchart.abstractions
import org.knowm.xchart.internal.chartpart.Chart

import org.knowm.xchart.XChartPanel

interface MatrixXChart<C extends Chart> {

  void exportPng(OutputStream os)
  void exportPng(File file)
  void exportSvg(OutputStream os)

  void exportSvg(File file)
  XChartPanel<C> exportSwing()

}