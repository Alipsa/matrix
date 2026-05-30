package export

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testutil.Slow

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.chartexport.ChartToSvg
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.Chart
import se.alipsa.matrix.pict.Plot
import se.alipsa.matrix.pict.ScatterChart

import java.awt.image.BufferedImage
import java.lang.reflect.Method
import java.nio.file.Path
import java.nio.file.Paths

import javax.imageio.ImageIO

/**
 * Tests for CharmChart export functionality that remains in matrix-charts.
 */
@Slow
class CharmExportTest {

  @Test
  void testCharmChartToImage() {
    CharmChart chart = buildCharmChart()
    BufferedImage image = ChartToImage.export(chart)
    assertNotNull(image, 'BufferedImage should not be null')
    assertTrue(image.getWidth() > 0, 'Image width should be greater than 0')
    assertTrue(image.getHeight() > 0, 'Image height should be greater than 0')
  }

  @Test
  void testCharmChartToPng() {
    CharmChart chart = buildCharmChart()
    Path buildDir = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    ).getParent().getParent().getParent()
    File file = buildDir.resolve('testCharmExportToPng.png').toFile()

    ChartToPng.export(chart, file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0, 'PNG file should not be empty')
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, 'PNG file should be readable by ImageIO')
    assertTrue(image.getWidth() > 0)
    assertTrue(image.getHeight() > 0)
  }

  @Test
  void testCharmChartToJpeg() {
    CharmChart chart = buildCharmChart()
    Path buildDir = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    ).getParent().getParent().getParent()
    File file = buildDir.resolve('testCharmExportToJpeg.jpg').toFile()

    ChartToJpeg.export(chart, file, 0.9)
    assertTrue(file.exists())
    assertTrue(file.length() > 0, 'JPEG file should not be empty')
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, 'JPEG file should be readable by ImageIO')
    assertTrue(image.getWidth() > 0)
    assertTrue(image.getHeight() > 0)
  }

  @Test
  void testCharmChartToSwing() {
    CharmChart chart = buildCharmChart()
    SvgPanel panel = ChartToSwing.export(chart)
    assertNotNull(panel, 'SvgPanel should not be null')
    assertNotNull(panel.getPreferredSize(), 'Panel should have preferred size')
    assertTrue(panel.getPreferredSize().width > 0)
    assertTrue(panel.getPreferredSize().height > 0)
  }

  @Test
  @CompileStatic
  void testChartToSwingTypedOverloads() {
    CharmChart charmChart = buildCharmChart()
    Svg svg = charmChart.render()
    String svgText = svg.toXml()
    CharSequence svgSequence = new StringBuilder(svgText)
    Chart pictChart = buildPictChart()
    PlotGrid grid = Charts.plotGrid([charmChart], 1)

    assertNotNull(ChartToSwing.export(svgText))
    assertNotNull(ChartToSwing.export(svgSequence))
    assertNotNull(ChartToSwing.export(svg))
    assertNotNull(ChartToSwing.export(charmChart))
    assertNotNull(Plot.swing(pictChart))
    assertNotNull(ChartToSwing.export(grid))
  }

  @Test
  void testChartToSwingExposesObjectFallback() {
    List<Method> objectOverloads = ChartToSwing.declaredMethods.findAll { Method method ->
      method.name == 'export' &&
          method.parameterTypes.length == 1 &&
          method.parameterTypes[0] == Object
    }
    assertFalse(objectOverloads.isEmpty(), 'ChartToSwing should expose export(Object) fallback')
  }

  @Test
  void testChartToSwingObjectDispatch() {
    Object chart = buildCharmChart()
    SvgPanel panel = ChartToSwing.export(chart)
    assertNotNull(panel, 'Object dispatch should produce an SvgPanel')
  }

  @Test
  void testCharmChartToPngOutputStream() {
    CharmChart chart = buildCharmChart()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPng.export(chart, baos)
    byte[] bytes = baos.toByteArray()
    assertTrue(bytes.length > 0, 'PNG output should not be empty')
    // Verify PNG magic bytes
    assertEquals((byte) 0x89, bytes[0])
    assertEquals((byte) 0x50, bytes[1])
    assertEquals((byte) 0x4E, bytes[2])
    assertEquals((byte) 0x47, bytes[3])
  }

  @Test
  void testChartToSvgCharmChartFile(@TempDir Path tempDir) {
    CharmChart chart = buildCharmChart()
    File file = tempDir.resolve('testChartToSvg.svg').toFile()

    ChartToSvg.export(chart, file)
    assertTrue(file.exists(), 'SVG file should be created')
    assertTrue(file.length() > 0, 'SVG file should not be empty')
    String content = file.text
    assertTrue(content.contains('<svg'), 'File should contain SVG content')
  }

  @Test
  void testChartToSvgCharmChartOutputStream() {
    CharmChart chart = buildCharmChart()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToSvg.export(chart, baos)
    String svg = baos.toString('UTF-8')
    assertTrue(svg.length() > 0, 'SVG output should not be empty')
    assertTrue(svg.contains('<svg'), 'Output should contain SVG content')
  }

  @Test
  void testChartToSvgCharmChartWriter() {
    CharmChart chart = buildCharmChart()
    StringWriter writer = new StringWriter()
    ChartToSvg.export(chart, writer)
    String svg = writer
    assertTrue(svg.length() > 0, 'SVG output should not be empty')
    assertTrue(svg.contains('<svg'), 'Output should contain SVG content')
  }

  @Test
  void testChartToSvgPictOutputStream() {
    def chart = buildPictChart()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    Plot.svg(chart, baos)
    String svg = baos.toString('UTF-8')
    assertTrue(svg.length() > 0, 'SVG output should not be empty')
    assertTrue(svg.contains('<svg'), 'Output should contain SVG content')
  }

  @Test
  void testChartToSvgLegacyWriter() {
    def chart = buildPictChart()
    StringWriter writer = new StringWriter()
    Plot.svg(chart, writer)
    String svg = writer
    assertTrue(svg.length() > 0, 'SVG output should not be empty')
    assertTrue(svg.contains('<svg'), 'Output should contain SVG content')
  }

  private static Chart buildPictChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4]])
        .build()
    ScatterChart.builder(data).title('Test').x('x').y('y').build()
  }

  private static CharmChart buildCharmChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4]])
        .build()
    Charts.plot(data).mapping(x: 'x', y: 'y').addLayer(new PointBuilder()).build()
  }

}
