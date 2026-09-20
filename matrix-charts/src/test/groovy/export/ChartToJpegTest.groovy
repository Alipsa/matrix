package export

import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToJpeg

class ChartToJpegTest {

  @Test
  void rejectsInvalidQuality() {
    Svg svg = new Svg(10, 10)
    ByteArrayOutputStream stream = new ByteArrayOutputStream()
    assertThrows(IllegalArgumentException) { ChartToJpeg.export(svg, stream, null) }
    assertThrows(IllegalArgumentException) { ChartToJpeg.export(svg, stream, -0.1) }
    assertThrows(IllegalArgumentException) { ChartToJpeg.export(svg, stream, 1.5) }
  }
}
