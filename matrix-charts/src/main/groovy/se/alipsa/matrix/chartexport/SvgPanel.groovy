package se.alipsa.matrix.chartexport

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader

import javax.swing.JPanel
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.nio.charset.StandardCharsets

class SvgPanel extends JPanel {

  private SVGDocument svgDocument
  private final SVGLoader loader = new SVGLoader()

  /**
   * The constructor loads an SVG from a raw String containing the svg.
   */
  SvgPanel(String svgContent) {
    if (svgContent == null || svgContent.isEmpty()) {
      this.svgDocument = null
      repaint()
      return
    }

    ByteArrayInputStream stream = new ByteArrayInputStream(
        svgContent.getBytes(StandardCharsets.UTF_8)
    )
    this.svgDocument = loader.load(stream, null, LoaderContext.createDefault())

    if (svgDocument != null) {
      setPreferredSize(new Dimension(
          (int) svgDocument.size().width,
          (int) svgDocument.size().height
      ))
    }

    revalidate()
    repaint()

  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (svgDocument == null) return;

    Graphics2D g2d = (Graphics2D) g.create();

    // 1. Enable Anti-Aliasing for crisp edges
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

    // 2. Calculate scale to fit the panel (Responsive resizing)
    // If you want the SVG to stretch to fill the panel:
    double svgWidth = svgDocument.size().width
    double svgHeight = svgDocument.size().height

    double panelWidth = getWidth()
    double panelHeight = getHeight()

    // Calculate aspect-preserving scale
    double scale = Math.min(panelWidth / svgWidth, panelHeight / svgHeight);

    // Center the SVG
    double xOffset = (panelWidth - (svgWidth * scale)) / 2
    double yOffset = (panelHeight - (svgHeight * scale)) / 2

    g2d.translate(xOffset, yOffset)
    g2d.scale(scale, scale)

    // 3. Render
    // The first argument is the component (null is usually fine for static images)
    // The second argument is the viewbox (null usually defaults to the whole SVG)
    svgDocument.render(this, g2d, null)
    g2d.dispose()
  }
}
