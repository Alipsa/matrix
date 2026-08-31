package se.alipsa.matrix.jupyter.render

import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions
import se.alipsa.matrix.pict.ScatterChart

/** Tests SVG rendering for legacy Pict charts. */
class PictRendererTest {
  @Test
  void rendersSvgAtRequestedNotebookDimensions() {
    Matrix data = Matrix.builder().columns(x: [1, 2], y: [2, 4]).build()
    ScatterChart chart = ScatterChart.create('Points', data, 'x', 'y')

    MimeBundle bundle = new PictRenderer().render(chart, new RenderOptions(50, 50, true, [:], 640, 480))

    String svg = bundle['image/svg+xml']
    assertTrue(svg.contains('<svg'))
    assertTrue(svg.contains('width="640"'))
    assertTrue(svg.contains('height="480"'))
  }
}
