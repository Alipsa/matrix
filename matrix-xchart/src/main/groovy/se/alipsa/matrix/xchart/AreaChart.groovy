package se.alipsa.matrix.xchart

import groovy.transform.CompileStatic
import org.knowm.xchart.XYSeries
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.abstractions.AbstractXYChart

/**
 * An area chart or area graph displays graphically quantitative data. It is based on an XY chart.
 * It is usually used to compare two or more quantities over time.
 * Sample usage:
 * <pre><code>
 * def ac = AreaChart.create(matrix, 800, 600)
 *   .addSeries('a', matrix.ax, matrix.ay) // columns can be directly added to a series
 *   .addSeries('b', matrix['bx'], matrix['by']) // alternative column notation
 *   .addSeries('c', 'cx', 'cy') // Columns can also be referenced by name only
 * File file = new File("build/testAreaChart.png")
 * ac.exportPng(file)
 * </code></pre>
 */
@CompileStatic
class AreaChart extends AbstractXYChart<AreaChart> {

  private AreaChart(Matrix matrix, int width, int height) {
    super(matrix, width, height, XYSeries.XYSeriesRenderStyle.Area)
  }

  static AreaChart create(Matrix matrix, int width, int height) {
    return new AreaChart(matrix, width, height)
  }

}
