package gg.adapter

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.adapter.GgCharmAdaptation
import se.alipsa.matrix.gg.adapter.GgCharmAdapter

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GgCharmAdapterGuideTest {

  private final GgCharmAdapter adapter = new GgCharmAdapter()

  private Matrix sampleData() {
    Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 3],
            [3, 4]
        ])
        .types(Integer, Integer)
        .build()
  }

  @Test
  void testChartWithGuideLegendDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(color: guide_legend())

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with guide_legend should delegate. Reasons: ${adaptation.reasons}")
    assertNotNull(adaptation.charmChart.guides, 'Guides should be mapped')
    assertEquals(GuideType.LEGEND, adaptation.charmChart.guides.getSpec('color').type)
  }

  @Test
  void testChartWithGuideColorbarDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(color: guide_colorbar())

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with guide_colorbar should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.COLORBAR, adaptation.charmChart.guides.getSpec('color').type)
  }

  @Test
  void testChartWithGuideNoneDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(color: guide_none())

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with guide_none should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.NONE, adaptation.charmChart.guides.getSpec('color').type)
  }

  @Test
  void testChartWithGuideColorstepsDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(color: guide_colorsteps())

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with guide_colorsteps should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.COLORSTEPS, adaptation.charmChart.guides.getSpec('color').type)
  }

  @Test
  void testChartWithCustomGuideDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(custom: guide_custom({ ctx -> }))

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with guide_custom should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.CUSTOM, adaptation.charmChart.guides.getSpec('custom').type)
  }

  @Test
  void testChartWithAxisLogTicksDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks())

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with axis_logticks should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.AXIS_LOGTICKS, adaptation.charmChart.guides.getSpec('x').type)
  }

  @Test
  void testChartWithAxisStackDelegates() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: 45)))

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with axis_stack should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.AXIS_STACK, adaptation.charmChart.guides.getSpec('x').type)
  }

  @Test
  void testChartWithGuideRenders() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(color: guide_legend())

    Svg svg = chart.render()
    assertNotNull(svg, 'Rendered SVG should not be null')
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'), 'Output should be valid SVG')
  }

  @Test
  void testChartWithMultipleGuideTypes() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        guides(color: guide_legend(), fill: guide_none())

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with multiple guides should delegate. Reasons: ${adaptation.reasons}")
    assertEquals(GuideType.LEGEND, adaptation.charmChart.guides.getSpec('color').type)
    assertEquals(GuideType.NONE, adaptation.charmChart.guides.getSpec('fill').type)
  }

  @Test
  void testScaleGuideParamsMapped() {
    GgChart chart = ggplot(sampleData(), aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_continuous(guide: 'axis')

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with scale guide param should delegate. Reasons: ${adaptation.reasons}")
    assertNotNull(adaptation.charmChart.guides.getSpec('x'), 'X guide should be set from scale')
    assertEquals(GuideType.AXIS, adaptation.charmChart.guides.getSpec('x').type)
  }

  @Test
  void testNoGuidesFallbackRemoved() {
    // Verify that charts with guides are no longer rejected
    // by the adapter. Even charts with complex guides should delegate.
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4]])
        .types(Integer, Integer)
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        guides(custom: guide_custom({ ctx ->
          ctx.svg.addCircle().cx(10).cy(10).r(5).fill('red')
        }))

    GgCharmAdaptation adaptation = adapter.adapt(chart)
    assertTrue(adaptation.delegated, "Chart with custom guide should no longer fall back. Reasons: ${adaptation.reasons}")
  }
}
