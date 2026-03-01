package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.bridge.GgCharmCompilation
import se.alipsa.matrix.gg.bridge.GgCharmCompiler

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.geom_point
import static se.alipsa.matrix.gg.GgPlot.geom_point_sampled
import static se.alipsa.matrix.gg.GgPlot.ggplot
import static se.alipsa.matrix.gg.GgPlot.stat_sample

class SampleStatBridgeTest {

  @Test
  void testGeomPointWithSampleStatParamMapsToCharmSample() {
    Matrix data = sampleData()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(stat: 'sample', n: 3, seed: 11, method: 'random')

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    assertEquals(CharmStatType.SAMPLE, adaptation.charmChart.layers.first().statType)
    assertEquals(3, adaptation.charmChart.layers.first().statSpec.params['n'])
    assertEquals(11, adaptation.charmChart.layers.first().statSpec.params['seed'])
    assertEquals('random', adaptation.charmChart.layers.first().statSpec.params['method'])
  }

  @Test
  void testGeomPointSampledConvenienceMapsToCharmSample() {
    Matrix data = sampleData()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point_sampled(n: 4, method: 'systematic')

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    assertEquals(CharmStatType.SAMPLE, adaptation.charmChart.layers.first().statType)
    assertEquals(4, adaptation.charmChart.layers.first().statSpec.params['n'])
    assertEquals('systematic', adaptation.charmChart.layers.first().statSpec.params['method'])
  }

  @Test
  void testStatSampleWrapperCreatesSampleLayer() {
    Matrix data = sampleData()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        stat_sample(n: 2, seed: 5, geom: 'point')

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    assertEquals(CharmStatType.SAMPLE, adaptation.charmChart.layers.first().statType)
    assertEquals(2, adaptation.charmChart.layers.first().statSpec.params['n'])
    assertEquals(5, adaptation.charmChart.layers.first().statSpec.params['seed'])
  }

  private static Matrix sampleData() {
    Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4], [4, 5], [5, 6]])
        .build()
  }
}
