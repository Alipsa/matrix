package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.CharmRenderException
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.core.Matrix

import java.nio.file.Files

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertSame
import static org.junit.jupiter.api.Assertions.assertThrows
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

  @Test
  void testAnimationRejectsCdataTerminatorInCssFields() {
    Matrix data = sampleData()
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
      animation {
        keyframes = 'from { opacity: 0; } ]]> to { opacity: 1; }'
      }
    }.build()

    assertThrows(CharmRenderException) {
      chart.render()
    }
  }

  @Test
  void testRasterStripPatternRemovesCdataAnimationStyle() {
    String svg = '''<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
  <style type="text/css"><![CDATA[/* charm-animation */
@keyframes pulse { from { opacity: 0; } to { opacity: 1; } }
.charm-point { animation-name: pulse; }
]]></style>
  <circle cx="5" cy="5" r="3" />
</svg>'''

    String pngSanitized = invokeStrip(ChartToPng, svg)
    String jpegSanitized = invokeStrip(ChartToJpeg, svg)

    assertFalse(pngSanitized.contains('charm-animation'))
    assertFalse(jpegSanitized.contains('charm-animation'))
    assertTrue(pngSanitized.contains('<circle'))
    assertTrue(jpegSanitized.contains('<circle'))
  }

  @Test
  void testRasterStripPatternFastPathWhenAnimationMarkerMissing() {
    String svg = '<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><circle cx="5" cy="5" r="3"/></svg>'

    String pngSanitized = invokeStrip(ChartToPng, svg)
    String jpegSanitized = invokeStrip(ChartToJpeg, svg)

    assertSame(svg, pngSanitized)
    assertSame(svg, jpegSanitized)
  }

  private static Matrix sampleData() {
    Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 6]])
        .build()
  }

  private static String invokeStrip(Class<?> clazz, String svg) {
    def method = clazz.getDeclaredMethod('stripAnimationCss', String)
    method.setAccessible(true)
    method.invoke(null, svg) as String
  }
}
