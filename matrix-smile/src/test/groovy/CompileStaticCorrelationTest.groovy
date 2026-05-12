import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.stats.CorrelationMethod
import se.alipsa.matrix.smile.stats.SmileStats

@CompileStatic
class CompileStaticCorrelationTest {

  @Test
  void testCorrelationMatrixExplicitNull() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.0, 4.0, 6.0, 8.0, 10.0]
        )
        .types([Double, Double])
        .build()

    // This should compile unambiguously as (Matrix, List<String>)
    Matrix corMatrix = SmileStats.correlationMatrix(matrix, (List<String>) null)
    assertNotNull(corMatrix)
    assertEquals(2, corMatrix.rowCount())
  }

  @Test
  void testCorrelationMatrixWithMethod() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.0, 4.0, 6.0, 8.0, 10.0]
        )
        .types([Double, Double])
        .build()

    Matrix corMatrix = SmileStats.correlationMatrix(matrix, CorrelationMethod.SPEARMAN)
    assertNotNull(corMatrix)
    assertEquals(2, corMatrix.rowCount())
  }

  @Test
  void testPValueMatrixExplicitNull() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.0, 4.0, 6.0, 8.0, 10.0]
        )
        .types([Double, Double])
        .build()

    Matrix pMatrix = SmileStats.pValueMatrix(matrix, (List<String>) null)
    assertNotNull(pMatrix)
    assertEquals(2, pMatrix.rowCount())
  }

  @Test
  void testCorrelationWithSignificanceExplicitNull() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.0, 4.0, 6.0, 8.0, 10.0]
        )
        .types([Double, Double])
        .build()

    Map<String, Matrix> result = SmileStats.correlationWithSignificance(matrix, (List<String>) null)
    assertNotNull(result)
    assertTrue(result.containsKey('correlation'))
    assertTrue(result.containsKey('pvalue'))
  }

}
