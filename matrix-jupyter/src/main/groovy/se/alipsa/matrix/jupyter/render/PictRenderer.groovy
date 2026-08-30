package se.alipsa.matrix.jupyter.render

import se.alipsa.matrix.jupyter.AbstractRenderer
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions
import se.alipsa.matrix.pict.CharmBridge
import se.alipsa.matrix.pict.Chart

/** Renders pict charts as inline SVG. */
class PictRenderer extends AbstractRenderer {
  @Override String rendererName() { 'PictRenderer' }
  @Override boolean available() { probe('se.alipsa.matrix.pict.Chart') }
  @Override Set<Class<?>> supportedTypes() { [Chart] as LinkedHashSet }
  @Override String preferredMime() { 'image/svg+xml' }
  @Override MimeBundle render(Object value, RenderOptions options) {
    MimeBundle.svg(SvgSupport.xml(CharmBridge.renderSvg((Chart) value, options.width, options.height)), value.toString())
  }
}
