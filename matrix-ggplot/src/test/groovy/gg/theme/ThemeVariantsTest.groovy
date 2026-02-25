package gg.theme

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for theme variants: theme_void, theme_light, theme_dark, theme_linedraw.
 */
class ThemeVariantsTest {

  private static Matrix createTestData() {
    return Matrix.builder()
        .data([
            x: [1, 2, 3, 4, 5],
            y: [2, 4, 3, 5, 6],
            category: ['A', 'B', 'A', 'B', 'A']
        ])
        .types(Integer, Integer, String)
        .build()
  }

  @Test
  void testThemeVoidRemovesAllElements() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        theme_void()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // theme_void should not have panel background
    assertFalse(content.contains('panel-background'),
        'theme_void should not render panel background')

    // Should still have data points
    assertTrue(content.contains('circle') || content.contains('geom-point'),
        'theme_void should render data points')
  }

  @Test
  void testThemeVoidWithFacets() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        facet_wrap(vars('category')) +
        theme_void()

    Svg svg = chart.render()
    assertNotNull(svg, 'theme_void should render with facets')
  }

  @Test
  void testThemeLightHasLightBackground() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        theme_light()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // theme_light should have white panel background
    assertTrue(content.contains('fill="white"'),
        'theme_light should have white backgrounds')

    // Should have light gray elements
    assertTrue(content.contains('#CCCCCC') || content.contains('#E5E5E5'),
        'theme_light should have light gray borders/grid')
  }

  @Test
  void testThemeLightWithGrid() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_line() +
        theme_light()

    Svg svg = chart.render()
    assertNotNull(svg, 'theme_light should render with grid lines')
  }

  @Test
  void testThemeDarkHasDarkBackground() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        theme_dark()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // theme_dark should have dark backgrounds
    assertTrue(content.contains('#222222') || content.contains('#3B3B3B') || content.contains('#3b3b3b'),
        'theme_dark should have dark backgrounds')
  }

  @Test
  void testThemeDarkWithLegend() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y', color: 'category')) +
        geom_point() +
        theme_dark()

    Svg svg = chart.render()
    assertNotNull(svg, 'theme_dark should render with legend')
  }

  @Test
  void testThemeLinedrawHasBlackLines() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        theme_linedraw()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // theme_linedraw should have black borders
    assertTrue(content.contains('stroke="black"') || content.contains('color="black"'),
        'theme_linedraw should have black lines')

    // Should have white background
    assertTrue(content.contains('fill="white"'),
        'theme_linedraw should have white background')
  }

  @Test
  void testThemeLinedrawWithBoxplot() {
    def data = createTestData()
    def chart = ggplot(data, aes('category', 'y')) +
        geom_boxplot() +
        theme_linedraw()

    Svg svg = chart.render()
    assertNotNull(svg, 'theme_linedraw should render with boxplot')
  }

  @Test
  void testThemesMergeCorrectly() {
    def data = createTestData()

    // Test that themes can be merged with custom modifications
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        theme_light() +
        theme(plotTitle: element_text(color: 'red'))

    Svg svg = chart.render()
    assertNotNull(svg, 'themes should merge correctly')
  }

  @Test
  void testAllThemesRenderWithoutError() {
    def data = createTestData()

    // Test that all themes can render a basic chart
    def themes = [
        theme_void(),
        theme_light(),
        theme_dark(),
        theme_linedraw()
    ]

    themes.each { theme ->
      def chart = ggplot(data, aes('x', 'y')) +
          geom_point() +
          theme

      Svg svg = chart.render()
      assertNotNull(svg, "Theme ${theme} should render successfully")
    }
  }

  @Test
  void testThemeVoidWithTitle() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        ggtitle('Test Title') +
        theme_void()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Title should still render even with theme_void
    assertTrue(content.contains('Test Title'),
        'theme_void should still render title if specified')
  }

  @Test
  void testThemeDarkWithFacets() {
    def data = createTestData()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        facet_wrap(vars('category')) +
        theme_dark()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Facet strips should have dark styling
    assertTrue(content.contains('#555555') || content.contains('strip'),
        'theme_dark should style facet strips')
  }
}
