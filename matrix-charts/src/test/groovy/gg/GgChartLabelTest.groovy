package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.GgPlot
import se.alipsa.matrix.gg.Label

import static org.junit.jupiter.api.Assertions.*

class GgChartLabelTest {

  @Test
  void testLabelSettersToggleFlags() {
    def label = new Label()
    label.x = 'X'
    label.y = 'Y'

    assertTrue(label.xSet)
    assertTrue(label.ySet)
  }

  @Test
  void testLabelMergeRespectsExplicitBlank() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2]])
        .build()
    GgChart chart = GgPlot.ggplot(data, GgPlot.aes('x', 'y'))

    def initial = new Label()
    initial.x = 'X'
    initial.y = 'Y'
    chart.plus(initial)

    def blank = new Label()
    blank.x = null
    chart.plus(blank)

    assertNull(chart.labels.x)
    assertEquals('Y', chart.labels.y)
  }

  @Test
  void testLabelMergeIgnoresUnsetAxes() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2]])
        .build()
    GgChart chart = GgPlot.ggplot(data, GgPlot.aes('x', 'y'))

    def initial = new Label()
    initial.x = 'X'
    initial.y = 'Y'
    chart.plus(initial)

    def update = new Label()
    update.title = 'Title'
    chart.plus(update)

    assertEquals('X', chart.labels.x)
    assertEquals('Y', chart.labels.y)
    assertEquals('Title', chart.labels.title)
  }
}
