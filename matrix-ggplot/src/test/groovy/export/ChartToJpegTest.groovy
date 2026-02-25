package export

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.chartexport.ChartToJpeg
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
class ChartToJpegTest {

  @Test
  void testExportToJpeg() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

    Path classLocation = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    )
    Path buildDir = classLocation.getParent().getParent().getParent()
    Path filePath = buildDir.resolve("testExportToJpeg.jpg")
    File file = filePath.toFile()

    GgExport.toJpeg(chart, file, 0.9)

    // Verify file exists
    assertTrue(file.exists())

    // Verify file size is greater than zero
    assertTrue(file.length() > 0, "JPEG file should not be empty")

    // Verify the image can be read successfully using ImageIO
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, "JPEG file should be readable by ImageIO")

    // Verify image dimensions are reasonable (greater than zero)
    assertTrue(image.getWidth() > 0, "Image width should be greater than 0")
    assertTrue(image.getHeight() > 0, "Image height should be greater than 0")
  }

  @Test
  void testExportWithNullSvgChart() {
    Path classLocation = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    )
    Path buildDir = classLocation.getParent().getParent().getParent()
    File file = buildDir.resolve("testNullSvg.jpg").toFile()

    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToJpeg.export((Svg) null, file, 0.9)
    })
    assertEquals("svgChart must not be null", exception.getMessage())
  }

  @Test
  void testExportWithNullTargetFile() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) + geom_point()
    Svg svg = chart.render()

    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToJpeg.export(svg, null, 0.9)
    })
    assertEquals("targetFile cannot be null", exception.getMessage())
  }

  @Test
  void testExportCharmChartToJpeg() {
    CharmChart chart = buildCharmChart()

    Path buildDir = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    ).getParent().getParent().getParent()
    File file = buildDir.resolve("testCharmChartToJpeg.jpg").toFile()

    ChartToJpeg.export(chart, file, 0.9)
    assertTrue(file.exists())
    assertTrue(file.length() > 0, "JPEG file should not be empty")
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, "JPEG file should be readable by ImageIO")
    assertTrue(image.getWidth() > 0)
    assertTrue(image.getHeight() > 0)
  }

  @Test
  void testExportCharmChartWithDefaultQuality() {
    CharmChart chart = buildCharmChart()

    Path buildDir = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    ).getParent().getParent().getParent()
    File file = buildDir.resolve("testCharmChartToJpegDefault.jpg").toFile()

    ChartToJpeg.export(chart, file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0, "JPEG file should not be empty")
  }

  private static CharmChart buildCharmChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4]])
        .build()
    Charts.plot(data).mapping(x: 'x', y: 'y').points().build()
  }
}
