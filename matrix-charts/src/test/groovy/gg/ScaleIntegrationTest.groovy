package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Integration tests for scale system with GgPlot rendering.
 */
class ScaleIntegrationTest {

  def mtcars = Dataset.mtcars()
  def iris = Dataset.iris()

  @Test
  void testAutoDetectsDiscreteXScale() {
    // When x is categorical (string), should auto-detect discrete scale
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10],
            ['B', 20],
            ['C', 30]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) + geom_point()

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    // Should have axis labels for categories
    assertTrue(content.contains('>A<'), "Should contain label A")
    assertTrue(content.contains('>B<'), "Should contain label B")
    assertTrue(content.contains('>C<'), "Should contain label C")
  }

  @Test
  void testAutoDetectsContinuousXScale() {
    // When x is numeric with many values, should auto-detect continuous
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) + geom_point()

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
  }

  @Test
  void testExplicitDiscreteScale() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            ['Cat1', 10],
            ['Cat2', 20],
            ['Cat3', 30]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_discrete(labels: ['First', 'Second', 'Third'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('>First<'), "Should use custom label")
    assertTrue(content.contains('>Second<'), "Should use custom label")
    assertTrue(content.contains('>Third<'), "Should use custom label")
  }

  @Test
  void testExplicitContinuousScale() {
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
        geom_point() +
        scale_x_continuous(limits: [50, 300], nBreaks: 6) +
        scale_y_continuous(limits: [10, 35])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
  }

  @Test
  void testColorGradientScale() {
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', color: 'wt')) +
        geom_point() +
        scale_color_gradient(low: 'blue', high: 'red')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
    // Points should have fill colors
    assertTrue(content.contains('fill="#'), "Points should have colors")
  }

  @Test
  void testColorManualScale() {
    // Use cylinder count which has 3 unique values: 4, 6, 8
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', color: 'cyl')) +
        geom_point() +
        scale_color_manual(values: ['#FF0000', '#00FF00', '#0000FF'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
  }

  @Test
  void testFillGradientScale() {
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', fill: 'wt')) +
        geom_point() +
        scale_fill_gradient(low: '#000000', high: '#FFFFFF')

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testScaleColorGradient2Diverging() {
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', color: 'wt')) +
        geom_point() +
        scale_color_gradient2(low: 'blue', mid: 'white', high: 'red', midpoint: 3)

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testMixedScales() {
    // Discrete x, continuous y, discrete color
    def data = Matrix.builder()
        .columnNames('category', 'value', 'group')
        .rows([
            ['A', 10, 'G1'],
            ['A', 15, 'G2'],
            ['B', 20, 'G1'],
            ['B', 25, 'G2'],
            ['C', 30, 'G1'],
            ['C', 35, 'G2']
        ])
        .types(String, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value', color: 'group')) +
        geom_point(size: 5) +
        labs(title: 'Mixed Scales Test')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('Mixed Scales Test'), "Should contain title")
    assertTrue(content.contains('<circle'), "Should contain points")
  }

  @Test
  void testScaleFactoryMethods() {
    // Verify all factory methods create valid scale objects
    def xCont = scale_x_continuous()
    assertEquals('x', xCont.aesthetic)

    def yCont = scale_y_continuous()
    assertEquals('y', yCont.aesthetic)

    def xDisc = scale_x_discrete()
    assertEquals('x', xDisc.aesthetic)

    def yDisc = scale_y_discrete()
    assertEquals('y', yDisc.aesthetic)

    def colorGrad = scale_color_gradient()
    assertEquals('color', colorGrad.aesthetic)

    def colourGrad = scale_colour_gradient()
    assertEquals('color', colourGrad.aesthetic)

    def colorGrad2 = scale_color_gradient2()
    assertEquals('color', colorGrad2.aesthetic)
    assertNotNull(colorGrad2.mid)

    def fillGrad = scale_fill_gradient()
    assertEquals('fill', fillGrad.aesthetic)

    def fillGrad2 = scale_fill_gradient2()
    assertEquals('fill', fillGrad2.aesthetic)
    assertNotNull(fillGrad2.mid)

    def colorMan = scale_color_manual(values: ['red'])
    assertEquals('color', colorMan.aesthetic)

    def colourMan = scale_colour_manual(values: ['red'])
    assertEquals('color', colourMan.aesthetic)

    def fillMan = scale_fill_manual(values: ['red'])
    assertEquals('fill', fillMan.aesthetic)
  }

  @Test
  void testScaleWithParamsFactory() {
    def xScale = scale_x_continuous(
        limits: [0, 100],
        nBreaks: 10,
        name: 'X Values'
    )

    assertEquals([0, 100], xScale.limits)
    assertEquals(10, xScale.nBreaks)
    assertEquals('X Values', xScale.name)
  }

  @Test
  void testRenderWithSmoothAndColorScale() {
    // Regression test for scatter + smooth with color scale
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes(x: 'cty', y: 'hwy', color: 'class')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'Fuel Economy by Class')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('Fuel Economy by Class'))
    assertTrue(content.contains('<circle'), "Should contain scatter points")
    assertTrue(content.contains('<line'), "Should contain smooth line")
  }

  @Test
  void testOutputToFile() {
    def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_color_manual(values: ['#E41A1C', '#377EB8', '#4DAF4A']) +
        labs(title: 'Iris with Manual Colors')

    File outputFile = new File('build/iris_manual_colors.svg')
    ggsave(chart, outputFile.path)

    assertTrue(outputFile.exists(), "Output file should exist")
    String content = outputFile.text
    assertTrue(content.contains('<svg'), "Should be valid SVG")
    assertTrue(content.contains('Iris with Manual Colors'))
  }

  @Test
  void testNoRegressionExistingTests() {
    // Ensure existing simple plots still work
    def chart1 = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
        geom_point() +
        theme_minimal()

    assertNotNull(chart1.render())

    def chart2 = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
        geom_point(color: 'steelblue') +
        geom_smooth(method: 'lm') +
        labs(title: 'HP vs MPG') +
        theme_gray()

    assertNotNull(chart2.render())
  }

  @Test
  void testViridisDiscreteScale() {
    // Test scale_color_viridis_d with discrete categorical data
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes(x: 'cty', y: 'hwy', color: 'class')) +
        geom_point() +
        scale_color_viridis_d() +
        labs(title: 'MPG with Viridis Colors')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
    assertTrue(content.contains('fill="#'), "Points should have colors")
    assertTrue(content.contains('MPG with Viridis Colors'))
  }

  @Test
  void testViridisScaleWithDifferentPalettes() {
    // Test different viridis palette options and verify they produce different colors
    def species = iris.column('Species').toList()

    // Create scales with different palettes
    def scaleMagma = scale_color_viridis_d(option: 'magma')
    def scalePlasma = scale_color_viridis_d(option: 'plasma')
    def scaleCividis = scale_color_viridis_d(option: 'cividis')

    // Train scales with the same data
    scaleMagma.train(species)
    scalePlasma.train(species)
    scaleCividis.train(species)

    // Get the computed colors for each palette
    List<String> magmaColors = scaleMagma.getColors()
    List<String> plasmaColors = scalePlasma.getColors()
    List<String> cividisColors = scaleCividis.getColors()

    // Verify all palettes generate 3 colors
    assertEquals(3, magmaColors.size())
    assertEquals(3, plasmaColors.size())
    assertEquals(3, cividisColors.size())

    // Different palettes should produce different color sets
    assertNotEquals(magmaColors, plasmaColors, "Magma and plasma should produce different colors")
    assertNotEquals(plasmaColors, cividisColors, "Plasma and cividis should produce different colors")
    assertNotEquals(magmaColors, cividisColors, "Magma and cividis should produce different colors")

    // Verify that rendering still works with these palettes
    def chart1 = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_color_viridis_d(option: 'magma')

    assertNotNull(chart1.render())

    def chart2 = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_color_viridis_d(option: 'plasma')

    assertNotNull(chart2.render())

    def chart3 = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_colour_viridis_d(option: 'cividis')

    assertNotNull(chart3.render())
  }

  @Test
  void testViridisScaleWithBeginEnd() {
    // Test using portion of the color scale
    def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_color_viridis_d(begin: 0.2, end: 0.8)

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testViridisScaleReversed() {
    // Test that direction: -1 actually reverses the color order
    def normalScale = scale_color_viridis_d()
    normalScale.train(['setosa', 'versicolor', 'virginica'])
    List<String> normalColors = normalScale.getColors()

    def reversedScale = scale_color_viridis_d(direction: -1)
    reversedScale.train(['setosa', 'versicolor', 'virginica'])
    List<String> reversedColors = reversedScale.getColors()

    // Verify we have colors
    assertEquals(3, normalColors.size())
    assertEquals(3, reversedColors.size())

    // Verify the first and last colors are swapped
    assertEquals(normalColors[0], reversedColors[2], "First normal color should match last reversed color")
    assertEquals(normalColors[2], reversedColors[0], "Last normal color should match first reversed color")

    // Also verify rendering works
    def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_color_viridis_d(direction: -1)

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testViridisFactoryMethods() {
    // Verify viridis factory methods create valid scale objects
    def colorViridis = scale_color_viridis_d()
    assertEquals('color', colorViridis.aesthetic)

    def colourViridis = scale_colour_viridis_d()
    assertEquals('color', colourViridis.aesthetic)

    def fillViridis = scale_fill_viridis_d()
    assertEquals('fill', fillViridis.aesthetic)

    // Test with options
    def magmaScale = scale_color_viridis_d(option: 'magma')
    assertEquals('color', magmaScale.aesthetic)
  }

  @Test
  void testViridisWithFillAesthetic() {
    // Test viridis fill scale with bar chart
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10],
            ['B', 20],
            ['C', 30],
            ['D', 25]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value', fill: 'category')) +
        geom_col() +
        scale_fill_viridis_d()

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testViridisDirectionValidation() {
    // Test that direction parameter is normalized to 1 or -1
    def scale1 = scale_color_viridis_d(direction: 1)
    assertEquals(1, scale1.direction)

    def scale2 = scale_color_viridis_d(direction: -1)
    assertEquals(-1, scale2.direction)

    // Test that values > 0 are normalized to 1
    def scale3 = scale_color_viridis_d(direction: 5)
    assertEquals(1, scale3.direction)

    // Test that values < 0 are normalized to -1
    def scale4 = scale_color_viridis_d(direction: -5)
    assertEquals(-1, scale4.direction)

    // Test that 0 is normalized to 1
    def scale5 = scale_color_viridis_d(direction: 0)
    assertEquals(1, scale5.direction)
  }

  @Test
  void testDiscreteAndContinuousScaleFactoryMethods() {
    // Test scale_color_discrete and scale_colour_discrete
    def colorDiscrete = scale_color_discrete()
    assertEquals('color', colorDiscrete.aesthetic)

    def colourDiscrete = scale_colour_discrete()
    assertEquals('color', colourDiscrete.aesthetic)

    // Test scale_color_continuous and scale_colour_continuous
    def colorContinuous = scale_color_continuous()
    assertEquals('color', colorContinuous.aesthetic)

    def colourContinuous = scale_colour_continuous()
    assertEquals('color', colourContinuous.aesthetic)

    // Test scale_fill_discrete
    def fillDiscrete = scale_fill_discrete()
    assertEquals('fill', fillDiscrete.aesthetic)

    // Test scale_fill_continuous
    def fillContinuous = scale_fill_continuous()
    assertEquals('fill', fillContinuous.aesthetic)
  }

  @Test
  void testDiscreteColorScaleWithChart() {
    // Test that scale_colour_discrete works in a chart
    def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', color: 'Species')) +
        geom_point() +
        scale_colour_discrete()

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
  }

  @Test
  void testContinuousColorScaleWithChart() {
    // Test that scale_colour_continuous works in a chart
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', color: 'wt')) +
        geom_point() +
        scale_colour_continuous(low: 'yellow', high: 'red')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
  }

  @Test
  void testFillDiscreteScaleWithChart() {
    // Test that scale_fill_discrete works in a chart
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10],
            ['B', 20],
            ['C', 30]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value', fill: 'category')) +
        geom_col() +
        scale_fill_discrete()

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testFillContinuousScaleWithChart() {
    // Test that scale_fill_continuous works in a chart
    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', fill: 'wt')) +
        geom_point() +
        scale_fill_continuous(low: 'blue', high: 'green')

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testHistogramWithAndWithoutAfterStat() {
    // Test that ggplot(mpg, aes(x: 'displ')) + geom_histogram()
    // generates identical result as
    // ggplot(mpg, aes(x: 'displ', y: after_stat('count'))) + geom_histogram()

    def mpg = Dataset.mpg()

    // Chart without explicit y aesthetic (default behavior uses count)
    def chart1 = ggplot(mpg, aes(x: 'displ')) + geom_histogram(bins: 10)
    Svg svg1 = chart1.render()
    assertNotNull(svg1)
    String content1 = SvgWriter.toXml(svg1)

    // Chart with explicit after_stat(count) for y
    def chart2 = ggplot(mpg, aes(x: 'displ', y: after_stat('count'))) + geom_histogram(bins: 10)
    Svg svg2 = chart2.render()
    assertNotNull(svg2)
    String content2 = SvgWriter.toXml(svg2)

    // Both should produce histograms with bars (rect elements)
    assertTrue(content1.contains('<rect'), "Chart 1 should contain histogram bars")
    assertTrue(content2.contains('<rect'), "Chart 2 should contain histogram bars")

    // Extract and compare rect elements to verify identical behavior
    def rects1 = extractRectAttributes(content1)
    def rects2 = extractRectAttributes(content2)
    
    // Verify same number of bars
    assertEquals(rects1.size(), rects2.size(), 
        "Both charts should have the same number of rect elements")
    
    // Verify each bar has identical attributes
    for (int i = 0; i < rects1.size(); i++) {
      def rect1 = rects1[i]
      def rect2 = rects2[i]
      
      // Compare numeric attributes with tolerance for floating-point precision
      assertEquals(Double.parseDouble(rect1.x), Double.parseDouble(rect2.x), 0.001,
          "Bar ${i}: x position should be identical")
      assertEquals(Double.parseDouble(rect1.y), Double.parseDouble(rect2.y), 0.001,
          "Bar ${i}: y position should be identical")
      assertEquals(Double.parseDouble(rect1.width), Double.parseDouble(rect2.width), 0.001,
          "Bar ${i}: width should be identical")
      assertEquals(Double.parseDouble(rect1.height), Double.parseDouble(rect2.height), 0.001,
          "Bar ${i}: height should be identical")
    }

    // The rendered output should be identical
    // (excluding any potential minor differences in whitespace or attribute order)
    // Compare the data-layer group content
    assertTrue(content1.contains('class="geomhistogram"'), "Chart 1 should have histogram class")
    assertTrue(content2.contains('class="geomhistogram"'), "Chart 2 should have histogram class")
  }
  
  /**
   * Helper method to extract rect element attributes from SVG content.
   * Returns a list of maps containing x, y, width, height for each rect.
   * Only includes rect elements that have all required attributes.
   */
  private List<Map<String, String>> extractRectAttributes(String svgContent) {
    def rects = []
    // Match rect elements and their attributes
    def pattern = /<rect[^>]*>/
    def matcher = (svgContent =~ pattern)
    
    matcher.each { rectTag ->
      def attrs = [:]
      
      // Extract x attribute
      def xMatch = (rectTag =~ /x="([^"]*)"/)
      if (xMatch) attrs.x = xMatch[0][1]
      
      // Extract y attribute
      def yMatch = (rectTag =~ /y="([^"]*)"/)
      if (yMatch) attrs.y = yMatch[0][1]
      
      // Extract width attribute
      def widthMatch = (rectTag =~ /width="([^"]*)"/)
      if (widthMatch) attrs.width = widthMatch[0][1]
      
      // Extract height attribute
      def heightMatch = (rectTag =~ /height="([^"]*)"/)
      if (heightMatch) attrs.height = heightMatch[0][1]
      
      // Only add rect if all required attributes are present
      if (attrs.x && attrs.y && attrs.width && attrs.height) {
        rects << attrs
      }
    }
    
    return rects
  }

  @Test
  void testAfterStatFactoryMethod() {
    // Test that after_stat() creates proper AfterStat objects
    def afterCount = after_stat('count')
    assertNotNull(afterCount)
    assertEquals('count', afterCount.stat)

    def afterDensity = after_stat('density')
    assertEquals('density', afterDensity.stat)

    // Test equality
    def afterCount2 = after_stat('count')
    assertEquals(afterCount, afterCount2)
  }

  @Test
  void testAesWithAfterStat() {
    // Test that Aes correctly identifies AfterStat values
    def aesWithAfterStat = aes(x: 'displ', y: after_stat('count'))

    assertTrue(aesWithAfterStat.isAfterStat('y'))
    assertFalse(aesWithAfterStat.isAfterStat('x'))
    assertEquals('count', aesWithAfterStat.getAfterStatName('y'))
    assertNull(aesWithAfterStat.yColName)  // Should be null for AfterStat
    assertEquals('displ', aesWithAfterStat.xColName)
  }
}
