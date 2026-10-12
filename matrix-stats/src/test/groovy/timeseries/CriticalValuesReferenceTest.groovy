package se.alipsa.matrix.stats.timeseries

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class CriticalValuesReferenceTest {

  @Test
  void responseSurfacesMatchUrCaAndTheirAsymptotes() {
    // urca 1.3.4 qunitroot(c(.01,.05,.1),N=25/100/Inf,trend='nc'/'c'/'ct').
    def references = [none: [[-2.66072d, -1.95504d, -1.60905d], [-2.58848d, -1.94407d, -1.61466d], [-2.56504d, -1.94085d, -1.61675d]],
                      drift: [[-3.72428d, -2.98622d, -2.63261d], [-3.49701d, -2.89061d, -2.58236d], [-3.43035d, -2.86137d, -2.56669d]],
                      trend: [[-4.37419d, -3.60321d, -3.23804d], [-4.05246d, -3.45541d, -3.15346d], [-3.95788d, -3.40984d, -3.12659d]]]
    references.each { String type, List rows ->
      [25, 100, 1000000].eachWithIndex { int n, int i ->
        [0.01d, 0.05d, 0.10d].eachWithIndex { double alpha, int j ->
          assertEquals(rows[i][j] as double, MacKinnonCriticalValues.value(type, n, alpha), 0.002d)
        }
      }
    }
    // MacKinnon 2010 Table 2 asymptotes; different source vintage from qunitroot above.
    assertEquals(-3.43035d, MacKinnonCriticalValues.value('drift', 1000000, 0.01d), 1e-4)
    assertEquals(-3.95877d, MacKinnonCriticalValues.value('trend', 1000000, 0.01d), 1e-4)
  }

  @Test
  void ersCriticalValuesUseUrCaRows() {
    def method = AdfGls.getDeclaredMethod('getCriticalValue', String, Integer.TYPE, Double.TYPE)
    method.accessible = true
    // urca 1.3.4 ur.ers source: ERS Table 1, row changes at 50/100/201.
    [49: -3.77d, 50: -3.58d, 99: -3.58d, 100: -3.46d, 200: -3.46d, 201: -3.48d].each { int n, double value ->
      assertEquals(value, method.invoke(null, 'trend', n, 0.01d) as double, 1e-12)
    }
    assertEquals(-2.575851d, method.invoke(null, 'drift', 200, 0.01d) as double, 1e-12)
    assertEquals(-1.94129d, method.invoke(null, 'drift', 200, 0.05d) as double, 1e-12)
  }
}
