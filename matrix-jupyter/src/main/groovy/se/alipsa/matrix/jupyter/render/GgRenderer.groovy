package se.alipsa.matrix.jupyter.render

import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.jupyter.AbstractRenderer
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions

/** Renders ggplot charts as inline SVG without mutating their dimensions. */
class GgRenderer extends AbstractRenderer {
  @Override String rendererName() { 'GgRenderer' }
  @Override boolean available() { probe('se.alipsa.matrix.gg.GgChart') }
  @Override Set<Class<?>> supportedTypes() { [GgChart] as LinkedHashSet }
  @Override String preferredMime() { 'image/svg+xml' }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.svg(SvgSupport.xml(((GgChart) value).render()), value.toString()) }
}
