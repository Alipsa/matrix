package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.linear.MatrixAlgebra

/**
 * Local regression (LOESS/LOWESS) fit method. Fits a weighted polynomial
 * at each observation using a neighborhood defined by a span parameter.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code span} (double, default 0.75): fraction of data in each local neighborhood</li>
 *   <li>{@code degree} (int, default 1): local polynomial degree (1 = linear, 2 = quadratic)</li>
 * </ul>
 *
 * <p>Uses the first predictor column as the smoothing variable. Accepts optional
 * weights from the model frame. Only univariate smoothing is supported; frames
 * with more than one predictor are rejected.
 *
 * <p>Loess is a local method with no global coefficients. {@link FitResult#coefficients}
 * and {@link FitResult#standardErrors} return empty arrays.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
class LoessMethod implements FitMethod {

  private static final Logger log = Logger.getLogger(LoessMethod)

  @Override
  FitResult fit(ModelFrameResult frame) {
    fit(frame, new LoessOptions())
  }

  /**
   * Fits a loess model with the given options.
   *
   * @param frame the model frame (must have exactly 1 predictor)
   * @param options loess-specific parameters (span, degree)
   * @return the fit result
   */
  FitResult fit(ModelFrameResult frame, LoessOptions options) {
    if (frame.offset != null) {
      throw new UnsupportedOperationException('loess does not yet support offsets')
    }
    if (frame.data.columnCount() > 1) {
      throw new IllegalArgumentException(
        "loess supports only univariate smoothing (1 predictor), got ${frame.data.columnCount()} predictors: ${frame.predictorNames}")
    }

    double span = options.span
    int degree = options.degree

    double[] response = FitUtils.toDoubleArray(frame.response)
    int n = response.length

    // Use the single predictor as smoothing variable
    Matrix designMatrix = frame.data
    double[] x = extractFirstPredictor(designMatrix, n)
    double[] weights = frame.weights != null ? FitUtils.toDoubleArray(frame.weights) : uniformWeights(n)

    // Sort by x for neighborhood computation
    int[] order = sortOrder(x)
    double[] xSorted = reorder(x, order)
    double[] ySorted = reorder(response, order)
    double[] wSorted = reorder(weights, order)

    int bandwidth = Math.max((int) Math.ceil(span * n), degree + 1)

    double[] fittedSorted = new double[n]
    for (int i = 0; i < n; i++) {
      fittedSorted[i] = fitLocal(xSorted, ySorted, wSorted, xSorted[i], bandwidth, degree)
    }

    // Unsort fitted values back to original order
    double[] fitted = new double[n]
    for (int i = 0; i < n; i++) {
      fitted[order[i]] = fittedSorted[i]
    }

    double[] residuals = FitUtils.computeResiduals(response, fitted)
    double rSquared = FitUtils.computeRSquared(response, residuals)

    // Loess has no global coefficients
    new FitResult([] as double[], [] as double[], fitted, residuals, rSquared, [])
  }

  /**
   * Fits a local weighted polynomial centered at the target point.
   */
  private static double fitLocal(double[] x, double[] y, double[] w,
                                  double target, int bandwidth, int degree) {
    int[] window = nearestWindow(x, target, bandwidth)
    int lo = window[0]
    int hi = window[1]

    double maxDist = 0.0d
    for (int i = lo; i <= hi; i++) {
      double d = Math.abs(x[i] - target)
      if (d > maxDist) {
        maxDist = d
      }
    }
    if (maxDist == 0.0d) {
      maxDist = 1.0d
    }

    int localN = hi - lo + 1
    int ncols = degree + 1
    double[][] xMat = new double[localN][ncols]
    double[] yLocal = new double[localN]
    double[] wLocal = new double[localN]

    for (int i = 0; i < localN; i++) {
      int idx = lo + i
      double u = Math.abs(x[idx] - target) / maxDist
      double tricube = tricubeWeight(u)
      wLocal[i] = w[idx] * tricube

      double xVal = x[idx] - target
      xMat[i][0] = 1.0d
      for (int d = 1; d < ncols; d++) {
        xMat[i][d] = xMat[i][d - 1] * xVal
      }
      yLocal[i] = y[idx]
    }

    // Weighted least squares: (X'WX)^-1 X'Wy
    double[][] xw = applyWeights(xMat, wLocal)
    double[][] xtwx = MatrixAlgebra.multiply(MatrixAlgebra.transpose(xw), xMat)
    double[] xtwy = FitUtils.multiplyVec(MatrixAlgebra.transpose(xw), yLocal)

    try {
      double[][] xtwxInv = MatrixAlgebra.inverse(xtwx)
      double[] beta = FitUtils.multiplyVec(xtwxInv, xtwy)
      // Evaluate at target (xVal = 0), so fitted = beta[0]
      beta[0]
    } catch (Exception e) {
      // Singular matrix — return weighted mean as fallback
      log.warn("Singular matrix in loess local fit at target ${target}, using weighted mean fallback: ${e.message}")
      double sumWY = 0.0d
      double sumW = 0.0d
      for (int i = 0; i < localN; i++) {
        sumWY += wLocal[i] * yLocal[i]
        sumW += wLocal[i]
      }
      sumW > 0.0d ? sumWY / sumW : 0.0d
    }
  }

  private static double tricubeWeight(double u) {
    if (u >= 1.0d) {
      return 0.0d
    }
    double t = 1.0d - u * u * u
    t * t * t
  }

  /**
   * Returns the contiguous window in sorted {@code x} containing the nearest
   * {@code bandwidth} observations to {@code target}.
   */
  private static int[] nearestWindow(double[] x, double target, int bandwidth) {
    int n = x.length
    int center = Arrays.binarySearch(x, target)
    if (center < 0) {
      center = -(center + 1)
    }

    int left = center - 1
    int right = center
    int selected = 0
    while (selected < bandwidth) {
      if (left < 0) {
        right++
      } else if (right >= n) {
        left--
      } else {
        double leftDistance = Math.abs(x[left] - target)
        double rightDistance = Math.abs(x[right] - target)
        if (leftDistance <= rightDistance) {
          left--
        } else {
          right++
        }
      }
      selected++
    }
    [left + 1, right - 1] as int[]
  }

  private static double[][] applyWeights(double[][] x, double[] w) {
    int rows = x.length
    int cols = x[0].length
    double[][] result = new double[rows][cols]
    for (int i = 0; i < rows; i++) {
      double sqrtW = Math.sqrt(w[i])
      for (int j = 0; j < cols; j++) {
        result[i][j] = x[i][j] * sqrtW
      }
    }
    result
  }

  private static double[] extractFirstPredictor(Matrix designMatrix, int n) {
    if (designMatrix.columnCount() == 0) {
      throw new IllegalArgumentException('loess requires at least one predictor column')
    }
    double[] result = new double[n]
    for (int i = 0; i < n; i++) {
      Object val = designMatrix[i, 0]
      result[i] = val != null ? (val as Number).doubleValue() : 0.0d
    }
    result
  }

  private static int[] sortOrder(double[] values) {
    List<Integer> indices = (0..<values.length).collect { it as Integer }
    indices.sort { Integer a, Integer b -> Double.compare(values[a], values[b]) }
    indices as int[]
  }

  private static double[] reorder(double[] values, int[] order) {
    double[] result = new double[values.length]
    for (int i = 0; i < values.length; i++) {
      result[i] = values[order[i]]
    }
    result
  }

  private static double[] uniformWeights(int n) {
    ([1.0d] * n) as double[]
  }

  @Override
  FitResult fit(ModelFrameResult frame, FitOptions options) {
    if (!(options instanceof LoessOptions)) {
      throw new IllegalArgumentException("loess requires LoessOptions, got ${options.getClass().simpleName}")
    }
    fit(frame, options as LoessOptions)
  }
}
