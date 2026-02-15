package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Geom
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.gg.GgPlot.geom_histogram
import static se.alipsa.matrix.gg.GgPlot.geom_line
import static se.alipsa.matrix.gg.GgPlot.geom_point
import static se.alipsa.matrix.gg.GgPlot.ggplot
import static se.alipsa.matrix.gg.GgPlot.labs

class CharmRendererFixtureParityTest {

  @Test
  void testPointFixtureStructureMatchesGg() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 3, 'A'],
            [2, 5, 'A'],
            [3, 4, 'B'],
            [4, 7, 'B']
        ])
        .build()

    Chart charmChart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      labels {
        title = 'Point Fixture'
      }
      theme {
        legend { position = 'none' }
      }
    }.build()

    GgChart ggChart = ggplot(data, se.alipsa.matrix.gg.GgPlot.aes(x: 'x', y: 'y')) +
        geom_point() +
        labs(title: 'Point Fixture')

    Map<String, Integer> charmCounts = primitiveCounts(charmChart.render())
    Map<String, Integer> ggCounts = primitiveCounts(ggChart.render())

    assertEquals(data.rowCount(), charmCounts.circle)
    assertEquals(data.rowCount(), ggCounts.circle)
    assertTrue(charmCounts.line > 0)
    assertTrue(ggCounts.line > 0)
  }

  @Test
  void testLineFixtureStructureMatchesGg() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 4],
            [3, 3],
            [4, 6],
            [5, 7]
        ])
        .build()

    Chart charmChart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      line {}
      theme {
        legend { position = 'none' }
      }
    }.build()

    GgChart ggChart = ggplot(data, se.alipsa.matrix.gg.GgPlot.aes(x: 'x', y: 'y')) +
        geom_line()

    Map<String, Integer> charmCounts = primitiveCounts(charmChart.render())
    Map<String, Integer> ggCounts = primitiveCounts(ggChart.render())

    assertEquals(0, charmCounts.circle)
    assertEquals(0, ggCounts.circle)
    assertTrue(charmCounts.line > 0)
    assertTrue(ggCounts.line > 0)
  }

  @Test
  void testHistogramFixtureStructureMatchesGg() {
    Matrix data = Matrix.builder()
        .columnNames('x')
        .rows([
            [1], [2], [2], [3], [3], [3], [4], [4], [5], [6], [7], [8]
        ])
        .build()

    Chart charmChart = plot(data) {
      aes {
        x = col.x
      }
      layer(Geom.HISTOGRAM, [bins: 5])
      theme {
        legend { position = 'none' }
      }
    }.build()

    GgChart ggChart = ggplot(data, se.alipsa.matrix.gg.GgPlot.aes(x: 'x')) +
        geom_histogram(bins: 5)

    Map<String, Integer> charmCounts = primitiveCounts(charmChart.render())
    Map<String, Integer> ggCounts = primitiveCounts(ggChart.render())

    assertEquals(0, charmCounts.circle)
    assertEquals(0, ggCounts.circle)
    assertTrue(charmCounts.rect >= 8)
    assertTrue(ggCounts.rect >= 5)
    // Charm and gg include different non-data scaffolding rects (panel/canvas/background),
    // but histogram structure should still stay in the same order of magnitude.
    assertTrue(Math.abs(charmCounts.rect - ggCounts.rect) <= 10)
  }

  private static Map<String, Integer> primitiveCounts(Svg svg) {
    List elements = svg.descendants()
    [
        circle: elements.count { it instanceof Circle } as int,
        line  : elements.count { it instanceof Line } as int,
        rect  : elements.count { it instanceof Rect } as int
    ]
  }
}
