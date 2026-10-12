package se.alipsa.matrix.stats.timeseries

import groovy.transform.PackageScope

/** MacKinnon (2010), Table 2, N=1 response surfaces for the Dickey-Fuller tau statistic. */
@PackageScope
@SuppressWarnings(['DuplicateNumberLiteral', 'UnnecessaryCast'])
class MacKinnonCriticalValues {

  private static final Map<String, List<List<BigDecimal>>> COEFFICIENTS = ['none': [[-2.56574, -2.2358, -3.627, 0.0], [-1.941, -0.2686, -3.365, 31.223], [-1.61682, 0.2656, -2.714, 25.364]], 'drift': [[-3.43035, -6.5393, -16.786, -79.433], [-2.86154, -2.8903, -4.234, -40.04], [-2.56677, -1.5384, -2.809, 0.0]], 'trend': [[-3.95877, -9.0531, -28.428, -134.155], [-3.41049, -4.3904, -9.036, -45.374], [-3.12705, -2.5856, -3.925, -22.38]]] as Map<String, List<List<BigDecimal>>>

  static double value(String type, int observations, double significance) {
    int index = significance == 0.01d ? 0 : significance == 0.05d ? 1 : 2
    List<BigDecimal> coefficients = COEFFICIENTS[type][index]
    double inverse = 1.0d / observations
    coefficients[0].doubleValue() + inverse * (coefficients[1].doubleValue() + inverse *
        (coefficients[2].doubleValue() + inverse * coefficients[3].doubleValue()))
  }
}
