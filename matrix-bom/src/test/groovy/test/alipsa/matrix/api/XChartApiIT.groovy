package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.BarChart
import se.alipsa.matrix.xchart.LineChart
import se.alipsa.matrix.xchart.PieChart

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers the XChart builders and their image export formats. */
@Tag('xchart')
class XChartApiIT implements ApiItSupport {

  @Test
  void chartBuildersExportAllDocumentedFormats(@TempDir Path directory) {
    Matrix data = Matrix.builder([category: [1, 2, 3], value: [1, 2, 3]], [Integer, Integer], 'xchart').build()
    def pie = PieChart.create(data).addSeries(data.category, data.value)
    def bar = BarChart.create(data).addSeries(data.category, data.value)
    def line = LineChart.create(data).addSeries(data.category, data.value)
    File pieFile = directory.resolve('pie.png').toFile()
    File barFile = directory.resolve('bar.png').toFile()
    File lineFile = directory.resolve('line.png').toFile()
    File svgFile = directory.resolve('line.svg').toFile()
    File pdfFile = directory.resolve('line.pdf').toFile()
    pie.exportPng(pieFile)
    bar.exportPng(barFile)
    line.exportPng(lineFile)
    line.exportSvg(svgFile)
    line.exportPdf(pdfFile)
    assertTrue([pieFile, barFile, lineFile, svgFile, pdfFile].every { it.isFile() && it.length() > 0 })
    assertTrue(Files.readString(svgFile.toPath()).contains('<svg'))
    assertTrue(new String(Files.readAllBytes(pdfFile.toPath()), StandardCharsets.US_ASCII).startsWith('%PDF-'))
  }
}
