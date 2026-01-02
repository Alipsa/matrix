package export

import org.girod.javafx.svgimage.SVGImage
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToJfx
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.GgChart

import static se.alipsa.matrix.gg.GgPlot.*
import static org.junit.jupiter.api.Assertions.*

class ChartToJfxTest {

  @Test
  void testExportFromString() {
    String svgContent = """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
      <rect width="200" height="100" style="fill:rgb(0,0,255);stroke-width:1;stroke:rgb(0,0,0)" />
    </svg>"""
    
    SVGImage svgImage = ChartToJfx.export(svgContent)
    
    assertNotNull(svgImage, "SVGImage should not be null")
    assertNotNull(svgImage.getSVGDocument(), "SVG document should not be null")
  }

  @Test
  void testExportFromSvg() {
    Svg svg = new Svg()
    svg.rect(width: 200, height: 100, style: 'fill:rgb(0,0,255);stroke-width:1;stroke:rgb(0,0,0)')
    
    SVGImage svgImage = ChartToJfx.export(svg)
    
    assertNotNull(svgImage, "SVGImage should not be null")
    assertNotNull(svgImage.getSVGDocument(), "SVG document should not be null")
  }

  @Test
  void testExportFromGgChart() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')
    
    SVGImage svgImage = ChartToJfx.export(chart)
    
    assertNotNull(svgImage, "SVGImage should not be null")
    assertNotNull(svgImage.getSVGDocument(), "SVG document should not be null")
  }

  @Test
  void testExportWithNullString() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToJfx.export((String) null)
    })
    assertEquals("svgChart must not be null", exception.getMessage())
  }

  @Test
  void testExportWithNullSvg() {
    // The export(Svg) method currently doesn't validate null, so it will throw NullPointerException
    // when calling chart.toXml() on null
    Exception exception = assertThrows(NullPointerException.class, {
      ChartToJfx.export((Svg) null)
    })
    assertNotNull(exception)
  }

  @Test
  void testExportWithNullGgChart() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToJfx.export((GgChart) null)
    })
    assertEquals("chart must not be null", exception.getMessage())
  }
}
