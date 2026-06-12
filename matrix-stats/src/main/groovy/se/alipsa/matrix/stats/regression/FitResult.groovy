package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Immutable result of a model fit, containing coefficients, diagnostics, and fitted values.
 *
 * <p>Local methods (e.g. loess) have no global coefficients and return empty arrays
 * for {@code coefficients} and {@code standardErrors}. Consumers should check
 * {@code coefficients.length} before accessing coefficient values.
 */
final class FitResult {

  final double[] coefficients
  final double[] standardErrors
  final double[] fittedValues
  final double[] residuals
  final double rSquared
  final List<String> predictorNames

  /**
   * Creates a fit result.
   *
   * @param coefficients the fitted coefficient vector (intercept first if included)
   * @param standardErrors standard errors for each coefficient
   * @param fittedValues predicted values for each observation
   * @param residuals observed minus fitted for each observation
   * @param rSquared coefficient of determination
   * @param predictorNames names corresponding to each coefficient
   */
  FitResult(
    double[] coefficients,
    double[] standardErrors,
    double[] fittedValues,
    double[] residuals,
    double rSquared,
    List<String> predictorNames
  ) {
    this.coefficients = coefficients.clone()
    this.standardErrors = standardErrors.clone()
    this.fittedValues = fittedValues.clone()
    this.residuals = residuals.clone()
    this.rSquared = rSquared
    this.predictorNames = List.copyOf(predictorNames)
  }

  /**
   * Returns the fitted coefficients as immutable {@code BigDecimal} values.
   *
   * @return the coefficient values, or an empty immutable list for local methods without global coefficients
   */
  List<BigDecimal> getCoefficientList() {
    NumericConversion.toBigDecimalList(coefficients, 'coefficients')
  }

  /**
   * Returns the coefficient standard errors as immutable {@code BigDecimal} values.
   *
   * @return the standard-error values, or an empty immutable list when standard errors are not available
   */
  List<BigDecimal> getStandardErrorList() {
    NumericConversion.toBigDecimalList(standardErrors, 'standardErrors')
  }

  /**
   * Returns the fitted values as immutable {@code BigDecimal} values.
   *
   * @return the fitted values
   */
  List<BigDecimal> getFittedList() {
    NumericConversion.toBigDecimalList(fittedValues, 'fittedValues')
  }

  /**
   * Returns the residuals as immutable {@code BigDecimal} values.
   *
   * @return the residual values
   */
  List<BigDecimal> getResidualList() {
    NumericConversion.toBigDecimalList(residuals, 'residuals')
  }

  /**
   * Returns the coefficient of determination as a {@code BigDecimal}.
   *
   * @return the R-squared value
   */
  BigDecimal getRSquaredValue() {
    BigDecimal.valueOf(rSquared)
  }
}
