package export

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.GgChart

import java.awt.image.BufferedImage

import static se.alipsa.matrix.gg.GgPlot.*
import static org.junit.jupiter.api.Assertions.*

class ChartToImageTest {

  @Test
  void testExportToBufferedImage() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')
    Svg svg = chart.render()

    BufferedImage image = ChartToImage.export(svg)

    // Verify image is not null
    assertNotNull(image, "BufferedImage should not be null")

    // Verify image dimensions are reasonable (greater than zero)
    assertTrue(image.getWidth() > 0, "Image width should be greater than 0")
    assertTrue(image.getHeight() > 0, "Image height should be greater than 0")
  }

  @Test
  void testExportWithNullSvgChart() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToImage.export(null)
    })
    assertEquals("chart must not be null", exception.getMessage())
  }
}
