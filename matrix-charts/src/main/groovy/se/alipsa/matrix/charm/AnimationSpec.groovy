package se.alipsa.matrix.charm

/**
 * CSS animation specification for Charm SVG output.
 */
@SuppressWarnings('DuplicateStringLiteral')
class AnimationSpec {

  boolean enabled = true
  String selector = '.charm-data-layer *'
  String name = 'charm-fade-in'
  String duration = '0.8s'
  String timingFunction = 'ease-out'
  String delay = '0s'
  String iterationCount = '1'
  String direction = 'normal'
  String fillMode = 'both'
  String playState = 'running'
  String keyframes = 'from { opacity: 0; } to { opacity: 1; }'

  /**
   * Returns true when this animation spec can be rendered to CSS.
   *
   * @return true if enabled with selector and keyframes
   */
  boolean isActive() {
    enabled && normalized(selector, '') && normalized(keyframes, '')
  }

  /**
   * Converts this animation spec into a CSS style block body scoped to the chart's
   * {@code #charm-root} SVG element. CharmRenderer creates that element when a chart
   * has an active animation or stylesheet, so this output is intended for rendered
   * Charm charts rather than standalone CSS.
   *
   * @return CSS text
   */
  String toCss() {
    validateForCdata()
    String keyframeName = normalized(name, 'charm-fade-in')
    String targetSelector = normalized(selector, '.charm-data-layer *')
    String keyframeBody = normalized(keyframes, 'from { opacity: 0; } to { opacity: 1; }')
    String animationDuration = normalized(duration, '0.8s')
    String animationTiming = normalized(timingFunction, 'ease-out')
    String animationDelay = normalized(delay, '0s')
    String animationIterations = normalized(iterationCount, '1')
    String animationDirection = normalized(direction, 'normal')
    String animationFill = normalized(fillMode, 'both')
    String animationPlayState = normalized(playState, 'running')

    """/* charm-animation */
@keyframes ${keyframeName} {
  ${keyframeBody}
}
${scopedSelector(targetSelector)} {
  animation-name: ${keyframeName};
  animation-duration: ${animationDuration};
  animation-timing-function: ${animationTiming};
  animation-delay: ${animationDelay};
  animation-iteration-count: ${animationIterations};
  animation-direction: ${animationDirection};
  animation-fill-mode: ${animationFill};
  animation-play-state: ${animationPlayState};
}
"""
  }

  /**
   * Creates a copy of this animation spec.
   *
   * @return copied animation spec
   */
  AnimationSpec copy() {
    new AnimationSpec(
        enabled: enabled,
        selector: selector,
        name: name,
        duration: duration,
        timingFunction: timingFunction,
        delay: delay,
        iterationCount: iterationCount,
        direction: direction,
        fillMode: fillMode,
        playState: playState,
        keyframes: keyframes
    )
  }

  private static String normalized(String value, String fallback) {
    String trimmed = value?.trim()
    trimmed ? trimmed : fallback
  }

  private static String scopedSelector(String selector) {
    topLevelSelectors(selector).collect { String target ->
      scopeSelector(target)
    }.join(', ')
  }

  private static String scopeSelector(String target) {
    if (target ==~ /#charm-root(?:\s|[>+~.:#\[]|$).*/) {
      return target
    }
    String withoutSvgRoot = target.replaceFirst(/^(?:svg|:root)(?:\s+|$)/, '').trim()
    withoutSvgRoot ? "#charm-root ${withoutSvgRoot}" : '#charm-root'
  }

  private static List<String> topLevelSelectors(String selector) {
    List<String> result = []
    StringBuilder part = new StringBuilder()
    int parens = 0
    int brackets = 0
    String quote = null
    boolean escaped = false
    selector.each { String character ->
      boolean split = false
      if (escaped) {
        escaped = false
      } else if (character == '\\') {
        escaped = true
      } else if (quote != null) {
        if (character == quote) {
          quote = null
        }
      } else if (character in ['"', "'"]) {
        quote = character
      } else if (character == '(') {
        parens++
      } else if (character == ')') {
        parens--
      } else if (character == '[') {
        brackets++
      } else if (character == ']') {
        brackets--
      } else if (character == ',' && parens == 0 && brackets == 0) {
        split = true
      }
      if (split) {
        if (part.toString().trim()) {
          result << part.toString().trim()
        }
        part.setLength(0)
      } else {
        part.append(character)
      }
    }
    if (part.toString().trim()) {
      result << part.toString().trim()
    }
    result ?: ['#charm-root']
  }

  private void validateForCdata() {
    ensureNoCdataTerminator(selector, 'selector')
    ensureNoCdataTerminator(name, 'name')
    ensureNoCdataTerminator(duration, 'duration')
    ensureNoCdataTerminator(timingFunction, 'timingFunction')
    ensureNoCdataTerminator(delay, 'delay')
    ensureNoCdataTerminator(iterationCount, 'iterationCount')
    ensureNoCdataTerminator(direction, 'direction')
    ensureNoCdataTerminator(fillMode, 'fillMode')
    ensureNoCdataTerminator(playState, 'playState')
    ensureNoCdataTerminator(keyframes, 'keyframes')
  }

  private static void ensureNoCdataTerminator(String value, String fieldName) {
    if (value?.contains(']]>')) {
      throw new IllegalArgumentException("Animation field '${fieldName}' must not contain ']]>'")
    }
  }

}
