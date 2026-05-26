package se.alipsa.matrix.charm.render.geom

import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * No-op renderer for blank geom.
 */
class BlankRenderer {

  @SuppressWarnings(['UnusedMethodParameter', 'EmptyMethod'])
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    // no-op
  }

}
