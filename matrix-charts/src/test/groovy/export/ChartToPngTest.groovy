package export

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.GgChart

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
    assertTrue(file.exists())
  }

  @Test
  void testExportWithNullTargetFile_String() {
    String svgContent = "<svg></svg>"
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToPng.export(svgContent, null)
    })
    assertEquals("targetFile cannot be null", exception.getMessage())
  }

  @Test
  void testExportWithNullTargetFile_Svg() {
    Svg svg = new Svg()
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToPng.export(svg, null)
    })
    assertEquals("targetFile cannot be null", exception.getMessage())
  }

  @Test
  void testExportWithNullTargetFile_GgChart() {
    def mpg = Dataset.mpg()
    GgChart chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) + geom_point()
    Exception exception = assertThrows(IllegalArgumentException.class, {
      ChartToPng.export(chart, null)
    })
    assertEquals("targetFile cannot be null", exception.getMessage())
  }
}
