package gg.adapter

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Geom
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.adapter.GgCharmAdaptation
import se.alipsa.matrix.gg.adapter.GgCharmAdapter
import se.alipsa.matrix.gg.adapter.GgCharmMappingRegistry
import se.alipsa.matrix.gg.scale.ScaleXLog10

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.geom_bar
import static se.alipsa.matrix.gg.GgPlot.geom_col
import static se.alipsa.matrix.gg.GgPlot.geom_histogram
import static se.alipsa.matrix.gg.GgPlot.geom_line
import static se.alipsa.matrix.gg.GgPlot.geom_point
import static se.alipsa.matrix.gg.GgPlot.geom_smooth
import static se.alipsa.matrix.gg.GgPlot.ggplot
import static se.alipsa.matrix.gg.GgPlot.theme_minimal

class GgCharmAdapterTest {

  @Test
  void testRegistryMapsCanonicalTargets() {
    GgCharmMappingRegistry registry = new GgCharmMappingRegistry()

    assertEquals(Geom.LINE, registry.mapGeom(new se.alipsa.matrix.gg.geom.GeomLine()))
    assertEquals(Scale.transform('log10').transform, registry.mapScale(new ScaleXLog10(), 'x')?.transform)
    assertEquals('color', GgCharmMappingRegistry.normalizeAesthetic('colour'))
  }

  @Test
  void testAdapterDelegatesSimplePointChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    GgChart ggChart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 3, color: '#336699')

    GgCharmAdapter adapter = new GgCharmAdapter()
    GgCharmAdaptation adaptation = adapter.adapt(ggChart)

    assertTrue(adaptation.delegated)
    assertNotNull(adaptation.charmChart)
    assertEquals(1, adaptation.charmChart.layers.size())
    assertEquals(Geom.POINT, adaptation.charmChart.layers.first().geom)

    Svg svg = ggChart.render()
    assertNotNull(svg)
    int circles = svg.descendants().count { it instanceof Circle } as int
    assertEquals(data.rowCount(), circles)
  }

  @Test
  void testAdapterDelegatesLineSmoothHistogramAndCol() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    GgCharmAdapter adapter = new GgCharmAdapter()

    GgChart lineChart = ggplot(data, aes(x: 'x', y: 'y')) + geom_line(size: 2, color: '#224488')
    GgCharmAdaptation lineAdaptation = adapter.adapt(lineChart)
    assertTrue(lineAdaptation.delegated)
    assertEquals(Geom.LINE, lineAdaptation.charmChart.layers.first().geom)
    assertEquals(2, lineAdaptation.charmChart.layers.first().params['lineWidth'])

    GgChart smoothChart = ggplot(data, aes(x: 'x', y: 'y')) + geom_smooth(method: 'lm', linewidth: 3)
    GgCharmAdaptation smoothAdaptation = adapter.adapt(smoothChart)
    assertTrue(smoothAdaptation.delegated)
    assertEquals(Geom.SMOOTH, smoothAdaptation.charmChart.layers.first().geom)
    assertEquals('lm', smoothAdaptation.charmChart.layers.first().params['method'])
    assertEquals(3, smoothAdaptation.charmChart.layers.first().params['lineWidth'])

    Matrix histogramData = Matrix.builder()
        .columnNames('x')
        .rows([[1], [1], [2], [2], [3], [4], [5]])
        .build()
    GgChart histogramChart = ggplot(histogramData, aes(x: 'x')) + geom_histogram(bins: 5, fill: '#777777')
    GgCharmAdaptation histogramAdaptation = adapter.adapt(histogramChart)
    assertTrue(histogramAdaptation.delegated)
    assertEquals(Geom.HISTOGRAM, histogramAdaptation.charmChart.layers.first().geom)
    assertEquals(5, histogramAdaptation.charmChart.layers.first().params['bins'])

    GgChart colChart = ggplot(data, aes(x: 'x', y: 'y')) + geom_col(width: 0.6, fill: '#55aa55')
    GgCharmAdaptation colAdaptation = adapter.adapt(colChart)
    assertTrue(colAdaptation.delegated)
    assertEquals(Geom.COL, colAdaptation.charmChart.layers.first().geom)
    assertEquals(0.6, colAdaptation.charmChart.layers.first().params['barWidth'])
  }

  @Test
  void testAdapterFallsBackForNonDefaultThemeOrUnsupportedStatOrMethod() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    GgCharmAdapter adapter = new GgCharmAdapter()

    GgChart themed = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        theme_minimal()
    GgCharmAdaptation themedAdaptation = adapter.adapt(themed)
    assertFalse(themedAdaptation.delegated)
    assertTrue(themedAdaptation.reasons.any { it.contains('theme') })

    GgChart barChart = ggplot(data, aes(x: 'x')) + geom_bar()
    GgCharmAdaptation barAdaptation = adapter.adapt(barChart)
    assertFalse(barAdaptation.delegated)
    assertTrue(barAdaptation.reasons.any { it.contains("stat 'COUNT'") })

    GgChart smoothLoessChart = ggplot(data, aes(x: 'x', y: 'y')) + geom_smooth(method: 'loess')
    GgCharmAdaptation smoothLoessAdaptation = adapter.adapt(smoothLoessChart)
    assertFalse(smoothLoessAdaptation.delegated)
    assertTrue(smoothLoessAdaptation.reasons.any { it.contains('smooth method') })
  }

  @Test
  void testEquivalentPointSemanticsProduceSameCharmModelAsNativeDsl() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    GgChart ggChart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 2, fill: '#55aa55')

    GgCharmAdaptation adaptation = new GgCharmAdapter().adapt(ggChart)
    assertTrue(adaptation.delegated)

    def nativeSpec = plot(data)
    nativeSpec.aes([x: 'x', y: 'y'])
    nativeSpec.layer(Geom.POINT, [size: 2, fill: '#55aa55'])
    Chart nativeChart = nativeSpec.build()

    assertEquals(nativeChart.aes.x.columnName(), adaptation.charmChart.aes.x.columnName())
    assertEquals(nativeChart.aes.y.columnName(), adaptation.charmChart.aes.y.columnName())
    assertEquals(nativeChart.layers.first().geom, adaptation.charmChart.layers.first().geom)
    assertEquals(nativeChart.layers.first().stat, adaptation.charmChart.layers.first().stat)
    assertEquals(nativeChart.layers.first().params['size'], adaptation.charmChart.layers.first().params['size'])
    assertEquals(nativeChart.layers.first().params['fill'], adaptation.charmChart.layers.first().params['fill'])
  }
}
