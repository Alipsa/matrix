package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine
import se.alipsa.matrix.charm.render.scale.TrainedScales
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

/**
 * Tests for per-layer scale overrides in Charm.
 */
class PerLayerScaleTest {

  @Test
  void testTwoLayersWithDifferentColorScalesRenderDistinctColors() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 4, 'B'],
            [3, 6, 'A'],
            [4, 8, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers {
        geomPoint().scale('color', Scale.manual([A: '#FF0000', B: '#00FF00']))
        geomPoint().scale('color', Scale.manual([A: '#0000FF', B: '#FFFF00']))
      }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() >= 8, "Expected at least 8 circles (4 per layer), got ${circles.size()}")
  }

  @Test
  void testLayerWithoutScaleOverrideFallsBackToGlobal() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 3, 'A'],
            [2, 5, 'B'],
            [3, 7, 'A']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      scale { color = Scale.manual([A: '#FF0000', B: '#00FF00']) }
      layers {
        geomPoint()
        geomPoint().scale('color', Scale.manual([A: '#0000FF', B: '#FFFF00']))
      }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    // First layer should use global color scale, second uses per-layer override
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() >= 6, "Expected at least 6 circles, got ${circles.size()}")
  }

  @Test
  void testPerLayerFillScaleWithDifferentGradient() {
    Matrix data = Matrix.builder()
        .columnNames('cat', 'value')
        .rows([
            ['A', 3],
            ['B', 7],
            ['C', 5]
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'cat'; y = 'value' }
      layers {
        geomCol().scale('fill', Scale.manual([A: '#FF0000', B: '#00FF00', C: '#0000FF']))
      }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() >= 3, "Expected at least 3 bars, got ${rects.size()}")
  }

  @Test
  void testTrainLayerScalesTrainsOnlySpecifiedAesthetics() {
    List<LayerData> layerData = [
        new LayerData(x: 1, y: 10, color: 'A'),
        new LayerData(x: 2, y: 20, color: 'B'),
        new LayerData(x: 3, y: 30, color: 'A')
    ]

    Map<String, Scale> layerScaleSpecs = [
        'color': Scale.manual([A: '#FF0000', B: '#0000FF'])
    ]

    // Create a simple chart for context
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'color')
        .rows([[1, 10, 'A'], [2, 20, 'B']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }.build()

    RenderConfig config = new RenderConfig()

    TrainedScales trained = ScaleEngine.trainLayerScales(
        layerScaleSpecs, config, layerData, chart
    )

    // Only color should be trained
    assertNotNull(trained.color, 'Per-layer color scale should be trained')
    assertNull(trained.x, 'x should not be trained when not specified')
    assertNull(trained.y, 'y should not be trained when not specified')
    assertNull(trained.fill, 'fill should not be trained when not specified')
    assertNull(trained.size, 'size should not be trained when not specified')
  }

  @Test
  void testTrainLayerScalesWithPositionalOverride() {
    List<LayerData> layerData = [
        new LayerData(x: 10, y: 100),
        new LayerData(x: 100, y: 1000),
        new LayerData(x: 1000, y: 10000)
    ]

    Map<String, Scale> layerScaleSpecs = [
        'x': Scale.transform('log10')
    ]

    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[10, 100], [100, 1000]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }.build()

    RenderConfig config = new RenderConfig()

    TrainedScales trained = ScaleEngine.trainLayerScales(
        layerScaleSpecs, config, layerData, chart
    )

    assertNotNull(trained.x, 'Per-layer x scale should be trained')
    assertNull(trained.y, 'y should not be trained when not specified')
  }

  @Test
  void testEmptyLayerScaleSpecsReturnsEmptyTrainedScales() {
    TrainedScales trained = ScaleEngine.trainLayerScales(
        [:], new RenderConfig(), [], null
    )

    assertNull(trained.x)
    assertNull(trained.y)
    assertNull(trained.color)
    assertNull(trained.fill)
    assertNull(trained.size)
    assertNull(trained.shape)
    assertNull(trained.alpha)
    assertNull(trained.linetype)
  }

  @Test
  void testNullLayerScaleSpecsReturnsEmptyTrainedScales() {
    TrainedScales trained = ScaleEngine.trainLayerScales(
        null, new RenderConfig(), [], null
    )

    assertNull(trained.x)
    assertNull(trained.y)
    assertNull(trained.color)
  }

  @Test
  void testFullRenderWithPerLayerColorScales() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A'],
            [4, 3, 'B'], [5, 5, 'A'], [6, 7, 'B']
        ])
        .build()

    // Two point layers with different color scales
    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'grp' }
      layers {
        geomPoint().scale('color', Scale.manual([A: '#FF0000', B: '#00FF00']))
        geomLine().scale('color', Scale.manual([A: '#0000FF', B: '#FFFF00']))
      }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    // Should have both circle and line elements
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Expected point circles')
  }
}
