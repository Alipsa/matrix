package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Supported contrast types for categorical predictor encoding.
 */
@CompileStatic
enum ContrastType {
  /**
   * Treatment contrasts (dummy coding): one level is the reference and omitted.
   * This is the default contrast type.
   */
  TREATMENT,

  /**
   * Sum contrasts: each level is compared to the grand mean.
   * The last level is omitted.
   */
  SUM,

  /**
   * Deviation contrasts: similar to sum contrasts but the omitted level
   * receives the negative sum of the others.
   */
  DEVIATION
}
