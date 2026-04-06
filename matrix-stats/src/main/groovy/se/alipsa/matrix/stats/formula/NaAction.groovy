package se.alipsa.matrix.stats.formula

/**
 * Policy for handling NA (null) values during model frame evaluation.
 */
enum NaAction {

  /** Drop rows containing null values in any formula-referenced column. This is the default. */
  OMIT,

  /** Throw an exception if any formula-referenced column contains null values. */
  FAIL
}
