package se.alipsa.matrix.stats.util;

/**
 * Internal primitive least-squares utilities for numerically dense regression paths.
 *
 * <p>The public Groovy-facing APIs in {@code matrix-stats} should continue to accept
 * {@code Number}, {@code List}, {@code Matrix}, and {@code Grid} inputs. This utility keeps
 * the hot floating-point linear algebra loops in Java so those Groovy APIs can remain
 * idiomatic without paying repeated dynamic-loop overhead in solver-heavy call paths.</p>
 */
public final class LeastSquaresKernel {

  private static final double SINGULARITY_THRESHOLD = 1e-14d;

  private LeastSquaresKernel() {
  }

  /**
   * Fit an ordinary least squares regression model and return the full computation result.
   *
   * @param response the response vector
   * @param predictors the design matrix
   * @return coefficients, standard errors, and residual metrics
   * @throws IllegalArgumentException if the inputs are invalid or the model is not estimable
   */
  public static OlsComputation fitMultipleLinearRegression(double[] response, double[][] predictors) {
    validateMultipleRegressionInputs(response, predictors);

    int observationCount = response.length;
    int predictorCount = predictors[0].length;

    double[][] transpose = transpose(predictors);
    double[][] xtx = multiply(transpose, predictors);
    double[][] xtxInverse = invertSquareMatrix(xtx);
    double[] xty = multiply(transpose, response);
    double[] coefficients = multiply(xtxInverse, xty);
    double residualSumOfSquares = calculateResidualSumOfSquaresUnchecked(response, predictors, coefficients);

    int degreesOfFreedom = observationCount - predictorCount;
    if (degreesOfFreedom <= 0) {
      throw new IllegalArgumentException(
          "Ordinary least squares requires more observations (" + observationCount + ") than predictors (" + predictorCount + ")"
      );
    }

    double errorVariance = residualSumOfSquares / degreesOfFreedom;
    double[] standardErrors = calculateStandardErrors(xtxInverse, errorVariance);
    return new OlsComputation(coefficients, standardErrors, residualSumOfSquares, errorVariance);
  }

  /**
   * Fit an ordinary least squares regression and return the coefficient vector.
   *
   * @param response the response vector
   * @param predictors the design matrix
   * @return the fitted coefficients
   * @throws IllegalArgumentException if the inputs are invalid or the normal equations cannot be solved
   */
  public static double[] fitOls(double[] response, double[][] predictors) {
    validateOlsInputs(response, predictors);

    double[][] transpose = transpose(predictors);
    double[][] xtx = multiply(transpose, predictors);
    double[] xty = multiply(transpose, response);
    return solveSquareSystem(xtx, xty);
  }

  /**
   * Calculate the residual sum of squares for a fitted linear model.
   *
   * @param response the observed response vector
   * @param predictors the design matrix
   * @param coefficients the fitted coefficient vector
   * @return the residual sum of squares
   * @throws IllegalArgumentException if the inputs are invalid
   */
  public static double calculateResidualSumOfSquares(double[] response, double[][] predictors, double[] coefficients) {
    validateResidualInputs(response, predictors, coefficients);
    return calculateResidualSumOfSquaresUnchecked(response, predictors, coefficients);
  }

  /**
   * Immutable OLS computation result.
   *
   * @param coefficients fitted coefficient vector
   * @param standardErrors coefficient standard errors
   * @param residualSumOfSquares residual sum of squares
   * @param errorVariance residual variance estimate
   */
  public record OlsComputation(
      double[] coefficients,
      double[] standardErrors,
      double residualSumOfSquares,
      double errorVariance
  ) {
  }

  private static void validateMultipleRegressionInputs(double[] response, double[][] predictors) {
    if (response == null || predictors == null) {
      throw new IllegalArgumentException("Response vector and design matrix cannot be null");
    }
    if (response.length == 0) {
      throw new IllegalArgumentException("Response vector cannot be empty");
    }
    if (predictors.length != response.length) {
      throw new IllegalArgumentException(
          "Design matrix row count (" + predictors.length + ") must match response length (" + response.length + ")"
      );
    }
    if (predictors.length == 0 || predictors[0].length == 0) {
      throw new IllegalArgumentException("Design matrix must contain at least one predictor");
    }

    int columnCount = predictors[0].length;
    if (response.length <= columnCount) {
      throw new IllegalArgumentException(
          "Ordinary least squares requires more observations (" + response.length + ") than predictors (" + columnCount + ")"
      );
    }
    for (double[] row : predictors) {
      if (row.length != columnCount) {
        throw new IllegalArgumentException("Design matrix rows must all have the same length");
      }
    }
  }

  private static void validateOlsInputs(double[] response, double[][] predictors) {
    if (response == null || predictors == null) {
      throw new IllegalArgumentException("Response vector and design matrix cannot be null");
    }
    if (predictors.length == 0 || predictors[0].length == 0) {
      throw new IllegalArgumentException("Design matrix must contain at least one row and one column");
    }
    if (predictors.length != response.length) {
      throw new IllegalArgumentException(
          "Design matrix row count (" + predictors.length + ") must match response length (" + response.length + ")"
      );
    }

    int columnCount = predictors[0].length;
    for (double[] row : predictors) {
      if (row.length != columnCount) {
        throw new IllegalArgumentException("Design matrix rows must all have the same length");
      }
    }
  }

  private static void validateResidualInputs(double[] response, double[][] predictors, double[] coefficients) {
    if (response == null || predictors == null || coefficients == null) {
      throw new IllegalArgumentException("Response vector, design matrix, and coefficients cannot be null");
    }
    if (predictors.length == 0) {
      throw new IllegalArgumentException("Design matrix must contain at least one row");
    }
    if (predictors.length != response.length) {
      throw new IllegalArgumentException(
          "Design matrix row count (" + predictors.length + ") must match response length (" + response.length + ")"
      );
    }
    if (predictors[0].length != coefficients.length) {
      throw new IllegalArgumentException(
          "Coefficient count (" + coefficients.length + ") must match design matrix column count (" + predictors[0].length + ")"
      );
    }
    for (double[] row : predictors) {
      if (row.length != coefficients.length) {
        throw new IllegalArgumentException("Design matrix rows must all have the same length");
      }
    }
  }

  private static double[][] transpose(double[][] matrix) {
    int rowCount = matrix.length;
    int columnCount = matrix[0].length;
    double[][] result = new double[columnCount][rowCount];
    for (int row = 0; row < rowCount; row++) {
      for (int column = 0; column < columnCount; column++) {
        result[column][row] = matrix[row][column];
      }
    }
    return result;
  }

  private static double[][] multiply(double[][] left, double[][] right) {
    int rowCount = left.length;
    int innerCount = left[0].length;
    int columnCount = right[0].length;
    double[][] result = new double[rowCount][columnCount];

    for (int row = 0; row < rowCount; row++) {
      for (int inner = 0; inner < innerCount; inner++) {
        double value = left[row][inner];
        if (value == 0.0d) {
          continue;
        }
        for (int column = 0; column < columnCount; column++) {
          result[row][column] += value * right[inner][column];
        }
      }
    }
    return result;
  }

  private static double[] multiply(double[][] matrix, double[] vector) {
    double[] result = new double[matrix.length];
    for (int row = 0; row < matrix.length; row++) {
      double sum = 0.0d;
      for (int column = 0; column < vector.length; column++) {
        sum += matrix[row][column] * vector[column];
      }
      result[row] = sum;
    }
    return result;
  }

  private static double[][] invertSquareMatrix(double[][] matrix) {
    int dimension = matrix.length;
    double[][] augmented = new double[dimension][2 * dimension];
    for (int row = 0; row < dimension; row++) {
      for (int column = 0; column < dimension; column++) {
        augmented[row][column] = matrix[row][column];
        augmented[row][column + dimension] = row == column ? 1.0d : 0.0d;
      }
    }

    for (int pivotColumn = 0; pivotColumn < dimension; pivotColumn++) {
      int pivotRow = findPivotRow(augmented, pivotColumn, dimension);
      if (pivotRow != pivotColumn) {
        swapRows(augmented, pivotColumn, pivotRow);
      }

      double pivot = augmented[pivotColumn][pivotColumn];
      if (Math.abs(pivot) <= SINGULARITY_THRESHOLD) {
        throw new IllegalArgumentException("Singular matrix at column " + pivotColumn + " - cannot invert matrix");
      }

      for (int column = 0; column < 2 * dimension; column++) {
        augmented[pivotColumn][column] /= pivot;
      }
      for (int row = 0; row < dimension; row++) {
        if (row == pivotColumn) {
          continue;
        }
        double factor = augmented[row][pivotColumn];
        if (factor == 0.0d) {
          continue;
        }
        for (int column = 0; column < 2 * dimension; column++) {
          augmented[row][column] -= factor * augmented[pivotColumn][column];
        }
      }
    }

    double[][] inverse = new double[dimension][dimension];
    for (int row = 0; row < dimension; row++) {
      System.arraycopy(augmented[row], dimension, inverse[row], 0, dimension);
    }
    return inverse;
  }

  private static double[] solveSquareSystem(double[][] matrix, double[] vector) {
    int dimension = vector.length;
    double[][] augmented = new double[dimension][dimension + 1];
    for (int row = 0; row < dimension; row++) {
      System.arraycopy(matrix[row], 0, augmented[row], 0, dimension);
      augmented[row][dimension] = vector[row];
    }

    for (int pivotColumn = 0; pivotColumn < dimension; pivotColumn++) {
      int pivotRow = findPivotRow(augmented, pivotColumn, dimension);
      if (pivotRow != pivotColumn) {
        swapRows(augmented, pivotColumn, pivotRow);
      }

      double pivot = augmented[pivotColumn][pivotColumn];
      if (Math.abs(pivot) <= SINGULARITY_THRESHOLD) {
        throw new IllegalArgumentException("Singular matrix at column " + pivotColumn + " - cannot solve linear system");
      }

      for (int row = pivotColumn + 1; row < dimension; row++) {
        double factor = augmented[row][pivotColumn] / pivot;
        for (int column = pivotColumn; column <= dimension; column++) {
          augmented[row][column] -= factor * augmented[pivotColumn][column];
        }
      }
    }

    double[] solution = new double[dimension];
    for (int row = dimension - 1; row >= 0; row--) {
      double sum = augmented[row][dimension];
      for (int column = row + 1; column < dimension; column++) {
        sum -= augmented[row][column] * solution[column];
      }
      double diagonal = augmented[row][row];
      if (Math.abs(diagonal) <= SINGULARITY_THRESHOLD) {
        throw new IllegalArgumentException("Singular matrix at column " + row + " - cannot solve linear system");
      }
      solution[row] = sum / diagonal;
    }
    return solution;
  }

  private static double calculateResidualSumOfSquaresUnchecked(double[] response, double[][] predictors, double[] coefficients) {
    double rss = 0.0d;
    for (int row = 0; row < response.length; row++) {
      double fitted = 0.0d;
      for (int column = 0; column < coefficients.length; column++) {
        fitted += predictors[row][column] * coefficients[column];
      }
      double residual = response[row] - fitted;
      rss += residual * residual;
    }
    return rss;
  }

  private static double[] calculateStandardErrors(double[][] xtxInverse, double variance) {
    double[] standardErrors = new double[xtxInverse.length];
    for (int index = 0; index < xtxInverse.length; index++) {
      double diagonal = Math.max(0.0d, xtxInverse[index][index]);
      standardErrors[index] = Math.sqrt(variance * diagonal);
    }
    return standardErrors;
  }

  private static int findPivotRow(double[][] matrix, int pivotColumn, int rowCount) {
    int maxRow = pivotColumn;
    double maxValue = Math.abs(matrix[pivotColumn][pivotColumn]);
    for (int row = pivotColumn + 1; row < rowCount; row++) {
      double value = Math.abs(matrix[row][pivotColumn]);
      if (value > maxValue) {
        maxValue = value;
        maxRow = row;
      }
    }
    return maxRow;
  }

  private static void swapRows(double[][] matrix, int firstRow, int secondRow) {
    double[] temp = matrix[firstRow];
    matrix[firstRow] = matrix[secondRow];
    matrix[secondRow] = temp;
  }
}
