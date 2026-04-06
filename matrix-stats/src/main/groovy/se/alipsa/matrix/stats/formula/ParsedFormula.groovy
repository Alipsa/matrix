package se.alipsa.matrix.stats.formula

/**
 * Parsed formula containing the raw expression AST for the response and predictors.
 */
final class ParsedFormula {

  final String source
  final FormulaExpression response
  final FormulaExpression predictors

  /**
   * Creates a parsed formula.
   *
   * @param source the original formula source
   * @param response the parsed response expression
   * @param predictors the parsed predictor expression
   */
  ParsedFormula(String source, FormulaExpression response, FormulaExpression predictors) {
    this.source = requireNonBlank(source, 'source')
    this.response = requireNonNullValue(response, 'response')
    this.predictors = requireNonNullValue(predictors, 'predictors')
  }

  /**
   * Normalizes the formula to canonical terms and intercept handling.
   *
   * @return the normalized formula
   */
  NormalizedFormula normalize() {
    FormulaSupport.normalize(this)
  }

  /**
   * Returns the canonical formula string after normalization.
   *
   * @return a canonical normalized formula string
   */
  String normalizedFormulaString() {
    normalize().asFormulaString()
  }

  @Override
  String toString() {
    "${response.asFormulaString()} ~ ${predictors.asFormulaString()}"
  }

  private static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }

  private static <T> T requireNonNullValue(T value, String label) {
    if (value == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    value
  }
}
