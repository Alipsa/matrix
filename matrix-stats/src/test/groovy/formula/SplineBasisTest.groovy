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
    for (int column = 0; column < df; column++) {
      double mean = 0.0d
      for (int row = 0; row < basis.length; row++) {
        mean += basis[row][column]
      }
      mean /= basis.length
      assertEquals(0.0d, mean, 1e-10, "Basis column ${column} should be centered")
    }
    assertNotEquals(-4.5d, basis[0][0], 1e-10, 'Basis should not include a raw centered x column')
  }

  @Test
  void testSplineBasisDf1() {
    double[] x = [1.0, 2.0, 3.0] as double[]
    double[][] basis = SplineBasisExpander.naturalCubicSplineBasis(x, 1)

    assertEquals(3, basis.length)
    assertEquals(1, basis[0].length)
    double mean = 0.0d
    for (int row = 0; row < basis.length; row++) {
      mean += basis[row][0]
    }
    mean /= basis.length
    assertEquals(0.0d, mean, 1e-10)
    assertNotEquals(-1.0d, basis[0][0], 1e-10, 'df=1 basis should not degrade to centered x')
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
  @CompileDynamic
  void testSmoothTermDoesNotDuplicateLinearX() {
    List<List> rows = (1..30).collect { int i ->
      BigDecimal x = i * 0.2
      BigDecimal y = (x * 3.0) + (Math.sin(x as double) as BigDecimal)
      [x, y]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x + s(x)', data).evaluate()

    assertEquals(5, frame.data.columnCount())
    assertTrue(frame.predictorNames.contains('x'))
    assertTrue(frame.predictorNames.any { String name -> name.startsWith('s_x_') })
    Set<BigDecimal> xColumnValues = (0..<frame.data.rowCount()).collect { int i ->
      frame.data[i, 'x'] as BigDecimal
    } as Set<BigDecimal>
    frame.predictorNames.findAll { String name -> name.startsWith('s_x_') }.each { String smoothName ->
      Set<BigDecimal> smoothColumnValues = (0..<frame.data.rowCount()).collect { int i ->
        frame.data[i, smoothName] as BigDecimal
      } as Set<BigDecimal>
      assertNotEquals(xColumnValues, smoothColumnValues, "Smooth basis column ${smoothName} must not duplicate the linear x column")
    }
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
