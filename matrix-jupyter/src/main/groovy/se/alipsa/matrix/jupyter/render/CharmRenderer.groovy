package se.alipsa.matrix.jupyter.render

import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.jupyter.AbstractRenderer
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions

/** Renders Charm charts as inline SVG. */
class CharmRenderer extends AbstractRenderer {
  @Override String rendererName() { 'CharmRenderer' }
  @Override boolean available() { probe('se.alipsa.matrix.charm.Chart') }
  @Override Set<Class<?>> supportedTypes() { [Chart, PlotGrid] as LinkedHashSet }
  @Override String preferredMime() { 'image/svg+xml' }
  @Override MimeBundle render(Object value, RenderOptions options) {
    def svg = value instanceof Chart ? ((Chart) value).render(options.width, options.height) : ((PlotGrid) value).render(options.width, options.height)
    MimeBundle.svg(SvgSupport.xml(svg), value.toString())
  }
}
