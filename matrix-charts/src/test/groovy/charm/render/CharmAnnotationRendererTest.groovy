package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CssAttributesSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
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

  @Test
  void testTextAnnotationParsesTextGeomStyleParams() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        text {
          x = 2
          y = 2
          label = 'styled'
          params = [hjust: 0.0, vjust: 0.0, nudge_x: 1, nudge_y: 1, alpha: 0.5, angle: 45]
        }
      }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('text-anchor="start"') || xml.contains("text-anchor='start'"))
    assertTrue(xml.contains('dominant-baseline') || xml.contains('alignment-baseline'))
    assertTrue(xml.contains('fill-opacity="0.5"') || xml.contains("fill-opacity='0.5'"))
    assertTrue(xml.contains('rotate(-45'))
  }

  @Test
  void testRectAnnotationSupportsLinetypeDashArray() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        rect {
          xmin = 1
          xmax = 2
          ymin = 1
          ymax = 2
          params = [fill: '#cccccc', color: '#333333', linetype: 'dashed']
        }
      }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('stroke-dasharray'))
  }

  @Test
  void testNegativeDrawOrderRendersBeforeDataLayers() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    def spec = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        raster {
          raster = [['#eeeeee']]
          xmin = 1
          xmax = 2
          ymin = 1
          ymax = 2
        }
      }
    }
    spec.annotations.first().drawOrder = -1

    String xml = SvgWriter.toXml(spec.build().render())
    int rasterPos = xml.indexOf('charm-annotation-raster')
    int pointPos = xml.indexOf('<circle')
    assertTrue(rasterPos >= 0)
    assertTrue(pointPos >= 0)
    assertTrue(rasterPos < pointPos, 'Negative drawOrder annotations should render before layer 0')
  }

  @Test
  void testAnnotationCssIdsAreUniqueAcrossSameOrderTypes() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      annotate {
        text {
          x = 1
          y = 1
          label = 'a'
        }
        text {
          x = 2
          y = 2
          label = 'b'
        }
        rect {
          xmin = 1
          xmax = 1.5
          ymin = 1
          ymax = 1.5
        }
        rect {
          xmin = 2
          xmax = 2.5
          ymin = 2
          ymax = 2.5
        }
        segment {
          x = 1
          y = 1
          xend = 2
          yend = 2
        }
        segment {
          x = 2
          y = 2
          xend = 3
          yend = 3
        }
        custom {
          xmin = 1
          xmax = 1.5
          ymin = 1
          ymax = 1.5
          grob = { G g, Map b ->
            g.addRect()
                .x([b.xmin, b.xmax].min() as int)
                .y([b.ymin, b.ymax].min() as int)
                .width((b.xmax - b.xmin).abs() as int)
                .height((b.ymax - b.ymin).abs() as int)
                .fill('red')
          }
        }
        custom {
          xmin = 2
          xmax = 2.5
          ymin = 2
          ymax = 2.5
          grob = { G g, Map b ->
            g.addRect()
                .x([b.xmin, b.xmax].min() as int)
                .y([b.ymin, b.ymax].min() as int)
                .width((b.xmax - b.xmin).abs() as int)
                .height((b.ymax - b.ymin).abs() as int)
                .fill('blue')
          }
        }
      }
    }.build()

    Chart cssChart = withCssIdsEnabled(chart)
    String xml = SvgWriter.toXml(cssChart.render())

    List<String> textIds = idsForToken(xml, '-text-')
    List<String> rectIds = idsForToken(xml, '-rect-')
    List<String> segmentIds = idsForToken(xml, '-segment-')
    List<String> customIds = idsForToken(xml, '-custom-')

    assertEquals(2, textIds.size())
    assertEquals(2, textIds.toSet().size())
    assertEquals(2, rectIds.size())
    assertEquals(2, rectIds.toSet().size())
    assertEquals(2, segmentIds.size())
    assertEquals(2, segmentIds.toSet().size())
    assertEquals(2, customIds.size())
    assertEquals(2, customIds.toSet().size())
  }

  @Test
  void testLogticksAndRasterCssIdsAreUniqueAcrossMultipleAnnotations() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [10, 2], [100, 3]])
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
        logticks {
          params = [sides: 't', base: 10]
        }
        raster {
          raster = [['#dddddd']]
          xmin = 1
          xmax = 3
          ymin = 1
          ymax = 2
        }
        raster {
          raster = [['#bbbbbb']]
          xmin = 1
          xmax = 3
          ymin = 2
          ymax = 3
        }
      }
    }.build()

    Chart cssChart = withCssIdsEnabled(chart)
    String xml = SvgWriter.toXml(cssChart.render())

    List<String> logticksIds = idsForToken(xml, '-logticks-')
    List<String> rasterIds = idsForToken(xml, '-raster-ann-')

    assertTrue(logticksIds.size() > 1)
    assertEquals(logticksIds.size(), logticksIds.toSet().size())
    assertEquals(2, rasterIds.size())
    assertEquals(2, rasterIds.toSet().size())
  }

  @Test
  void testMapCssIdsAreUniqueAcrossMultipleAnnotations() {
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
        map {
          map = mapData
          params = [fill: '#bbbbbb', color: '#111111']
        }
      }
    }.build()

    Chart cssChart = withCssIdsEnabled(chart)
    String xml = SvgWriter.toXml(cssChart.render())
    List<String> mapIds = idsForToken(xml, '-map-')
    assertEquals(2, mapIds.size())
    assertEquals(2, mapIds.toSet().size())
  }

  private static Chart withCssIdsEnabled(Chart chart) {
    new Chart(
        chart.data,
        chart.aes,
        chart.layers,
        chart.scale,
        chart.theme,
        chart.facet,
        chart.coord,
        chart.labels,
        chart.guides,
        chart.annotations,
        new CssAttributesSpec(enabled: true, includeIds: true, includeClasses: true, includeDataAttributes: false)
    )
  }

  private static List<String> idsForToken(String xml, String token) {
    (xml =~ /id=['"]([^'"]+)['"]/)
        .collect { it[1] as String }
        .findAll { String id -> id.contains(token) }
  }
}
