package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.AreaChart
import se.alipsa.matrix.pict.BarChart
import se.alipsa.matrix.pict.CharmBridge
import se.alipsa.matrix.pict.ChartType
import se.alipsa.matrix.pict.Histogram
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.PieChart
import se.alipsa.matrix.pict.Plot
import se.alipsa.matrix.pict.ScatterChart

/**
 * Tests verifying that PICT chart types produce valid output through Plot
 * and remain compatible with the underlying image export APIs.
 */
class PlotCompatibilityTest {

  private static final byte[] PNG_HEADER = [0x89, 0x50, 0x4E, 0x47] as byte[]

  private static Matrix sampleData() {
    Matrix.builder()
        .matrixName('Sample')
        .columns([
            category: ['A', 'B', 'C', 'D'],
            value   : [10, 25, 15, 30]
        ])
        .types([String, Number])
        .build()
  }

  private static Matrix sampleNumericData() {
    Matrix.builder()
        .matrixName('Numeric')
        .columns([
            x: [1, 2, 3, 4],
            y: [10, 20, 15, 25]
        ])
        .types([int, Number])
        .build()
  }

  @Test
  void testPngToFileWithAreaChart() {
    Matrix data = sampleData()
    AreaChart chart = AreaChart.create('Area PNG', data, 'category', 'value')
    File file = File.createTempFile('AreaChart', '.png')
    try {
      ChartToPng.export(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 100, 'PNG file should have content')
      assertPngHeader(file)
    } finally {
      file.delete()
    }
  }

  @Test
  void testPngToFileWithBarChart() {
    Matrix data = sampleData()
    BarChart chart = BarChart.createVertical('Bar PNG', data, 'category', ChartType.BASIC, 'value')
    File file = File.createTempFile('BarChart', '.png')
    try {
      ChartToPng.export(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 100, 'PNG file should have content')
      assertPngHeader(file)
    } finally {
      file.delete()
    }
  }

  @Test
  void testPngToFileWithLineChart() {
    Matrix data = Matrix.builder()
        .matrixName('LineData')
        .columns([
            x: [1, 2, 3, 4],
            y: [10, 20, 15, 25]
        ])
        .types([int, Number])
        .build()
    LineChart chart = LineChart.create('Line PNG', data, 'x', 'y')
    File file = File.createTempFile('LineChart', '.png')
    try {
      ChartToPng.export(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 100, 'PNG file should have content')
      assertPngHeader(file)
    } finally {
      file.delete()
    }
  }

  @Test
  void testPngToFileWithPieChart() {
    Matrix data = sampleData()
    PieChart chart = PieChart.create('Pie PNG', data, 'category', 'value')
    File file = File.createTempFile('PieChart', '.png')
    try {
      ChartToPng.export(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 100, 'PNG file should have content')
      assertPngHeader(file)
    } finally {
      file.delete()
    }
  }

  @Test
  void testPngToFileWithScatterChart() {
    Matrix data = Matrix.builder()
        .matrixName('Scatter')
        .columns([
            x: [1, 2, 3, 4, 5],
            y: [2, 4, 1, 5, 3]
        ])
        .types([Number, Number])
        .build()
    ScatterChart chart = ScatterChart.create('Scatter PNG', data, 'x', 'y')
    File file = File.createTempFile('ScatterChart', '.png')
    try {
      ChartToPng.export(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 100, 'PNG file should have content')
      assertPngHeader(file)
    } finally {
      file.delete()
    }
  }

  @Test
  void testPngToFileWithHistogram() {
    Matrix data = Matrix.builder()
        .matrixName('Hist')
        .columns([score: [55, 60, 65, 70, 75, 80, 85, 90]])
        .types([Number])
        .build()
    Histogram chart = Histogram.create('Hist PNG', data, 'score', 4)
    File file = File.createTempFile('Histogram', '.png')
    try {
      ChartToPng.export(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 100, 'PNG file should have content')
      assertPngHeader(file)
    } finally {
      file.delete()
    }
  }

  @Test
  void testPngToOutputStream() {
    Matrix data = sampleData()
    BarChart chart = BarChart.createVertical('Bar OS', data, 'category', ChartType.BASIC, 'value')
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ChartToPng.export(chart, baos)
    byte[] bytes = baos.toByteArray()
    assertTrue(bytes.length > 100, 'PNG output should have content')
    for (int i = 0; i < PNG_HEADER.length; i++) {
      assertEquals(PNG_HEADER[i], bytes[i], "PNG header byte ${i} should match")
    }
  }

  @Test
  void testBase64ReturnsValidDataUri() {
    Matrix data = sampleData()
    PieChart chart = PieChart.create('Pie Base64', data, 'category', 'value')
    String uri = ChartToImage.base64(chart)
    assertTrue(uri.startsWith('data:image/png;base64,'), 'Should start with data URI prefix')
    String base64Part = uri.substring('data:image/png;base64,'.length())
    byte[] decoded = Base64.decoder.decode(base64Part)
    assertTrue(decoded.length > 100, 'Decoded PNG should have content')
    for (int i = 0; i < PNG_HEADER.length; i++) {
      assertEquals(PNG_HEADER[i], decoded[i], "PNG header byte ${i} should match in base64 output")
    }
  }

  @Test
  void testLegacyAxisScaleAppliesToCharmBridge() {
    Matrix data = Matrix.builder()
        .matrixName('AxisScaleData')
        .columns([
            x: [0, 1, 2, 3, 4, 5],
            y: [0, 10, 20, 30, 40, 50]
        ])
        .types([Number, Number])
        .build()
    LineChart chart = LineChart.builder(data)
        .title('Axis Scale')
        .x('x')
        .y('y')
        .xAxisScale(0, 5, 1)
        .yAxisScale(0, 50, 10)
        .build()

    List<String> labels = textContent(CharmBridge.renderSvg(chart, 800, 600))

    assertTrue(labels.containsAll(['0', '1', '2', '3', '4', '5']))
    assertTrue(labels.containsAll(['10', '20', '30', '40', '50']))
  }

  @Test
  void testLegacyYLabelsApplyToCharmBridge() {
    Matrix data = Matrix.builder()
        .matrixName('YLabelData')
        .columns([
            x: [1, 2, 3],
            y: [10, 20, 30]
        ])
        .types([Number, Number])
        .build()
    LineChart chart = LineChart.builder(data)
        .title('Y Labels')
        .x('x')
        .y('y')
        .xAxisVisible(true)
        .build()
    chart.style.yLabels = ['30': 'High', '10': 'Low', '20': 'Middle']

    List<String> labels = textContent(CharmBridge.renderSvg(chart, 800, 600))
    se.alipsa.matrix.charm.Chart charmChart = CharmBridge.convert(chart)

    assertTrue(labels.containsAll(['Low', 'Middle', 'High']))
    assertEquals(['10', '20', '30'], charmChart.scale.y.breaks*.toString())
    assertEquals(['Low', 'Middle', 'High'], charmChart.scale.y.labels)
  }

  @Test
  void testLegacyCssApiInjectsRawCharmCss() {
    Matrix data = sampleData()
    BarChart chart = BarChart.builder(data)
        .title('CSS Compatibility')
        .x('category')
        .y('value')
        .css('.legacy-custom { fill: red; }')
        .build()

    assertEquals('.legacy-custom { fill: red; }', chart.style.css)
    String xml = SvgWriter.toXml(CharmBridge.renderSvg(chart, 800, 600))
    assertTrue(xml.contains('<style'))
    assertTrue(xml.contains('.legacy-custom { fill: red; }'))
  }

  @Test
  void testPlotSvgReturnsSvgObject() {
    Matrix data = sampleData()
    BarChart chart = BarChart.createVertical('SVG Test', data, 'category', ChartType.BASIC, 'value')
    Svg svg = Plot.svg(chart)
    assertNotNull(svg, 'Plot.svg() should return a non-null Svg')
    String xml = SvgWriter.toXml(svg)
    assertTrue(xml.contains('<svg'), 'Result should contain an SVG element')
  }

  @Test
  void testPlotSvgWithExplicitDimensions() {
    LineChart chart = LineChart.create('Sized SVG', sampleNumericData(), 'x', 'y')
    Svg svg = Plot.svg(chart, 1200, 900)
    assertNotNull(svg, 'Plot.svg() with dimensions should return a non-null Svg')
    assertEquals('1200', svg.width.toString())
    assertEquals('900', svg.height.toString())
  }

  @Test
  void testPlotSvgRejectsNullChart() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Plot.svg((se.alipsa.matrix.pict.Chart) null)
    }
    assertEquals('chart cannot be null', exception.message)
  }

  @Test
  void testPlotPngRejectsNullChart() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Plot.png((se.alipsa.matrix.pict.Chart) null, new ByteArrayOutputStream())
    }
    assertEquals('chart cannot be null', exception.message)
  }

  @Test
  void testPlotPngRejectsNullTargets() {
    BarChart chart = BarChart.createVertical('PNG Null Targets', sampleData(), 'category', ChartType.BASIC, 'value')

    IllegalArgumentException fileException = assertThrows(IllegalArgumentException) {
      Plot.png(chart, (File) null)
    }
    assertEquals('file cannot be null', fileException.message)

    IllegalArgumentException outputStreamException = assertThrows(IllegalArgumentException) {
      Plot.png(chart, (OutputStream) null)
    }
    assertEquals('output stream cannot be null', outputStreamException.message)
  }

  @Test
  void testPlotBase64RejectsNullChart() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Plot.base64((se.alipsa.matrix.pict.Chart) null)
    }
    assertEquals('chart cannot be null', exception.message)
  }

  @Test
  void testPlotSvgToFile() {
    BarChart chart = BarChart.createVertical('SVG File', sampleData(), 'category', ChartType.BASIC, 'value')
    File file = File.createTempFile('BarChart', '.svg')
    try {
      Plot.svg(chart, file, 1200, 900)
      String xml = file.text
      assertTrue(xml.contains('<svg'), 'SVG file should contain an SVG element')
      assertTrue(xml.contains('width="1200"'), 'SVG file should reflect requested width')
      assertTrue(xml.contains('height="900"'), 'SVG file should reflect requested height')
    } finally {
      file.delete()
    }
  }

  @Test
  void testPlotSvgToOutputStream() {
    BarChart chart = BarChart.createVertical('SVG Stream', sampleData(), 'category', ChartType.BASIC, 'value')
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    Plot.svg(chart, baos)
    assertTrue(baos.toString('UTF-8').contains('<svg'), 'SVG output stream should contain an SVG element')
  }

  @Test
  void testPlotSvgToWriter() {
    BarChart chart = BarChart.createVertical('SVG Writer', sampleData(), 'category', ChartType.BASIC, 'value')
    StringWriter writer = new StringWriter()
    Plot.svg(chart, writer)
    assertTrue(writer.toString().contains('<svg'), 'SVG writer should contain an SVG element')
  }

  @Test
  void testPlotSvgRejectsNullTargets() {
    BarChart chart = BarChart.createVertical('SVG Null Targets', sampleData(), 'category', ChartType.BASIC, 'value')

    IllegalArgumentException fileException = assertThrows(IllegalArgumentException) {
      Plot.svg(chart, (File) null)
    }
    assertEquals('file cannot be null', fileException.message)

    IllegalArgumentException outputStreamException = assertThrows(IllegalArgumentException) {
      Plot.svg(chart, (OutputStream) null)
    }
    assertEquals('output stream cannot be null', outputStreamException.message)

    IllegalArgumentException writerException = assertThrows(IllegalArgumentException) {
      Plot.svg(chart, (Writer) null)
    }
    assertEquals('writer cannot be null', writerException.message)
  }

  @Test
  void testJfxReturnsJavafxNode() {
    Assumptions.assumeTrue(
        System.getenv('DISPLAY') != null || 'true' == System.getProperty('headless'),
        'No DISPLAY available; skipping JavaFX test. Run with -Pheadless=true for headless mode.'
    )
    Matrix data = sampleData()
    ScatterChart chart = ScatterChart.create('JFX Test', data, 'category', 'value')
    javafx.scene.Node node = Plot.jfx(chart)
    assertNotNull(node, 'export() should return a non-null Node')
    assertTrue(node instanceof javafx.scene.Node, 'Result should be a javafx.scene.Node')
  }

  @Test
  void testPlotJfxRejectsNullChart() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Plot.jfx((se.alipsa.matrix.pict.Chart) null)
    }
    assertEquals('chart cannot be null', exception.message)
  }

  private static void assertPngHeader(File file) {
    byte[] header = new byte[4]
    file.withInputStream { InputStream is ->
      is.read(header)
    }
    for (int i = 0; i < PNG_HEADER.length; i++) {
      assertEquals(PNG_HEADER[i], header[i], "PNG header byte ${i} should match")
    }
  }

  private static List<String> textContent(Svg svg) {
    svg.descendants().findAll { it instanceof Text }*.content
  }

}
