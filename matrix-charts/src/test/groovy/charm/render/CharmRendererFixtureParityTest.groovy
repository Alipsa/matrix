package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.gg.GgPlot.geom_boxplot
import static se.alipsa.matrix.gg.GgPlot.geom_col
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

    Map<String, Integer> charmCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderCharm(charmChart))
    Map<String, Integer> ggCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderGg(ggChart))

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

    Map<String, Integer> charmCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderCharm(charmChart))
    Map<String, Integer> ggCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderGg(ggChart))

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
      layer(CharmGeomType.HISTOGRAM, [bins: 5])
      theme {
        legend { position = 'none' }
      }
    }.build()

    GgChart ggChart = ggplot(data, se.alipsa.matrix.gg.GgPlot.aes(x: 'x')) +
        geom_histogram(bins: 5)

    Map<String, Integer> charmCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderCharm(charmChart))
    Map<String, Integer> ggCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderGg(ggChart))

    assertEquals(0, charmCounts.circle)
    assertEquals(0, ggCounts.circle)
    assertTrue(charmCounts.rect >= 8)
    assertTrue(ggCounts.rect >= 5)
    // Charm and gg include different non-data scaffolding rects (panel/canvas/background),
    // but histogram structure should still stay in the same order of magnitude.
    assertTrue(Math.abs(charmCounts.rect - ggCounts.rect) <= 10)
  }

  @Test
  void testBarFixtureStructureMatchesGg() {
    Matrix data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10],
            ['B', 20],
            ['C', 15],
            ['D', 25]
        ])
        .types(String, Integer)
        .build()

    Chart charmChart = plot(data) {
      aes {
        x = col.category
        y = col.value
      }
      layer(CharmGeomType.COL, [:])
      theme {
        legend { position = 'none' }
      }
    }.build()

    GgChart ggChart = ggplot(data, se.alipsa.matrix.gg.GgPlot.aes(x: 'category', y: 'value')) +
        geom_col()

    Map<String, Integer> charmCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderCharm(charmChart))
    Map<String, Integer> ggCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderGg(ggChart))

    // Both should produce rectangles for bars; counts should match within scaffolding tolerance
    assertTrue(charmCounts.rect >= data.rowCount(), "Charm should render at least ${data.rowCount()} rects")
    assertTrue(ggCounts.rect >= data.rowCount(), "Gg should render at least ${data.rowCount()} rects")
    assertTrue(Math.abs(charmCounts.rect - ggCounts.rect) <= 10,
        "Charm rect count (${charmCounts.rect}) and gg rect count (${ggCounts.rect}) should be within tolerance")
  }

  @Test
  void testBoxplotFixtureStructureMatchesGg() {
    Matrix data = Matrix.builder()
        .columnNames('group', 'value')
        .rows([
            ['A', 10], ['A', 12], ['A', 14], ['A', 15], ['A', 13],
            ['B', 20], ['B', 22], ['B', 25], ['B', 28], ['B', 23]
        ])
        .types(String, Integer)
        .build()

    Chart charmChart = plot(data) {
      aes {
        x = col.group
        y = col.value
      }
      layer(CharmGeomType.BOXPLOT, [:])
      theme {
        legend { position = 'none' }
      }
    }.build()

    GgChart ggChart = ggplot(data, se.alipsa.matrix.gg.GgPlot.aes(x: 'group', y: 'value')) +
        geom_boxplot()

    Map<String, Integer> charmCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderCharm(charmChart))
    Map<String, Integer> ggCounts = CharmRenderTestUtil.primitiveCounts(CharmRenderTestUtil.renderGg(ggChart))

    // Both should produce rectangles for box bodies and lines for whiskers/medians
    assertTrue(charmCounts.rect > 0, "Charm should render rects for box bodies")
    assertTrue(ggCounts.rect > 0, "Gg should render rects for box bodies")
    assertTrue(charmCounts.line > 0, "Charm should render lines for whiskers/medians")
    assertTrue(ggCounts.line > 0, "Gg should render lines for whiskers/medians")
    // Rect counts should be comparable between renderers
    assertTrue(Math.abs(charmCounts.rect - ggCounts.rect) <= 10,
        "Charm rect count (${charmCounts.rect}) and gg rect count (${ggCounts.rect}) should be within tolerance")
    // Line counts differ more due to axis ticks, grid lines, and whisker rendering differences,
    // so we use a wider tolerance but still verify they are in the same order of magnitude
    assertTrue(Math.abs(charmCounts.line - ggCounts.line) <= 40,
        "Charm line count (${charmCounts.line}) and gg line count (${ggCounts.line}) should be within tolerance")
  }

}
