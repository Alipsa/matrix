package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleAlphaBinned
import se.alipsa.matrix.gg.scale.ScaleAlphaContinuous
import se.alipsa.matrix.gg.scale.ScaleAlphaDiscrete
import se.alipsa.matrix.gg.scale.ScaleSizeBinned
import se.alipsa.matrix.gg.scale.ScaleSizeContinuous
import se.alipsa.matrix.gg.scale.ScaleSizeDiscrete
import se.alipsa.matrix.gg.scale.ScaleUtils

import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

class ScaleNaValueTest {

  @Test
  void testNaValueCoercionForAlphaScales() {
    def cases = [
        [new ScaleAlphaContinuous(naValue: 0.5d), new BigDecimal('0.5')],
        [new ScaleAlphaBinned(naValue: 0.25f), new BigDecimal('0.25')],
        [new ScaleAlphaDiscrete(naValue: 1), new BigDecimal('1')],
        [new ScaleAlphaContinuous(naValue: '0.75'), new BigDecimal('0.75')]
    ]

    cases.each { scale, expected ->
      assertTrue(scale.naValue instanceof BigDecimal)
      assertTrue(scale.naValue.compareTo(expected) == 0)
    }
  }

  @Test
  void testNaValueCoercionForSizeScales() {
    def cases = [
        [new ScaleSizeContinuous(naValue: 2.5d), new BigDecimal('2.5')],
        [new ScaleSizeBinned(naValue: 3.25f), new BigDecimal('3.25')],
        [new ScaleSizeDiscrete(naValue: 4), new BigDecimal('4')],
        [new ScaleSizeContinuous(naValue: '6.75'), new BigDecimal('6.75')]
    ]

    cases.each { scale, expected ->
      assertTrue(scale.naValue instanceof BigDecimal)
      assertTrue(scale.naValue.compareTo(expected) == 0)
    }
  }

  @Test
  void testScaleUtilsCoerceToNumber() {
    assertTrue(ScaleUtils.coerceToNumber(1.5d).compareTo(new BigDecimal('1.5')) == 0)
    assertTrue(ScaleUtils.coerceToNumber(2.25f).compareTo(new BigDecimal('2.25')) == 0)
    assertTrue(ScaleUtils.coerceToNumber(3).compareTo(new BigDecimal('3')) == 0)
    assertTrue(ScaleUtils.coerceToNumber('4.125').compareTo(new BigDecimal('4.125')) == 0)
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
