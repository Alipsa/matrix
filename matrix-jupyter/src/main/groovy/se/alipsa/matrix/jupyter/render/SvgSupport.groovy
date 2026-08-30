package se.alipsa.matrix.jupyter.render

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter

import java.util.concurrent.atomic.AtomicLong

/** SVG serialization with a unique namespace. */
@SuppressWarnings('PropertyName')
class SvgSupport {
  static final AtomicLong counter = new AtomicLong()

  static String xml(Svg svg) {
    String prefix = "mjx${counter.incrementAndGet()}-"
    SvgWriter.toXml(svg, prefix)
  }
}
