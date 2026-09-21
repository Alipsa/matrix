package chart

import static org.junit.jupiter.api.Assertions.*

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.chartexport.SvgPanel
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.Plot

import java.awt.image.BufferedImage
import java.nio.file.Path
import javafx.scene.Group

import javax.imageio.ImageIO

/** Sized export overloads must render at the requested pixel dimensions, not the 800x600 default. */
class PlotExportTest {

  private static final int WIDTH = 400
  private static final int HEIGHT = 300
  /** ChartToPdf writes pages at 72 pt per 96 px. */
  private static final float PDF_POINT_SCALE = 0.75f

  private static LineChart chart() {
    Matrix data = Matrix.builder().columns([x: [1, 2, 3], y: [2, 4, 3]]).types([Integer, Integer]).build()
    LineChart.builder(data).title('Sized').x('x').y('y').build()
  }

  private static BufferedImage decode(byte[] bytes) {
    ImageIO.read(new ByteArrayInputStream(bytes))
  }

  private static void assertSize(BufferedImage image, int width, int height) {
    assertEquals(width, image.width)
    assertEquals(height, image.height)
  }

  private static void assertPdfPage(byte[] bytes, int width, int height) {
    PDDocument doc = Loader.loadPDF(bytes)
    try {
      PDRectangle box = doc.getPage(0).mediaBox
      assertEquals(width * PDF_POINT_SCALE, box.width, 0.01f)
      assertEquals(height * PDF_POINT_SCALE, box.height, 0.01f)
    } finally {
      doc.close()
    }
  }

  @Test
  void jpgRendersAtRequestedSize(@TempDir Path dir) {
    File file = dir.resolve('c.jpg').toFile()
    Plot.jpg(chart(), file, WIDTH, HEIGHT)
    assertSize(ImageIO.read(file), WIDTH, HEIGHT)

    Plot.jpg(chart(), file, WIDTH, HEIGHT, 0.8)
    assertSize(ImageIO.read(file), WIDTH, HEIGHT)

    ByteArrayOutputStream out = new ByteArrayOutputStream()
    Plot.jpg(chart(), out, WIDTH, HEIGHT)
    assertSize(decode(out.toByteArray()), WIDTH, HEIGHT)

    ByteArrayOutputStream defaults = new ByteArrayOutputStream()
    Plot.jpg(chart(), defaults)
    assertSize(decode(defaults.toByteArray()), 800, 600)
  }

  @Test
  void pdfRendersAtRequestedSize(@TempDir Path dir) {
    File file = dir.resolve('c.pdf').toFile()
    Plot.pdf(chart(), file, WIDTH, HEIGHT)
    assertPdfPage(file.bytes, WIDTH, HEIGHT)

    ByteArrayOutputStream out = new ByteArrayOutputStream()
    Plot.pdf(chart(), out, WIDTH, HEIGHT)
    assertPdfPage(out.toByteArray(), WIDTH, HEIGHT)

    ByteArrayOutputStream defaults = new ByteArrayOutputStream()
    Plot.pdf(chart(), defaults)
    assertPdfPage(defaults.toByteArray(), 800, 600)
  }

  @Test
  void base64RendersAtRequestedSizeAndDefaults() {
    String sized = Plot.base64(chart(), WIDTH, HEIGHT)
    String defaults = Plot.base64(chart())
    assertTrue(sized.startsWith('data:image/png;base64,'))
    assertTrue(defaults.startsWith('data:image/png;base64,'))
    assertSize(decode(Base64.decoder.decode(sized.substring(sized.indexOf(',') + 1))), WIDTH, HEIGHT)
    assertSize(decode(Base64.decoder.decode(defaults.substring(defaults.indexOf(',') + 1))), 800, 600)
  }

  @Test
  void swingPanelPrefersRequestedSize() {
    SvgPanel panel = Plot.swing(chart(), WIDTH, HEIGHT)
    assertEquals(WIDTH, panel.preferredSize.width)
    assertEquals(HEIGHT, panel.preferredSize.height)
    assertEquals(800, Plot.swing(chart()).preferredSize.width)
  }

  @Test
  void jfxRendersAtRequestedSize() {
    Assumptions.assumeTrue(
        System.getenv('DISPLAY') != null || 'true' == System.getProperty('headless'),
        'No DISPLAY available; skipping JavaFX test. Run with -Pheadless=true for headless mode.'
    )
    Group sized = Plot.jfx(chart(), WIDTH, HEIGHT)
    assertEquals(WIDTH, sized.boundsInLocal.width, 0.01d)
    assertEquals(HEIGHT, sized.boundsInLocal.height, 0.01d)

    Group defaults = Plot.jfx(chart())
    assertEquals(800, defaults.boundsInLocal.width, 0.01d)
    assertEquals(600, defaults.boundsInLocal.height, 0.01d)
  }

  @Test
  void sizedExportsRejectNullChart() {
    assertThrows(IllegalArgumentException) { Plot.jpg(null, new ByteArrayOutputStream(), WIDTH, HEIGHT) }
    assertThrows(IllegalArgumentException) { Plot.pdf(null, new ByteArrayOutputStream(), WIDTH, HEIGHT) }
    assertThrows(IllegalArgumentException) { Plot.swing(null, WIDTH, HEIGHT) }
    assertThrows(IllegalArgumentException) { Plot.jfx(null, WIDTH, HEIGHT) }
    assertThrows(IllegalArgumentException) { Plot.base64(null, WIDTH, HEIGHT) }
  }

}
