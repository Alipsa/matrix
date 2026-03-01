package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.core.Matrix

import java.nio.file.Files

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class AnimationTest {

  @Test
  void testAnimationCssIsNotInjectedByDefault() {
    Matrix data = sampleData()
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertFalse(xml.contains('charm-animation'))
    assertFalse(xml.contains('@keyframes'))
  }

  @Test
  void testAnimationCssIsInjectedWhenConfigured() {
    Matrix data = sampleData()
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
      animation {
        selector = '.charm-point'
        name = 'pulse'
        duration = '2s'
        keyframes = 'from { opacity: 0.2; } to { opacity: 1; }'
      }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('/* charm-animation */'))
    assertTrue(xml.contains('@keyframes pulse'))
    assertTrue(xml.contains('.charm-point'))
    assertTrue(xml.contains('animation-duration: 2s'))
  }

  @Test
  void testAnimationDoesNotBreakRasterExport() {
    Matrix data = sampleData()
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
      animation {
        selector = '.charm-point'
        keyframes = 'from { opacity: 0; } to { opacity: 1; }'
      }
    }.build()

    File png = Files.createTempFile('charm-animation-', '.png').toFile()
    File jpeg = Files.createTempFile('charm-animation-', '.jpg').toFile()
    try {
      ChartToPng.export(chart, png)
      ChartToJpeg.export(chart, jpeg)
      assertTrue(png.exists() && png.length() > 0)
      assertTrue(jpeg.exists() && jpeg.length() > 0)
    } finally {
      png.delete()
      jpeg.delete()
    }
  }

  private static Matrix sampleData() {
    Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 6]])
        .build()
  }
}
