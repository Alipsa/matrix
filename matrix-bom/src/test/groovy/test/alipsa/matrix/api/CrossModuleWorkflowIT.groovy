package test.alipsa.matrix.api

import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions
import se.alipsa.matrix.csv.CsvExporter
import se.alipsa.matrix.csv.CsvImporter
import se.alipsa.matrix.stats.regression.LinearRegression

import java.nio.charset.StandardCharsets

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

/** Exercises the documented import, transform, statistics, chart, and export workflow. */
class CrossModuleWorkflowIT implements ApiItSupport {

  @Test
  void importsTransformsModelsChartsAndExports() {
    Matrix data = mtcars().rename('mpg', 'milesPerGallon')
    Matrix transformed = data.top(12)
    def regression = new LinearRegression(transformed, 'wt', 'milesPerGallon')
    assertTrue(regression.getRsquared() > 0G, 'workflow regression should explain some variation')

    def spec = plot(transformed) {
      mapping { x = 'wt'; y = 'milesPerGallon' }
      layers { geomPoint() }
      labels { title = 'workflow' }
    }
    Chart chart = spec.build()
    Svg svg = chart.render()
    assertRenderedSvg(svg)

    StringWriter output = new StringWriter()
    CsvExporter.exportToCsv(transformed, CSVFormat.DEFAULT, output)
    def input = new ByteArrayInputStream(output.toString().getBytes(StandardCharsets.UTF_8))
    Matrix roundTrip = CsvImporter.importCsv(input, CSVFormat.DEFAULT, true)
    MatrixAssertions.assertContentMatches(transformed, roundTrip, transformed.diff(roundTrip))
  }
}
