package regression

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.regression.FitResult

/**
 * Tests for fit result value accessors.
 */
@CompileStatic
class FitResultTest {

  @Test
  void testBigDecimalListAccessors() {
    FitResult result = new FitResult(
      [1.5d, 2.0d] as double[],
      [0.1d, 0.2d] as double[],
      [3.5d, 5.5d] as double[],
      [0.5d, -0.5d] as double[],
      0.95d,
      ['(Intercept)', 'x']
    )

    assertEquals([1.5G, 2.0G], result.coefficientList)
    assertEquals([0.1G, 0.2G], result.standardErrorList)
    assertEquals([3.5G, 5.5G], result.fittedList)
    assertEquals([0.5G, -0.5G], result.residualList)
    assertEquals(0.95G, result.RSquaredValue)
  }

  @Test
  void testListAccessorsAreImmutable() {
    FitResult result = new FitResult(
      [1.5d] as double[],
      [0.1d] as double[],
      [3.5d] as double[],
      [0.5d] as double[],
      0.95d,
      ['(Intercept)']
    )

    assertThrows(UnsupportedOperationException) {
      result.coefficientList.add(9.0G)
    }
    assertThrows(UnsupportedOperationException) {
      result.standardErrorList.add(9.0G)
    }
    assertThrows(UnsupportedOperationException) {
      result.fittedList.add(9.0G)
    }
    assertThrows(UnsupportedOperationException) {
      result.residualList.add(9.0G)
    }
  }

  @Test
  void testConstructorClonesArrayInputs() {
    double[] coefficients = [1.5d, 2.0d] as double[]
    double[] standardErrors = [0.1d, 0.2d] as double[]
    double[] fittedValues = [3.5d, 5.5d] as double[]
    double[] residuals = [0.5d, -0.5d] as double[]

    FitResult result = new FitResult(
      coefficients,
      standardErrors,
      fittedValues,
      residuals,
      0.95d,
      ['(Intercept)', 'x']
    )

    coefficients[0] = 999.0d
    standardErrors[0] = 999.0d
    fittedValues[0] = 999.0d
    residuals[0] = 999.0d

    assertEquals(1.5d, result.coefficients[0], 1e-10d)
    assertEquals(0.1d, result.standardErrors[0], 1e-10d)
    assertEquals(3.5d, result.fittedValues[0], 1e-10d)
    assertEquals(0.5d, result.residuals[0], 1e-10d)
    assertEquals(1.5G, result.coefficientList[0])
    assertEquals(0.1G, result.standardErrorList[0])
    assertEquals(3.5G, result.fittedList[0])
    assertEquals(0.5G, result.residualList[0])
  }

  @Test
  void testEmptyCoefficientVectorsReturnEmptyImmutableLists() {
    FitResult result = new FitResult(
      [] as double[],
      [] as double[],
      [1.0d, 2.0d] as double[],
      [0.0d, 0.0d] as double[],
      1.0d,
      []
    )

    assertTrue(result.coefficientList.isEmpty())
    assertTrue(result.standardErrorList.isEmpty())
    assertEquals([1.0G, 2.0G], result.fittedList)
    assertEquals([0.0G, 0.0G], result.residualList)
    assertThrows(UnsupportedOperationException) {
      result.coefficientList.add(1.0G)
    }
  }

  @Test
  void testRSquaredValueRejectsNonFiniteValues() {
    FitResult result = new FitResult(
      [] as double[],
      [] as double[],
      [] as double[],
      [] as double[],
      Double.NaN,
      []
    )

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      result.RSquaredValue
    }
    assertTrue(exception.message.contains('must be finite'))
  }
}
