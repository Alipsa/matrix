package se.alipsa.matrix.gg.facet

import groovy.transform.CompileStatic

/**
 * Parser for ggplot2-style formula strings.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.facet.FormulaParser} instead.
 *             This class delegates to the charm package implementation.
 */
@CompileStatic
class FormulaParser {

  /**
   * Parse a formula string into row and column variable lists.
   *
   * @param formula Formula string (e.g., "year ~ drv", "~ class", "cyl ~ .")
   * @return Map with 'rows' and 'cols' keys containing List<String> of variable names
   * @throws IllegalArgumentException if formula is null or doesn't contain ~
   */
  static Map<String, List<String>> parse(String formula) {
    se.alipsa.matrix.charm.facet.FormulaParser.parse(formula)
  }
}
