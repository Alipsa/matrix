package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

/**
 * Immutable compiled Charm chart.
 */
@CompileStatic
class Chart {

  private final Matrix data
  private final Aes aes
  private final List<Layer> layers
  private final ScaleSpec scale
  private final Theme theme
  private final Facet facet
  private final Coord coord
  private final Labels labels
  private final List<AnnotationSpec> annotations

  /**
   * Creates a new compiled chart model.
   *
   * @param data source matrix
   * @param aes plot-level aesthetics
   * @param layers layer list
   * @param scale scale spec
   * @param theme theme spec
   * @param facet facet spec
   * @param coord coord spec
   * @param labels labels spec
   * @param annotations annotation list
   */
  Chart(
      Matrix data,
      Aes aes,
      List<Layer> layers,
      ScaleSpec scale,
      Theme theme,
      Facet facet,
      Coord coord,
      Labels labels,
      List<AnnotationSpec> annotations
  ) {
    this.data = data
    this.aes = aes
    this.layers = Collections.unmodifiableList(new ArrayList<>(layers))
    this.scale = scale
    this.theme = theme
    this.facet = facet
    this.coord = coord
    this.labels = labels
    this.annotations = Collections.unmodifiableList(new ArrayList<>(annotations))
  }

  /**
   * Returns source data.
   *
   * @return source matrix
   */
  Matrix getData() {
    data
  }

  /**
   * Returns plot-level aesthetics.
   *
   * @return aesthetics
   */
  Aes getAes() {
    aes
  }

  /**
   * Returns immutable layers.
   *
   * @return layer list
   */
  List<Layer> getLayers() {
    layers
  }

  /**
   * Returns scale specification.
   *
   * @return scale spec
   */
  ScaleSpec getScale() {
    scale
  }

  /**
   * Returns theme specification.
   *
   * @return theme spec
   */
  Theme getTheme() {
    theme
  }

  /**
   * Returns facet specification.
   *
   * @return facet spec
   */
  Facet getFacet() {
    facet
  }

  /**
   * Returns coord specification.
   *
   * @return coord spec
   */
  Coord getCoord() {
    coord
  }

  /**
   * Returns labels specification.
   *
   * @return labels spec
   */
  Labels getLabels() {
    labels
  }

  /**
   * Returns immutable annotations.
   *
   * @return annotations list
   */
  List<AnnotationSpec> getAnnotations() {
    annotations
  }

  /**
   * Renders this chart to an SVG object.
   *
   * @return SVG model object
   */
  Svg render() {
    try {
      Svg svg = new Svg()
      svg.width(800)
      svg.height(600)
      svg.viewBox('0 0 800 600')
      return svg
    } catch (Exception e) {
      throw new CharmRenderException("Failed to render Charm chart: ${e.message}", e)
    }
  }

  /**
   * Writes rendered SVG to the target file path.
   *
   * @param targetPath path to output `.svg` file
   */
  void writeTo(String targetPath) {
    if (targetPath == null || targetPath.trim().isEmpty()) {
      throw new IllegalArgumentException('targetPath cannot be blank')
    }
    writeTo(new File(targetPath))
  }

  /**
   * Writes rendered SVG to a target file.
   *
   * @param targetFile output file
   */
  void writeTo(File targetFile) {
    if (targetFile == null) {
      throw new IllegalArgumentException('targetFile cannot be null')
    }
    targetFile.text = SvgWriter.toXmlPretty(render())
  }
}
