package se.alipsa.matrix.stats

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/**
 * Confidence ellipse calculations for bivariate normal data.
 */
@CompileStatic
class Ellipse {

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
   * @param type Ellipse type: 't', 'norm', or 'euclid' (default 't')
   * @param segments Number of points to generate (default 51)
   * @return EllipseData containing x,y coordinates of the ellipse
   */
  static EllipseData calculate(List<BigDecimal> xValues, List<BigDecimal> yValues,
                                double level = 0.95, String type = 't', int segments = 51) {
    if (xValues.size() < 3 || yValues.size() < 3 || xValues.size() != yValues.size()) {
      // Need at least 3 points for ellipse
      return new EllipseData([], [])
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

    // Angle of rotation
    double theta
    if (Math.abs(covXY) < 1e-10) {
      theta = 0
    } else {
      theta = Math.atan2(lambda1 - varX, covXY)
    }

    // Scale factor based on confidence level
    double scale
    if (type == 't' || type == 'norm') {
      // Use chi-square quantile for multivariate normal (2 degrees of freedom)
      // For bivariate normal, confidence ellipse is based on chi-square(2)
      ChiSquaredDistribution chiSq = new ChiSquaredDistribution(2)
      double quantile = chiSq.inverseCumulativeProbability(level)
      scale = Math.sqrt(quantile)
    } else {
      scale = 1.0
    }

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

    return new EllipseData(ellipseX, ellipseY)
  }
}
