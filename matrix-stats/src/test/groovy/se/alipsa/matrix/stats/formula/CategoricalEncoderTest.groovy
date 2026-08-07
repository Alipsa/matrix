package se.alipsa.matrix.stats.formula

import static org.junit.jupiter.api.Assertions.assertEquals

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

/**
 * Tests categorical level discovery.
 */
@CompileStatic
class CategoricalEncoderTest {

  @Test
  void levelsConvertsNullLevelsToTheStringNull() {
    Matrix data = Matrix.builder().columns(category: ['a', null, 'b']).build()

    assertEquals(['a', 'b', 'null'], new CategoricalEncoder(data).levels('category'))
  }
}
