package util

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.util.DoubleArrayUtils

class DoubleArrayUtilsTest {

  @Test
  void deepCopyPreservesRaggedRowsWithoutAliasing() {
    double[][] source = [[1.0d], [2.0d, 3.0d]] as double[][]

    double[][] copy = DoubleArrayUtils.deepCopy(source)

    assertNotSame(source, copy)
    assertEquals(source.length, copy.length)
    for (int i = 0; i < source.length; i++) {
      assertNotSame(source[i], copy[i])
      assertArrayEquals(source[i], copy[i])
    }
  }

  @Test
  void copyMethodsRejectNullSourcesDescriptively() {
    IllegalArgumentException vectorException = assertThrows(IllegalArgumentException) {
      DoubleArrayUtils.copy(null)
    }
    assertEquals('Source array cannot be null', vectorException.message)

    IllegalArgumentException matrixException = assertThrows(IllegalArgumentException) {
      DoubleArrayUtils.deepCopy(null)
    }
    assertEquals('Source matrix cannot be null', matrixException.message)

    IllegalArgumentException rowException = assertThrows(IllegalArgumentException) {
      DoubleArrayUtils.deepCopy([[1.0d] as double[], null] as double[][])
    }
    assertEquals('Source matrix row 1 cannot be null', rowException.message)
  }
}
