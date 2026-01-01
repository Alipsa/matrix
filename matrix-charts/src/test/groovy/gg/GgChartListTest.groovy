package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
import static se.alipsa.matrix.gg.GgPlot.*

class GgChartListTest {

  @Test
  void testPlusList() {
    def data = Matrix.builder()
        .data(x: [1, 2], y: [2, 3])
        .build()

    def chart = ggplot(data, aes('x', 'y')) + [geom_point(), geom_line()]

    assertEquals(2, chart.layers.size())
  }

  @Test
  void testPlusNestedList() {
    def data = Matrix.builder()
        .data(x: [1, 2], y: [2, 3])
        .build()

    def chart = ggplot(data, aes('x', 'y')) + [geom_point(), [geom_line(), geom_point()]]

    assertEquals(3, chart.layers.size())
  }
}
