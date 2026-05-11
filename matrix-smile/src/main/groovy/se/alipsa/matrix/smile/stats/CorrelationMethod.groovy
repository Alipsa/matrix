package se.alipsa.matrix.smile.stats

import groovy.transform.CompileStatic

import smile.stat.hypothesis.CorTest

/**
 * Correlation method for correlation matrix computations.
 * Each constant delegates to the corresponding Smile {@link CorTest} factory method
 * via {@link #correlate(double[], double[])}.
 */
@CompileStatic
enum CorrelationMethod {
  PEARSON,
  SPEARMAN,
  KENDALL

  /**
   * Compute the correlation test between two arrays using this method.
   *
   * @param x first data array
   * @param y second data array
   * @return a CorTest result containing the correlation coefficient, t-statistic, and p-value
   */
  CorTest correlate(double[] x, double[] y) {
    switch (this) {
      case PEARSON -> CorTest.pearson(x, y)
      case SPEARMAN -> CorTest.spearman(x, y)
      case KENDALL -> CorTest.kendall(x, y)
    }
  }
}
