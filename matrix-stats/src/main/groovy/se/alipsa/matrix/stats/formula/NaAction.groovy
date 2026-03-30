package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Policy for handling NA (null) values during model frame evaluation.
 */
@CompileStatic
enum NaAction {

  /** Drop rows containing null values in any formula-referenced column. */
  OMIT,

  /** Throw an exception if any formula-referenced column contains null values. */
  FAIL
}
