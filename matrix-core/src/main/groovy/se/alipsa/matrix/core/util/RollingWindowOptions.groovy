package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

/**
 * Internal configuration for rolling window operations.
 */
@CompileStatic
class RollingWindowOptions {

  final int window
  final int minPeriods
  final boolean center
  final String by

  /**
   * Create validated rolling options.
   *
   * @param window the rolling window size
   * @param minPeriods the minimum number of values required for a result
   * @param center whether the window should be centered on the current row
   * @param by optional matrix column name used for rolling order
   */
  RollingWindowOptions(int window, int minPeriods, boolean center = false, String by = null) {
    if (window <= 0) {
      throw new IllegalArgumentException("rolling window must be greater than 0 but was ${window}")
    }
    if (minPeriods <= 0) {
      throw new IllegalArgumentException("rolling minPeriods must be greater than 0 but was ${minPeriods}")
    }
    if (minPeriods > window) {
      throw new IllegalArgumentException("rolling minPeriods (${minPeriods}) cannot be greater than window (${window})")
    }
    this.window = window
    this.minPeriods = minPeriods
    this.center = center
    this.by = by
  }

  /**
   * Parse rolling options for a column rolling view.
   *
   * @param options rolling options
   * @return validated rolling options without {@code by}
   */
  static RollingWindowOptions column(Map<String, ?> options) {
    from(options, false)
  }

  /**
   * Parse rolling options for a matrix rolling view.
   *
   * @param options rolling options
   * @return validated rolling options including optional {@code by}
   */
  static RollingWindowOptions matrix(Map<String, ?> options) {
    from(options, true)
  }

  private static RollingWindowOptions from(Map<String, ?> options, boolean allowBy) {
    if (options == null || options.isEmpty()) {
      throw new IllegalArgumentException('rolling options cannot be null or empty')
    }
    Set<String> allowedKeys = allowBy
        ? ['window', 'minPeriods', 'center', 'by'] as Set<String>
        : ['window', 'minPeriods', 'center'] as Set<String>
    Set<String> optionKeys = options.keySet().collect { Object key -> String.valueOf(key) } as Set<String>
    Set<String> unknownKeys = optionKeys.findAll { String key -> !(key in allowedKeys) } as Set<String>
    if (!unknownKeys.isEmpty()) {
      throw new IllegalArgumentException("Unknown rolling option(s): ${unknownKeys.sort().join(', ')}")
    }
    if (!optionKeys.contains('window')) {
      throw new IllegalArgumentException('rolling options must include window')
    }
    int window = intValue(options['window'], 'window')
    int minPeriods = options.containsKey('minPeriods')
        ? intValue(options['minPeriods'], 'minPeriods')
        : window
    boolean center = options.containsKey('center') && booleanValue(options['center'], 'center')
    Object byValue = options['by']
    String by = byValue == null ? null : String.valueOf(byValue)
    new RollingWindowOptions(window, minPeriods, center, by)
  }

  private static int intValue(Object value, String name) {
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("rolling ${name} must be a number but was ${value}")
    }
    try {
      new BigDecimal(String.valueOf(value)).intValueExact()
    } catch (NumberFormatException | ArithmeticException e) {
      throw new IllegalArgumentException("rolling ${name} must be an integer within int range but was ${value}", e)
    }
  }

  private static boolean booleanValue(Object value, String name) {
    if (!(value instanceof Boolean)) {
      throw new IllegalArgumentException("rolling ${name} must be a boolean but was ${value}")
    }
    value as boolean
  }
}
