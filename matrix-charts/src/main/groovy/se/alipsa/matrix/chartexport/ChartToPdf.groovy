package se.alipsa.matrix.chartexport

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart as CharmChart
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.pict.Chart

import java.awt.image.BufferedImage

/**
 * Exports charts as PDF documents.
 *
 * <p>The current implementation renders the chart through the existing SVG-to-image
 * pipeline and embeds the resulting image on a single PDF page.</p>
 */
@SuppressWarnings('DuplicateStringLiteral')
class ChartToPdf {

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
   * Export a legacy {@link Chart} as a PDF file.
   *
   * @param chart the legacy chart to export
   * @param targetFile the PDF file to write
   */
  static void export(Chart chart, File targetFile) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    writePdf(ChartToImage.export(chart), targetFile)
  }

  /**
   * Export a legacy {@link Chart} as PDF to an {@link OutputStream}.
   *
   * @param chart the legacy chart to export
   * @param os the output stream to write
   */
  static void export(Chart chart, OutputStream os) throws IOException {
    if (chart == null) {
      throw new IllegalArgumentException('chart cannot be null')
    }
    writePdf(ChartToImage.export(chart), os)
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

  private static PDDocument createDocument(BufferedImage image) throws IOException {
    if (image == null) {
      throw new IllegalArgumentException('image cannot be null')
    }
    PDDocument document = new PDDocument()
    PDPage page = new PDPage(new PDRectangle(image.width as float, image.height as float))
    document.addPage(page)
    PDImageXObject pdfImage = LosslessFactory.createFromImage(document, image)
    PDPageContentStream contentStream = new PDPageContentStream(document, page)
    try {
      contentStream.drawImage(pdfImage, 0, 0, image.width as float, image.height as float)
    } finally {
      contentStream.close()
    }
    document
  }

}
