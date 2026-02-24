package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

import testutil.Slow

@Slow
class GeomQuantileTest {

  @Test
  void testBasicQuantilePlot() {
    // Simple linear data with noise
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..20).collect { [it, it * 2 + Math.random() * 5] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_quantile()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testCustomQuantiles() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..20).collect { [it, it * 2 + Math.random() * 5] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_quantile(quantiles: [0.1, 0.5, 0.9])

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testSingleQuantile() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..20).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_quantile(quantiles: [0.5])

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testCustomColor() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile(color: 'red')

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testCustomColour() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile(colour: 'blue')

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testCustomLinetype() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile(linetype: 'dashed')

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('stroke-dasharray'))
  }

  @Test
  void testCustomSize() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile(size: 3)

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testCustomAlpha() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile(alpha: 0.5)

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('stroke-opacity'))
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testInsufficientData() {
    // Only one point - should not crash
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2]])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile()

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testWithFacets() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'group'])
        .rows((1..20).collect { [it, it * 2 + Math.random() * 5, it % 2 == 0 ? 'A' : 'B'] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_quantile(quantiles: [0.5]) +
        facet_wrap(vars('group'))

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testStatQuantile() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(stat: stat_quantile(quantiles: [0.5]))

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testCustomN() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile(n: 50)

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testMultipleLinetypes() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..20).collect { [it, it * 2 + Math.random() * 5] })
        .build()

    // Test various linetypes
    def linetypes = ['solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash']
    for (String linetype : linetypes) {
      def chart = ggplot(data, aes(x: 'x', y: 'y')) +
          geom_quantile(linetype: linetype)

      assertNotNull(chart)
      def svg = chart.render()
      assertNotNull(svg)
    }
  }

  @Test
  void testAsymmetricData() {
    // Data with asymmetric noise - upper quantile should diverge from lower
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([
            [1, 2], [2, 4], [3, 6], [4, 8], [5, 10],
            [6, 12], [7, 14], [8, 16], [9, 20], [10, 25]  // Last two are outliers
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        geom_quantile(quantiles: [0.25, 0.5, 0.75])

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testWithLabels() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows((1..10).collect { [it, it * 2] })
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_quantile() +
        labs(
            title: 'Quantile Regression Test',
            x: 'X Axis',
            y: 'Y Axis'
        )

    assertNotNull(chart)
    def svg = chart.render()
    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('Quantile Regression Test'))
  }
}
