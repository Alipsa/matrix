package interpolation

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.interpolation.Interpolation

class InterpolationTest {

  private static final double TOLERANCE = 1e-12

  @Test
  void testLinearInterpolationForExplicitDomain() {
    double result = Interpolation.linear(
      [0.0d, 2.0d, 4.0d] as double[],
      [0.0d, 10.0d, 20.0d] as double[],
      1.0d
    )

    assertEquals(5.0d, result, TOLERANCE)
  }

  @Test
  void testLinearInterpolationForListOverload() {
    double result = Interpolation.linear([1.0, 4.0, 10.0], [2.0, 8.0, 20.0], 7.0)

    assertEquals(14.0d, result, TOLERANCE)
  }

  @Test
  void testLinearInterpolationReturnsExactKnotValue() {
    double result = Interpolation.linear(
      [1.0d, 2.0d, 4.0d] as double[],
      [3.0d, 6.0d, 12.0d] as double[],
      2.0d
    )

    assertEquals(6.0d, result, TOLERANCE)
  }

  @Test
  void testSeriesInterpolationUsesZeroBasedPositions() {
    double result = Interpolation.linear([10.0d, 20.0d, 40.0d] as double[], 1.5d)

    assertEquals(30.0d, result, TOLERANCE)
  }

  @Test
  void testMatrixColumnInterpolation() {
    Matrix data = Matrix.builder()
      .columnNames(['time', 'value'])
      .rows([
        [1.0, 3.0],
        [2.0, 6.0],
        [4.0, 12.0],
      ])
      .types([Double, Double])
      .build()

    assertEquals(9.0d, Interpolation.linear(data, 'time', 'value', 3.0), TOLERANCE)
    assertEquals(9.0d, Interpolation.linear(data, 'value', 1.5), TOLERANCE)
  }

  @Test
  void testGridColumnInterpolation() {
    Grid<Number> data = new Grid<Number>([
      [1.0, 3.0],
      [2.0, 6.0],
      [4.0, 12.0],
    ])

    assertEquals(9.0d, Interpolation.linear(data, 0, 1, 3.0), TOLERANCE)
    assertEquals(9.0d, Interpolation.linear(data, 1, 1.5), TOLERANCE)
  }

  @Test
  void testRejectsUnsortedExplicitDomain() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0d, 4.0d, 3.0d] as double[],
        [2.0d, 8.0d, 6.0d] as double[],
        2.0d
      )
    }

    assertTrue(exception.message.contains('strictly increasing'))
  }

  @Test
  void testRejectsDuplicateExplicitDomainValues() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0d, 2.0d, 2.0d] as double[],
        [3.0d, 6.0d, 7.0d] as double[],
        2.0d
      )
    }

    assertTrue(exception.message.contains('strictly increasing'))
  }

  @Test
  void testRejectsExtrapolation() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0d, 2.0d, 4.0d] as double[],
        [3.0d, 6.0d, 12.0d] as double[],
        0.5d
      )
    }

    assertTrue(exception.message.contains('outside the interpolation domain'))
  }

  @Test
  void testRejectsLengthMismatch() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0d, 2.0d, 4.0d] as double[],
        [3.0d, 6.0d] as double[],
        2.0d
      )
    }

    assertTrue(exception.message.contains('must match'))
  }

  @Test
  void testRejectsNonNumericMatrixColumn() {
    Matrix data = Matrix.builder()
      .columnNames(['time', 'label'])
      .rows([
        [1.0, 'A'],
        [2.0, 'B'],
        [4.0, 'C'],
      ])
      .types([Double, String])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(data, 'time', 'label', 2.0)
    }

    assertTrue(exception.message.contains('must be numeric'))
  }

  @Test
  void testRejectsRaggedGrid() {
    Grid<Number> data = new Grid<Number>([
      [1.0, 3.0],
      [2.0],
    ])

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(data, 0, 1, 1.5)
    }

    assertNotNull(exception.message)
  }
}
