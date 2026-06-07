package gg.scale

import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.gg.scale.ScaleAlphaBinned
import se.alipsa.matrix.gg.scale.ScaleAlphaContinuous
import se.alipsa.matrix.gg.scale.ScaleAlphaDiscrete
import se.alipsa.matrix.gg.scale.ScaleSizeBinned
import se.alipsa.matrix.gg.scale.ScaleSizeContinuous
import se.alipsa.matrix.gg.scale.ScaleSizeDiscrete
import se.alipsa.matrix.gg.scale.ScaleUtils

class ScaleNaValueTest {

  @Test
  void testNaValueCoercionForAlphaScales() {
    def cases = [
        [new ScaleAlphaContinuous(naValue: 0.5d), 0.5G],
        [new ScaleAlphaBinned(naValue: 0.25f), 0.25G],
        [new ScaleAlphaDiscrete(naValue: 1), 1G],
        [new ScaleAlphaContinuous(naValue: '0.75'), 0.75G]
    ]

    cases.each { scale, expected ->
      assertTrue(scale.naValue instanceof BigDecimal)
      assertTrue((scale.naValue <=> expected) == 0)
    }
  }

  @Test
  void testNaValueCoercionForSizeScales() {
    def cases = [
        [new ScaleSizeContinuous(naValue: 2.5d), 2.5G],
        [new ScaleSizeBinned(naValue: 3.25f), 3.25G],
        [new ScaleSizeDiscrete(naValue: 4), 4G],
        [new ScaleSizeContinuous(naValue: '6.75'), 6.75G]
    ]

    cases.each { scale, expected ->
      assertTrue(scale.naValue instanceof BigDecimal)
      assertTrue((scale.naValue <=> expected) == 0)
    }
  }

  @Test
  void testScaleUtilsCoerceToNumber() {
    assertTrue((ScaleUtils.coerceToNumber(1.5d) <=> 1.5G) == 0)
    assertTrue((ScaleUtils.coerceToNumber(2.25f) <=> 2.25G) == 0)
    assertTrue((ScaleUtils.coerceToNumber(3) <=> 3G) == 0)
    assertTrue((ScaleUtils.coerceToNumber('4.125') <=> 4.125G) == 0)
    assertNull(ScaleUtils.coerceToNumber('NA'))
    assertNull(ScaleUtils.coerceToNumber('null'))
    assertNull(ScaleUtils.coerceToNumber('NULL'))
    assertNull(ScaleUtils.coerceToNumber(Double.NaN))
    assertNull(ScaleUtils.coerceToNumber(Double.POSITIVE_INFINITY))
    assertNull(ScaleUtils.coerceToNumber(Double.NEGATIVE_INFINITY))
    assertNull(ScaleUtils.coerceToNumber(Float.POSITIVE_INFINITY))
    assertNull(ScaleUtils.coerceToNumber(null))
  }
}
