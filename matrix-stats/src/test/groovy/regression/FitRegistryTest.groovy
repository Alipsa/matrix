package regression

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.regression.FitMethod
import se.alipsa.matrix.stats.regression.FitRegistry
import se.alipsa.matrix.stats.regression.FitResult

@CompileStatic
class FitRegistryTest {

  @Test
  void testFitResultProperties() {
    double[] coefficients = [1.5, 2.0] as double[]
    double[] standardErrors = [0.1, 0.2] as double[]
    double[] fittedValues = [3.5, 5.5] as double[]
    double[] residuals = [0.5, -0.5] as double[]

    FitResult result = new FitResult(
      coefficients, standardErrors, fittedValues, residuals,
      0.95, ['(Intercept)', 'x']
    )

    assertEquals(2, result.coefficients.length)
    assertEquals(1.5, result.coefficients[0], 1e-10)
    assertEquals(0.95, result.rSquared, 1e-10)
    assertEquals(2, result.predictorNames.size())
    assertEquals('(Intercept)', result.predictorNames[0])
    // defensive copies
    coefficients[0] = 999.0
    assertEquals(1.5, result.coefficients[0], 1e-10)
  }

  @Test
  void testRegistryLookup() {
    FitRegistry registry = FitRegistry.instance()
    assertNotNull(registry.get('lm'))
    assertThrows(IllegalArgumentException) {
      registry.get('nonexistent')
    }
  }

  @Test
  void testRegistryContains() {
    FitRegistry registry = FitRegistry.instance()
    assertTrue(registry.contains('lm'))
    assertTrue(registry.contains('loess'))
    assertTrue(registry.contains('gam'))
    assertFalse(registry.contains('nonexistent'))
  }
}
