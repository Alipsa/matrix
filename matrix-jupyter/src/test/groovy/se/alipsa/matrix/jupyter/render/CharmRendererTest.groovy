package se.alipsa.matrix.jupyter.render

import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions

/** Tests Charm SVG rendering and per-bundle namespace isolation. */
class CharmRendererTest {
  @Test
  void namespacesRootIdsAndBuiltInAnimationSelectorsForEachBundle() {
    Matrix data = Matrix.builder().columns(x: [1, 2], y: [2, 4]).build()
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
      animation { selector = '.charm-point' }
    }.build()
    CharmRenderer renderer = new CharmRenderer()

    MimeBundle first = renderer.render(chart, new RenderOptions())
    MimeBundle second = renderer.render(chart, new RenderOptions())
    String firstSvg = first['image/svg+xml']
    String secondSvg = second['image/svg+xml']

    assertTrue(firstSvg.contains('id="mjx'))
    assertTrue(firstSvg.contains('-charm-root"'))
    assertTrue(firstSvg.contains('#mjx'))
    assertNotEquals(firstSvg, secondSvg)
  }
}
