package gg

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertInstanceOf
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.I
import static se.alipsa.matrix.gg.GgPlot.after_scale
import static se.alipsa.matrix.gg.GgPlot.after_stat
import static se.alipsa.matrix.gg.GgPlot.cut_width
import static se.alipsa.matrix.gg.GgPlot.factor

import org.junit.jupiter.api.Test

import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity

class AesTypedSetterTest {

  @Test
  void testStringAssignmentUsesTypedSetter() {
    Aes aes = new Aes()
    aes.x = 'mpg'

    assertEquals('mpg', aes.x)
  }

  @Test
  void testIdentityAssignmentIsConstant() {
    Aes aes = new Aes()
    aes.x = I(42)

    assertTrue(aes.isConstant('x'))
    assertEquals(42, aes.getConstantValue('x'))
    assertInstanceOf(Identity, aes.x)
  }

  @Test
  void testFactorAssignmentIsFactor() {
    Aes aes = new Aes()
    aes.x = factor('cyl')

    assertTrue(aes.isFactor('x'))
    assertEquals('cyl', (aes.getFactor('x') as Factor).value)
  }

  @Test
  void testClosureAssignmentIsExpressionCompatible() {
    Aes aes = new Aes()
    aes.x = { row -> row.mpg * 2 }

    assertTrue(aes.isExpression('x'))
    assertTrue(aes.x instanceof Closure)
    assertNotNull(aes.getExpression('x'))
  }

  @Test
  void testAfterStatAssignment() {
    Aes aes = new Aes()
    aes.y = after_stat('count')

    assertTrue(aes.isAfterStat('y'))
    assertEquals('count', aes.getAfterStatName('y'))
  }

  @Test
  void testAfterScaleAssignment() {
    Aes aes = new Aes()
    aes.color = after_scale('fill')

    assertTrue(aes.isAfterScale('color'))
    assertEquals('fill', aes.getAfterScaleName('color'))
  }

  @Test
  void testCutWidthAssignment() {
    Aes aes = new Aes()
    aes.group = cut_width('displ', 1)

    assertTrue(aes.isCutWidth('group'))
    assertEquals('displ', aes.getCutWidth('group').column)
  }

  @Test
  void testAssignAestheticFallbackForUnmappedType() {
    List<Integer> values = [1, 2, 3]
    Aes aes = new Aes([x: values])

    assertTrue(aes.x instanceof List)
    assertEquals(values, aes.x)
  }

  @Test
  void testMapConstructorSupportsWrappersAndAliases() {
    Aes aes = new Aes([
        x        : factor('cyl'),
        color    : I('red'),
        lineWidth: 'widthCol',
        mapId    : 'idCol'
    ])
    Aes colourAlias = new Aes([colour: 'species'])

    assertTrue(aes.isFactor('x'))
    assertInstanceOf(Identity, aes.color)
    assertEquals('widthCol', aes.linewidth)
    assertEquals('idCol', aes.map_id)
    assertEquals('species', colourAlias.color)
  }
}
