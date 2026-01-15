package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for GeomParallel - parallel coordinates plots.
 */
class GeomParallelTest {

  @Test
  void testBasicParallelPlot() {
    // Simple 3-variable dataset
    def data = Matrix.builder()
        .data([
            var1: [1, 2, 3, 4, 5],
            var2: [2, 4, 3, 5, 1],
            var3: [5, 3, 4, 2, 1]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel()

    Svg svg = chart.render()
    assertNotNull(svg, 'Basic parallel plot should render')
  }

  @Test
  void testParallelWithSpecificVars() {
    // Dataset with more columns, but only use subset
    def data = Matrix.builder()
        .data([
            id: [1, 2, 3],
            a: [10, 20, 15],
            b: [5, 15, 10],
            c: [8, 12, 9],
            d: [3, 7, 5]
        ])
        .types(Integer, Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(vars: ['a', 'b', 'c'])

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with specific vars should render')
  }

  @Test
  void testParallelWithColor() {
    // Dataset with categorical grouping
    def data = Matrix.builder()
        .data([
            x1: [1, 2, 3, 1, 2, 3],
            x2: [2, 3, 4, 3, 4, 5],
            x3: [3, 4, 5, 4, 5, 6],
            group: ['A', 'A', 'A', 'B', 'B', 'B']
        ])
        .types(Integer, Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(color: 'group')) + geom_parallel()

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with color grouping should render')
  }

  @Test
  void testParallelWithAlpha() {
    def data = Matrix.builder()
        .data([
            v1: [1, 2, 3, 4],
            v2: [2, 3, 4, 5],
            v3: [3, 4, 5, 6]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(alpha: 0.3, linewidth: 2)

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with alpha should render')
  }

  @Test
  void testParallelGlobalMinMaxScaling() {
    def data = Matrix.builder()
        .data([
            small: [1, 2, 3],
            large: [100, 200, 300]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(scale: 'globalminmax')

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with global scaling should render')
  }

  @Test
  void testParallelCenterScaling() {
    def data = Matrix.builder()
        .data([
            x1: [10, 15, 20],
            x2: [5, 10, 15],
            x3: [20, 25, 30]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(scale: 'center')

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with center scaling should render')
  }

  @Test
  void testParallelStdScaling() {
    def data = Matrix.builder()
        .data([
            x1: [1, 2, 3, 4, 5],
            x2: [10, 20, 30, 40, 50],
            x3: [100, 200, 300, 400, 500]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(scale: 'std')

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with standardized scaling should render')
  }

  @Test
  void testParallelNoScaling() {
    def data = Matrix.builder()
        .data([
            x1: [1, 2, 3],
            x2: [10, 20, 30]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(scale: 'none')

    Svg svg = chart.render()
    assertNotNull(svg, 'Parallel plot with no scaling should render')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x1', 'x2', 'x3')
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error on empty data')
  }

  @Test
  void testSingleVariable() {
    def data = Matrix.builder()
        .data([
            only: [1, 2, 3, 4, 5]
        ])
        .types(Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should handle single variable')
  }

  @Test
  void testMixedNumericNonNumeric() {
    // Should only use numeric columns by default
    def data = Matrix.builder()
        .data([
            num1: [1, 2, 3],
            text: ['A', 'B', 'C'],
            num2: [4, 5, 6],
            num3: [7, 8, 9]
        ])
        .types(Integer, String, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should automatically select only numeric columns')
  }

  @Test
  void testCustomColor() {
    def data = Matrix.builder()
        .data([
            x1: [1, 2, 3],
            x2: [2, 3, 4],
            x3: [3, 4, 5]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes()) + geom_parallel(color: 'darkgreen')

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render with custom color')
  }

  @Test
  void testFactoryMethods() {
    def geom1 = geom_parallel()
    assertNotNull(geom1)
    assertTrue(geom1 instanceof se.alipsa.matrix.gg.geom.GeomParallel)

    def geom2 = geom_parallel(alpha: 0.4, vars: ['a', 'b'])
    assertNotNull(geom2)
    assertEquals(0.4, geom2.alpha)
    assertEquals(['a', 'b'], geom2.vars)
  }

  @Test
  void testIrisExample() {
    // Classic parallel coordinates example with iris-like data
    def data = Matrix.builder()
        .data([
            'Sepal.Length': [5.1, 4.9, 4.7, 7.0, 6.4],
            'Sepal.Width': [3.5, 3.0, 3.2, 3.2, 3.2],
            'Petal.Length': [1.4, 1.4, 1.3, 4.7, 4.5],
            'Petal.Width': [0.2, 0.2, 0.2, 1.4, 1.5],
            'Species': ['setosa', 'setosa', 'setosa', 'versicolor', 'versicolor']
        ])
        .types(BigDecimal, BigDecimal, BigDecimal, BigDecimal, String)
        .build()

    def chart = ggplot(data, aes(color: 'Species')) +
        geom_parallel(vars: ['Sepal.Length', 'Sepal.Width', 'Petal.Length', 'Petal.Width']) +
        labs(title: 'Iris Parallel Coordinates')

    Svg svg = chart.render()
    assertNotNull(svg, 'Iris-style parallel plot should render')
  }
}
