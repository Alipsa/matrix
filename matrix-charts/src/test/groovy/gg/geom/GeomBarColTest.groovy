package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.layer.StatType

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomBarColTest {

  // ============== GeomBar Tests ==============

  @Test
  void testGeomBarDefaults() {
    GeomBar geom = new GeomBar()

    assertEquals('#595959', geom.fill)
    assertNull(geom.color)
    assertNull(geom.width)
    assertEquals(1.0, geom.alpha)
    assertEquals(0.5, geom.linewidth)
    assertEquals(StatType.COUNT, geom.defaultStat)
    assertEquals(['x'], geom.requiredAes)
  }

  @Test
  void testGeomBarWithParams() {
    GeomBar geom = new GeomBar(
        fill: 'steelblue',
        color: 'black',
        width: 0.8,
        alpha: 0.7
    )

    assertEquals('steelblue', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(0.8, geom.width)
    assertEquals(0.7, geom.alpha)
  }

  @Test
  void testGeomBarBritishSpelling() {
    GeomBar geom = new GeomBar(colour: 'red')
    assertEquals('red', geom.color)
  }

  @Test
  void testSimpleBarChart() {
    // Data with categories to count
    def data = Matrix.builder()
        .columnNames('category')
        .rows([
            ['A'],
            ['A'],
            ['A'],
            ['B'],
            ['B'],
            ['C']
        ])
        .types(String)
        .build()

    def chart = ggplot(data, aes(x: 'category')) +
        geom_bar() +
        labs(title: 'Simple Bar Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements")
    assertTrue(content.contains('Simple Bar Chart'), "Should contain title")

    // Write for visual inspection
    File outputFile = new File('build/simple_bar_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBarChartWithFill() {
    def data = Matrix.builder()
        .columnNames('category')
        .rows([
            ['A'], ['A'], ['A'],
            ['B'], ['B'],
            ['C']
        ])
        .types(String)
        .build()

    def chart = ggplot(data, aes(x: 'category')) +
        geom_bar(fill: 'steelblue', color: 'black')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill=\"steelblue\"'), "Should have steelblue fill")
    assertTrue(content.contains('stroke=\"black\"'), "Should have black stroke")
  }

  @Test
  void testBarChartWithAlpha() {
    def data = Matrix.builder()
        .columnNames('category')
        .rows([
            ['A'], ['A'],
            ['B']
        ])
        .types(String)
        .build()

    def chart = ggplot(data, aes(x: 'category')) +
        geom_bar(alpha: 0.5)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill-opacity'), "Should have fill-opacity attribute")
  }

  // ============== GeomCol Tests ==============

  @Test
  void testGeomColDefaults() {
    GeomCol geom = new GeomCol()

    assertEquals('#595959', geom.fill)
    assertNull(geom.color)
    assertEquals(StatType.IDENTITY, geom.defaultStat)
    assertEquals(['x', 'y'], geom.requiredAes)
  }

  @Test
  void testGeomColWithParams() {
    GeomCol geom = new GeomCol(
        fill: 'coral',
        color: 'darkred',
        width: 0.7
    )

    assertEquals('coral', geom.fill)
    assertEquals('darkred', geom.color)
    assertEquals(0.7, geom.width)
    assertEquals(StatType.IDENTITY, geom.defaultStat)
  }

  @Test
  void testSimpleColumnChart() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10],
            ['B', 25],
            ['C', 15],
            ['D', 30]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_col() +
        labs(title: 'Simple Column Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements")
    assertTrue(content.contains('Simple Column Chart'), "Should contain title")

    // Write for visual inspection
    File outputFile = new File('build/simple_column_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testColumnChartWithColors() {
    def data = Matrix.builder()
        .columnNames('month', 'sales')
        .rows([
            ['Jan', 100],
            ['Feb', 150],
            ['Mar', 120],
            ['Apr', 180]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'month', y: 'sales')) +
        geom_col(fill: 'forestgreen', color: 'darkgreen') +
        labs(title: 'Monthly Sales', x: 'Month', y: 'Sales ($)')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill=\"forestgreen\"'), "Should have forestgreen fill")
    assertTrue(content.contains('stroke=\"darkgreen\"'), "Should have darkgreen stroke")

    File outputFile = new File('build/colored_column_chart.svg')
    write(svg, outputFile)
  }

  @Test
  void testColumnChartWithNegativeValues() {
    def data = Matrix.builder()
        .columnNames('quarter', 'profit')
        .rows([
            ['Q1', 50],
            ['Q2', -20],
            ['Q3', 30],
            ['Q4', -10]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'quarter', y: 'profit')) +
        geom_col(fill: 'steelblue') +
        labs(title: 'Quarterly Profit/Loss')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/profit_loss_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Factory Method Tests ==============

  @Test
  void testFactoryMethods() {
    // Test geom_bar factory methods
    def bar1 = geom_bar()
    assertNotNull(bar1)
    assertTrue(bar1 instanceof GeomBar)

    def bar2 = geom_bar(fill: 'red')
    assertEquals('red', bar2.fill)

    // Test geom_col factory methods
    def col1 = geom_col()
    assertNotNull(col1)
    assertTrue(col1 instanceof GeomCol)

    def col2 = geom_col(fill: 'blue', width: 0.5)
    assertEquals('blue', col2.fill)
    assertEquals(0.5, col2.width)
  }

  // ============== Combined Chart Tests ==============

  @Test
  void testBarChartWithTheme() {
    def data = Matrix.builder()
        .columnNames('type')
        .rows([
            ['Small'], ['Small'], ['Small'],
            ['Medium'], ['Medium'],
            ['Large']
        ])
        .types(String)
        .build()

    def chart = ggplot(data, aes(x: 'type')) +
        geom_bar(fill: '#3498db') +
        theme_minimal() +
        labs(title: 'Distribution by Type')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/themed_bar_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testColChartWithTheme() {
    def data = Matrix.builder()
        .columnNames('product', 'revenue')
        .rows([
            ['Widget A', 1200],
            ['Widget B', 800],
            ['Widget C', 1500],
            ['Widget D', 600]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'product', y: 'revenue')) +
        geom_col(fill: '#e74c3c') +
        theme_bw() +
        labs(title: 'Product Revenue', x: 'Product', y: 'Revenue ($)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/themed_column_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBarChartNumericCategories() {
    // Test with numeric categories (should still work with discrete scale)
    def data = Matrix.builder()
        .columnNames('year')
        .rows([
            [2020], [2020], [2020],
            [2021], [2021],
            [2022]
        ])
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'year')) +
        geom_bar(fill: 'purple')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/numeric_bar_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testColChartWithDecimalValues() {
    def data = Matrix.builder()
        .columnNames('item', 'score')
        .rows([
            ['A', 3.5],
            ['B', 4.2],
            ['C', 2.8],
            ['D', 4.9]
        ])
        .types(String, BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'item', y: 'score')) +
        geom_col(fill: 'teal') +
        labs(title: 'Item Scores')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/decimal_column_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('category')
        .rows([])
        .types(String)
        .build()

    def chart = ggplot(data, aes(x: 'category')) +
        geom_bar()

    // Should not throw, just produce empty chart
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testSingleCategory() {
    def data = Matrix.builder()
        .columnNames('type')
        .rows([
            ['Only'], ['Only'], ['Only']
        ])
        .types(String)
        .build()

    def chart = ggplot(data, aes(x: 'type')) +
        geom_bar(fill: 'orange')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/single_category_bar.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }
}
