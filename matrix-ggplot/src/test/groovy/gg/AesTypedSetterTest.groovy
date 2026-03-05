package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertInstanceOf
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.I
import static se.alipsa.matrix.gg.GgPlot.factor

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
  void testClosureAssignmentWrapsExpression() {
    Aes aes = new Aes()
    aes.x = { row -> row.mpg * 2 }

    assertTrue(aes.isExpression('x'))
    assertTrue(aes.x instanceof Closure)
    assertNotNull(aes.getExpression('x'))
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
