package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomBlank
import se.alipsa.matrix.gg.geom.GeomText
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.ScaleYContinuous

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GuideAnnotateLimitsTest {

  @Test
  void testAnnotateTextLayer() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[0, 0]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        annotate('text', x: 1, y: 2, label: 'Note')

    assertEquals(1, chart.layers.size())
    def layer = chart.layers.first()
    assertTrue(layer.geom instanceof GeomText)
    assertFalse(layer.inheritAes)
    assertEquals(['x', 'y', 'label'], layer.data.columnNames())
    assertEquals([1], layer.data['x'])
    assertEquals([2], layer.data['y'])
    assertEquals(['Note'], layer.data['label'])

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('Note'))
  }

  @Test
  void testExpandLimitsLayer() {
    def layer = expand_limits(x: [0, 10], y: 5)
    assertTrue(layer.geom instanceof GeomBlank)
    assertFalse(layer.inheritAes)
    assertEquals(['x', 'y'], layer.data.columnNames())
    assertEquals([0, 10], layer.data['x'])
    assertEquals([5, 5], layer.data['y'])
  }

  @Test
  void testLimsBuildsScales() {
    def limits = lims(x: [0, 10], y: [0, 5])
    def xScale = limits.find { it.aesthetic == 'x' }
    def yScale = limits.find { it.aesthetic == 'y' }

    assertTrue(xScale instanceof ScaleXContinuous)
    assertEquals([0, 10], xScale.limits)
    assertTrue(yScale instanceof ScaleYContinuous)
    assertEquals([0, 5], yScale.limits)

    def discrete = lims(x: ['a', 'b'])
    def xDiscrete = discrete.find { it.aesthetic == 'x' }
    assertTrue(xDiscrete instanceof ScaleXDiscrete)
    assertEquals(['a', 'b'], xDiscrete.limits)
  }

  @Test
  void testGuidesHideLegend() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B'],
            [3, 1, 'C']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        guides(color: 'none')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertFalse(content.contains('id="legend"'))
  }

  @Test
  void testGuideLegendForContinuousScale() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 2, 0],
            [2, 3, 5],
            [3, 1, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10], labels: ['break-low', 'break-mid', 'break-high']) +
        guides(color: guide_legend())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('>break-mid<'))
  }

  @Test
  void testGuideColorbarForContinuousScale() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 2, 0],
            [2, 3, 5],
            [3, 1, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10], labels: ['break-low', 'break-mid', 'break-high']) +
        guides(color: guide_colorbar())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertFalse(content.contains('>break-mid<'))
  }

  @Test
  void testGuideHelpers() {
    def legend = guide_legend()
    assertEquals('legend', legend.type)

    def colorbar = guide_colorbar()
    assertEquals('colorbar', colorbar.type)

    def colourbar = guide_colourbar()
    assertEquals('colorbar', colourbar.type)

    def guideSpec = guides(color: legend)
    assertEquals(legend, guideSpec.specs['color'])
  }
}
