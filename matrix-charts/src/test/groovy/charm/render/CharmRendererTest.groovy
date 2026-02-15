package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.Geom
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
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
      aes {
        x = col.x
        y = col.y
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
      aes {
        x = col.x
        y = col.y
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
      aes {
        x = col.cat
        y = col.value
      }
      layer(Geom.COL, [fill: '#336699'])
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg barSvg = barChart.render()
    Map<String, Integer> barCounts = primitiveCounts(barSvg)
    assertTrue(barCounts.rect >= 2)

    Chart histogram = plot(barData) {
      aes {
        x = col.value
      }
      layer(Geom.HISTOGRAM, [bins: 4, fill: '#cc6677'])
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
      aes {
        x = col.x
        y = col.y
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
      aes {
        x = col.x
        y = col.y
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
    assertEquals('x', chart.aes.x.columnName())
    assertEquals(1, chart.layers.size())
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
