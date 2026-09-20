package charm.render.scale

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.render.scale.ScaleUtils

@SuppressWarnings('ExplicitCallToCompareToMethod')
class ScaleUtilsTest {

  @Test
  void strictNumericParsingRejectsDigitBearingLabels() {
    assertTrue(ScaleUtils.isStrictNumeric(3))
    assertTrue(ScaleUtils.isStrictNumeric(' -1.5e3 '))
    assertFalse(ScaleUtils.isStrictNumeric('Q1'))
    assertFalse(ScaleUtils.isStrictNumeric('g1'))
    assertFalse(ScaleUtils.isStrictNumeric('1,234'))
    assertFalse(ScaleUtils.isStrictNumeric(Double.NaN))
    assertNull(ScaleUtils.coerceStrictNumber('Q1'))
  }

  @Test
  void niceNumUsesDecimalMagnitudeForSmallAndLargeNumbers() {
    assertEquals(0, ScaleUtils.niceNum(0.00003, true).compareTo(0.00005))
    assertEquals(0, ScaleUtils.niceNum(0.0015, true).compareTo(0.002))
    assertEquals(0, ScaleUtils.niceNum(30_000_000, true).compareTo(50_000_000))
    assertEquals(0, ScaleUtils.niceNum(-1.5, true).compareTo(-2))
  }
}
