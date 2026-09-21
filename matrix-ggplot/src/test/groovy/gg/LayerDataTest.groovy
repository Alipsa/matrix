package gg

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.geom_histogram
import static se.alipsa.matrix.gg.GgPlot.ggplot
import static se.alipsa.matrix.gg.GgPlot.ggplot_build
import static se.alipsa.matrix.gg.GgPlot.layer_data

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

class LayerDataTest {

  private static final Matrix DATA = Matrix.builder().data(v: [1, 2, 2, 3, 3, 3]).build()

  @Test
  void layerDataReturnsPostStatHistogramRows() {
    GgChart chart = ggplot(DATA, aes(x: 'v')) + geom_histogram(bins: 3)

    Matrix built = layer_data(chart)

    assertEquals(3, built.rowCount())
    assertTrue(built.columnNames().containsAll(['PANEL', 'x', 'y', 'xmin', 'xmax']))
    assertEquals([1, 1, 1], built['PANEL'] as List)
    assertEquals(6G, (built['y'] as List<BigDecimal>).sum() as BigDecimal)
    assertEquals(['v'], DATA.columnNames())
  }

  @Test
  void buildIsLayerOrderedAndRejectsInvalidIndexes() {
    GgChart chart = ggplot(DATA, aes(x: 'v')) + geom_histogram(bins: 2)

    assertEquals(1, ggplot_build(chart).size())
    assertThrows(IllegalArgumentException) { layer_data(chart, 0) }
    assertThrows(IllegalArgumentException) { layer_data(chart, 2) }
  }
}
