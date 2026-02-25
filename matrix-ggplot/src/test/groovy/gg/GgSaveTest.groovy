package gg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

import testutil.Slow

/**
 * Tests for ggsave methods.
 */
@Slow
class GgSaveTest {

  @TempDir
  Path tempDir

  @Test
  void testGgSaveSingleSvgToSvgFile() {
    def svg = new Svg()
    svg.width(200)
    svg.height(100)
    svg.addRect(200, 100).fill('blue')

    File svgFile = tempDir.resolve("single.svg").toFile()
    ggsave(svgFile.absolutePath, svg)

    assertTrue(svgFile.exists(), "SVG file should be created")
    String content = svgFile.text
    assertTrue(content.contains('<svg'), "File should contain SVG content")
    assertTrue(content.contains('width="200"'), "SVG should have correct width")
    assertTrue(content.contains('height="100"'), "SVG should have correct height")
  }

  @Test
  void testGgSaveSingleSvgToPngFile() {
    def svg = new Svg()
    svg.width(200)
    svg.height(100)
    svg.addRect(200, 100).fill('green')

    File pngFile = tempDir.resolve("single.png").toFile()
    ggsave(pngFile.absolutePath, svg)

    assertTrue(pngFile.exists(), "PNG file should be created")
    assertTrue(pngFile.length() > 0, "PNG file should not be empty")
  }

  @Test
  void testGgSaveMultipleSvgsToSvgFile() {
    def svg1 = new Svg()
    svg1.width(300)
    svg1.height(150)
    svg1.addRect(300, 150).fill('red')
    svg1.addText('Chart 1').x(10).y(20)

    def svg2 = new Svg()
    svg2.width(300)
    svg2.height(150)
    svg2.addRect(300, 150).fill('blue')
    svg2.addText('Chart 2').x(10).y(20)

    File svgFile = tempDir.resolve("multiple.svg").toFile()
    ggsave(svgFile.absolutePath, svg1, svg2)

    assertTrue(svgFile.exists(), "SVG file should be created")
    String content = svgFile.text
    assertTrue(content.contains('<svg'), "File should contain SVG content")
    // Should have combined height (150 + 150 = 300)
    assertTrue(content.contains('height="300"'), "Combined SVG should have total height")
    // Should have max width (300)
    assertTrue(content.contains('width="300"'), "Combined SVG should have max width")
    // Should contain both chart contents
    assertTrue(content.contains('Chart 1'), "Should contain first chart")
    assertTrue(content.contains('Chart 2'), "Should contain second chart")
  }

  @Test
  void testGgSaveMultipleSvgsToPngFile() {
    def svg1 = new Svg()
    svg1.width(200)
    svg1.height(100)
    svg1.addRect(200, 100).fill('yellow')

    def svg2 = new Svg()
    svg2.width(200)
    svg2.height(100)
    svg2.addRect(200, 100).fill('purple')

    File pngFile = tempDir.resolve("multiple.png").toFile()
    ggsave(pngFile.absolutePath, svg1, svg2)

    assertTrue(pngFile.exists(), "PNG file should be created")
    assertTrue(pngFile.length() > 0, "PNG file should not be empty")
  }

  @Test
  void testGgSaveSingleChartToSvgFile() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 1]])
        .types(Integer, Integer)
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_point()

    File svgFile = tempDir.resolve("chart.svg").toFile()
    ggsave(svgFile.absolutePath, chart)

    assertTrue(svgFile.exists(), "SVG file should be created")
    String content = svgFile.text
    assertTrue(content.contains('<svg'), "File should contain SVG content")
    assertTrue(content.contains('<circle'), "Chart should contain points")
  }

  @Test
  void testGgSaveSingleChartToPngFile() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 1]])
        .types(Integer, Integer)
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_line()

    File pngFile = tempDir.resolve("chart.png").toFile()
    ggsave(pngFile.absolutePath, chart)

    assertTrue(pngFile.exists(), "PNG file should be created")
    assertTrue(pngFile.length() > 0, "PNG file should not be empty")
  }

  @Test
  void testGgSaveMultipleChartsToSvgFile() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 1]])
        .types(Integer, Integer)
        .build()

    GgChart chart1 = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        labs(title: 'Chart 1')

    GgChart chart2 = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        labs(title: 'Chart 2')

    File svgFile = tempDir.resolve("charts.svg").toFile()
    ggsave(svgFile.absolutePath, chart1, chart2)

    assertTrue(svgFile.exists(), "SVG file should be created")
    String content = svgFile.text
    assertTrue(content.contains('<svg'), "File should contain SVG content")
    assertTrue(content.contains('Chart 1'), "Should contain first chart title")
    assertTrue(content.contains('Chart 2'), "Should contain second chart title")
  }

  @Test
  void testGgSaveMultipleChartsToPngFile() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 1]])
        .types(Integer, Integer)
        .build()

    GgChart chart1 = ggplot(data, aes(x: 'x', y: 'y')) + geom_point()
    GgChart chart2 = ggplot(data, aes(x: 'x', y: 'y')) + geom_line()

    File pngFile = tempDir.resolve("charts.png").toFile()
    ggsave(pngFile.absolutePath, chart1, chart2)

    assertTrue(pngFile.exists(), "PNG file should be created")
    assertTrue(pngFile.length() > 0, "PNG file should not be empty")
  }

  @Test
  void testGgSaveInvalidExtension() {
    def svg = new Svg()
    svg.width(200)
    svg.height(100)

    File invalidFile = tempDir.resolve("test.pdf").toFile()

    def exception = assertThrows(IllegalArgumentException.class) {
      ggsave(invalidFile.absolutePath, svg)
    }

    assertTrue(exception.message.contains("File extension must be .svg"),
        "Should throw error for invalid extension")
  }

  @Test
  void testGgSaveNoExtension() {
    def svg = new Svg()
    svg.width(200)
    svg.height(100)

    File noExtFile = tempDir.resolve("test_no_extension").toFile()

    def exception = assertThrows(IllegalArgumentException.class) {
      ggsave(noExtFile.absolutePath, svg)
    }

    assertTrue(exception.message.contains("File path must have a valid extension"),
        "Should throw error for file without extension")
  }

  @Test
  void testGgSaveTrailingDot() {
    def svg = new Svg()
    svg.width(200)
    svg.height(100)

    File trailingDotFile = tempDir.resolve("test.").toFile()

    def exception = assertThrows(IllegalArgumentException.class) {
      ggsave(trailingDotFile.absolutePath, svg)
    }

    assertTrue(exception.message.contains("File path must have a valid extension"),
        "Should throw error for file with trailing dot but no extension")
  }

  @Test
  void testGgSaveChartNoExtension() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 1]])
        .types(Integer, Integer)
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_point()
    File noExtFile = tempDir.resolve("chart_no_ext").toFile()

    def exception = assertThrows(IllegalArgumentException.class) {
      ggsave(noExtFile.absolutePath, chart)
    }

    assertTrue(exception.message.contains("File path must have a valid extension"),
        "Should throw error for chart save without extension")
  }

  @Test
  void testGgSaveNoSvgs() {
    File svgFile = tempDir.resolve("empty.svg").toFile()

    def exception = assertThrows(IllegalArgumentException.class) {
      Svg[] emptyArray = []
      ggsave(svgFile.absolutePath, emptyArray)
    }

    assertTrue(exception.message.contains("At least one SVG object must be provided"),
        "Should throw error when no SVGs provided")
  }

  @Test
  void testGgSaveNoCharts() {
    File svgFile = tempDir.resolve("empty.svg").toFile()

    def exception = assertThrows(IllegalArgumentException.class) {
      GgChart[] charts = []
      ggsave(svgFile.absolutePath, charts)
    }

    assertTrue(exception.message.contains("At least one GgChart object must be provided"),
        "Should throw error when no charts provided")
  }

  @Test
  void testCombineVerticallyWithDifferentWidths() {
    // Create SVGs with different widths
    def svg1 = new Svg()
    svg1.width(200)
    svg1.height(100)
    svg1.addRect(200, 100).fill('red')

    def svg2 = new Svg()
    svg2.width(400)  // Wider
    svg2.height(150)
    svg2.addRect(400, 150).fill('blue')

    def svg3 = new Svg()
    svg3.width(300)
    svg3.height(80)
    svg3.addRect(300, 80).fill('green')

    File svgFile = tempDir.resolve("different_widths.svg").toFile()
    ggsave(svgFile.absolutePath, svg1, svg2, svg3)

    assertTrue(svgFile.exists(), "SVG file should be created")
    String content = svgFile.text

    // Should have max width (400)
    assertTrue(content.contains('width="400"'), "Combined SVG should have max width")
    // Should have total height (100 + 150 + 80 = 330)
    assertTrue(content.contains('height="330"'), "Combined SVG should have total height")
  }
}
