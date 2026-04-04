package linalg

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.linalg.Linalg
import se.alipsa.matrix.stats.linalg.LinalgSingularMatrixException

class LinalgTest {

  private static final double TOLERANCE = 1e-9

  @Test
  void testInverseForDenseArray() {
    double[][] inverse = Linalg.inverse([
      [4.0d, 7.0d],
      [2.0d, 6.0d],
    ] as double[][])

    assertMatrixEquals([
      [0.6d, -0.7d],
      [-0.2d, 0.4d],
    ] as double[][], inverse)
  }

  @Test
  void testInverseForMatrixUsesSyntheticColumnNames() {
    Matrix source = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [4.0, 7.0],
        [2.0, 6.0],
      ])
      .types([Double, Double])
      .build()

    Matrix inverse = Linalg.inverse(source)

    assertEquals(['c0', 'c1'], inverse.columnNames())
    assertMatrixEquals([
      [0.6d, -0.7d],
      [-0.2d, 0.4d],
    ] as double[][], inverse)
  }

  @Test
  void testInverseForGrid() {
    Grid<Number> source = new Grid<Number>([
      [4.0, 7.0],
      [2.0, 6.0],
    ])

    Matrix inverse = Linalg.inverse(source)

    assertEquals(['c0', 'c1'], inverse.columnNames())
    assertMatrixEquals([
      [0.6d, -0.7d],
      [-0.2d, 0.4d],
    ] as double[][], inverse)
  }

  @Test
  void testDeterminant() {
    assertEquals(10.0d, Linalg.det([
      [4.0d, 7.0d],
      [2.0d, 6.0d],
    ] as double[][]), TOLERANCE)
  }

  @Test
  void testSolveForArrayMatrixAndListVector() {
    double[] solution = Linalg.solve([
      [4.0d, 7.0d],
      [2.0d, 6.0d],
    ] as double[][], [1.0, 0.0])

    assertArrayEquals([0.6d, -0.2d] as double[], solution, TOLERANCE)
  }

  @Test
  void testSolveForMatrixAndGrid() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [4.0, 7.0],
        [2.0, 6.0],
      ])
      .types([Double, Double])
      .build()
    Grid<Number> grid = new Grid<Number>([
      [4.0, 7.0],
      [2.0, 6.0],
    ])

    assertArrayEquals([0.6d, -0.2d] as double[], Linalg.solve(matrix, [1.0, 0.0]), TOLERANCE)
    assertArrayEquals([0.6d, -0.2d] as double[], Linalg.solve(grid, [1.0, 0.0]), TOLERANCE)
  }

  @Test
  void testEigenvaluesSupportGeneralMatrixWithRealSpectrum() {
    double[] eigenvalues = Linalg.eigenvalues([
      [4.0d, 2.0d],
      [1.0d, 3.0d],
    ] as double[][])

    assertArrayEquals([5.0d, 2.0d] as double[], eigenvalues, TOLERANCE)
  }

  @Test
  void testEigenvaluesRejectComplexSpectrum() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Linalg.eigenvalues([
        [0.0d, -1.0d],
        [1.0d, 0.0d],
      ] as double[][])
    }

    assertTrue(exception.message.contains('Complex eigenvalues'))
  }

  @Test
  void testSvdReconstructsOriginalMatrix() {
    def result = Linalg.svd([
      [3.0d, 1.0d],
      [1.0d, 3.0d],
      [1.0d, 1.0d],
    ] as double[][])

    assertArrayEquals([4.242640687119285d, 2.0d] as double[], result.singularValues, 1e-8)
    assertMatrixEquals([
      [3.0d, 1.0d],
      [1.0d, 3.0d],
      [1.0d, 1.0d],
    ] as double[][], result.reconstruct(), 1e-8)
  }

  @Test
  void testRejectsSingularMatrixForInverseAndSolve() {
    double[][] singular = [
      [1.0d, 2.0d],
      [2.0d, 4.0d],
    ] as double[][]

    assertThrows(LinalgSingularMatrixException) {
      Linalg.inverse(singular)
    }
    assertThrows(LinalgSingularMatrixException) {
      Linalg.solve(singular, [1.0d, 2.0d] as double[])
    }
  }

  @Test
  void testRejectsNonSquareInput() {
    IllegalArgumentException inverse = assertThrows(IllegalArgumentException) {
      Linalg.inverse([
        [1.0d, 2.0d, 3.0d],
        [4.0d, 5.0d, 6.0d],
      ] as double[][])
    }
    IllegalArgumentException determinant = assertThrows(IllegalArgumentException) {
      Linalg.det([
        [1.0d, 2.0d, 3.0d],
        [4.0d, 5.0d, 6.0d],
      ] as double[][])
    }

    assertTrue(inverse.message.contains('square'))
    assertTrue(determinant.message.contains('square'))
  }

  @Test
  void testRejectsVectorDimensionMismatch() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [4.0, 7.0],
        [2.0, 6.0],
      ])
      .types([Double, Double])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Linalg.solve(matrix, [1.0, 0.0, 2.0])
    }

    assertTrue(exception.message.contains('row count'))
  }

  private static void assertMatrixEquals(double[][] expected, Matrix actual, double tolerance = TOLERANCE) {
    assertEquals(expected.length, actual.rowCount())
    assertEquals(expected[0].length, actual.columnCount())
    for (int row = 0; row < expected.length; row++) {
      for (int col = 0; col < expected[row].length; col++) {
        assertEquals(expected[row][col], actual[row, col] as double, tolerance, "Mismatch at [${row},${col}]")
      }
    }
  }

  private static void assertMatrixEquals(double[][] expected, double[][] actual, double tolerance = TOLERANCE) {
    assertEquals(expected.length, actual.length)
    assertEquals(expected[0].length, actual[0].length)
    for (int row = 0; row < expected.length; row++) {
      assertArrayEquals(expected[row], actual[row], tolerance, "Mismatch at row ${row}")
    }
  }
}
