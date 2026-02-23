package se.alipsa.matrix.gg.export

import groovy.transform.CompileStatic
import org.girod.javafx.svgimage.SVGImage
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToJfx
import se.alipsa.matrix.chartexport.ChartToJpeg
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.chartexport.ChartToSwing
import se.alipsa.matrix.chartexport.SvgPanel
import se.alipsa.matrix.gg.GgChart

import java.awt.image.BufferedImage

/**
 * Convenience export utility for {@link GgChart} instances.
 *
 * <p>Each method renders the chart to SVG and delegates to the
 * corresponding {@code ChartTo*} class in matrix-charts.</p>
 */
@CompileStatic
class GgExport {

  /**
   * Export a {@link GgChart} to a {@link BufferedImage}.
   *
   * @param chart the {@link GgChart} to export
   * @return rendered image
   * @throws IllegalArgumentException if chart is null
   */
  static BufferedImage toImage(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    ChartToImage.export(chart.render())
  }

  /**
   * Export a {@link GgChart} as a PNG image file.
   *
   * @param chart the {@link GgChart} to export
   * @param targetFile the {@link File} where the PNG image will be written
   * @throws IOException if an error occurs during file writing
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void toPng(GgChart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile must not be null")
    }
    ChartToPng.export(chart.render(), targetFile)
  }

  /**
   * Export a {@link GgChart} as a JPEG image file.
   *
   * @param chart the {@link GgChart} to export
   * @param targetFile the {@link File} where the JPEG image will be written
   * @param quality JPEG compression quality (0.0 to 1.0)
   * @throws IllegalArgumentException if chart or targetFile is null
   */
  static void toJpeg(GgChart chart, File targetFile, BigDecimal quality = 1.0) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    if (targetFile == null) {
      throw new IllegalArgumentException("targetFile must not be null")
    }
    ChartToJpeg.export(chart.render(), targetFile, quality)
  }

  /**
   * Export a {@link GgChart} as a Swing {@link SvgPanel}.
   *
   * @param chart the {@link GgChart} to export
   * @return a {@link SvgPanel} displaying the rendered chart
   * @throws IllegalArgumentException if chart is null
   */
  static SvgPanel toSwing(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    ChartToSwing.export(chart.render())
  }

  /**
   * Export a {@link GgChart} as a JavaFX SVGImage node.
   *
   * @param chart the {@link GgChart} to export
   * @return a JavaFX SVGImage representing the rendered chart
   * @throws IllegalArgumentException if chart is null
   */
  static SVGImage toJfx(GgChart chart) {
    if (chart == null) {
      throw new IllegalArgumentException("chart must not be null")
    }
    ChartToJfx.export(chart.render())
  }
}
