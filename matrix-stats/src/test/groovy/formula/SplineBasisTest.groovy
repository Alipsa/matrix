package formula

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.formula.SplineBasisExpander

@CompileStatic
class SplineBasisTest {

  @Test
  void testSplineBasisColumns() {
    double[] x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0] as double[]
    int df = 4
    double[][] basis = SplineBasisExpander.naturalCubicSplineBasis(x, df)

    // df columns, n rows
    assertEquals(10, basis.length)
    assertEquals(4, basis[0].length)
    // column 0 is centered x — mean of x is 5.5, so values should be centered
    double expectedCenter = -4.5 // 1.0 - 5.5
    assertEquals(expectedCenter, basis[0][0], 1e-10, "First column should be centered x")
    // no constant column: verify column 0 is NOT all ones
    boolean allOnes = true
    for (int i = 0; i < 10; i++) {
      if (Math.abs(basis[i][0] - 1.0d) > 1e-10) {
        allOnes = false
        break
      }
    }
    assertFalse(allOnes, "Basis must not contain an intercept column")
  }

  @Test
  void testSplineBasisDf1() {
    double[] x = [1.0, 2.0, 3.0] as double[]
    double[][] basis = SplineBasisExpander.naturalCubicSplineBasis(x, 1)

    assertEquals(3, basis.length)
    assertEquals(1, basis[0].length)
    // Should be centered x (mean = 2.0)
    assertEquals(-1.0, basis[0][0], 1e-10)
    assertEquals(0.0, basis[1][0], 1e-10)
    assertEquals(1.0, basis[2][0], 1e-10)
  }

  @Test
  void testSplineBasisRejectsInvalidDf() {
    double[] x = [1.0, 2.0] as double[]
    assertThrows(IllegalArgumentException) {
      SplineBasisExpander.naturalCubicSplineBasis(x, 0)
    }
  }

  @Test
  @CompileDynamic
  void testSmoothTermInDesignMatrix() {
    List<List> rows = (1..20).collect { int i ->
      [i as BigDecimal, (Math.sin(i * 0.3) * 10) as BigDecimal]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ s(x)', data).evaluate()

    // s(x) with default df=4 should produce 4 basis columns
    assertEquals(4, frame.data.columnCount())
    assertTrue(frame.predictorNames[0].startsWith('s_x_'))
    assertEquals(20, frame.response.size())
  }

  @Test
  @CompileDynamic
  void testSmoothTermWithExplicitDf() {
    List<List> rows = (1..20).collect { int i ->
      [i as BigDecimal, (i * i) as BigDecimal]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ s(x, 6)', data).evaluate()

    assertEquals(6, frame.data.columnCount())
  }

  @Test
  void testRejectSmoothTermInInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'z', 'y'])
      .rows([
        [1.0, 2.0, 3.0],
        [2.0, 3.0, 5.0],
        [3.0, 4.0, 7.0],
        [4.0, 5.0, 9.0],
        [5.0, 6.0, 11.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ s(x):z', data).evaluate()
    }

    assertTrue(ex.message.contains('Smooth terms cannot be used in interactions'))
  }

  @Test
  void testRejectExpandedSmoothInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'z', 'y'])
      .rows([
        [1.0, 2.0, 3.0],
        [2.0, 3.0, 5.0],
        [3.0, 4.0, 7.0],
        [4.0, 5.0, 9.0],
        [5.0, 6.0, 11.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ s(x) * z', data).evaluate()
    }

    assertTrue(ex.message.contains('Smooth terms cannot be used in interactions'))
  }
}
