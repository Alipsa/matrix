package se.alipsa.matrix.core.util

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

class ValueComparisonTest {

  @Test
  void testGroovyEqualCharacterStringAndNumberValuesHaveEqualHashes() {
    Character character = 'a' as Character

    assertFalse(ValueComparison.valuesAreDifferent(character, 'a', BigDecimal.ZERO))
    assertFalse(ValueComparison.valuesAreDifferent('a', 97, BigDecimal.ZERO))
    assertFalse(ValueComparison.valuesAreDifferent(97, character, BigDecimal.ZERO))
    assertEquals(ValueComparison.normalizedValueHash(character), ValueComparison.normalizedValueHash('a'))
    assertEquals(ValueComparison.normalizedValueHash('a'), ValueComparison.normalizedValueHash(97))
  }

  @Test
  void testNormalizedHashUsesCharacterRangeOnlyForIntegralValues() {
    assertEquals(65_535, ValueComparison.normalizedValueHash(65_535))
    assertEquals(new BigDecimal('65536').hashCode(), ValueComparison.normalizedValueHash(65_536))
    assertEquals(new BigDecimal('-1').hashCode(), ValueComparison.normalizedValueHash(-1))
    assertEquals(new BigDecimal('0.5').hashCode(), ValueComparison.normalizedValueHash(0.5d))
  }

  @Test
  void testToleranceAppliesOnlyToOrderedSequences() {
    BigDecimal allowedDiff = 0.0001

    assertFalse(ValueComparison.valuesAreDifferent([1.0d], [1.00005d], allowedDiff))
    assertFalse(ValueComparison.valuesAreDifferent([1.0d] as double[], [1.00005d] as double[], allowedDiff))
    assertTrue(ValueComparison.valuesAreDifferent([1.0d] as LinkedHashSet, [1.00005d] as LinkedHashSet, allowedDiff))
    assertTrue(ValueComparison.valuesAreDifferent([value: 1.0d], [value: 1.00005d], allowedDiff))
  }

  @Test
  void testMapsKeepGroovyNumericKeySemantics() {
    assertTrue(ValueComparison.valuesAreDifferent([1: 'value'], [1.0d: 'value'], BigDecimal.ZERO))
    assertFalse(ValueComparison.valuesAreDifferent([value: 1], [value: 1.0d], BigDecimal.ZERO))
  }
}
