package se.alipsa.matrix.charm.facet

import groovy.transform.CompileStatic

/**
 * Parser for ggplot2-style formula strings.
 *
 * Formulas define row and column faceting variables using the tilde (~) separator:
 * - "year ~ drv"       - rows by year, columns by drv
 * - ". ~ drv" or "~ drv" - columns only (no rows)
 * - "year ~ ."         - rows only (no columns)
 * - "year + cyl ~ drv" - multiple row variables
 * - "year ~ drv + gear" - multiple column variables
 *
 * The dot (.) represents "no variable" on that side.
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
    if (formula == null) {
      throw new IllegalArgumentException("Formula cannot be null")
    }

    String trimmed = formula.trim()
    if (!trimmed.contains('~')) {
      throw new IllegalArgumentException("Formula must contain '~' separator: ${formula}")
    }

    // Split on ~ to get left (rows) and right (cols) sides
    int tildeIndex = trimmed.indexOf('~')
    String leftSide = trimmed.substring(0, tildeIndex).trim()
    String rightSide = trimmed.substring(tildeIndex + 1).trim()

    List<String> rows = parseVariables(leftSide)
    List<String> cols = parseVariables(rightSide)

    [rows: rows, cols: cols]
  }

  /**
   * Parse one side of the formula into a list of variable names.
   * Handles:
   * - Empty string or "." -> empty list
   * - Single variable -> single-element list
   * - Multiple variables separated by "+" -> multi-element list
   *
   * @param side One side of the formula (before or after ~)
   * @return List of variable names
   */
  private static List<String> parseVariables(String side) {
    if (side == null || side.isEmpty() || side == '.') {
      return []
    }

    // Split on + for multiple variables
    side.split('\\+')
        .collect { it.trim() }
        .findAll { it && it != '.' } as List<String>
  }
}
