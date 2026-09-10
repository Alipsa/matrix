package util

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotSame

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
}
