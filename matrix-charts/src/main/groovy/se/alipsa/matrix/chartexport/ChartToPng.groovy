package se.alipsa.matrix.chartexport

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.pict.Chart
import se.alipsa.matrix.pict.CharmBridge

import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Exports charts as PNG images.
 *
 * <p>Accepts SVG strings, {@link Svg} objects,
 * and {@link CharmChart} instances. All paths converge through SVG rendering
 * via jsvg, with no JavaFX toolkit dependency.</p>
 *
 * <p>For GgPlot export, see {@code se.alipsa.matrix.gg.export.GgExport} in matrix-ggplot.</p>
 */
class ChartToPng {

  private static final Pattern CHARM_ANIMATION_STYLE = Pattern.compile(
      '(?is)<style\\b[^>]*>\\s*/\\*\\s*charm-animation\\s*\\*/.*?</style>'
  )

  /**
   * Export an SVG chart as a PNG image file.
   *
   * @param svgChart the SVG content as a {@link String}
   * @param targetFile the {@link File} where the PNG image will be written
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if svgChart is null or empty, or targetFile is null, or if the SVG document is invalid
   */
  static void export(String svgChart, File targetFile) throws IOException {
    if (svgChart == null || svgChart.isEmpty()) {
      throw new IllegalArgumentException("svgChart cannot be null or empty")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    ImageIO.write(renderToImage(stripAnimationCss(svgChart)), "png", targetFile)
  }

  /**
   * Export an SVG chart as PNG to an {@link OutputStream}.
   *
   * @param svgChart the SVG content as a {@link String}
   * @param os the output stream to write the PNG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if svgChart is null or empty, or os is null
   */
  static void export(String svgChart, OutputStream os) throws IOException {
    if (svgChart == null || svgChart.isEmpty()) {
      throw new IllegalArgumentException("svgChart cannot be null or empty")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    ImageIO.write(renderToImage(stripAnimationCss(svgChart)), "png", os)
  }

  /**
   * Export an {@link Svg} chart as a PNG image file.
   *
   * @param svgChart the {@link Svg} object containing the chart
   * @param targetFile the {@link File} where the PNG image will be written
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if svgChart or targetFile is null
   */
  static void export(Svg svgChart, File targetFile) throws IOException {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(svgChart.toXml(), targetFile)
  }

  /**
   * Export an {@link Svg} chart as PNG to an {@link OutputStream}.
   *
   * @param svgChart the {@link Svg} object containing the chart
   * @param os the output stream to write the PNG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if svgChart or os is null
   */
  static void export(Svg svgChart, OutputStream os) throws IOException {
    if (svgChart == null) {
      throw new IllegalArgumentException("svgChart cannot be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    export(svgChart.toXml(), os)
  }

  /**
   * Export a Charm {@link CharmChart} as a PNG image file.
   *
   * @param chart the Charm chart to export
   * @param targetFile the {@link File} where the PNG image will be written
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(CharmChart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(chart.render(), targetFile)
  }

  /**
   * Export a Charm {@link CharmChart} as PNG to an {@link OutputStream}.
   *
   * @param chart the Charm chart to export
   * @param os the output stream to write the PNG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if chart or os is null
   */
  static void export(CharmChart chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    export(chart.render(), os)
  }

  /**
   * Export a legacy {@link Chart} (e.g. BarChart, ScatterChart) as a PNG image file.
   *
   * @param chart the legacy chart to export
   * @param targetFile the {@link File} where the PNG image will be written
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void export(Chart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile cannot be null")
    }
    export(CharmBridge.convert(chart).render(), targetFile)
  }

  /**
   * Export a legacy {@link Chart} (e.g. BarChart, ScatterChart) as PNG to an {@link OutputStream}.
   *
   * @param chart the legacy chart to export
   * @param os the output stream to write the PNG to
   * @throws IOException if an error occurs during writing
   * @throws IllegalArgumentException if chart or os is null
   */
  static void export(Chart chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart cannot be null")
    }
    if (os == null) {
      throw new IllegalArgumentException("outputStream cannot be null")
    }
    export(CharmBridge.convert(chart).render(), os)
  }

  /**
   * Renders SVG content to a {@link BufferedImage} using jsvg.
   *
   * @param svgContent the SVG XML string
   * @return rendered image with transparency support
   * @throws IllegalArgumentException if the SVG document is invalid or has non-positive dimensions
   */
  private static BufferedImage renderToImage(String svgContent) {
    SVGLoader loader = new SVGLoader()
    ByteArrayInputStream svgStream = new ByteArrayInputStream(svgContent.getBytes(StandardCharsets.UTF_8))
    SVGDocument svgDocument = loader.load(svgStream, null, LoaderContext.createDefault())
    if (svgDocument == null) {
      throw new IllegalArgumentException("Invalid SVG document")
    }
    int width = (svgDocument.size().width as BigDecimal).ceil() as int
    int height = (svgDocument.size().height as BigDecimal).ceil() as int
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException(
          "SVG document has non-positive dimensions (${width}x${height}); " +
          "ensure the SVG specifies width/height attributes or a valid viewBox"
      )
    }
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    Graphics2D g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    svgDocument.render(null, g)
    g.dispose()
    image
  }

  private static String stripAnimationCss(String svgContent) {
    CHARM_ANIMATION_STYLE.matcher(svgContent).replaceAll('')
  }
}
