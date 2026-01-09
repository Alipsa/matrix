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
    BaseTest.assertEquals([0, 10], xScale.limits)
    assertTrue(yScale instanceof ScaleYContinuous)
    BaseTest.assertEquals([0, 5], yScale.limits)

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

  @Test
  void testGuideNone() {
    // Test guide_none() factory function
    def none = guide_none()
    assertEquals('none', none.type)
    assertNotNull(none.params)
    assertTrue(none.params.isEmpty())

    // Test that guide_none() suppresses legend
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
        guides(color: guide_none())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertFalse(content.contains('id="legend"'))
  }

  @Test
  void testGuideAxis() {
    // Test guide_axis() factory function
    def axisGuide = guide_axis()
    assertEquals('axis', axisGuide.type)
    assertNotNull(axisGuide.params)
    assertTrue(axisGuide.params.isEmpty())

    // Test with parameters
    def axisWithParams = guide_axis(angle: 45, 'check.overlap': true)
    assertEquals('axis', axisWithParams.type)
    assertEquals(45, axisWithParams.params.angle)
    assertEquals(true, axisWithParams.params['check.overlap'])
  }

  @Test
  void testGuideAxisRotation() {
    // Test that guide_axis with angle parameter rotates labels
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['LongLabel1', 10],
            ['LongLabel2', 20],
            ['LongLabel3', 15]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_col() +
        scale_x_discrete(guide: guide_axis(angle: 45))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Check that rotation transform is applied
    assertTrue(content.contains('rotate(45'))
  }

  @Test
  void testGuideAxisCheckOverlap() {
    // Test that guide_axis with check.overlap hides some labels
    def rows = (1..20).collect { i -> [i, i * 2] }
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows(rows)
        .types(Integer, Integer)
        .build()

    // Without check.overlap - all labels should be present
    def chartNormal = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line()

    Svg svgNormal = chartNormal.render()
    String contentNormal = SvgWriter.toXml(svgNormal)
    int normalLabelCount = contentNormal.findAll(/<text/).size()

    // With check.overlap - fewer labels should be present
    def chartOverlap = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_continuous(guide: guide_axis('check.overlap': true))

    Svg svgOverlap = chartOverlap.render()
    String contentOverlap = SvgWriter.toXml(svgOverlap)
    int overlapLabelCount = contentOverlap.findAll(/<text/).size()

    // With overlap checking, we should have fewer labels
    // (or at least not more - depending on spacing)
    assertTrue(overlapLabelCount <= normalLabelCount)
  }

  @Test
  void testGuideBins() {
    // Test guide_bins() factory function
    def bins = guide_bins()
    assertEquals('bins', bins.type)
    assertNotNull(bins.params)
    assertTrue(bins.params.isEmpty())

    // Test with parameters
    def binsWithParams = guide_bins('show.limits': true)
    assertEquals('bins', binsWithParams.type)
    assertEquals(true, binsWithParams.params['show.limits'])

    // Test that guide_bins can be used (renders as legend for now)
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 2, 5],
            [2, 3, 10],
            [3, 1, 15]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient() +
        guides(color: guide_bins())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    // Should render (as legend for now)
    assertTrue(content.contains('svg'))
  }

  @Test
  void testMultipleGuideTypes() {
    // Test using different guide types in the same chart
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cat', 'value')
        .rows([
            [1, 2, 'A', 5],
            [2, 3, 'B', 10],
            [3, 1, 'C', 15]
        ])
        .types(Integer, Integer, String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat', size: 'value')) +
        geom_point() +
        scale_x_continuous(guide: guide_axis(angle: 45)) +
        guides(color: guide_legend(), size: guide_none())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should have rotation on x-axis
    assertTrue(content.contains('rotate(45'))
    // Should render successfully
    assertTrue(content.contains('svg'))
  }
}
