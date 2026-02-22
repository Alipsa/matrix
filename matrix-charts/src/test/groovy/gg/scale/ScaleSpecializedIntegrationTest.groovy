package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

import testutil.Slow

/**
 * Slow integration smoke tests for specialized step/fermenter color scales.
 * Keeps one representative render per scale family.
 */
@Slow
class ScaleSpecializedIntegrationTest {

  @Test
  void testStepsFillRendersWithGeomTile() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 1, 10], [2, 1, 30], [3, 1, 50],
        [1, 2, 70], [2, 2, 90], [3, 2, 100]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
      geom_tile() +
      scale_fill_steps(bins: 5)

    Svg svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<rect'))
  }

  @Test
  void testStepsNColorRendersWithGeomPoint() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 2, 10],
        [2, 4, 30],
        [3, 6, 50],
        [4, 8, 70],
        [5, 10, 90]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
      geom_point() +
      scale_color_stepsn(bins: 5, colors: ['red', 'yellow', 'green', 'blue'])

    Svg svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<circle'))
  }

  @Test
  void testSteps2FillRendersWithGeomTile() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 1, -50], [2, 1, -25], [3, 1, 0],
        [1, 2, 25], [2, 2, 50], [3, 2, 75]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
      geom_tile() +
      scale_fill_steps2(bins: 7, midpoint: 0)

    Svg svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<rect'))
  }

  @Test
  void testFermenterFillRendersWithGeomTile() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 1, 10],
        [2, 1, 20],
        [3, 1, 30],
        [1, 2, 40],
        [2, 2, 50],
        [3, 2, 60]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
      geom_tile() +
      scale_fill_fermenter(palette: 'YlOrRd', type: 'seq')

    Svg svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<rect'))
  }

  @Test
  void testFermenterDivergingRendersWithGeomTile() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 1, -10],
        [2, 1, -5],
        [3, 1, 0],
        [4, 1, 5],
        [5, 1, 10]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
      geom_tile() +
      scale_fill_fermenter(type: 'div', palette: 'RdBu', direction: 1)

    Svg svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<rect'))
  }
}
