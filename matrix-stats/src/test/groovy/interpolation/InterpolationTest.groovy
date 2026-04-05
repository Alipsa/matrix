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

  @Test
  void testLinearInterpolationForExplicitDomain() {
    BigDecimal result = Interpolation.linear(
      [0.0, 2.0, 4.0],
      [0.0, 10.0, 20.0],
      1.0d
    )

    assertEquals(5.0, result)
  }

  @Test
  void testLinearInterpolationForListOverload() {
    BigDecimal result = Interpolation.linear([1.0, 4.0, 10.0], [2.0, 8.0, 20.0], 7.0)

    assertEquals(14.0, result)
  }

  @Test
  void testLinearInterpolationReturnsExactKnotValue() {
    BigDecimal result = Interpolation.linear(
      [1.0, 2.0, 4.0],
      [3.0, 6.0, 12.0],
      2.0d
    )

    assertEquals(6.0, result)
  }

  @Test
  void testSeriesInterpolationUsesZeroBasedPositions() {
    BigDecimal result = Interpolation.linear([10.0, 20.0, 40.0], 1.5d)

    assertEquals(30.0, result)
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

    assertEquals(9.0, Interpolation.linear(data, 'time', 'value', 3.0))
    assertEquals(9.0, Interpolation.linear(data, 'value', 1.5))
  }

  @Test
  void testGridColumnInterpolation() {
    Grid<Number> data = new Grid<Number>([
      [1.0, 3.0],
      [2.0, 6.0],
      [4.0, 12.0],
    ])

    assertEquals(9.0, Interpolation.linear(data, 0, 1, 3.0))
    assertEquals(9.0, Interpolation.linear(data, 1, 1.5))
  }

  @Test
  void testRejectsGridColumnOutOfBounds() {
    Grid<Number> data = new Grid<Number>([
      [1.0, 3.0],
      [2.0, 6.0],
    ])

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(data, 5, 1, 1.5)
    }

    assertTrue(exception.message.contains('out of bounds'))
  }

  @Test
  void testRejectsUnsortedExplicitDomain() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0, 4.0, 3.0],
        [2.0, 8.0, 6.0],
        2.0d
      )
    }

    assertTrue(exception.message.contains('strictly increasing'))
  }

  @Test
  void testRejectsDuplicateExplicitDomainValues() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0, 2.0, 2.0],
        [3.0, 6.0, 7.0],
        2.0d
      )
    }

    assertTrue(exception.message.contains('strictly increasing'))
  }

  @Test
  void testRejectsExtrapolation() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0, 2.0, 4.0],
        [3.0, 6.0, 12.0],
        0.5d
      )
    }

    assertTrue(exception.message.contains('outside the interpolation domain'))
  }

  @Test
  void testRejectsLengthMismatch() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Interpolation.linear(
        [1.0, 2.0, 4.0],
        [3.0, 6.0],
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
