package gg.theme

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for theme_test - the stable theme for visual unit tests.
 */
class ThemeTestTest {

  @Test
  void testThemeTestBasicStructure() {
    // Test: Basic chart with theme_test
    // Verify: Renders with expected theme characteristics
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [2, 4], [3, 6], [4, 8]])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
                geom_point() +
                theme_test()

    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('</svg>'))
  }

  @Test
  void testNoGridLines() {
    // Test: theme_test should have no grid lines
    // Verify: No grid line elements in theme
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [2, 4], [3, 6]])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
                geom_line() +
                theme_test()

    def theme = chart.theme

    // Grid lines should be explicitly null
    assertNull(theme.panelGridMajor, "Major grid should be null")
    assertNull(theme.panelGridMinor, "Minor grid should be null")
    assertTrue(theme.explicitNulls.contains('panelGridMajor'))
    assertTrue(theme.explicitNulls.contains('panelGridMinor'))
  }

  @Test
  void testNoAxisLines() {
    // Test: theme_test should have no axis lines
    // Verify: Axis lines are null
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                ylim(0, 10) +
                theme_test()

    def theme = chart.theme

    // Axis lines should be explicitly null
    assertNull(theme.axisLineX, "X-axis line should be null")
    assertNull(theme.axisLineY, "Y-axis line should be null")
    assertTrue(theme.explicitNulls.contains('axisLineX'))
    assertTrue(theme.explicitNulls.contains('axisLineY'))
  }

  @Test
  void testHasAxisTicksAndLabels() {
    // Test: theme_test should retain axis ticks and labels
    // Verify: Ticks and text elements are present
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                theme_test()

    def theme = chart.theme

    // Ticks should be present
    assertNotNull(theme.axisTicksX, "X-axis ticks should exist")
    assertNotNull(theme.axisTicksY, "Y-axis ticks should exist")

    // Text elements should be present
    assertNotNull(theme.axisTextX, "X-axis text should exist")
    assertNotNull(theme.axisTextY, "Y-axis text should exist")
    assertNotNull(theme.axisTitleX, "X-axis title should exist")
    assertNotNull(theme.axisTitleY, "Y-axis title should exist")
  }

  @Test
  void testBaseSizeParameter() {
    // Test: Custom base size affects text elements
    // Verify: Font sizes scale with base_size parameter
    def chartDefault = ggplot(null, null) + theme_test()
    def chartLarge = ggplot(null, null) + theme_test(16)

    assertEquals(11, chartDefault.theme.baseSize)
    assertEquals(16, chartLarge.theme.baseSize)

    // Text sizes should scale
    assertTrue(chartLarge.theme.axisTextX.size > chartDefault.theme.axisTextX.size)
  }

  @Test
  void testBaseFamilyParameter() {
    // Test: Custom font family is applied
    // Verify: Font family propagates to text elements
    def chart = ggplot(null, null) + theme_test(11, 'monospace')

    assertEquals('monospace', chart.theme.baseFamily)
    assertEquals('monospace', chart.theme.axisTextX.family)
    assertEquals('monospace', chart.theme.plotTitle.family)
  }

  @Test
  void testPanelBackground() {
    // Test: Panel has white background with subtle border
    // Verify: Background rect has correct properties
    def chart = ggplot(null, null) + theme_test()
    def theme = chart.theme

    assertNotNull(theme.panelBackground)
    assertEquals('white', theme.panelBackground.fill)
    assertEquals('#CCCCCC', theme.panelBackground.color)
  }

  @Test
  void testStripBackgroundForFacets() {
    // Test: Facet strips have appropriate styling
    // Verify: Strip elements are configured
    def chart = ggplot(null, null) + theme_test()
    def theme = chart.theme

    assertNotNull(theme.stripBackground)
    assertEquals('white', theme.stripBackground.fill)
    assertNotNull(theme.stripText)
  }

  @Test
  void testLegendElements() {
    // Test: Legend has standard settings
    // Verify: Legend elements are present and styled
    def chart = ggplot(null, null) + theme_test()
    def theme = chart.theme

    assertNotNull(theme.legendBackground)
    assertNotNull(theme.legendKey)
    assertNotNull(theme.legendText)
    assertNotNull(theme.legendTitle)
    assertEquals('white', theme.legendBackground.fill)
  }

  @Test
  void testWithVariousGeoms() {
    // Test: theme_test works with different geom types
    // Verify: Renders successfully with points, lines, and bars
    def dataDiscrete = Matrix.builder()
        .columnNames(['category', 'value'])
        .rows([['A', 10], ['B', 20], ['C', 15]])
        .build()

    def dataNumeric = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 10], [2, 20], [3, 15]])
        .build()

    // Points
    def chartPoint = ggplot(dataDiscrete, aes(x: 'category', y: 'value')) +
                     geom_point() +
                     theme_test()
    assertNotNull(chartPoint.render())

    // Lines (use numeric data)
    def chartLine = ggplot(dataNumeric, aes(x: 'x', y: 'y')) +
                    geom_line() +
                    theme_test()
    assertNotNull(chartLine.render())

    // Bars
    def chartBar = ggplot(dataDiscrete, aes(x: 'category', y: 'value')) +
                   geom_col() +
                   theme_test()
    assertNotNull(chartBar.render())
  }

  @Test
  void testStabilityWithLabels() {
    // Test: Works correctly with titles and labels
    // Verify: Labels render with theme_test styling
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [2, 4]])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
                geom_point() +
                ggtitle('Test Plot') +
                xlab('X Axis') +
                ylab('Y Axis') +
                theme_test()

    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('Test Plot'))
    assertTrue(svgContent.contains('X Axis'))
    assertTrue(svgContent.contains('Y Axis'))
  }

  @Test
  void testThemeCanBeCustomized() {
    // Test: theme_test can be further customized
    // Verify: Additional theme() calls can modify theme_test
    def chart = ggplot(null, null) +
                theme_test() +
                theme([
                    'plot.background': new se.alipsa.matrix.gg.theme.ElementRect(fill: '#F0F0F0')
                ])

    def theme = chart.theme
    assertEquals('#F0F0F0', theme.plotBackground.fill)

    // Original theme_test characteristics should remain
    assertNull(theme.panelGridMajor)
    assertNull(theme.axisLineX)
  }
}
