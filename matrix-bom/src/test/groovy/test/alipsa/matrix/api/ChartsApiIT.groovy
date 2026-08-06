package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPdf
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.chartexport.ChartToSvg
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.ExportFormat

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers the documented Charm DSL, grid, facets, styles, and native exporters. */
@Tag('charts')
class ChartsApiIT implements ApiItSupport {

  @TempDir
  Path tempDir

  @Test
  void charmDslBuildsFacetedStyledCharts() {
    def spec = Charts.plot(mtcars()) {
      mapping { x = 'wt'; y = 'mpg'; color = 'cyl' }
      layers { geomPoint(); geomSmooth() }
      facet { wrap { vars = ['cyl']; ncol = 2 } }
      scale { x = Scale.continuous(); y = Scale.continuous() }
      labels { title = 'Charm API' }
      stylesheet('.charm-point { stroke-width: 2; }')
    }
    Chart chart = spec.build()
    assertNotNull(chart)
    assertTrue(chart.render().descendants().size() > 0)
    assertTrue(chart.stylesheet.contains('stroke-width'))
  }

  @Test
  void chartsAndPlotGridRender() {
    Chart first = Charts.plot(mtcars()) { mapping { x = 'wt'; y = 'mpg' }; layers { geomPoint() } }.build()
    Chart second = Charts.chart(mtcars()) { mapping { x = 'hp'; y = 'mpg' }; layers { geomLine() } }.build()
    PlotGrid grid = Charts.plotGrid([first, second], 2)
    assertEquals(2, grid.charts.size())
    assertTrue(grid.render(800, 500).descendants().size() > 0)
    assertThrows(IllegalArgumentException) { new PlotGrid([], 1) }
  }

  @Test
  void nativeExportersAndFormatValidation(@TempDir Path directory) {
    Chart chart = Charts.plot(mtcars()) { mapping { x = 'wt'; y = 'mpg' }; layers { geomPoint() } }.build()
    File svg = directory.resolve('chart.svg').toFile()
    File png = directory.resolve('chart.png').toFile()
    File jpeg = directory.resolve('chart.jpg').toFile()
    File pdf = directory.resolve('chart.pdf').toFile()
    ChartToSvg.export(chart, svg)
    ChartToPng.export(chart, png)
    ChartToJpeg.export(chart, jpeg)
    ChartToPdf.export(chart, pdf)
    assertTrue([svg, png, jpeg, pdf].every { it.isFile() && it.length() > 0 })
    assertNotNull(ChartToImage.export(chart))
    assertNotNull(ChartToSwing.export(chart))
    assertThrows(IllegalArgumentException) { ExportFormat.fromExtension('txt') }
  }
}
