package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

/**
 * Internal configuration for rolling window operations.
 */
@CompileStatic
class RollingWindowOptions {
  private static final String WINDOW_OPTION = 'window'
  private static final String MIN_PERIODS_OPTION = 'minPeriods'
  private static final String CENTER_OPTION = 'center'
  private static final String BY_OPTION = 'by'
  private static final Set<String> COLUMN_OPTION_KEYS = [WINDOW_OPTION, MIN_PERIODS_OPTION, CENTER_OPTION] as Set<String>
  private static final Set<String> MATRIX_OPTION_KEYS = [WINDOW_OPTION, MIN_PERIODS_OPTION, CENTER_OPTION, BY_OPTION] as Set<String>

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
    Map<String, ?> normalizedOptions = [:]
    options.each { Object key, Object value ->
      normalizedOptions[String.valueOf(key)] = value
    }
    Set<String> allowedKeys = allowBy ? MATRIX_OPTION_KEYS : COLUMN_OPTION_KEYS
    Set<String> optionKeys = normalizedOptions.keySet()
    Set<String> unknownKeys = optionKeys.findAll { String key -> !(key in allowedKeys) } as Set<String>
    if (!unknownKeys.isEmpty()) {
      throw new IllegalArgumentException("Unknown rolling option(s): ${unknownKeys.sort().join(', ')}")
    }
    if (!optionKeys.contains(WINDOW_OPTION)) {
      throw new IllegalArgumentException("rolling options must include ${WINDOW_OPTION}")
    }
    int window = intValue(normalizedOptions[WINDOW_OPTION], WINDOW_OPTION)
    int minPeriods = normalizedOptions.containsKey(MIN_PERIODS_OPTION)
        ? intValue(normalizedOptions[MIN_PERIODS_OPTION], MIN_PERIODS_OPTION)
        : window
    boolean center = normalizedOptions.containsKey(CENTER_OPTION) && booleanValue(normalizedOptions[CENTER_OPTION], CENTER_OPTION)
    Object byValue = normalizedOptions[BY_OPTION]
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
