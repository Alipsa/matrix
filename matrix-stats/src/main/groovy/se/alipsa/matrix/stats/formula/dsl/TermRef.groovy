package se.alipsa.matrix.stats.formula.dsl

/**
 * Reference to a formula variable or backtick-quoted column name.
 */
class TermRef extends TermExpr {

  final String name
  final boolean quoted

  /**
   * Creates a term reference.
   *
   * @param name the column or variable name
   * @param quoted whether the name must be rendered with backticks
   */
  TermRef(String name, boolean quoted = false) {
    this.name = requireNonBlank(name, 'name')
    this.quoted = quoted
  }

  /**
   * Builds a full formula expression using this term as the response.
   *
   * @param predictors the predictor-side term expression
   * @return the full formula specification
   */
  GroovyFormulaSpec or(TermExpr predictors) {
    new GroovyFormulaSpec(this, requireTerm(predictors, 'predictors'))
  }

  @Override
  String render() {
    if (quoted || !simpleIdentifier(name)) {
      return "`${name.replace('`', '``')}`"
    }
    name
  }

  private static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }

  private static boolean simpleIdentifier(String value) {
    if (value == '.') {
      return false
    }
    char first = value.charAt(0)
    if (!(Character.isLetter(first) || first == '_' || first == '.')) {
      return false
    }
    if (first == '.' && value.length() > 1 && Character.isDigit(value.charAt(1))) {
      return false
    }
    for (int i = 1; i < value.length(); i++) {
      char current = value.charAt(i)
      if (!(Character.isLetterOrDigit(current) || current == '_' || current == '.')) {
        return false
      }
    }
    true
  }
}
