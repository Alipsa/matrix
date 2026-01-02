package export

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.GgChart

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths

import static se.alipsa.matrix.gg.GgPlot.*
import static org.junit.jupiter.api.Assertions.*

class ChartToPngTest {

  @TempDir
  Path tempDir;

  @Test
  void testExportToPng() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')
    Path classLocation = Paths.get(
        getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
    )
    Path buildDir = classLocation.getParent().getParent().getParent()
    Path svgPath = buildDir.resolve("testExportToPng.svg")
    File svgFile = svgPath.toFile()
    write(chart, svgFile)
    println("Exported svg to ${svgFile.absolutePath}")
    assertTrue(svgFile.exists())
    Path filePath = buildDir.resolve("testExportToPng.png")
    File file = filePath.toFile()
    ChartToPng.export(chart, file)
    println("Exported png to ${file.absolutePath}")
    
    // Verify file exists
    assertTrue(file.exists())
    
    // Verify file size is greater than zero
    assertTrue(file.length() > 0, "PNG file should not be empty")
    
    // Verify the image can be read successfully using ImageIO
    BufferedImage image = ImageIO.read(file)
    assertNotNull(image, "PNG file should be readable by ImageIO")
    
    // Verify image dimensions are reasonable (greater than zero)
    assertTrue(image.getWidth() > 0, "Image width should be greater than 0")
    assertTrue(image.getHeight() > 0, "Image height should be greater than 0")
  }
}
