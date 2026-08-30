package se.alipsa.matrix.jupyter.render

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.geom_point
import static se.alipsa.matrix.gg.GgPlot.ggplot

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions

/** Tests SVG rendering for ggplot charts. */
class GgRendererTest {
  @Test
  void rendersSvgWithoutMutatingChartDimensions() {
    Matrix data = Matrix.builder().columns(x: [1, 2], y: [2, 4]).build()
    GgChart chart = ggplot(data, aes('x', 'y')) + geom_point()
    chart.width = 640
    chart.height = 480

    MimeBundle bundle = new GgRenderer().render(chart, new RenderOptions(50, 50, true, [:], 1024, 768))

    assertTrue(bundle['image/svg+xml'].contains('<svg'))
    assertEquals(640, chart.width)
    assertEquals(480, chart.height)
  }
}
