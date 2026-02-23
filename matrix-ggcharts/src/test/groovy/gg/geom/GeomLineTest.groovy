package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomLine

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*
import testutil.Slow

class GeomLineTest {

  @Test
  void testGeomLineDefaults() {
    GeomLine geom = new GeomLine()

    assertEquals('black', geom.color)
    assertEquals(1, geom.size)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
    assertEquals(['x', 'y'], geom.requiredAes)
  }

  @Test
  void testGeomLineWithParams() {
    GeomLine geom = new GeomLine(
        color: 'red',
        size: 2,
        linetype: 'dashed',
        alpha: 0.5
    )

    assertEquals('red', geom.color)
    assertEquals(2, geom.size)
    assertEquals('dashed', geom.linetype)
    assertEquals(0.5, geom.alpha)
  }

  @Test
  void testGeomLineWithBritishSpelling() {
    GeomLine geom = new GeomLine(colour: 'blue')
    assertEquals('blue', geom.color)
  }

  @Test
  void testGeomLineWithLinewidth() {
    GeomLine geom = new GeomLine(linewidth: 3)
    assertEquals(3, geom.size)
  }

  @Slow


  @Test
  void testSimpleLineChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 15],
            [4, 25],
            [5, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        labs(title: 'Simple Line Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")
    assertTrue(content.contains('Simple Line Chart'), "Should contain title")

    // Write for visual inspection
    File outputFile = new File('build/simple_line_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Slow


  @Test
  void testLineChartWithColor() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(color: 'steelblue', size: 2)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('stroke="steelblue"'), "Should have steelblue stroke")
    assertTrue(content.contains('stroke-width="2"'), "Should have stroke-width 2")
  }

  @Slow


  @Test
  void testLineChartWithDashedLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(linetype: 'dashed')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('stroke-dasharray="8,4"'), "Should have dashed line pattern")
  }

  @Slow


  @Test
  void testLineChartWithGroups() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 10, 'A'],
            [2, 20, 'A'],
            [3, 15, 'A'],
            [1, 5, 'B'],
            [2, 15, 'B'],
            [3, 25, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'group')) +
        geom_line() +
        labs(title: 'Grouped Line Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    // Should have multiple lines (one per group)
    assertTrue(content.contains('<line'), "Should contain line elements")

    // Write for visual inspection
    File outputFile = new File('build/grouped_line_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Slow


  @Test
  void testLineChartWithColorScale() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'series')
        .rows([
            [1, 10, 'Series A'],
            [2, 15, 'Series A'],
            [3, 20, 'Series A'],
            [1, 8, 'Series B'],
            [2, 12, 'Series B'],
            [3, 18, 'Series B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'series')) +
        geom_line(size: 2) +
        scale_color_manual(values: ['#FF0000', '#0000FF'])

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('<line'), "Should contain line elements")
  }

  @Slow


  @Test
  void testLineChartWithAlpha() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(alpha: 0.5)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('stroke-opacity'), "Should have stroke-opacity attribute")
  }

  @Slow


  @Test
  void testLineChartWithPointsAndLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 25],
            [3, 15],
            [4, 30],
            [5, 20]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(color: 'blue') +
        geom_point(color: 'red', size: 4) +
        labs(title: 'Line with Points')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('<line'), "Should contain line elements")
    assertTrue(content.contains('<circle'), "Should contain circle elements")

    // Write for visual inspection
    File outputFile = new File('build/line_with_points.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Slow


  @Test
  void testLineChartWithRealDataset() {
    // Use economics-style data (time series)
    def data = Matrix.builder()
        .columnNames('year', 'value')
        .rows([
            [2010, 100],
            [2011, 105],
            [2012, 110],
            [2013, 108],
            [2014, 115],
            [2015, 120],
            [2016, 125],
            [2017, 130],
            [2018, 135],
            [2019, 140],
            [2020, 130],
            [2021, 145]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'year', y: 'value')) +
        geom_line(color: 'darkblue', size: 1.5) +
        labs(title: 'Time Series', x: 'Year', y: 'Value') +
        theme_minimal()

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/time_series_line.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Slow


  @Test
  void testLineChartSortsPointsByX() {
    // Data intentionally not sorted by x
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [3, 30],
            [1, 10],
            [5, 50],
            [2, 20],
            [4, 40]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should render successfully even with unsorted data
    assertTrue(content.contains('<line'), "Should contain line elements")
  }

  @Slow


  @Test
  void testLineChartWithDiscreteX() {
    def data = Matrix.builder()
        .columnNames('month', 'sales')
        .rows([
            ['Jan', 100],
            ['Feb', 120],
            ['Mar', 110],
            ['Apr', 130],
            ['May', 150]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'month', y: 'sales')) +
        geom_line(color: 'green') +
        geom_point(color: 'green', size: 4) +
        labs(title: 'Monthly Sales')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")
    assertTrue(content.contains('Jan'), "Should have month labels")

    File outputFile = new File('build/discrete_x_line.svg')
    write(svg, outputFile)
  }

  @Slow


  @Test
  void testMultipleLineTypes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 30]
        ])
        .types(Integer, Integer)
        .build()

    // Test various line types
    ['solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash'].each { type ->
      def chart = ggplot(data, aes(x: 'x', y: 'y')) +
          geom_line(linetype: type)

      Svg svg = chart.render()
      assertNotNull(svg, "Should render with linetype: $type")
    }
  }

  @Test
  void testFactoryMethods() {
    // Test factory methods create valid objects
    def geom1 = geom_line()
    assertNotNull(geom1)
    assertTrue(geom1 instanceof GeomLine)

    def geom2 = geom_line(color: 'red', size: 2)
    assertEquals('red', geom2.color)
    assertEquals(2, geom2.size)
  }
}
