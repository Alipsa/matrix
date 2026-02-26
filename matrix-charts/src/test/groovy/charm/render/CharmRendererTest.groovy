package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class CharmRendererTest {

  @Test
  void testPointLayerRendersCirclesAxesAndClipPath() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 3],
            [2, 5],
            [3, 4],
            [4, 6]
        ])
        .build()

    Chart chart = plot(data) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    Map<String, Integer> counts = primitiveCounts(svg)

    assertEquals(data.rowCount(), counts.circle)
    assertTrue(counts.line > 0, 'Expected axis/grid line elements')
    assertEquals(1, counts.clipPath)
  }

  @Test
  void testLineLayerRendersLineSegments() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 3],
            [3, 5],
            [4, 4],
            [5, 7]
        ])
        .build()

    Chart chart = plot(data) {
      mapping {
        x = 'x'
        y = 'y'
      }
      line {}
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    Map<String, Integer> counts = primitiveCounts(svg)

    assertEquals(0, counts.circle)
    assertTrue(counts.line >= data.rowCount() - 1, 'Expected at least one segment per adjacent pair')
  }

  @Test
  void testBarAndHistogramRenderRectangles() {
    Matrix barData = Matrix.builder()
        .columnNames('cat', 'value')
        .rows([
            ['A', 3],
            ['B', 7],
            ['C', 5]
        ])
        .build()

    Chart barChart = plot(barData) {
      mapping {
        x = 'cat'
        y = 'value'
      }
      layer(CharmGeomType.COL, [fill: '#336699'])
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg barSvg = barChart.render()
    Map<String, Integer> barCounts = primitiveCounts(barSvg)
    assertTrue(barCounts.rect >= 2)

    Chart histogram = plot(barData) {
      mapping {
        x = 'value'
      }
      layer(CharmGeomType.HISTOGRAM, [bins: 4, fill: '#cc6677'])
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg histSvg = histogram.render()
    Map<String, Integer> histCounts = primitiveCounts(histSvg)
    assertTrue(histCounts.rect >= 5)
    assertEquals(0, histCounts.circle)
  }

  @Test
  void testFacetWrapCreatesPanelScopedClipPaths() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'],
            [2, 4, 'A'],
            [1, 3, 'B'],
            [2, 6, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      facet {
        wrap {
          vars = ['grp']
          ncol = 2
        }
      }
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    Map<String, Integer> counts = primitiveCounts(svg)

    assertEquals(2, counts.clipPath)
  }

  @Test
  void testFacetWrapWithEmptyDataStillRendersSinglePanel() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([])
        .build()

    Chart chart = plot(data) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      facet {
        wrap {
          vars = ['grp']
          ncol = 2
        }
      }
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    Map<String, Integer> counts = primitiveCounts(svg)

    assertEquals(0, counts.circle)
    assertEquals(1, counts.clipPath)
  }

  @Test
  void testFacetGridWithEmptyDataStillRendersSinglePanel() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([])
        .build()

    Chart chart = plot(data) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      facet {
        rows = ['grp']
      }
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    Map<String, Integer> counts = primitiveCounts(svg)

    assertEquals(0, counts.circle)
    assertEquals(1, counts.clipPath)
  }

  @Test
  void testRenderConfigControlsCanvasSizeAndRenderIsDeterministic() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [2, 2],
            [3, 3]
        ])
        .build()

    Chart chart = Charts.plot(data) {
      mapping {
        x = 'x'
        y = 'y'
      }
      line {}
      labels {
        title = 'Deterministic'
      }
      theme {
        legend { position = 'none' }
      }
    }.build()

    CharmRenderer renderer = new CharmRenderer()
    RenderConfig config = new RenderConfig(width: 640, height: 420, marginRight: 40)
    Svg first = renderer.render(chart, config)
    Svg second = renderer.render(chart, config)

    assertEquals('640', first.width.toString())
    assertEquals('420', first.height.toString())
    assertEquals(primitiveCounts(first), primitiveCounts(second))
    assertEquals('x', chart.mapping.x.columnName())
    assertEquals(1, chart.layers.size())
  }

  @Test
  void testLayerSpecificDataProducesDifferentOutput() {
    Matrix chartData = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 30]
        ])
        .build()

    Matrix layerData = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [4, 40],
            [5, 50],
            [6, 60]
        ])
        .build()

    Chart chart = plot(chartData) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      layer(CharmGeomType.POINT, [__layer_data: layerData])
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    Map<String, Integer> counts = primitiveCounts(svg)

    assertEquals(chartData.rowCount() + layerData.rowCount(), counts.circle,
        'Expected circles from both chart-level and layer-specific datasets')

    // Verify that circles span distinct x positions from both datasets.
    // chartData x: [1,2,3] and layerData x: [4,5,6] should produce 6 distinct
    // cx values after scaling. If layer data were ignored, only 3 would appear.
    List<Circle> circles = svg.descendants().findAll { it instanceof Circle } as List<Circle>
    Set<String> distinctCx = circles.collect { it.cx } as Set
    assertTrue(distinctCx.size() > 3,
        "Expected more than 3 distinct cx positions when using layer-specific data, got ${distinctCx.size()}")

    Set<String> distinctCy = circles.collect { it.cy } as Set
    assertTrue(distinctCy.size() > 3,
        "Expected more than 3 distinct cy positions when using layer-specific data, got ${distinctCy.size()}")
  }

  @Test
  void testLayerSpecificDataAffectsScaleRange() {
    Matrix narrowData = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20]
        ])
        .build()

    Matrix wideData = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [100, 1000],
            [200, 2000]
        ])
        .build()

    // Baseline: two point layers both using the narrow chart data
    Chart baseline = plot(narrowData) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      points {}
      theme {
        legend { position = 'none' }
      }
    }.build()

    // Test chart: second layer uses wide data, which should shift scale domain
    Chart withLayerData = plot(narrowData) {
      mapping {
        x = 'x'
        y = 'y'
      }
      points {}
      layer(CharmGeomType.POINT, [__layer_data: wideData])
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg baselineSvg = baseline.render()
    Svg layerSvg = withLayerData.render()

    List<Circle> baselineCircles = baselineSvg.descendants().findAll { it instanceof Circle } as List<Circle>
    List<Circle> layerCircles = layerSvg.descendants().findAll { it instanceof Circle } as List<Circle>

    assertEquals(4, baselineCircles.size())
    assertEquals(4, layerCircles.size())

    // With layer-specific data (x up to 200), the baseline circles from narrowData (x 1-2)
    // should be compressed to the left of the panel compared to the baseline-only chart.
    Set<String> baselineCxSet = baselineCircles.collect { it.cx } as Set
    Set<String> layerCxSet = layerCircles.collect { it.cx } as Set
    assertNotEquals(baselineCxSet, layerCxSet,
        'Layer-specific data should change scale range, producing different circle positions')
  }

  private static Map<String, Integer> primitiveCounts(Svg svg) {
    List elements = svg.descendants()
    [
        circle  : elements.count { it instanceof Circle } as int,
        line    : elements.count { it instanceof Line } as int,
        rect    : elements.count { it instanceof Rect } as int,
        clipPath: elements.count { it.class.simpleName == 'ClipPath' } as int
    ]
  }
}
