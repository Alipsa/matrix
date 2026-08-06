package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.pict.BarChart
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.Plot
import se.alipsa.matrix.pict.ScatterChart

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers chart-type-first Pict builders, styles, and file exporters. */
@Tag('pict')
class PictApiIT implements ApiItSupport {

  @Test
  void chartBuildersAndExporters(@TempDir Path directory) {
    def data = mtcars()
    def scatter = ScatterChart.builder(data).title('scatter').x('wt').y('mpg').css('.point { opacity: .8; }').build()
    def line = LineChart.builder(data).title('line').x('wt').y('mpg').build()
    def bar = BarChart.builder(data).title('bar').x('cyl').y('mpg').build()
    File svg = directory.resolve('pict.svg').toFile()
    File png = directory.resolve('pict.png').toFile()
    Plot.svg(scatter, svg)
    Plot.png(scatter, png)
    assertTrue(svg.isFile() && svg.length() > 0 && png.isFile() && png.length() > 0)
  }
}
