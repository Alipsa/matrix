package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.Plot

import java.nio.file.Path

/** Exercises SVG export and Groovy method discovery without the optional JavaFX runtime. */
class PlotWithoutJavafxTest {

  @Test
  void svgExportsWithoutJavafx(@TempDir Path dir) {
    assertThrows(ClassNotFoundException) { getClass().classLoader.loadClass('javafx.scene.Group') }
    Matrix data = Matrix.builder().columns([x: [1, 2, 3], y: [2, 4, 3]])
        .types([Integer, Integer]).build()
    def chart = LineChart.builder(data).title('No JavaFX').x('x').y('y').build()

    Svg svg = Plot.svg(chart)
    assertEquals('800', svg.width.toString())
    assertEquals('600', svg.height.toString())
    assertFalse(svg.descendants().isEmpty())

    StringWriter writer = new StringWriter()
    Plot.svg(chart, writer, 400, 300)
    assertTrue(writer.toString().contains('<svg'))
    assertTrue(writer.toString().contains('No JavaFX'))

    ByteArrayOutputStream stream = new ByteArrayOutputStream()
    Plot.svg(chart, stream)
    assertTrue(stream.toString('UTF-8').contains('<svg'))

    File file = dir.resolve('chart.svg').toFile()
    Plot.svg(chart, file)
    assertTrue(file.getText('UTF-8').contains('<svg'))
  }
}
