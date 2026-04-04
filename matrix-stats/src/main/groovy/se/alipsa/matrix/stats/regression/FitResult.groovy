package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

/**
 * Immutable result of a model fit, containing coefficients, diagnostics, and fitted values.
 *
 * <p>Local methods (e.g. loess) have no global coefficients and return empty arrays
 * for {@code coefficients} and {@code standardErrors}. Consumers should check
 * {@code coefficients.length} before accessing coefficient values.
 */
@CompileStatic
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
}
