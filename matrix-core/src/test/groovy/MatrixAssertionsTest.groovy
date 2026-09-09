import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixAssertions

class MatrixAssertionsTest {

  @Test
  void contentNotEqualsIsExactComplementOfContentEquals() {
    Matrix expected = Matrix.builder().data(value: [1.0]).types(BigDecimal).build()
    Matrix withinTolerance = Matrix.builder().data(value: [1.000005]).types(BigDecimal).build()
    Matrix outsideTolerance = Matrix.builder().data(value: [1.00005]).types(BigDecimal).build()
    Matrix differentType = Matrix.builder().data(value: ['1.0']).types(String).build()

    MatrixAssertions.assertContentEquals(expected, withinTolerance)
    assertThrows(IllegalArgumentException) {
      MatrixAssertions.assertContentNotEquals(expected, withinTolerance)
    }
    assertThrows(IllegalArgumentException) {
      MatrixAssertions.assertContentEquals(expected, outsideTolerance)
    }
    MatrixAssertions.assertContentNotEquals(expected, outsideTolerance)
    MatrixAssertions.assertContentNotEquals(expected, differentType)
  }
}
