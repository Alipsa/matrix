package se.alipsa.matrix.stats.util;

/**
 * Internal primitive least-squares utilities for numerically dense regression paths.
 *
 * <p>The public Groovy-facing APIs in {@code matrix-stats} should continue to accept
 * {@code Number}, {@code List}, {@code Matrix}, and {@code Grid} inputs. This utility keeps
 * the hot floating-point linear algebra loops in Java so those Groovy APIs can remain
 * idiomatic without paying repeated dynamic-loop overhead in solver-heavy call paths.</p>
 *
 * <p>Regression uses Householder QR with order-preserving column selection and a
 * column-relative rank tolerance, following the limited pivoting used by R's lm.</p>
 */
public final class LeastSquaresKernel {

  private static final double RANK_TOLERANCE = 1e-7d;

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

    OlsComputation result = fitRankRevealing(response, predictors);
    requireFullRank(result, predictors[0].length);
    return result;
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

    OlsComputation result = fitRankRevealing(response, predictors);
    requireFullRank(result, predictors[0].length);
    return result.coefficients();
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
      double errorVariance,
      int rank,
      boolean[] aliased,
      double[][] unscaledCovariance
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

  /**
   * Fits a possibly rank-deficient design, retaining independent columns in input order.
   * Aliased coefficients and standard errors are NaN; residual metrics use only retained columns.
   *
   * @param response finite response values
   * @param predictors finite rectangular design matrix
   * @return coefficients, uncertainty, residual metrics, rank and alias information
   */
  public static OlsComputation fitRankRevealing(double[] response, double[][] predictors) {
    validateOlsInputs(response, predictors);
    int n = response.length;
    int p = predictors[0].length;
    double[][] qr = new double[n][p];
    double[] qty = response.clone();
    double[] originalNorm = new double[p];
    for (int i = 0; i < n; i++) {
      if (!Double.isFinite(response[i])) {
        throw new IllegalArgumentException("Response values must be finite");
      }
      for (int j = 0; j < p; j++) {
        double value = predictors[i][j];
        if (!Double.isFinite(value)) {
          throw new IllegalArgumentException("Predictor values must be finite");
        }
        qr[i][j] = value;
        originalNorm[j] = Math.hypot(originalNorm[j], value);
      }
    }
    int[] retained = new int[Math.min(n, p)];
    boolean[] aliased = new boolean[p];
    java.util.Arrays.fill(aliased, true);
    int rank = 0;
    for (int j = 0; j < p && rank < n; j++) {
      double norm = 0.0d;
      for (int i = rank; i < n; i++) {
        norm = Math.hypot(norm, qr[i][j]);
      }
      if (originalNorm[j] == 0.0d || norm / originalNorm[j] < RANK_TOLERANCE) {
        continue;
      }
      double alpha = -Math.copySign(norm, qr[rank][j]);
      double[] v = new double[n - rank];
      for (int i = rank; i < n; i++) {
        v[i - rank] = qr[i][j] / norm;
      }
      v[0] -= alpha / norm;
      double vv = 0.0d;
      for (double value : v) {
        vv += value * value;
      }
      for (int column = j + 1; column < p; column++) {
        double dot = 0.0d;
        for (int i = rank; i < n; i++) {
          dot += v[i - rank] * qr[i][column];
        }
        double factor = 2.0d * dot / vv;
        for (int i = rank; i < n; i++) {
          qr[i][column] -= factor * v[i - rank];
        }
      }
      double dot = 0.0d;
      for (int i = rank; i < n; i++) {
        dot += v[i - rank] * qty[i];
      }
      for (int i = rank; i < n; i++) {
        qty[i] -= 2.0d * dot / vv * v[i - rank];
      }
      qr[rank][j] = alpha;
      retained[rank++] = j;
      aliased[j] = false;
    }
    double[] coefficients = new double[p];
    double[] standardErrors = new double[p];
    java.util.Arrays.fill(coefficients, Double.NaN);
    java.util.Arrays.fill(standardErrors, Double.NaN);
    double[][] inverseR = new double[rank][rank];
    for (int i = rank - 1; i >= 0; i--) {
      double value = qty[i];
      for (int j = i + 1; j < rank; j++) {
        value -= qr[i][retained[j]] * coefficients[retained[j]];
      }
      coefficients[retained[i]] = value / qr[i][retained[i]];
      for (int column = i; column < rank; column++) {
        double rhs = i == column ? 1.0d : 0.0d;
        for (int j = i + 1; j < rank; j++) {
          rhs -= qr[i][retained[j]] * inverseR[j][column];
        }
        inverseR[i][column] = rhs / qr[i][retained[i]];
      }
    }
    double rss = calculateResidualSumOfSquaresUnchecked(response, predictors, coefficients);
    double variance = n > rank ? rss / (n - rank) : Double.NaN;
    for (int i = 0; i < rank; i++) {
      double diagonal = 0.0d;
      for (int j = i; j < rank; j++) {
        diagonal += inverseR[i][j] * inverseR[i][j];
      }
      standardErrors[retained[i]] = Math.sqrt(variance * diagonal);
    }
    double[][] covariance = new double[p][p];
    for (int i = 0; i < rank; i++) {
      for (int j = 0; j < rank; j++) {
        for (int k = Math.max(i, j); k < rank; k++) {
          covariance[retained[i]][retained[j]] += inverseR[i][k] * inverseR[j][k];
        }
      }
    }
    return new OlsComputation(coefficients, standardErrors, rss, variance, rank, aliased, covariance);
  }

  private static void requireFullRank(OlsComputation result, int columns) {
    if (result.rank() < columns) {
      throw new IllegalArgumentException("Singular matrix: design rank " + result.rank() + " is less than " + columns);
    }
  }

  private static double calculateResidualSumOfSquaresUnchecked(double[] response, double[][] predictors, double[] coefficients) {
    double rss = 0.0d;
    for (int row = 0; row < response.length; row++) {
      double fitted = 0.0d;
      for (int column = 0; column < coefficients.length; column++) {
        if (!Double.isNaN(coefficients[column])) {
          fitted += predictors[row][column] * coefficients[column];
        }
      }
      double residual = response[row] - fitted;
      rss += residual * residual;
    }
    return rss;
  }

}
