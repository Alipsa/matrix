package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

import se.alipsa.matrix.stats.linear.MatrixAlgebra

/**
 * Ordinary least squares multiple linear regression for a fixed design matrix.
 */
@CompileStatic
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

    double[][] transpose = MatrixAlgebra.transpose(predictors)
    double[][] xtx = MatrixAlgebra.multiply(transpose, predictors)
    double[][] xtxInverse = MatrixAlgebra.inverse(xtx)
    double[] xty = multiply(transpose, response)

    coefficients = multiply(xtxInverse, xty)
    residualSumOfSquares = calculateResidualSumOfSquares(response, predictors, coefficients)

    int degreesOfFreedom = observationCount - predictorCount
    if (degreesOfFreedom <= 0) {
      throw new IllegalArgumentException(
        "Ordinary least squares requires more observations (${observationCount}) than predictors (${predictorCount})"
      )
    }
    errorVariance = residualSumOfSquares / degreesOfFreedom
    standardErrors = calculateStandardErrors(xtxInverse, errorVariance)
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
   * Returns the standard errors for the fitted coefficients.
   *
   * @return a defensive copy of the standard-error vector
   */
  double[] getStandardErrors() {
    standardErrors.clone()
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
   * Returns the estimated residual variance.
   *
   * @return the residual variance estimate
   */
  double getErrorVariance() {
    errorVariance
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

  private static double[] multiply(double[][] matrix, double[] vector) {
    double[] result = new double[matrix.length]
    for (int i = 0; i < matrix.length; i++) {
      double sum = 0.0d
      for (int j = 0; j < vector.length; j++) {
        sum += matrix[i][j] * vector[j]
      }
      result[i] = sum
    }
    result
  }

  private static double calculateResidualSumOfSquares(double[] response, double[][] predictors, double[] beta) {
    double rss = 0.0d
    for (int i = 0; i < response.length; i++) {
      double fitted = 0.0d
      for (int j = 0; j < beta.length; j++) {
        fitted += predictors[i][j] * beta[j]
      }
      double residual = response[i] - fitted
      rss += residual * residual
    }
    rss
  }

  private static double[] calculateStandardErrors(double[][] xtxInverse, double variance) {
    double[] result = new double[xtxInverse.length]
    for (int i = 0; i < xtxInverse.length; i++) {
      double diagonal = Math.max(0.0d, xtxInverse[i][i])
      result[i] = Math.sqrt(variance * diagonal)
    }
    result
  }
}
