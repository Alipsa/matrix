package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleAlphaContinuous
import se.alipsa.matrix.gg.scale.ScaleAlphaBinned
import se.alipsa.matrix.gg.scale.ScaleAlphaDiscrete
import se.alipsa.matrix.gg.scale.ScaleColorBrewer
import se.alipsa.matrix.gg.scale.ScaleColorDistiller
import se.alipsa.matrix.gg.scale.ScaleColorGradientN
import se.alipsa.matrix.gg.scale.ScaleColorGrey
import se.alipsa.matrix.gg.scale.ScaleSizeArea
import se.alipsa.matrix.gg.scale.ScaleSizeBinned
import se.alipsa.matrix.gg.scale.ScaleSizeContinuous
import se.alipsa.matrix.gg.scale.ScaleSizeDiscrete
import se.alipsa.matrix.gg.scale.ScaleRadius

import static org.junit.jupiter.api.Assertions.assertEquals

class ScaleBrewerAlphaSizeTest {

  @Test
  void testScaleColorBrewerSet1() {
    ScaleColorBrewer scale = new ScaleColorBrewer(palette: 'Set1')
    scale.train(['A', 'B', 'C'])

    assertEquals('#E41A1C', scale.transform('A'))
    assertEquals('#FF7F00', scale.transform('B'))
    assertEquals('#999999', scale.transform('C'))
  }

  @Test
  void testScaleColorDistillerBluesEndpoints() {
    ScaleColorDistiller scale = new ScaleColorDistiller(palette: 'Blues')
    scale.train([0, 100])

    assertEquals('#F7FBFF', scale.transform(0))
    assertEquals('#08306B', scale.transform(100))
  }

  @Test
  void testScaleColorGreyRange() {
    ScaleColorGrey scale = new ScaleColorGrey(start: 0.2, end: 0.8)
    scale.train(['A', 'B'])

    assertEquals('#333333', scale.transform('A'))
    assertEquals('#CCCCCC', scale.transform('B'))
  }

  @Test
  void testScaleColorGradientNMidpoint() {
    ScaleColorGradientN scale = new ScaleColorGradientN(colors: ['#FF0000', '#00FF00', '#0000FF'])
    scale.train([0, 100])

    assertEquals('#FF0000', scale.transform(0))
    assertEquals('#00FF00', scale.transform(50))
    assertEquals('#0000FF', scale.transform(100))
  }

  @Test
  void testScaleAlphaContinuous() {
    ScaleAlphaContinuous scale = new ScaleAlphaContinuous(range: [0.2, 0.8])
    scale.train([0, 10])

    assertEquals(0.2d, scale.transform(0) as double, 1e-9)
    assertEquals(0.8d, scale.transform(10) as double, 1e-9)
  }

  @Test
  void testScaleAlphaDiscrete() {
    ScaleAlphaDiscrete scale = new ScaleAlphaDiscrete(range: [0.2, 1.0])
    scale.train(['A', 'B'])

    assertEquals(0.2d, scale.transform('A') as double, 1e-9)
    assertEquals(1.0d, scale.transform('B') as double, 1e-9)
  }

  @Test
  void testScaleAlphaBinned() {
    ScaleAlphaBinned scale = new ScaleAlphaBinned(range: [0.2, 1.0], bins: 4)
    scale.train([0, 100])

    assertEquals(0.2d, scale.transform(0) as double, 1e-9)
    assertEquals(1.0d, scale.transform(100) as double, 1e-9)
    assertEquals(0.7333333333333334d, scale.transform(50) as double, 1e-9)
  }

  @Test
  void testScaleSizeContinuous() {
    ScaleSizeContinuous scale = new ScaleSizeContinuous(range: [1.0, 5.0])
    scale.train([0, 10])

    assertEquals(1.0d, scale.transform(0) as double, 1e-9)
    assertEquals(5.0d, scale.transform(10) as double, 1e-9)
  }

  @Test
  void testScaleSizeDiscrete() {
    ScaleSizeDiscrete scale = new ScaleSizeDiscrete(range: [1.0, 5.0])
    scale.train(['A', 'B'])

    assertEquals(1.0d, scale.transform('A') as double, 1e-9)
    assertEquals(5.0d, scale.transform('B') as double, 1e-9)
  }

  @Test
  void testScaleSizeBinned() {
    ScaleSizeBinned scale = new ScaleSizeBinned(range: [1.0, 5.0], bins: 4)
    scale.train([0, 100])

    assertEquals(1.0d, scale.transform(0) as double, 1e-9)
    assertEquals(5.0d, scale.transform(100) as double, 1e-9)
  }

  @Test
  void testScaleSizeAreaMidpoint() {
    ScaleSizeArea scale = new ScaleSizeArea(range: [1.0, 5.0])
    scale.train([0, 10])

    double mid = scale.transform(5) as double
    double expected = Math.sqrt((1.0d * 1.0d + 5.0d * 5.0d) / 2.0d)
    assertEquals(expected, mid, 1e-9)
  }

  @Test
  void testScaleRadiusUsesContinuousMapping() {
    ScaleRadius scale = new ScaleRadius(range: [2.0, 6.0])
    scale.train([0, 10])

    assertEquals(2.0d, scale.transform(0) as double, 1e-9)
    assertEquals(6.0d, scale.transform(10) as double, 1e-9)
  }
}
