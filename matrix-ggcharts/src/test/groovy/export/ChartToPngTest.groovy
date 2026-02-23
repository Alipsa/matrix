package export

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.export.GgExport

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths

import se.alipsa.matrix.charm.Charts
import static se.alipsa.matrix.gg.GgPlot.*
import static org.junit.jupiter.api.Assertions.*

import testutil.Slow

@Slow
class ChartToPngTest {

  @Test
  void testExportToPng() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')
    Path classLocation = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    )
    Path buildDir = classLocation.getParent().getParent().getParent()
    Path svgPath = buildDir.resolve("testExportToPng.svg")
    File svgFile = svgPath.toFile()
    write(chart, svgFile)
    assertTrue(svgFile.exists())
    Path filePath = buildDir.resolve("testExportToPng.png")
    File file = filePath.toFile()
    GgExport.toPng(chart, file)

    // Verify file exists
    assertTrue(file.exists())

    // Verify file size is greater than zero
    assertTrue(file.length() > 0, "PNG file should not be empty")

    // Verify the image can be read successfully using ImageIO
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, "PNG file should be readable by ImageIO")

    // Verify image dimensions are reasonable (greater than zero)
    assertTrue(image.getWidth() > 0, "Image width should be greater than 0")
    assertTrue(image.getHeight() > 0, "Image height should be greater than 0")
  }

  @Test
  void testExportWithNullTargetFile_String() {
    String svgContent = "<svg></svg>"
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToPng.export(svgContent, (File) null)
    })
    assertEquals("targetFile cannot be null", exception.getMessage())
  }

  @Test
  void testExportWithNullTargetFile_Svg() {
    Svg svg = new Svg()
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToPng.export(svg, (File) null)
    })
    assertEquals("targetFile cannot be null", exception.getMessage())
  }

  @Test
  void testExportWithNullTargetFile_GgChart() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) + geom_point()
    Exception exception = assertThrows(IllegalArgumentException.class, {
      GgExport.toPng(chart, null)
    })
    assertEquals("targetFile must not be null", exception.getMessage())
  }

  @Test
  void testExportCharmChartToFile() {
    CharmChart chart = buildCharmChart()

    Path buildDir = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    ).getParent().getParent().getParent()
    File file = buildDir.resolve("testCharmChartToPng.png").toFile()

    ChartToPng.export(chart, file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0, "PNG file should not be empty")
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, "PNG file should be readable by ImageIO")
    assertTrue(image.getWidth() > 0)
    assertTrue(image.getHeight() > 0)
  }

  @Test
  void testExportCharmChartToOutputStream() {
    CharmChart chart = buildCharmChart()

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPng.export(chart, baos)
    byte[] bytes = baos.toByteArray()
    assertTrue(bytes.length > 0, "PNG output should not be empty")
    // Verify PNG magic bytes
    assertEquals((byte) 0x89, bytes[0])
    assertEquals((byte) 0x50, bytes[1])
    assertEquals((byte) 0x4E, bytes[2])
    assertEquals((byte) 0x47, bytes[3])
  }

  private static CharmChart buildCharmChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4]])
        .build()
    Charts.plot(data).aes(x: 'x', y: 'y').points().build()
  }
}
