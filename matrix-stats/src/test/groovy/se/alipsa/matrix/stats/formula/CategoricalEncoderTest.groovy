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

  private static final String COLUMN_NAME = 'category'

  @Test
  void levelsConvertsNullLevelsToTheStringNull() {
    Matrix data = Matrix.builder().columns((COLUMN_NAME): ['a', null, 'b']).build()

    assertEquals(['a', 'b', 'null'], new CategoricalEncoder(data).levels(COLUMN_NAME))
  }

  @Test
  void levelsAndEncodedIndicatorsUseTheSameNumericOrder() {
    Matrix data = Matrix.builder().columns((COLUMN_NAME): [10, 2, 1]).build()
    CategoricalEncoder encoder = new CategoricalEncoder(data)

    List<String> levels = encoder.levels(COLUMN_NAME)
    Map<String, List<BigDecimal>> encoded = encoder.encode(COLUMN_NAME, ContrastType.TREATMENT)

    assertEquals(['1', '10', '2'], levels)
    assertEquals(levels.subList(1, levels.size()), encoded.keySet().collect { String name ->
      name - "${COLUMN_NAME}_"
    })
  }
}
