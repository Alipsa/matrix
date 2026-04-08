package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.stats.util.LeastSquaresKernel
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Ordinary least squares multiple linear regression for a fixed design matrix.
 *
 * This implementation solves the normal equations {@code (X'X)^-1 X'y}, which is sufficient for the
 * current low-dimensional stats callers such as {@code Adf}. If this class grows into a broader
 * regression utility for higher-dimensional or more ill-conditioned problems, it should move to a
 * QR- or SVD-based solver rather than relying on the normal equations. The public Groovy-facing
 * API remains idiomatic, while the dense least-squares kernel now runs in Java for the section-4.6
 * hotspot decision.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class MultipleLinearRegression {

  private final double[] coefficients
  private final double[] standardErrors
  private final double residualSumOfSquares
  private final double errorVariance
  private final int observationCount
  private final int predictorCount

  /**
   * Fits an ordinary least squares model.
   *
   * @param response the response vector
   * @param predictors the design matrix
   * @throws IllegalArgumentException if the inputs are invalid or the model is not estimable
   */
  MultipleLinearRegression(double[] response, double[][] predictors) {
    validateInputs(response, predictors)

    observationCount = response.length
    predictorCount = predictors[0].length

    LeastSquaresKernel.OlsComputation computation = LeastSquaresKernel.fitMultipleLinearRegression(response, predictors)
    coefficients = computation.coefficients()
    standardErrors = computation.standardErrors()
    residualSumOfSquares = computation.residualSumOfSquares()
    errorVariance = computation.errorVariance()
  }

  /**
   * Fits an ordinary least squares model from idiomatic Groovy-facing inputs.
   *
   * @param response the response vector
   * @param predictors the design matrix rows
   * @throws IllegalArgumentException if the inputs are invalid or the model is not estimable
   */
  MultipleLinearRegression(List<? extends Number> response, List<? extends List<? extends Number>> predictors) {
    this(
      NumericConversion.toDoubleArray(response, 'response'),
      NumericConversion.toDoubleMatrix(predictors, 'predictors')
    )
  }

  /**
   * Returns the fitted regression coefficients.
   *
   * @return a defensive copy of the coefficient vector
   */
  double[] getCoefficients() {
    coefficients.clone()
  }

  /**
   * Returns the fitted regression coefficients as immutable {@code BigDecimal} values.
   *
   * @return the coefficient values
   */
  List<BigDecimal> getCoefficientValues() {
    NumericConversion.toBigDecimalList(coefficients, 'coefficients')
  }

  /**
   * Returns the standard errors for the fitted coefficients.
   *
   * @return a defensive copy of the standard-error vector
   */
  double[] getStandardErrors() {
    standardErrors.clone()
  }

  /**
   * Returns the coefficient standard errors as immutable {@code BigDecimal} values.
   *
   * @return the standard-error values
   */
  List<BigDecimal> getStandardErrorValues() {
    NumericConversion.toBigDecimalList(standardErrors, 'standardErrors')
  }

  /**
   * Returns the residual sum of squares.
   *
   * @return the residual sum of squares
   */
  double getResidualSumOfSquares() {
    residualSumOfSquares
  }

  /**
   * Returns the residual sum of squares as {@code BigDecimal}.
   *
   * @return the residual sum of squares
   */
  BigDecimal getResidualSumOfSquaresValue() {
    BigDecimal.valueOf(residualSumOfSquares)
  }

  /**
   * Returns the estimated residual variance.
   *
   * @return the residual variance estimate
   */
  double getErrorVariance() {
    errorVariance
  }

  /**
   * Returns the residual variance estimate as {@code BigDecimal}.
   *
   * @return the residual variance estimate
   */
  BigDecimal getErrorVarianceValue() {
    BigDecimal.valueOf(errorVariance)
  }

  /**
   * Returns the number of observations used for fitting.
   *
   * @return the observation count
   */
  int getObservationCount() {
    observationCount
  }

  /**
   * Returns the number of predictors in the design matrix.
   *
   * @return the predictor count
   */
  int getPredictorCount() {
    predictorCount
  }

  private static void validateInputs(double[] response, double[][] predictors) {
    if (response == null || predictors == null) {
      throw new IllegalArgumentException("Response vector and design matrix cannot be null")
    }
    if (response.length == 0) {
      throw new IllegalArgumentException("Response vector cannot be empty")
    }
    if (predictors.length != response.length) {
      throw new IllegalArgumentException(
        "Design matrix row count (${predictors.length}) must match response length (${response.length})"
      )
    }
    if (predictors.length == 0 || predictors[0].length == 0) {
      throw new IllegalArgumentException("Design matrix must contain at least one predictor")
    }

    int columnCount = predictors[0].length
    if (response.length <= columnCount) {
      throw new IllegalArgumentException(
        "Ordinary least squares requires more observations (${response.length}) than predictors (${columnCount})"
      )
    }
    for (double[] row : predictors) {
      if (row.length != columnCount) {
        throw new IllegalArgumentException("Design matrix rows must all have the same length")
      }
    }
  }
}
