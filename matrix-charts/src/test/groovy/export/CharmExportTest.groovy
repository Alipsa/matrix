package export

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testutil.Slow

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgReader
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
  void testCharmChartToBase64() {
    CharmChart chart = buildCharmChart()
    String dataUri = ChartToImage.base64(chart)
    assertNotNull(dataUri)
    assertTrue(dataUri.startsWith('data:image/png;base64,'))
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
    PlotGrid grid = Charts.plotGrid([charmChart], 1)

    assertNotNull(ChartToSwing.export(svgText))
    assertNotNull(ChartToSwing.export(svgSequence))
    assertNotNull(ChartToSwing.export(svg))
    assertNotNull(ChartToSwing.export(charmChart))
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
  void testChartToSvgRenderedSvgDestinations(@TempDir Path tempDir) {
    Svg svg = buildCharmChart().render()

    File file = tempDir.resolve('rendered.svg').toFile()
    ChartToSvg.export(svg, file)
    assertTrue(file.text.contains('<svg'))

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    ChartToSvg.export(svg, outputStream)
    assertTrue(outputStream.toString('UTF-8').contains('<svg'))

    StringWriter writer = new StringWriter()
    ChartToSvg.export(svg, writer)
    assertTrue(writer.toString().contains('<svg'))
  }

  @Test
  void testChartToSvgObjectFallbackAcceptsRenderedSvg(@TempDir Path tempDir) {
    Object svg = buildCharmChart().render()

    File file = tempDir.resolve('object-rendered.svg').toFile()
    ChartToSvg.export(svg, file)
    assertTrue(file.text.contains('<svg'))

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    ChartToSvg.export(svg, outputStream)
    assertTrue(outputStream.toString('UTF-8').contains('<svg'))

    StringWriter writer = new StringWriter()
    ChartToSvg.export(svg, writer)
    assertTrue(writer.toString().contains('<svg'))
  }

  @Test
  @CompileStatic
  void testChartToSvgRenderedSvgNullValidation(@TempDir Path tempDir) {
    Svg svg = buildCharmChart().render()
    Svg nullSvg = null
    File targetFile = tempDir.resolve('unused.svg').toFile()
    File nullFile = null
    OutputStream nullOutputStream = null
    Writer nullWriter = null

    assertEquals('svg cannot be null', assertThrows(IllegalArgumentException) {
      ChartToSvg.export(nullSvg, targetFile)
    }.message)
    assertEquals('targetFile cannot be null', assertThrows(IllegalArgumentException) {
      ChartToSvg.export(svg, nullFile)
    }.message)
    assertEquals('outputStream cannot be null', assertThrows(IllegalArgumentException) {
      ChartToSvg.export(svg, nullOutputStream)
    }.message)
    assertEquals('writer cannot be null', assertThrows(IllegalArgumentException) {
      ChartToSvg.export(svg, nullWriter)
    }.message)
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    assertEquals('svg cannot be null', assertThrows(IllegalArgumentException) {
      ChartToSvg.export(nullSvg, outputStream)
    }.message)
    StringWriter writer = new StringWriter()
    assertEquals('svg cannot be null', assertThrows(IllegalArgumentException) {
      ChartToSvg.export(nullSvg, writer)
    }.message)
  }

  @Test
  void testWriteToCreatesMissingParentDirectories(@TempDir Path tempDir) {
    CharmChart chart = buildCharmChart()
    Map<String, File> files = [
        svg: tempDir.resolve('svg/nested/chart.svg').toFile(),
        png: tempDir.resolve('png/nested/chart.png').toFile(),
        pdf: tempDir.resolve('pdf/nested/chart.pdf').toFile(),
        jpg: tempDir.resolve('jpg/nested/chart.jpg').toFile()
    ]

    files.each { String extension, File file ->
      assertFalse(file.parentFile.exists())
      chart.writeTo(file)
      assertTrue(file.exists(), "Expected .$extension export to create its parent directories")
      assertTrue(file.length() > 0, "Expected .$extension export to contain data")
    }

    assertNotNull(ImageIO.read(files.png))
    assertNotNull(ImageIO.read(files.jpg))
  }

  @Test
  void testPlotGridWriteToCreatesMissingSvgParentDirectories(@TempDir Path tempDir) {
    CharmChart chart = buildCharmChart()
    PlotGrid grid = Charts.plotGrid([chart, chart], 2)
    File file = tempDir.resolve('grid/nested/grid.svg').toFile()

    assertFalse(file.parentFile.exists())
    grid.writeTo(file)

    assertTrue(file.exists())
    assertTrue(file.text.contains('<svg'))
  }

  @Test
  void testWriteToSizedCreatesMissingSvgParentDirectories(@TempDir Path tempDir) {
    CharmChart chart = buildCharmChart()
    File file = tempDir.resolve('sized/nested/chart.svg').toFile()

    assertFalse(file.parentFile.exists())
    chart.writeTo(file, 800, 600)

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    assertTrue(file.text.contains('<svg'))
  }

  @Test
  void testPlotGridWriteToSizedCreatesMissingSvgParentDirectories(@TempDir Path tempDir) {
    CharmChart chart = buildCharmChart()
    PlotGrid grid = Charts.plotGrid([chart, chart], 2)
    File file = tempDir.resolve('grid-sized/nested/grid.svg').toFile()

    assertFalse(file.parentFile.exists())
    grid.writeTo(file, 800, 600)

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    assertTrue(file.text.contains('<svg'))
  }

  @Test
  void testChartToPngScaledDimensions(@TempDir Path tempDir) {
    CharmChart chart = buildCharmChart()
    File naturalFile = tempDir.resolve('natural.png').toFile()
    File scaledFile = tempDir.resolve('scaled.png').toFile()

    ChartToPng.export(chart, naturalFile)
    ChartToPng.export(chart.render(), scaledFile, 2)

    BufferedImage natural = ImageIO.read(naturalFile)
    BufferedImage scaled = ImageIO.read(scaledFile)
    assertNotNull(natural)
    assertNotNull(scaled)
    assertEquals(natural.width * 2, scaled.width)
    assertEquals(natural.height * 2, scaled.height)
  }

  @Test
  void testChartToPngNaturalFractionalDimensions(@TempDir Path tempDir) {
    Svg svg = SvgReader.parse('''<svg xmlns="http://www.w3.org/2000/svg" width="100.5" height="50.3">
      <rect width="100.5" height="50.3" fill="red" />
    </svg>''')
    File file = tempDir.resolve('fractional.png').toFile()

    ChartToPng.export(svg, file)

    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertEquals(101, image.width)
    assertEquals(51, image.height)
  }

  @Test
  void testChartToPngStringPathStillWorks(@TempDir Path tempDir) {
    String svgContent = '''<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
      <rect width="200" height="100" fill="blue" />
    </svg>'''
    File file = tempDir.resolve('from-string.png').toFile()

    ChartToPng.export(svgContent, file)

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertEquals(200, image.width)
    assertEquals(100, image.height)
  }

  @Test
  void testChartToPngScaleValidation(@TempDir Path tempDir) {
    Svg svg = SvgReader.parse('''<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
      <rect width="100" height="100" fill="red" />
    </svg>''')
    File file = tempDir.resolve('scale-valid.png').toFile()
    OutputStream os = new ByteArrayOutputStream()

    assertEquals('scale must be > 0, but was null', assertThrows(IllegalArgumentException) {
      ChartToPng.export(svg, file, null)
    }.message)
    assertEquals('scale must be > 0, but was 0', assertThrows(IllegalArgumentException) {
      ChartToPng.export(svg, file, 0)
    }.message)
    assertEquals('scale must be > 0, but was -1', assertThrows(IllegalArgumentException) {
      ChartToPng.export(svg, file, -1)
    }.message)
    assertEquals('scale must be > 0, but was null', assertThrows(IllegalArgumentException) {
      ChartToPng.export(svg, os, null)
    }.message)
    assertEquals('scale must be > 0, but was 0', assertThrows(IllegalArgumentException) {
      ChartToPng.export(svg, os, 0)
    }.message)
    assertEquals('scale must be > 0, but was -1', assertThrows(IllegalArgumentException) {
      ChartToPng.export(svg, os, -1)
    }.message)
  }

  private static CharmChart buildCharmChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4]])
        .build()
    Charts.plot(data).mapping(x: 'x', y: 'y').addLayer(new PointBuilder()).build()
  }

}
