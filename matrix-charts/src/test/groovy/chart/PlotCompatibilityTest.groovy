package chart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.pict.AreaChart
import se.alipsa.matrix.pict.BarChart
import se.alipsa.matrix.pict.ChartType
import se.alipsa.matrix.pict.Histogram
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.PieChart
import se.alipsa.matrix.pict.Plot
import se.alipsa.matrix.pict.ScatterChart
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*

/**
 * Compatibility tests verifying that {@link Plot} methods produce valid
 * output for each chart type after the Charm rewiring.
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

  @Test
  void testPngToFileWithAreaChart() {
    Matrix data = sampleData()
    AreaChart chart = AreaChart.create('Area PNG', data, 'category', 'value')
    File file = File.createTempFile('AreaChart', '.png')
    try {
      Plot.png(chart, file)
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
      Plot.png(chart, file)
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
      Plot.png(chart, file)
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
      Plot.png(chart, file)
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
      Plot.png(chart, file)
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
      Plot.png(chart, file)
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
    Plot.png(chart, baos)
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
    String uri = Plot.base64(chart)
    assertTrue(uri.startsWith('data:image/png;base64,'), 'Should start with data URI prefix')
    String base64Part = uri.substring('data:image/png;base64,'.length())
    byte[] decoded = Base64.decoder.decode(base64Part)
    assertTrue(decoded.length > 100, 'Decoded PNG should have content')
    for (int i = 0; i < PNG_HEADER.length; i++) {
      assertEquals(PNG_HEADER[i], decoded[i], "PNG header byte ${i} should match in base64 output")
    }
  }

  @Test
  void testJfxReturnsJavafxNode() {
    Matrix data = sampleData()
    ScatterChart chart = ScatterChart.create('JFX Test', data, 'category', 'value')
    javafx.scene.Node node = Plot.jfx(chart)
    assertNotNull(node, 'jfx() should return a non-null Node')
    assertTrue(node instanceof javafx.scene.Node, 'Result should be a javafx.scene.Node')
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
}
