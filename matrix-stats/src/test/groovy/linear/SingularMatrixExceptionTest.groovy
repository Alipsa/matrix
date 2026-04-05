package linear

import static org.junit.jupiter.api.Assertions.assertInstanceOf

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.linalg.LinalgSingularMatrixException
import se.alipsa.matrix.stats.linear.SingularMatrixException

class SingularMatrixExceptionTest {

  @Test
  void testInternalExceptionExtendsPublicLinalgException() {
    assertInstanceOf(LinalgSingularMatrixException, new SingularMatrixException('singular'))
  }
}
