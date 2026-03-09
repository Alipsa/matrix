package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

/**
 * Internal helper for matrix comparison and validation operations.
 */
@CompileStatic
class ComparisonHelper {

  private static final Logger log = Logger.getLogger(ComparisonHelper)

  /**
   * Handle comparison errors by either throwing or logging.
   *
   * @param msg the error message
   * @param throwException whether to throw an IllegalArgumentException instead of logging
   */
  static void handleError(String msg, boolean throwException) {
    if (throwException) {
      throw new IllegalArgumentException(msg)
    } else {
      log.info(msg)
    }
  }

  /**
   * Determine whether the supplied row contains any non-null values.
   *
   * @param row the row values to inspect
   * @return true if at least one value is non-null, otherwise false
   */
  static boolean containsValues(Iterable row) {
    def strVal
    for (def element in row) {
      strVal = String.valueOf(element)
      if (element != null && strVal != 'null' && !strVal.isBlank()) return true
    }
    false
  }
}
