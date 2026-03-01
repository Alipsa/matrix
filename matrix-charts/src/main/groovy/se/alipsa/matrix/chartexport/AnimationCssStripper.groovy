package se.alipsa.matrix.chartexport

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgReader

import java.util.regex.Pattern

/**
 * Shared helper for stripping Charm animation CSS from SVG content.
 */
@CompileStatic
final class AnimationCssStripper {

  private static final String CHARM_ANIMATION_MARKER = 'charm-animation'
  private static final Pattern CHARM_ANIMATION_STYLE = Pattern.compile(
      '(?is)<style\\b[^>]*>\\s*(?:<!\\[CDATA\\[\\s*)?/\\*\\s*charm-animation\\s*\\*/.*?(?:\\]\\]>\\s*)?</style>'
  )

  private AnimationCssStripper() {
    throw new UnsupportedOperationException('Utility class')
  }

  static String stripFromXml(String svgXml) {
    if (!containsAnimationMarker(svgXml)) {
      return svgXml
    }
    CHARM_ANIMATION_STYLE.matcher(svgXml).replaceAll('')
  }

  static Svg stripFromSvg(Svg svg) {
    String xml = svg.toXml()
    if (!containsAnimationMarker(xml)) {
      return svg
    }
    String sanitized = stripFromXml(xml)
    sanitized == xml ? svg : SvgReader.parse(sanitized)
  }

  private static boolean containsAnimationMarker(String svgXml) {
    svgXml?.contains(CHARM_ANIMATION_MARKER)
  }
}
