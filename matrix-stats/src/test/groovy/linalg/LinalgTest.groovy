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

    Grid<BigDecimal> inverse = Linalg.inverse(source)

    assertGridEquals([
      [0.6d, -0.7d],
      [-0.2d, 0.4d],
    ] as double[][], inverse)
  }

  @Test
  void testDeterminant() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [4.0, 7.0],
        [2.0, 6.0],
      ])
      .types([Double, Double])
      .build()

    assertEquals(10.0, Linalg.det(matrix))
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

    assertVectorEquals([0.6d, -0.2d] as double[], Linalg.solve(matrix, [1.0, 0.0]))
    assertVectorEquals([0.6d, -0.2d] as double[], Linalg.solve(grid, [1.0, 0.0]))
  }

  @Test
  void testEigenvaluesSupportGeneralMatrixWithRealSpectrum() {
    Matrix matrix = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [4.0, 2.0],
        [1.0, 3.0],
      ])
      .types([Double, Double])
      .build()

    assertEquals([5.0, 2.0], Linalg.eigenvalues(matrix))
  }

  @Test
  void testEigenvaluesRejectComplexSpectrum() {
    Grid<Number> matrix = new Grid<Number>([
      [0.0, -1.0],
      [1.0, 0.0],
    ])

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Linalg.eigenvalues(matrix)
    }

    assertTrue(exception.message.contains('Complex eigenvalues'))
  }

  @Test
  void testSvdReconstructsOriginalMatrix() {
    Grid<Number> matrix = new Grid<Number>([
      [3.0, 1.0],
      [1.0, 3.0],
      [1.0, 1.0],
    ])

    def result = Linalg.svd(matrix)

    assertArrayEquals([4.242640687119285d, 2.0d] as double[], result.singularValues, 1e-8)
    assertMatrixEquals([
      [3.0d, 1.0d],
      [1.0d, 3.0d],
      [1.0d, 1.0d],
    ] as double[][], result.reconstruct(), 1e-8)
  }

  @Test
  void testRejectsSingularMatrixForInverseAndSolve() {
    Grid<Number> singular = new Grid<Number>([
      [1.0, 2.0],
      [2.0, 4.0],
    ])

    assertThrows(LinalgSingularMatrixException) {
      Linalg.inverse(singular)
    }
    assertThrows(LinalgSingularMatrixException) {
      Linalg.solve(singular, [1.0d, 2.0d])
    }
  }

  @Test
  void testRejectsNonSquareInput() {
    Grid<Number> grid = new Grid<Number>([
      [1.0, 2.0, 3.0],
      [4.0, 5.0, 6.0],
    ])
    Matrix matrix = Matrix.builder()
      .columnNames(['a', 'b', 'c'])
      .rows([
        [1.0, 2.0, 3.0],
        [4.0, 5.0, 6.0],
      ])
      .types([Double, Double, Double])
      .build()

    IllegalArgumentException inverse = assertThrows(IllegalArgumentException) {
      Linalg.inverse(grid)
    }
    IllegalArgumentException determinant = assertThrows(IllegalArgumentException) {
      Linalg.det(matrix)
    }
    IllegalArgumentException eigenvalues = assertThrows(IllegalArgumentException) {
      Linalg.eigenvalues(grid)
    }

    assertTrue(inverse.message.contains('square'))
    assertTrue(determinant.message.contains('square'))
    assertTrue(eigenvalues.message.contains('square'))
  }

  @Test
  void testRejectsNullFacadeInputs() {
    assertTrue(assertThrows(IllegalArgumentException) {
      Linalg.inverse((Matrix) null)
    }.message.contains('cannot be null'))

    assertTrue(assertThrows(IllegalArgumentException) {
      Linalg.det((Grid) null)
    }.message.contains('cannot be null'))

    assertTrue(assertThrows(IllegalArgumentException) {
      Linalg.solve((Matrix) null, [1.0d])
    }.message.contains('cannot be null'))

    Matrix matrix = Matrix.builder()
      .columnNames(['x'])
      .rows([[1.0]])
      .types([Double])
      .build()
    assertTrue(assertThrows(IllegalArgumentException) {
      Linalg.solve(matrix, (List<Number>) null)
    }.message.contains('cannot be null'))

    assertTrue(assertThrows(IllegalArgumentException) {
      Linalg.eigenvalues((Grid) null)
    }.message.contains('cannot be null'))

    assertTrue(assertThrows(IllegalArgumentException) {
      Linalg.svd((Matrix) null)
    }.message.contains('cannot be null'))
  }

  @Test
  void testRejectsRaggedGridInputForSvd() {
    Grid<Number> grid = new Grid<Number>()
    grid << [1.0, 2.0]
    grid << [3.0]

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Linalg.svd(grid)
    }

    assertTrue(exception.message.contains('same length'))
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

  private static void assertGridEquals(double[][] expected, Grid<BigDecimal> actual, double tolerance = TOLERANCE) {
    Map<String, Integer> dimensions = actual.dimensions()
    assertEquals(expected.length, dimensions.observations)
    assertEquals(expected[0].length, dimensions.variables)
    for (int row = 0; row < expected.length; row++) {
      for (int col = 0; col < expected[row].length; col++) {
        assertEquals(expected[row][col], actual[row, col] as double, tolerance, "Mismatch at [${row},${col}]")
      }
    }
  }

  private static void assertVectorEquals(double[] expected, List<BigDecimal> actual, double tolerance = TOLERANCE) {
    assertEquals(expected.length, actual.size())
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual[i] as double, tolerance, "Mismatch at index ${i}")
    }
  }
}
