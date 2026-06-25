package se.alipsa.matrix.chartexport

import groovy.transform.CompileDynamic

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgReader
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charm.PlotGrid

import java.awt.image.BufferedImage

/**
 * Exports charts as PDF documents.
 *
 * <p>The current implementation renders the chart through the existing SVG-to-image
 * pipeline and embeds the resulting image on a single PDF page.</p>
 *
 * <p>Accepts SVG strings, {@link Svg} objects, {@link CharmChart} instances,
 * and {@link PlotGrid} grids.</p>
 */
@SuppressWarnings('DuplicateStringLiteral')
class ChartToPdf {

  private static final float PDF_POINT_SCALE = 0.75f  // 72 PDF points / 96 screen pixels

  /**
   * Export an {@link Svg} chart as a PDF file.
   *
   * @param svgChart the SVG chart to export
   * @param targetFile the PDF file to write
   */
  static void export(Svg svgChart, File targetFile) throws IOException {
    if (svgChart == null) {
      throw new IllegalArgumentException('svgChart cannot be null')
    }
    writePdf(ChartToImage.export(svgChart), targetFile)
  }

  /**
   * Export an {@link Svg} chart as PDF to an {@link OutputStream}.
   *
   * @param svgChart the SVG chart to export
   * @param os the output stream to write
   */
  static void export(Svg svgChart, OutputStream os) throws IOException {
    if (svgChart == null) {
      throw new IllegalArgumentException('svgChart cannot be null')
    }
    writePdf(ChartToImage.export(svgChart), os)
  }

  /**
   * Export an SVG string as a PDF file.
   *
   * @param svgChart the SVG content as a {@link String}
   * @param targetFile the PDF file to write
   */
  static void export(String svgChart, File targetFile) throws IOException {
    if (svgChart == null || svgChart.isEmpty()) {
      throw new IllegalArgumentException('svgChart cannot be null or empty')
    }
    export(SvgReader.parse(svgChart), targetFile)
  }

  /**
   * Export an SVG string as PDF to an {@link OutputStream}.
   *
   * @param svgChart the SVG content as a {@link String}
   * @param os the output stream to write
   */
  static void export(String svgChart, OutputStream os) throws IOException {
    if (svgChart == null || svgChart.isEmpty()) {
      throw new IllegalArgumentException('svgChart cannot be null or empty')
    }
    export(SvgReader.parse(svgChart), os)
  }

  /**
   * Export a Charm {@link CharmChart} as a PDF file.
   *
   * @param chart the chart to export
   * @param targetFile the PDF file to write
   */
  static void export(CharmChart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    export(chart.render(), targetFile)
  }

  /**
   * Export a Charm {@link CharmChart} as PDF to an {@link OutputStream}.
   *
   * @param chart the chart to export
   * @param os the output stream to write
   */
  static void export(CharmChart chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    export(chart.render(), os)
  }

  /**
   * Export a {@link PlotGrid} as a PDF file.
   *
   * @param grid the plot grid to export
   * @param targetFile the PDF file to write
   */
  static void export(PlotGrid grid, File targetFile) throws IOException {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    writePdf(ChartToImage.export(grid), targetFile)
  }

  /**
   * Export a {@link PlotGrid} as PDF to an {@link OutputStream}.
   *
   * @param grid the plot grid to export
   * @param os the output stream to write
   */
  static void export(PlotGrid grid, OutputStream os) throws IOException {
    if (grid == null) {
      throw new IllegalArgumentException('grid cannot be null')
    }
    writePdf(ChartToImage.export(grid), os)
  }

  private static void writePdf(BufferedImage image, File targetFile) throws IOException {
    if (targetFile == null) {
      throw new IllegalArgumentException('targetFile cannot be null')
    }
    File parent = targetFile.parentFile
    if (parent != null && !parent.exists()) {
      parent.mkdirs()
    }
    PDDocument document = createDocument(image)
    try {
      document.save(targetFile)
    } finally {
      document.close()
    }
  }

  private static void writePdf(BufferedImage image, OutputStream os) throws IOException {
    if (os == null) {
      throw new IllegalArgumentException('outputStream cannot be null')
    }
    PDDocument document = createDocument(image)
    try {
      document.save(os)
    } finally {
      document.close()
    }
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, Chart, PlotGrid, Svg, or CharSequence)
   * @param targetFile the PDF file to write
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static void export(Object chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid, targetFile)
      case CharmChart -> export(chart as CharmChart, targetFile)
      case Svg -> export(chart as Svg, targetFile)
      case CharSequence -> export(chart.toString(), targetFile)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  /**
   * Fallback that accepts an untyped chart and dispatches to the appropriate typed overload.
   *
   * @param chart a chart object (CharmChart, Chart, PlotGrid, Svg, or CharSequence)
   * @param os the output stream to write
   * @throws IllegalArgumentException if chart is null or of an unsupported type
   */
  @CompileDynamic
  static void export(Object chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    switch (chart) {
      case PlotGrid -> export(chart as PlotGrid, os)
      case CharmChart -> export(chart as CharmChart, os)
      case Svg -> export(chart as Svg, os)
      case CharSequence -> export(chart.toString(), os)
      default -> throw new IllegalArgumentException("Unsupported chart type: ${chart.getClass().name}")
    }
  }

  private static PDDocument createDocument(BufferedImage image) throws IOException {
    if (image == null) {
      throw new IllegalArgumentException('image cannot be null')
    }
    float pageWidth = image.width * PDF_POINT_SCALE
    float pageHeight = image.height * PDF_POINT_SCALE
    PDDocument document = new PDDocument()
    try {
      PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight))
      document.addPage(page)
      PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image)
      PDPageContentStream contentStream = new PDPageContentStream(document, page)
      try {
        contentStream.drawImage(pdfImage, 0, 0, pageWidth, pageHeight)
      } finally {
        contentStream.close()
      }
    } catch (IOException e) {
      try {
        document.close()
      } catch (IOException closeEx) {
        e.addSuppressed(closeEx)
      }
      throw e
    }
    document
  }

}
