import static se.alipsa.matrix.charm.Charts.plot

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.jupyter.RenderOptions
import se.alipsa.matrix.jupyter.render.CharmRenderer
import se.alipsa.matrix.jupyter.render.CoreRenderer

assert GroovySystem.version.startsWith('6.')

Matrix data = Matrix.builder().columns(x: [1, 2], y: [2, 4]).build()
assert new CoreRenderer().render(data, new RenderOptions())['text/html'].contains('<table')

def chart = plot(data) {
  mapping { x = 'x'; y = 'y' }
  layers { geomPoint() }
}.build()
assert new CharmRenderer().render(chart, new RenderOptions())['image/svg+xml'].contains('<svg')
