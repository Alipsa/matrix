package se.alipsa.matrix.smile.stats

import groovy.transform.CompileStatic
import smile.stat.hypothesis.CorTest

/**
 * Correlation method for correlation matrix computations.
 */
@CompileStatic
enum CorrelationMethod {
  PEARSON,
  SPEARMAN,
  KENDALL

  CorTest correlate(double[] x, double[] y) {
    switch (this) {
      case SPEARMAN -> CorTest.spearman(x, y)
      case KENDALL -> CorTest.kendall(x, y)
      default -> CorTest.pearson(x, y)
    }
  }
}
