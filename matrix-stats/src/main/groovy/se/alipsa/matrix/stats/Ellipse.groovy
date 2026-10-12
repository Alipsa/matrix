package se.alipsa.matrix.stats

import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.distribution.FDistribution

/**
 * Confidence ellipse calculations for bivariate normal data.
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class Ellipse {

  private static final Logger log = Logger.getLogger(Ellipse)

  /**
   * Data class to hold ellipse calculation results.
   */
  static class EllipseData {
    List<BigDecimal> x
    List<BigDecimal> y

    EllipseData(List<BigDecimal> x, List<BigDecimal> y) {
      this.x = x
      this.y = y
    }
  }

  /**
   * Calculate confidence ellipse for bivariate data.
   *
   * @param xValues X coordinates
   * @param yValues Y coordinates
   * @param level Confidence level (default 0.95)
   * @param type Ellipse type: 't', 'norm', or 'euclid' (default 't'). The 't' type uses an
   * F-distribution scale and MASS cov.trob robust covariance (nu=5).
   * The 'norm' type uses sample covariance and the same F radius. Euclid is a circle of radius level.
   * @param segments Number of points to generate (default 51)
   * @return EllipseData containing x,y coordinates of the ellipse
   * @throws IllegalArgumentException if either input has fewer than 4 points or the input sizes differ
   */
  static EllipseData calculate(List<? extends Number> xValues, List<? extends Number> yValues,
                                double level = 0.95, String type = 't', int segments = 51) {
    if (xValues.size() != yValues.size()) {
      throw new IllegalArgumentException("X and Y must have the same number of points (got ${xValues.size()} and ${yValues.size()})")
    }
    if (xValues.size() < 4) {
      throw new IllegalArgumentException("Ellipse calculation requires at least 4 points per axis (got ${xValues.size()})")
    }

    if (segments < 3 || !(type in ['norm', 't', 'euclid']) || !Double.isFinite(level) || level <= 0 || level >= 1) {
      throw new IllegalArgumentException('Require segments >= 3, type norm/t/euclid and level in (0,1)')
    }
    int n = xValues.size()

    // Calculate means
    BigDecimal sumX = xValues.sum() as BigDecimal
    BigDecimal sumY = yValues.sum() as BigDecimal
    double meanX = (sumX / n) as double
    double meanY = (sumY / n) as double

    // Calculate covariance matrix
    double varX = 0
    double varY = 0
    double covXY = 0

    for (int i = 0; i < n; i++) {
      double dx = (xValues[i] as double) - meanX
      double dy = (yValues[i] as double) - meanY
      varX += dx * dx
      varY += dy * dy
      covXY += dx * dy
    }

    varX /= (n - 1)
    varY /= (n - 1)
    covXY /= (n - 1)

    if (type == 't') {
      double[] robust = robustCovariance(xValues, yValues, meanX, meanY)
      meanX = robust[0]
      meanY = robust[1]
      varX = robust[2]
      varY = robust[3]
      covXY = robust[4]
    } else if (type == 'euclid') {
      varX = 1
      varY = 1
      covXY = 0
    }

    // Eigenvalues and eigenvectors for rotation
    double trace = varX + varY
    double det = varX * varY - covXY * covXY
    double discriminant = (trace * trace) / 4 - det
    if (discriminant < 0) {
      // Guard against small negative values due to numerical precision
      discriminant = 0
    }
    double lambda1 = trace / 2 + Math.sqrt(discriminant)
    double lambda2 = trace / 2 - Math.sqrt(discriminant)

    // Guard against negative eigenvalues (shouldn't happen but protect against numerical issues)
    if (lambda1 < 0) {
      lambda1 = 0
    }
    if (lambda2 < 0) {
      lambda2 = 0
    }

    // Angle of rotation
    double theta = 0.5d * Math.atan2(2 * covXY, varX - varY)

    double scale = scaleFor(type, level, n)

    // Generate ellipse points
    List<BigDecimal> ellipseX = []
    List<BigDecimal> ellipseY = []

    for (int i = 0; i < segments; i++) {
      double angle = 2 * Math.PI * i / (segments - 1)

      // Point on unit circle
      double ux = Math.cos(angle)
      double uy = Math.sin(angle)

      // Scale by eigenvalues (semi-axes)
      double sx = ux * Math.sqrt(lambda1) * scale
      double sy = uy * Math.sqrt(lambda2) * scale

      // Rotate by theta
      double rx = sx * Math.cos(theta) - sy * Math.sin(theta)
      double ry = sx * Math.sin(theta) + sy * Math.cos(theta)

      // Translate to mean
      ellipseX << ((rx + meanX) as BigDecimal)
      ellipseY << ((ry + meanY) as BigDecimal)
    }

    new EllipseData(ellipseX, ellipseY)
  }

  // MASS 7.3.65 cov.trob: final covariance uses X from the start of the last iteration.
  private static double[] robustCovariance(List<? extends Number> xs, List<? extends Number> ys, double mx, double my) {
    int n = xs.size()
    double[] weights = new double[n]
    Arrays.fill(weights, 1.4d)
    double[] dx = new double[n]
    double[] dy = new double[n]
    double[] q = new double[n]
    boolean converged = false
    for (int iteration = 0; iteration < 25; iteration++) {
      double sumWeights = weights.sum() as double
      double vx = 0
      double vy = 0
      double cov = 0
      (0..<n).each { int i ->
        dx[i] = xs[i].doubleValue() - mx
        dy[i] = ys[i].doubleValue() - my
        vx += weights[i] * dx[i] * dx[i] / sumWeights
        vy += weights[i] * dy[i] * dy[i] / sumWeights
        cov += weights[i] * dx[i] * dy[i] / sumWeights
      }
      double determinant = vx * vy - cov * cov
      if (determinant <= 0) {
        throw new IllegalArgumentException('Robust ellipse requires nonsingular covariance')
      }
      double maxChange = 0
      (0..<n).each { int i ->
        q[i] = (vy * dx[i] * dx[i] - 2 * cov * dx[i] * dy[i] + vx * dy[i] * dy[i]) / determinant
        double next = 7.0d / (5.0d + q[i])
        maxChange = Math.max(maxChange, Math.abs(next - weights[i]))
        weights[i] = next
      }
      sumWeights = weights.sum() as double
      mx = (0..<n).sum { int i -> weights[i] * xs[i].doubleValue() } as double
      my = (0..<n).sum { int i -> weights[i] * ys[i].doubleValue() } as double
      mx /= sumWeights
      my /= sumWeights
      if (maxChange < 0.01d) {
        converged = true
        break
      }
    }
    double weightMean = (weights.sum() as double) / n
    double weightedQ = ((0..<n).sum { int i -> weights[i] * q[i] } as double) / (2 * n)
    if (!converged || Math.abs(weightMean - 1) > 0.01d || Math.abs(weightedQ - 1) > 0.01d) {
      log.warn('Probable convergence failure')
    }
    double vx = 0
    double vy = 0
    double cov = 0
    (0..<n).each { int i ->
      vx += weights[i] * dx[i] * dx[i] / n
      vy += weights[i] * dy[i] * dy[i] / n
      cov += weights[i] * dx[i] * dy[i] / n
    }
    [mx, my, vx, vy, cov] as double[]
  }

  private static double scaleFor(String type, Number level, int n) {
    if (type == 'euclid') {
      return level.doubleValue()
    }
    FDistribution fDist = new FDistribution(2, n - 1)
    Math.sqrt(2.0d * (fDist.inverseCumulativeProbability(level) as double))
  }
}
