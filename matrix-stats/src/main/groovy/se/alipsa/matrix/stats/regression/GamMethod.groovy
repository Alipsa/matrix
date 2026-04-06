package se.alipsa.matrix.stats.regression

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.formula.Terms
import se.alipsa.matrix.stats.linear.MatrixAlgebra

/**
 * Minimal Generalized Additive Model (GAM) using penalized (ridge) least squares.
 * Smooth terms ({@code s(x)}) are expanded into natural cubic spline basis columns
 * by {@link se.alipsa.matrix.stats.formula.DesignMatrixBuilder} before reaching this method.
 *
 * <p>This implementation adds a penalty matrix scaled by {@code lambda}
 * to the normal equations: {@code (X'X + lambda*P)^-1 X'y}. The penalty is applied
 * only to columns that belong to smooth terms (identified via {@link Terms.TermInfo#isSmooth}).
 * Linear terms and the intercept are unpenalized.
 *
 * <p>Weights and offsets are not yet supported. If the model frame carries non-null
 * weights or offsets, an {@link UnsupportedOperationException} is thrown.
 *
 * <p>Supported parameters:
 * <ul>
 *   <li>{@code lambda} (double, default 1.0): smoothing penalty parameter</li>
 * </ul>
 *
 * <p>This method expects smooth terms to be expanded by the formula pipeline into
 * spline basis columns and relies on {@link Terms} metadata to identify which columns
 * should be penalized. Unsupported frame features are rejected rather than ignored.
 *
 * <h3>Known Limitations</h3>
 * <ul>
 *   <li>Tensor product smooth terms ({@code te(x, z)}) are not supported</li>
 *   <li>Non-Gaussian families (only Gaussian/identity link)</li>
 *   <li>Automatic smoothing parameter selection (GCV/REML) — lambda is user-specified</li>
 *   <li>Thin plate regression splines — uses natural cubic spline basis only</li>
 *   <li>Multivariate smooth terms ({@code s(x, z)}) — only univariate is supported</li>
 *   <li>Smooth terms inside interactions — rejected during design-matrix construction</li>
 *   <li>Weights and offsets</li>
 * </ul>
 */
@SuppressWarnings('DuplicateNumberLiteral')
class GamMethod implements FitMethod {

  private static final Logger log = Logger.getLogger(GamMethod)

  @Override
  FitResult fit(ModelFrameResult frame) {
    fit(frame, new GamOptions())
  }

  /**
   * Fits a GAM with the given options.
   *
   * @param frame the model frame
   * @param options GAM-specific parameters (lambda)
   * @return the fit result
   */
  FitResult fit(ModelFrameResult frame, GamOptions options) {
    if (frame.weights != null) {
      throw new UnsupportedOperationException('gam does not yet support weights')
    }
    if (frame.offset != null) {
      throw new UnsupportedOperationException('gam does not yet support offsets')
    }

    double lambda = options.lambda
    if (lambda > 1000.0d) {
      log.warn("GAM lambda ${lambda} is very large; consider a smaller value for better numerical stability")
    }

    int n = frame.response.size()
    Matrix designMatrix = frame.data
    boolean addIntercept = frame.includeIntercept

    List<String> names = FitUtils.buildPredictorNames(addIntercept, frame.predictorNames)
    double[][] predictors = FitUtils.buildDesignArray(designMatrix, n, addIntercept)
    double[] response = FitUtils.toDoubleArray(frame.response)

    int p = predictors[0].length

    // Build penalty mask: true for columns belonging to smooth terms
    boolean[] penaltyMask = buildPenaltyMask(addIntercept, frame.predictorNames, frame.terms)

    // (X'X + lambda * P)
    double[][] xt = MatrixAlgebra.transpose(predictors)
    double[][] xtx = MatrixAlgebra.multiply(xt, predictors)

    // Add penalty only to smooth columns
    for (int i = 0; i < p; i++) {
      if (penaltyMask[i]) {
        xtx[i][i] += lambda
      }
    }

    double[][] xtxInv = MatrixAlgebra.inverse(xtx)
    double[] xty = FitUtils.multiplyVec(xt, response)
    double[] coefficients = FitUtils.multiplyVec(xtxInv, xty)

    double[] fitted = FitUtils.computeFitted(predictors, coefficients)
    double[] residuals = FitUtils.computeResiduals(response, fitted)
    double rSquared = FitUtils.computeRSquared(response, residuals)

    // Standard errors from diagonal of (X'X + lambda*P)^-1 * sigma^2
    double ssResidual = 0.0d
    for (double r : residuals) {
      ssResidual += r * r
    }
    int dfResidual = Math.max(1, n - p)
    double sigma2 = ssResidual / dfResidual
    double[] standardErrors = new double[p]
    for (int i = 0; i < p; i++) {
      standardErrors[i] = Math.sqrt(Math.max(0.0d, sigma2 * xtxInv[i][i]))
    }

    new FitResult(coefficients, standardErrors, fitted, residuals, rSquared, names)
  }

  /**
   * Builds a boolean mask indicating which design matrix columns should be penalized.
   * The intercept column (if present) is never penalized. Columns belonging to smooth
   * terms (identified by {@link Terms.TermInfo#isSmooth}) are penalized. Linear terms
   * are not penalized.
   */
  private static boolean[] buildPenaltyMask(boolean addIntercept, List<String> predictorNames, Terms terms) {
    int offset = addIntercept ? 1 : 0
    int totalCols = predictorNames.size() + offset
    boolean[] mask = new boolean[totalCols]

    // Build a set of column names that belong to smooth terms
    Set<String> smoothColumns = [] as Set
    for (Terms.TermInfo term : terms.terms) {
      if (term.isSmooth && !term.isDropped) {
        smoothColumns.addAll(term.columns)
      }
    }

    for (int i = 0; i < predictorNames.size(); i++) {
      mask[i + offset] = smoothColumns.contains(predictorNames[i])
    }

    mask
  }

  @Override
  FitResult fit(ModelFrameResult frame, FitOptions options) {
    if (!(options instanceof GamOptions)) {
      throw new IllegalArgumentException("gam requires GamOptions, got ${options.getClass().simpleName}")
    }
    fit(frame, options as GamOptions)
  }

}
