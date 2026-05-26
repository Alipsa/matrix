package export

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.charm.Charts.plotGrid

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testutil.Slow

import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPdf
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.chartexport.ChartToSvg
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel
import se.alipsa.matrix.core.Matrix

import java.awt.image.BufferedImage
import java.nio.file.Path

import javax.imageio.ImageIO

@Slow
class WriteToAndPlotGridExportTest {

  @Test
  void testWriteToPng(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart.png').toFile()
    chart.writeTo(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertTrue(image.width > 0 && image.height > 0)
  }

  @Test
  void testWriteToJpeg(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart.jpg').toFile()
    chart.writeTo(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertTrue(image.width > 0 && image.height > 0)
  }

  @Test
  void testWriteToJpegExtension(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart.jpeg').toFile()
    chart.writeTo(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertTrue(image.width > 0 && image.height > 0)
  }

  @Test
  void testWriteToSvg(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart.svg').toFile()
    chart.writeTo(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    String content = file.text
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testWriteToPdf(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart.pdf').toFile()
    chart.writeTo(file)
    assertPdfDimensions(file, 800, 600)
  }

  @Test
  void testWriteToStringPath(@TempDir Path tempDir) {
    Chart chart = buildChart()
    String path = tempDir.resolve('chart.png') as String
    chart.writeTo(path)
    File file = new File(path)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
  }

  @Test
  void testWriteToDefaultsToSvg(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart.unknown').toFile()
    chart.writeTo(file)
    assertTrue(file.exists())
    String content = file.text
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testChartWriteToSvgWithDimensions(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart-sized.svg').toFile()
    chart.writeTo(file, 320, 240)
    assertTrue(file.exists())
    String content = file.text
    assertTrue(content.contains('<svg'))
    assertTrue(content.contains('width="320"'))
    assertTrue(content.contains('height="240"'))
  }

  @Test
  void testChartWriteToPngWithDimensions(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart-sized.png').toFile()
    chart.writeTo(file, 320, 240)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertEquals(320, image.width)
    assertEquals(240, image.height)
  }

  @Test
  void testChartWriteToJpegStringPathWithDimensions(@TempDir Path tempDir) {
    Chart chart = buildChart()
    String path = tempDir.resolve('chart-sized.jpg') as String
    chart.writeTo(path, 320, 240)
    File file = new File(path)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertEquals(320, image.width)
    assertEquals(240, image.height)
  }

  @Test
  void testChartWriteToPdfWithDimensions(@TempDir Path tempDir) {
    Chart chart = buildChart()
    File file = tempDir.resolve('chart-sized.pdf').toFile()
    chart.writeTo(file, 320, 240)
    assertPdfDimensions(file, 320, 240)
  }

  @Test
  void testPlotSpecRenderConvenienceMethods() {
    PlotSpec spec = buildPlotSpec()
    assertEquals('800', spec.render().width.toString())
    assertEquals('600', spec.render().height.toString())
    assertEquals('320', spec.render(320, 240).width.toString())
    assertEquals('240', spec.render(320, 240).height.toString())
  }

  @Test
  void testPlotSpecWriteToConvenienceMethods(@TempDir Path tempDir) {
    PlotSpec spec = buildPlotSpec()
    File fileTarget = tempDir.resolve('plot-spec-file.svg').toFile()
    String pathTarget = tempDir.resolve('plot-spec-path.svg') as String

    spec.writeTo(fileTarget)
    spec.writeTo(pathTarget)

    assertTrue(fileTarget.exists())
    assertTrue(fileTarget.text.contains('<svg'))
    File pathFile = new File(pathTarget)
    assertTrue(pathFile.exists())
    assertTrue(pathFile.text.contains('<svg'))
  }

  // ---- PlotGrid.writeTo ----

  @Test
  void testPlotGridWriteToPng(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid.png').toFile()
    grid.writeTo(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertTrue(image.width > 0 && image.height > 0)
  }

  @Test
  void testPlotGridWriteToJpeg(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid.jpg').toFile()
    grid.writeTo(file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
    assertTrue(image.width > 0 && image.height > 0)
  }

  @Test
  void testPlotGridWriteToSvg(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid.svg').toFile()
    grid.writeTo(file)
    assertTrue(file.exists())
    String content = file.text
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testPlotGridWriteToPdf(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid.pdf').toFile()
    grid.writeTo(file)
    assertPdfDimensions(file, 800, 600)
  }

  @Test
  void testPlotGridWriteToStringPath(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    String path = tempDir.resolve('grid.png') as String
    grid.writeTo(path)
    File file = new File(path)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
  }

  // ---- PlotGrid export classes ----

  @Test
  void testPlotGridExportToPngFile(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid-export.png').toFile()
    ChartToPng.export(grid, file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
  }

  @Test
  void testPlotGridExportToPngOutputStream() {
    PlotGrid grid = buildGrid()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPng.export(grid, baos)
    byte[] bytes = baos.toByteArray()
    assertTrue(bytes.length > 0)
    assertEquals((byte) 0x89, bytes[0])
    assertEquals((byte) 0x50, bytes[1])
  }

  @Test
  void testPlotGridExportToJpegFile(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid-export.jpg').toFile()
    ChartToJpeg.export(grid, file)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image)
  }

  @Test
  void testPlotGridExportToJpegOutputStream() {
    PlotGrid grid = buildGrid()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToJpeg.export(grid, baos)
    assertTrue(baos.size() > 0)
  }

  @Test
  void testPlotGridExportToPdfFile(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid-export.pdf').toFile()
    ChartToPdf.export(grid, file)
    assertPdfDimensions(file, 800, 600)
  }

  @Test
  void testPlotGridExportToPdfOutputStream() {
    PlotGrid grid = buildGrid()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPdf.export(grid, baos)
    byte[] bytes = baos.toByteArray()
    assertTrue(bytes.length > 0)
    assertEquals((byte) 0x25, bytes[0])
    assertEquals((byte) 0x50, bytes[1])
    assertEquals((byte) 0x44, bytes[2])
    assertEquals((byte) 0x46, bytes[3])
  }

  @Test
  void testPlotGridExportToSvgFile(@TempDir Path tempDir) {
    PlotGrid grid = buildGrid()
    File file = tempDir.resolve('grid-export.svg').toFile()
    ChartToSvg.export(grid, file)
    assertTrue(file.exists())
    String content = file.text
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testPlotGridExportToSvgOutputStream() {
    PlotGrid grid = buildGrid()
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToSvg.export(grid, baos)
    String svg = baos.toString('UTF-8')
    assertTrue(svg.contains('<svg'))
  }

  @Test
  void testPlotGridExportToSvgWriter() {
    PlotGrid grid = buildGrid()
    StringWriter writer = new StringWriter()
    ChartToSvg.export(grid, writer)
    String svg = writer as String
    assertTrue(svg.contains('<svg'))
  }

  @Test
  void testPlotGridExportToSwing() {
    PlotGrid grid = buildGrid()
    SvgPanel panel = ChartToSwing.export(grid)
    assertNotNull(panel)
    assertTrue(panel.preferredSize.width > 0)
    assertTrue(panel.preferredSize.height > 0)
  }

  @Test
  void testPlotGridExportToBufferedImage() {
    PlotGrid grid = buildGrid()
    BufferedImage image = ChartToImage.export(grid)
    assertNotNull(image)
    assertTrue(image.width > 0 && image.height > 0)
  }

  @Test
  void testPlotGridExportToBase64() {
    PlotGrid grid = buildGrid()
    String dataUri = ChartToImage.base64(grid)
    assertNotNull(dataUri)
    assertTrue(dataUri.startsWith('data:image/png;base64,'))
  }

  private static Chart buildChart() {
    buildPlotSpec().build()
  }

  private static PlotSpec buildPlotSpec() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 6]])
        .build()
    plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }
  }

  private static PlotGrid buildGrid() {
    Chart c1 = buildChart()
    Chart c2 = buildChart()
    plotGrid {
      add c1, c2
      ncol 2
    }
  }

  private static void assertPdfDimensions(File file, int width, int height) {
    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    PDDocument document = Loader.loadPDF(file)
    try {
      assertEquals(1, document.numberOfPages)
      PDRectangle mediaBox = document.getPage(0).mediaBox
      assertEquals(width as float, mediaBox.width, 0.01f)
      assertEquals(height as float, mediaBox.height, 0.01f)
    } finally {
      document.close()
    }
  }

}
