package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

/**
 * Marker interface for method-specific fit options. Each fit method defines its
 * own options class (e.g. {@link LoessOptions}, {@link GamOptions}). Methods that
 * need no options (e.g. lm) accept any {@code FitOptions} and ignore it.
 *
 * <p>A singleton {@link #NONE} instance is provided for methods with no options.
 */
@CompileStatic
interface FitOptions {

  /** Empty options for methods that require no configuration. */
  static final FitOptions NONE = new FitOptions() {}
}
