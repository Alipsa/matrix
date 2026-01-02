package se.alipsa.matrix.chartexport

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart

import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets

class ChartToPng {

  static void export(String svgChart, File targetFile) throws IOException {
    if (svgChart == null || svgChart.isEmpty()) {
      throw new IllegalArgumentException("Invalid SVG content, cannot be null or empty")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    SVGLoader loader = new SVGLoader()
    ByteArrayInputStream svgStream = new ByteArrayInputStream(svgChart.getBytes(StandardCharsets.UTF_8))
    SVGDocument svgDocument = loader.load(svgStream, null, LoaderContext.createDefault())

    if (svgDocument == null) {
      throw new IllegalArgumentException("Invalid SVG document")
    }
    int width = (int) svgDocument.size().width
    int height = (int) svgDocument.size().height

    // TYPE_INT_ARGB is crucial for transparency support
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    Graphics2D g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    svgDocument.render(null, g)
    g.dispose()

    ImageIO.write(image, "png", targetFile)
  }

  static void export(Svg svgChart, File targetFile) throws IOException  {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(svgChart.toXml(), targetFile)
  }

  static void export(GgChart chart, File targetFile) throws IOException  {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(chart.render().toXml(), targetFile)
  }
}
