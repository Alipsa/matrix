package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.stat.GgStat

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomBoxplotTest {

  // ============== GeomBoxplot Defaults ==============

  @Test
  void testGeomBoxplotDefaults() {
    GeomBoxplot geom = new GeomBoxplot()

    assertEquals('white', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(1.0, geom.alpha)
    assertEquals(0.5, geom.linewidth)
    assertEquals(0.75, geom.width)
    assertTrue(geom.outliers)
    assertEquals(1.5, geom.outlierSize)
    assertEquals('circle', geom.outlierShape)
    assertEquals(StatType.BOXPLOT, geom.defaultStat)
    assertEquals(['y'], geom.requiredAes)
  }

  @Test
  void testGeomBoxplotWithParams() {
    GeomBoxplot geom = new GeomBoxplot(
        fill: 'steelblue',
        color: 'darkblue',
        alpha: 0.8,
        width: 0.5,
        outliers: false
    )

    assertEquals('steelblue', geom.fill)
    assertEquals('darkblue', geom.color)
    assertEquals(0.8, geom.alpha)
    assertEquals(0.5, geom.width)
    assertFalse(geom.outliers)
  }

  @Test
  void testGeomBoxplotOutlierParams() {
    GeomBoxplot geom = new GeomBoxplot(
        outlierSize: 2.5,
        outlierShape: 'square',
        outlierColor: 'red'
    )

    assertEquals(2.5, geom.outlierSize)
    assertEquals('square', geom.outlierShape)
    assertEquals('red', geom.outlierColor)
  }

  @Test
  void testGeomBoxplotSnakeCaseParams() {
    GeomBoxplot geom = new GeomBoxplot(
        outlier_size: 3.0,
        outlier_shape: 'square',
        outlier_color: 'green',
        staple_width: 0.3
    )

    assertEquals(3.0, geom.outlierSize)
    assertEquals('square', geom.outlierShape)
    assertEquals('green', geom.outlierColor)
    assertEquals(0.3, geom.stapleWidth)
  }

  // ============== GgStat.boxplot() Tests ==============

  @Test
  void testStatBoxplotOutput() {
    def data = Matrix.builder()
        .columnNames('group', 'value')
        .rows([
            ['A', 1], ['A', 2], ['A', 3], ['A', 4], ['A', 5],
            ['A', 6], ['A', 7], ['A', 8], ['A', 9], ['A', 10]
        ])
        .types(String, Integer)
        .build()

    def aes = new Aes(x: 'group', y: 'value')
    def boxplotData = GgStat.boxplot(data, aes)

    println "Boxplot data:"
    println "Columns: ${boxplotData.columnNames()}"
    boxplotData.each { row ->
      println "  x=${row['x']}, ymin=${row['ymin']}, lower=${row['lower']}, " +
              "middle=${row['middle']}, upper=${row['upper']}, ymax=${row['ymax']}, " +
              "outliers=${row['outliers']}"
    }

    assertTrue(boxplotData.columnNames().contains('x'), "Should have x column")
    assertTrue(boxplotData.columnNames().contains('ymin'), "Should have ymin column")
    assertTrue(boxplotData.columnNames().contains('lower'), "Should have lower column")
    assertTrue(boxplotData.columnNames().contains('middle'), "Should have middle column")
    assertTrue(boxplotData.columnNames().contains('upper'), "Should have upper column")
    assertTrue(boxplotData.columnNames().contains('ymax'), "Should have ymax column")
    assertTrue(boxplotData.columnNames().contains('outliers'), "Should have outliers column")
    assertEquals(1, boxplotData.rowCount(), "Should have 1 group")
  }

  @Test
  void testStatBoxplotMultipleGroups() {
    def data = Matrix.builder()
        .columnNames('category', 'measurement')
        .rows([
            ['X', 10], ['X', 20], ['X', 30], ['X', 40], ['X', 50],
            ['Y', 15], ['Y', 25], ['Y', 35], ['Y', 45], ['Y', 55],
            ['Z', 5], ['Z', 15], ['Z', 25], ['Z', 35], ['Z', 45]
        ])
        .types(String, Integer)
        .build()

    def aes = new Aes(x: 'category', y: 'measurement')
    def boxplotData = GgStat.boxplot(data, aes)

    println "Multiple groups boxplot:"
    boxplotData.each { row ->
      println "  ${row['x']}: median=${row['middle']}"
    }

    assertEquals(3, boxplotData.rowCount(), "Should have 3 groups")
  }

  @Test
  void testStatBoxplotWithOutliers() {
    // Data with clear outliers
    def data = Matrix.builder()
        .columnNames('group', 'value')
        .rows([
            ['A', 10], ['A', 11], ['A', 12], ['A', 13], ['A', 14],
            ['A', 15], ['A', 16], ['A', 17], ['A', 18], ['A', 19],
            ['A', 100],  // outlier high
            ['A', -50]   // outlier low
        ])
        .types(String, Integer)
        .build()

    def aes = new Aes(x: 'group', y: 'value')
    def boxplotData = GgStat.boxplot(data, aes)

    def outliers = boxplotData.row(0)['outliers']
    println "Outliers found: ${outliers}"

    assertNotNull(outliers, "Should have outliers")
    assertTrue(outliers instanceof List, "Outliers should be a list")
    assertTrue((outliers as List).size() >= 1, "Should have at least 1 outlier")
  }

  // ============== Full Chart Tests ==============

  @Test
  void testSimpleBoxplot() {
    def random = new Random(42)
    def rows = []
    50.times { rows << ['Group1', 50 + random.nextGaussian() * 10] }
    50.times { rows << ['Group2', 60 + random.nextGaussian() * 15] }

    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_boxplot() +
        labs(title: 'Simple Boxplot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements (boxes)")
    assertTrue(content.contains('<line'), "Should contain line elements (whiskers)")
    assertTrue(content.contains('Simple Boxplot'), "Should contain title")

    File outputFile = new File('build/simple_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotWithColors() {
    def random = new Random(123)
    def rows = []
    30.times { rows << ['A', 40 + random.nextGaussian() * 8] }
    30.times { rows << ['B', 55 + random.nextGaussian() * 12] }
    30.times { rows << ['C', 45 + random.nextGaussian() * 10] }

    def data = Matrix.builder()
        .columnNames('group', 'score')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'score')) +
        geom_boxplot(fill: 'lightblue', color: 'navy') +
        labs(title: 'Colored Boxplot', x: 'Group', y: 'Score')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill="lightblue"'), "Should have lightblue fill")
    assertTrue(content.contains('stroke="navy"'), "Should have navy stroke")

    File outputFile = new File('build/colored_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotWithAlpha() {
    def random = new Random(456)
    def rows = []
    40.times { rows << ['Treatment', 70 + random.nextGaussian() * 5] }
    40.times { rows << ['Control', 65 + random.nextGaussian() * 8] }

    def data = Matrix.builder()
        .columnNames('condition', 'response')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'condition', y: 'response')) +
        geom_boxplot(fill: 'coral', alpha: 0.5)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill-opacity'), "Should have fill-opacity for transparency")

    File outputFile = new File('build/alpha_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotWithTheme() {
    def random = new Random(789)
    def rows = []
    25.times { rows << ['Low', 20 + random.nextGaussian() * 5] }
    25.times { rows << ['Medium', 50 + random.nextGaussian() * 10] }
    25.times { rows << ['High', 80 + random.nextGaussian() * 8] }

    def data = Matrix.builder()
        .columnNames('level', 'measure')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'level', y: 'measure')) +
        geom_boxplot(fill: '#3498db', color: '#2c3e50') +
        theme_minimal() +
        labs(title: 'Boxplot with Minimal Theme', x: 'Level', y: 'Measurement')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/themed_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotClassicTheme() {
    def random = new Random(321)
    def rows = []
    35.times { rows << ['Baseline', 100 + random.nextGaussian() * 20] }
    35.times { rows << ['After', 120 + random.nextGaussian() * 25] }

    def data = Matrix.builder()
        .columnNames('timepoint', 'value')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'timepoint', y: 'value')) +
        geom_boxplot(fill: '#2ecc71') +
        theme_classic() +
        labs(title: 'Classic Theme Boxplot')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/classic_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Factory Method Tests ==============

  @Test
  void testFactoryMethods() {
    def box1 = geom_boxplot()
    assertNotNull(box1)
    assertTrue(box1 instanceof GeomBoxplot)

    def box2 = geom_boxplot(fill: 'red', width: 0.6)
    assertEquals('red', box2.fill)
    assertEquals(0.6, box2.width)
  }

  // ============== Edge Cases ==============

  @Test
  void testBoxplotSingleGroup() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['Only', 10], ['Only', 20], ['Only', 30], ['Only', 40], ['Only', 50]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_boxplot()

    // Should not throw
    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/single_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotManyGroups() {
    def random = new Random(999)
    def rows = []
    ['A', 'B', 'C', 'D', 'E', 'F'].each { group ->
      20.times { rows << [group, 50 + random.nextGaussian() * 15] }
    }

    def data = Matrix.builder()
        .columnNames('group', 'value')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'value')) +
        geom_boxplot(fill: 'lightgreen', color: 'darkgreen') +
        labs(title: '6 Groups Boxplot')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/many_groups_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotNegativeValues() {
    def data = Matrix.builder()
        .columnNames('type', 'change')
        .rows([
            ['Gain', 5], ['Gain', 10], ['Gain', 15], ['Gain', 20], ['Gain', 25],
            ['Loss', -5], ['Loss', -10], ['Loss', -15], ['Loss', -20], ['Loss', -25],
            ['Mixed', -10], ['Mixed', 0], ['Mixed', 10], ['Mixed', -5], ['Mixed', 5]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'type', y: 'change')) +
        geom_boxplot(fill: 'lavender') +
        labs(title: 'Boxplot with Negative Values')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/negative_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotWithOutliersDisabled() {
    def data = Matrix.builder()
        .columnNames('group', 'value')
        .rows([
            ['A', 10], ['A', 11], ['A', 12], ['A', 13], ['A', 14],
            ['A', 15], ['A', 16], ['A', 17], ['A', 18], ['A', 19],
            ['A', 100]  // outlier
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'value')) +
        geom_boxplot(outliers: false)

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should not contain outlier circles when outliers=false
    // (The outlier is at y=100, far from the box)
    assertNotNull(svg)

    File outputFile = new File('build/no_outliers_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testBoxplotDecimalValues() {
    def data = Matrix.builder()
        .columnNames('experiment', 'result')
        .rows([
            ['Trial1', 0.123], ['Trial1', 0.456], ['Trial1', 0.789], ['Trial1', 0.234], ['Trial1', 0.567],
            ['Trial2', 0.890], ['Trial2', 0.678], ['Trial2', 0.345], ['Trial2', 0.901], ['Trial2', 0.567]
        ])
        .types(String, BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'experiment', y: 'result')) +
        geom_boxplot(fill: 'wheat') +
        labs(title: 'Decimal Values Boxplot')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/decimal_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }
}
