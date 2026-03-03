package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.LegendDirection
import se.alipsa.matrix.charm.LegendPosition
import se.alipsa.matrix.charm.LinetypeName
import se.alipsa.matrix.charm.ShapeName

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertSame

class CharmEnumTest {

  // ---- LegendPosition ----

  @Test
  void testLegendPositionFromStringCaseInsensitive() {
    assertEquals(LegendPosition.RIGHT, LegendPosition.fromString('right'))
    assertEquals(LegendPosition.RIGHT, LegendPosition.fromString('RIGHT'))
    assertEquals(LegendPosition.RIGHT, LegendPosition.fromString('Right'))
    assertEquals(LegendPosition.NONE, LegendPosition.fromString('none'))
    assertEquals(LegendPosition.TOP, LegendPosition.fromString('TOP'))
    assertEquals(LegendPosition.BOTTOM, LegendPosition.fromString('bottom'))
    assertEquals(LegendPosition.LEFT, LegendPosition.fromString('left'))
  }

  @Test
  void testLegendPositionFromStringTrimsWhitespace() {
    assertEquals(LegendPosition.RIGHT, LegendPosition.fromString('  right  '))
    assertEquals(LegendPosition.NONE, LegendPosition.fromString(' none '))
  }

  @Test
  void testLegendPositionFromStringNullAndUnknown() {
    assertNull(LegendPosition.fromString(null))
    assertNull(LegendPosition.fromString(''))
    assertNull(LegendPosition.fromString('invalid'))
    assertNull(LegendPosition.fromString('center'))
  }

  @Test
  void testLegendPositionNormalizeEnum() {
    assertSame(LegendPosition.RIGHT, LegendPosition.normalize(LegendPosition.RIGHT))
    assertSame(LegendPosition.NONE, LegendPosition.normalize(LegendPosition.NONE))
  }

  @Test
  void testLegendPositionNormalizeString() {
    assertEquals(LegendPosition.TOP, LegendPosition.normalize('top'))
    assertEquals(LegendPosition.BOTTOM, LegendPosition.normalize('BOTTOM'))
  }

  @Test
  void testLegendPositionNormalizeListPassthrough() {
    List pos = [0.5, 0.8]
    assertSame(pos, LegendPosition.normalize(pos))
  }

  @Test
  void testLegendPositionNormalizeNullPassthrough() {
    assertNull(LegendPosition.normalize(null))
  }

  @Test
  void testLegendPositionNormalizeUnknownStringPassthrough() {
    assertEquals('custom', LegendPosition.normalize('custom'))
  }

  // ---- LegendDirection ----

  @Test
  void testLegendDirectionFromStringCaseInsensitive() {
    assertEquals(LegendDirection.VERTICAL, LegendDirection.fromString('vertical'))
    assertEquals(LegendDirection.VERTICAL, LegendDirection.fromString('VERTICAL'))
    assertEquals(LegendDirection.HORIZONTAL, LegendDirection.fromString('horizontal'))
    assertEquals(LegendDirection.HORIZONTAL, LegendDirection.fromString('Horizontal'))
  }

  @Test
  void testLegendDirectionFromStringTrimsWhitespace() {
    assertEquals(LegendDirection.HORIZONTAL, LegendDirection.fromString(' horizontal '))
  }

  @Test
  void testLegendDirectionFromStringNullAndUnknown() {
    assertNull(LegendDirection.fromString(null))
    assertNull(LegendDirection.fromString(''))
    assertNull(LegendDirection.fromString('diagonal'))
  }

  @Test
  void testLegendDirectionNormalize() {
    assertSame(LegendDirection.VERTICAL, LegendDirection.normalize(LegendDirection.VERTICAL))
    assertEquals(LegendDirection.HORIZONTAL, LegendDirection.normalize('horizontal'))
    assertNull(LegendDirection.normalize(null))
  }

  @Test
  void testLegendDirectionNormalizeUnknownPassthrough() {
    assertEquals('custom', LegendDirection.normalize('custom'))
  }

  // ---- ShapeName ----

  @Test
  void testShapeNameFromStringCaseInsensitive() {
    assertEquals(ShapeName.CIRCLE, ShapeName.fromString('circle'))
    assertEquals(ShapeName.CIRCLE, ShapeName.fromString('CIRCLE'))
    assertEquals(ShapeName.SQUARE, ShapeName.fromString('Square'))
    assertEquals(ShapeName.TRIANGLE, ShapeName.fromString('triangle'))
    assertEquals(ShapeName.DIAMOND, ShapeName.fromString('diamond'))
    assertEquals(ShapeName.PLUS, ShapeName.fromString('plus'))
    assertEquals(ShapeName.X, ShapeName.fromString('x'))
    assertEquals(ShapeName.CROSS, ShapeName.fromString('cross'))
  }

  @Test
  void testShapeNameFromStringTrimsWhitespace() {
    assertEquals(ShapeName.CIRCLE, ShapeName.fromString(' circle '))
  }

  @Test
  void testShapeNameFromStringNullAndUnknown() {
    assertNull(ShapeName.fromString(null))
    assertNull(ShapeName.fromString(''))
    assertNull(ShapeName.fromString('star'))
  }

  @Test
  void testShapeNameNormalize() {
    assertSame(ShapeName.CIRCLE, ShapeName.normalize(ShapeName.CIRCLE))
    assertEquals(ShapeName.SQUARE, ShapeName.normalize('square'))
    assertNull(ShapeName.normalize(null))
  }

  @Test
  void testShapeNameNormalizeUnknownPassthrough() {
    assertEquals('custom_shape', ShapeName.normalize('custom_shape'))
    assertEquals(42, ShapeName.normalize(42))
  }

  // ---- LinetypeName ----

  @Test
  void testLinetypeNameFromStringCaseInsensitive() {
    assertEquals(LinetypeName.SOLID, LinetypeName.fromString('solid'))
    assertEquals(LinetypeName.SOLID, LinetypeName.fromString('SOLID'))
    assertEquals(LinetypeName.DASHED, LinetypeName.fromString('dashed'))
    assertEquals(LinetypeName.DOTTED, LinetypeName.fromString('dotted'))
    assertEquals(LinetypeName.DOTDASH, LinetypeName.fromString('dotdash'))
    assertEquals(LinetypeName.LONGDASH, LinetypeName.fromString('longdash'))
    assertEquals(LinetypeName.TWODASH, LinetypeName.fromString('twodash'))
  }

  @Test
  void testLinetypeNameFromStringTrimsWhitespace() {
    assertEquals(LinetypeName.DASHED, LinetypeName.fromString(' dashed '))
  }

  @Test
  void testLinetypeNameFromStringNullAndUnknown() {
    assertNull(LinetypeName.fromString(null))
    assertNull(LinetypeName.fromString(''))
    assertNull(LinetypeName.fromString('wavy'))
  }

  @Test
  void testLinetypeNameNormalize() {
    assertSame(LinetypeName.SOLID, LinetypeName.normalize(LinetypeName.SOLID))
    assertEquals(LinetypeName.DOTTED, LinetypeName.normalize('dotted'))
    assertNull(LinetypeName.normalize(null))
  }

  @Test
  void testLinetypeNameNormalizeUnknownPassthrough() {
    assertEquals('custom_line', LinetypeName.normalize('custom_line'))
    assertEquals(3, LinetypeName.normalize(3))
  }
}
