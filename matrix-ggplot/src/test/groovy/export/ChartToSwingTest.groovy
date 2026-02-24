package export

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgReader
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.export.GgExport

import se.alipsa.matrix.charm.Charts
import static se.alipsa.matrix.gg.GgPlot.*
import static org.junit.jupiter.api.Assertions.*

import testutil.Slow

@Slow
class ChartToSwingTest {

  @Test
  void testExportFromString() {
    // Create a simple SVG string
    String svgContent = """<svg width="100" height="100" xmlns="http://www.w3.org/2000/svg">
      <circle cx="50" cy="50" r="40" fill="blue" />
    </svg>"""
    
    // Export to SvgPanel
    SvgPanel panel = ChartToSwing.export(svgContent)
    
    // Verify panel is not null
    assertNotNull(panel, "SvgPanel should not be null")
    
    // Verify panel has preferred size set
    assertNotNull(panel.getPreferredSize(), "Panel should have preferred size")
    assertTrue(panel.getPreferredSize().width > 0, "Panel width should be greater than 0")
    assertTrue(panel.getPreferredSize().height > 0, "Panel height should be greater than 0")
  }

  @Test
  void testExportFromSvg() {
    // Create an Svg object
    Svg svg = SvgReader.parse("""<svg width="200" height="150" xmlns="http://www.w3.org/2000/svg">
      <rect x="10" y="10" width="180" height="130" fill="red" />
    </svg>""")
    
    // Export to SvgPanel
    SvgPanel panel = ChartToSwing.export(svg)
    
    // Verify panel is not null
    assertNotNull(panel, "SvgPanel should not be null")
    
    // Verify panel has preferred size set
    assertNotNull(panel.getPreferredSize(), "Panel should have preferred size")
    assertTrue(panel.getPreferredSize().width > 0, "Panel width should be greater than 0")
    assertTrue(panel.getPreferredSize().height > 0, "Panel height should be greater than 0")
  }

  @Test
  void testExportFromGgChart() {
    // Create a GgChart
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

    // Export to SvgPanel via GgExport
    SvgPanel panel = GgExport.toSwing(chart)

    // Verify panel is not null
    assertNotNull(panel, "SvgPanel should not be null")

    // Verify panel has preferred size set
    assertNotNull(panel.getPreferredSize(), "Panel should have preferred size")
    assertTrue(panel.getPreferredSize().width > 0, "Panel width should be greater than 0")
    assertTrue(panel.getPreferredSize().height > 0, "Panel height should be greater than 0")
  }

  @Test
  void testExportWithNullString() {
    String svgContent = null
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToSwing.export(svgContent)
    })
    assertEquals("svgChart must not be null", exception.getMessage())
  }

  @Test
  void testExportWithNullSvg() {
    Svg svg = null
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToSwing.export(svg)
    })
    assertEquals("svgChart must not be null", exception.getMessage())
  }

  @Test
  void testExportWithNullGgChart() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      GgExport.toSwing(null)
    })
    assertEquals("chart must not be null", exception.getMessage())
  }

  @Test
  void testExportWithEmptyString() {
    String svgContent = ""
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToSwing.export(svgContent)
    })
    // The exception will be thrown by SvgPanel constructor
    assertTrue(exception.getMessage().contains("cannot be null or empty"))
  }

  @Test
  void testExportWithInvalidSvg() {
    PrintStream originalErr = System.err
    // Redirect stderr to "nowhere"
    System.setErr(new PrintStream(OutputStream.nullOutputStream()))
    try {
      Exception exception = assertThrows(IllegalArgumentException.class, {
        ChartToSwing.export("This is not valid SVG content")
      })
      // The exception will be thrown by SvgPanel constructor when it fails to load
      assertTrue(exception.getMessage().contains("Failed to load SVG content"))
    } finally {
      System.setErr(originalErr)
    }
  }

  @Test
  void testExportFromCharmChart() {
    CharmChart chart = buildCharmChart()

    SvgPanel panel = ChartToSwing.export(chart)
    assertNotNull(panel, "SvgPanel should not be null")
    assertNotNull(panel.getPreferredSize(), "Panel should have preferred size")
    assertTrue(panel.getPreferredSize().width > 0)
    assertTrue(panel.getPreferredSize().height > 0)
  }

  @Test
  void testExportObjectDispatchCharmChart() {
    CharmChart chart = buildCharmChart()

    SvgPanel panel = ChartToSwing.export((Object) chart)
    assertNotNull(panel, "SvgPanel should not be null via Object dispatch")
  }

  private static CharmChart buildCharmChart() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4]])
        .build()
    Charts.plot(data).mapping(x: 'x', y: 'y').points().build()
  }
}
