package se.alipsa.matrix.core.util

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class DecimalColumnProfileTest {

  @Test
  void testProfilesCompatiblePrecisionAndScale() {
    DecimalColumnProfile small = DecimalColumnProfile.profile([0.001g, 0.002g])
    DecimalColumnProfile mixed = DecimalColumnProfile.profile([123.4g, 0.001g])

    assertTrue(small.hasValues)
    assertEquals(4, small.precision)
    assertEquals(3, small.scale)
    assertEquals(6, mixed.precision)
    assertEquals(3, mixed.scale)
  }

  @Test
  void testIgnoresNullsAndReportsEmptyInput() {
    DecimalColumnProfile empty = DecimalColumnProfile.profile([null, null])

    assertFalse(empty.hasValues)
    assertEquals(0, empty.precision)
    assertEquals(0, empty.scale)
  }

  @Test
  void testAccommodatesNegativeScale() {
    DecimalColumnProfile profile = DecimalColumnProfile.profile([new BigDecimal('1E+3'), 0.01g])

    assertEquals(6, profile.precision)
    assertEquals(2, profile.scale)
  }

  @Test
  void testMergesProfilesWithoutLosingIntegerDigitsOrScale() {
    DecimalColumnProfile smallScale = DecimalColumnProfile.profile([123.4g])    // precision 4, scale 1 (3 integer digits)
    DecimalColumnProfile longInteger = DecimalColumnProfile.profile([1234g])    // precision 4, scale 0 (4 integer digits)
    DecimalColumnProfile empty = DecimalColumnProfile.profile([null])

    DecimalColumnProfile merged = smallScale.merge(longInteger)
    assertEquals(5, merged.precision)
    assertEquals(1, merged.scale)

    assertSame(smallScale, smallScale.merge(empty))
    assertSame(longInteger, empty.merge(longInteger))
    assertSame(smallScale, smallScale.merge(null))
  }
}
