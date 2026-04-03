package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Shared utility methods for fit method implementations.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
final class FitUtils {

  private FitUtils() {}

  /**
   * Converts a list of numbers to a primitive double array.
   *
   * @param values the number list
   * @return the double array
   */
  static double[] toDoubleArray(List<Number> values) {
    double[] result = new double[values.size()]
    for (int i = 0; i < values.size(); i++) {
      result[i] = values[i].doubleValue()
    }
    result
  }

  /**
   * Computes the coefficient of determination (R-squared).
   *
   * @param response the observed response values
   * @param residuals the residuals (observed - fitted)
   * @return the R-squared value
   */
  static double computeRSquared(double[] response, double[] residuals) {
    double mean = 0.0d
    for (double v : response) {
      mean += v
    }
    mean /= response.length
    double ssTotal = 0.0d
    double ssResidual = 0.0d
    for (int i = 0; i < response.length; i++) {
      ssTotal += (response[i] - mean) * (response[i] - mean)
      ssResidual += residuals[i] * residuals[i]
    }
    ssTotal == 0.0d ? 0.0d : 1.0d - ssResidual / ssTotal
  }

  /**
   * Builds a design array from a Matrix, optionally prepending an intercept column.
   *
   * @param designMatrix the predictor data matrix
   * @param n number of rows
   * @param addIntercept whether to prepend a column of ones
   * @return the design array
   */
  static double[][] buildDesignArray(Matrix designMatrix, int n, boolean addIntercept) {
    int ncol = designMatrix.columnCount()
    int totalCols = addIntercept ? ncol + 1 : ncol
    double[][] predictors = new double[n][totalCols]
    for (int i = 0; i < n; i++) {
      int col = 0
      if (addIntercept) {
        predictors[i][col++] = 1.0d
      }
      for (int j = 0; j < ncol; j++) {
        Object val = designMatrix[i, j]
        predictors[i][col++] = val != null ? (val as Number).doubleValue() : 0.0d
      }
    }
    predictors
  }

  /**
   * Builds the predictor name list, optionally prepending '(Intercept)'.
   *
   * @param addIntercept whether to include the intercept name
   * @param predictorNames the predictor column names
   * @return the full name list
   */
  static List<String> buildPredictorNames(boolean addIntercept, List<String> predictorNames) {
    List<String> names = []
    if (addIntercept) {
      names << '(Intercept)'
    }
    names.addAll(predictorNames)
    names
  }

  /**
   * Multiplies a matrix by a vector.
   *
   * @param matrix the matrix (m x n)
   * @param vector the vector (length n)
   * @return the result vector (length m)
   */
  static double[] multiplyVec(double[][] matrix, double[] vector) {
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

  /**
   * Computes fitted values from the design matrix and coefficients.
   *
   * @param predictors the design matrix
   * @param coefficients the coefficient vector
   * @return the fitted values
   */
  static double[] computeFitted(double[][] predictors, double[] coefficients) {
    int n = predictors.length
    double[] fitted = new double[n]
    for (int i = 0; i < n; i++) {
      double sum = 0.0d
      for (int j = 0; j < coefficients.length; j++) {
        sum += predictors[i][j] * coefficients[j]
      }
      fitted[i] = sum
    }
    fitted
  }

  /**
   * Computes residuals (observed - fitted).
   *
   * @param response the observed values
   * @param fitted the fitted values
   * @return the residuals
   */
  static double[] computeResiduals(double[] response, double[] fitted) {
    double[] residuals = new double[response.length]
    for (int i = 0; i < response.length; i++) {
      residuals[i] = response[i] - fitted[i]
    }
    residuals
  }
}
