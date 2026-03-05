package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.AfterScale
import se.alipsa.matrix.gg.aes.AfterStat
import se.alipsa.matrix.gg.aes.CutWidth
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class AesDslTest {

  @Test
  void testBasicUnquotedColumnNames() {
    Aes mapping = aes { x = mpg; y = wt }

    assertEquals('mpg', mapping.x)
    assertEquals('wt', mapping.y)
  }

  @Test
  void testQuotedColumnNamesWithSpaces() {
    Aes mapping = aes { x = 'Sepal Length'; y = 'Petal Width' }

    assertEquals('Sepal Length', mapping.x)
    assertEquals('Petal Width', mapping.y)
  }

  @Test
  void testMixedQuotedAndUnquotedMappings() {
    Aes mapping = aes { x = displ; y = 'Highway MPG' }

    assertEquals('displ', mapping.x)
    assertEquals('Highway MPG', mapping.y)
  }

  @Test
  void testColourAliasSetsColor() {
    Aes mapping = aes { colour = species }

    assertEquals('species', mapping.color)
  }

  @Test
  void testColAliasSetsColor() {
    Aes mapping = aes { col = species }

    assertEquals('species', mapping.color)
  }

  @Test
  void testClosureWithParameterUsesLegacyPositionalXBehavior() {
    Aes mapping = aes { row -> row.mpg }

    assertTrue(mapping.x instanceof Closure)
    assertEquals(1, (mapping.x as Closure).maximumNumberOfParameters)
    assertNull(mapping.y)
  }

  @Test
  void testImplicitItExpressionClosureUsesLegacyPositionalXBehavior() {
    Aes mapping = aes { it.mpg }

    assertTrue(mapping.x instanceof Closure)
    assertEquals(1, (mapping.x as Closure).maximumNumberOfParameters)
    assertNull(mapping.y)
  }

  @Test
  void testIdentityWrapperInClosureAes() {
    Aes mapping = aes { x = mpg; color = I('red') }

    assertEquals('mpg', mapping.x)
    assertTrue(mapping.color instanceof Identity)
    assertEquals('red', (mapping.color as Identity).value)
  }

  @Test
  void testFactorWrapperInClosureAes() {
    Aes mapping = aes { x = factor(cyl); y = mpg }

    assertTrue(mapping.x instanceof Factor)
    assertEquals('cyl', (mapping.x as Factor).value)
    assertEquals('mpg', mapping.y)
  }

  @Test
  void testAllKnownAestheticsInClosureAes() {
    Aes mapping = aes {
      x = xValue
      y = yValue
      color = colorValue
      fill = fillValue
      size = sizeValue
      shape = shapeValue
      alpha = alphaValue
      linetype = linetypeValue
      linewidth = linewidthValue
      group = groupValue
      label = labelValue
      tooltip = tooltipValue
      weight = weightValue
      geometry = geometryValue
      map_id = mapIdValue
    }

    assertEquals('xValue', mapping.x)
    assertEquals('yValue', mapping.y)
    assertEquals('colorValue', mapping.color)
    assertEquals('fillValue', mapping.fill)
    assertEquals('sizeValue', mapping.size)
    assertEquals('shapeValue', mapping.shape)
    assertEquals('alphaValue', mapping.alpha)
    assertEquals('linetypeValue', mapping.linetype)
    assertEquals('linewidthValue', mapping.linewidth)
    assertEquals('groupValue', mapping.group)
    assertEquals('labelValue', mapping.label)
    assertEquals('tooltipValue', mapping.tooltip)
    assertEquals('weightValue', mapping.weight)
    assertEquals('geometryValue', mapping.geometry)
    assertEquals('mapIdValue', mapping.map_id)
  }

  @Test
  void testCamelCaseAliasesForLineWidthAndMapId() {
    Aes mapping = aes { lineWidth = widthCol; mapId = idCol }

    assertEquals('widthCol', mapping.linewidth)
    assertEquals('idCol', mapping.map_id)
  }

  @Test
  void testColumnNamesMatchingAestheticNames() {
    Aes mapping = aes { x = x; y = y; color = color; fill = fill }

    assertEquals('x', mapping.x)
    assertEquals('y', mapping.y)
    assertEquals('color', mapping.color)
    assertEquals('fill', mapping.fill)
  }

  @Test
  void testReadingAssignedAestheticUsesAssignedValue() {
    Aes mapping = aes { x = mpg; y = x }

    assertEquals('mpg', mapping.x)
    assertEquals('mpg', mapping.y)
  }

  @Test
  void testClosureAesRendersPointChart() {
    Matrix data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([
            [1, 2],
            [2, 4],
            [3, 6],
            [4, 8]
        ])
        .build()

    def chart = ggplot(data, aes { x = x; y = y }) + geom_point()
    Svg svg = chart.render()

    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<svg'))
    assertTrue(svg.descendants().any { it instanceof Circle })
  }

  @Test
  void testLayerLevelClosureAesMapping() {
    Matrix data = Matrix.builder()
        .columnNames(['xValue', 'yValue', 'species'])
        .rows([
            [1, 2, 'a'],
            [2, 4, 'b'],
            [3, 3, 'a'],
            [4, 5, 'b']
        ])
        .build()

    def chart = ggplot(data, aes { x = xValue; y = yValue }) +
        geom_point(mapping: aes { color = species })

    assertEquals('xValue', chart.globalAes.x)
    assertEquals('yValue', chart.globalAes.y)
    assertNotNull(chart.layers[0].aes)
    assertEquals('species', chart.layers[0].aes.color)

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testDslToAesSupportsMixedWrapperTypes() {
    Aes mapping = aes {
      x = factor(cyl)
      y = after_stat('count')
      color = after_scale('fill')
      group = cut_width('displ', 1)
      fill = I('red')
    }

    assertInstanceOf(Factor, mapping.x)
    assertInstanceOf(AfterStat, mapping.y)
    assertInstanceOf(AfterScale, mapping.color)
    assertInstanceOf(CutWidth, mapping.group)
    assertInstanceOf(Identity, mapping.fill)
  }
}
