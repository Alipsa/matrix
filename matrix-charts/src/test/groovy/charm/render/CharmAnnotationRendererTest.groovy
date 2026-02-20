package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class CharmAnnotationRendererTest {

  @Test
  void testCustomAnnotationDslRendersCustomElements() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        custom {
          grob = { G g, Map b ->
            g.addRect()
                .x([b.xmin, b.xmax].min() as int)
                .y([b.ymin, b.ymax].min() as int)
                .width((b.xmax - b.xmin).abs() as int)
                .height((b.ymax - b.ymin).abs() as int)
                .fill('red')
          }
          xmin = 1
          xmax = 3
          ymin = 1
          ymax = 3
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)
    assertNotNull(svg)
    assertTrue(xml.contains('charm-annotation-custom'))
    assertTrue(xml.contains('<rect'))
  }

  @Test
  void testLogticksAnnotationDslRendersLines() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [10, 2], [100, 3], [1000, 4]])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      scale {
        x = Scale.transform('log10')
      }
      points {}
      annotate {
        logticks {
          params = [sides: 'b', base: 10]
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)
    assertNotNull(svg)
    assertTrue(xml.contains('charm-annotation-logticks'))
  }

  @Test
  void testRasterAnnotationDslRendersRasterCells() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        raster {
          raster = [
              ['red', 'green'],
              ['blue', 'yellow']
          ]
          xmin = 1
          xmax = 4
          ymin = 1
          ymax = 4
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)
    assertNotNull(svg)
    assertTrue(xml.contains('charm-annotation-raster'))
    assertTrue(xml.contains('<rect'))
  }

  @Test
  void testMapAnnotationDslRendersPaths() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[0, 0], [1, 1]])
        .build()
    Matrix mapData = Matrix.builder()
        .columnNames('long', 'lat', 'group', 'region')
        .rows([
            [0, 0, 1, 'A'],
            [1, 0, 1, 'A'],
            [1, 1, 1, 'A'],
            [0, 1, 1, 'A'],
            [0, 0, 1, 'A']
        ])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        map {
          map = mapData
          params = [fill: '#dddddd', color: '#333333']
        }
      }
    }.build()

    Svg svg = chart.render()
    String xml = SvgWriter.toXml(svg)
    assertNotNull(svg)
    assertTrue(xml.contains('charm-annotation-map'))
    assertTrue(xml.contains('<path'))
  }
}
