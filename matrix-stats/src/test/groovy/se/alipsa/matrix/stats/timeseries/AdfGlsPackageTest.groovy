package se.alipsa.matrix.stats.timeseries

import static org.junit.jupiter.api.Assertions.assertEquals

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

/**
 * Package-level regression tests for ADF-GLS implementation details shared with UnitRoot.
 */
@CompileStatic
class AdfGlsPackageTest {

  @Test
  void lagSelectionCanBeAppliedBeforeTheFinalFit() {
    Random random = new Random(999)
    double[] data = new double[100]
    data[0] = random.nextGaussian()
    data[1] = random.nextGaussian()
    for (int i = 2; i < data.length; i++) {
      data[i] = 1.2d * data[i - 1] - 0.45d * data[i - 2] + random.nextGaussian()
    }

    int selectedLag = AdfGls.selectLags(data, 'drift')

    assertEquals(selectedLag, AdfGls.test(data, selectedLag, 'drift').lags)
  }
}
