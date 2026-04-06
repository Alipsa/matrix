package se.alipsa.matrix.stats.formula

/**
 * Public entry point for formula parsing, normalization, and updates.
 *
 * <p>The formula syntax follows a compact R-style subset built around a response and
 * predictor side separated by {@code ~}. Supported constructs include:
 * <ul>
 *   <li>additive terms: {@code y ~ x + z}</li>
 *   <li>intercept control: {@code y ~ 0 + x}, {@code y ~ x - 1}</li>
 *   <li>interactions: {@code :}, {@code *}, {@code ^}, and nesting via {@code /}</li>
 *   <li>quoted identifiers using backticks, for example {@code `gross margin`}</li>
 *   <li>identity expressions via {@code I(...)}</li>
 *   <li>polynomial expansion via {@code poly(x, n)}</li>
 *   <li>smooth terms via {@code s(x)} and {@code s(x, df)}</li>
 * </ul>
 *
 * <p>Normalization canonicalizes predictor ordering and expands shorthand such as
 * {@code *} and grouped powers into explicit interaction terms. Dot expansion is
 * resolved later by {@link ModelFrame} because it depends on the available data columns.
 */
final class Formula {

  private Formula() {
  }

  /**
   * Parses a formula that explicitly contains a response and predictor side.
   *
   * @param formula the formula source, for example {@code y ~ x + z}
   * @return the parsed formula AST
   */
  static ParsedFormula parse(String formula) {
    FormulaSupport.parse(formula, null)
  }

  /**
   * Parses a formula, defaulting to {@code defaultResponse ~ formula} when no {@code ~} is present.
   *
   * @param formula the formula or predictor-only source
   * @param defaultResponse the default response variable to use when needed
   * @return the parsed formula AST
   */
  static ParsedFormula parse(String formula, String defaultResponse) {
    FormulaSupport.parse(formula, defaultResponse)
  }

  /**
   * Creates a parsed formula directly from response and predictor fragments.
   *
   * @param response the response fragment
   * @param predictors the predictor fragment
   * @return the parsed formula AST
   */
  static ParsedFormula of(String response, String predictors) {
    String safeResponse = requireFragment(response, 'response')
    String safePredictors = requireFragment(predictors, 'predictors')
    parse("${safeResponse} ~ ${safePredictors}")
  }

  /**
   * Normalizes a formula to canonical terms and intercept handling.
   *
   * @param formula the formula source
   * @return the normalized formula
   */
  static NormalizedFormula normalize(String formula) {
    parse(formula).normalize()
  }

  /**
   * Normalizes a formula, defaulting to {@code defaultResponse ~ formula} when no {@code ~} is present.
   *
   * @param formula the formula or predictor-only source
   * @param defaultResponse the response to default when needed
   * @return the normalized formula
   */
  static NormalizedFormula normalize(String formula, String defaultResponse) {
    parse(formula, defaultResponse).normalize()
  }

  /**
   * Returns the canonical normalized formula string.
   *
   * @param formula the formula source
   * @return the canonical normalized formula string
   */
  static String canonicalize(String formula) {
    normalize(formula).asFormulaString()
  }

  /**
   * Updates a formula using R-style dot substitution.
   *
   * @param baseFormula the existing formula
   * @param updateFormula the update expression, for example {@code . ~ . - z + w}
   * @return the updated parsed formula
   */
  static ParsedFormula update(String baseFormula, String updateFormula) {
    FormulaSupport.update(baseFormula, updateFormula)
  }

  private static String requireFragment(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }

}
