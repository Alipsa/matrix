package gg.adapter

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.LogticksAnnotationSpec
import se.alipsa.matrix.charm.MapAnnotationSpec
import se.alipsa.matrix.charm.RasterAnnotationSpec
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.charm.CustomAnnotationSpec
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.Annotate
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.adapter.GgCharmAdaptation
import se.alipsa.matrix.gg.adapter.GgCharmAdapter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

class GgCharmAdapterAnnotationTest {

  @Test
  void testAdapterDelegatesAnnotationCustom() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        annotation_custom(
            grob: { G g, Map b ->
              g.addRect()
                  .x([b.xmin, b.xmax].min() as int)
                  .y([b.ymin, b.ymax].min() as int)
                  .width((b.xmax - b.xmin).abs() as int)
                  .height((b.ymax - b.ymin).abs() as int)
                  .fill('red')
            },
            xmin: 1, xmax: 3, ymin: 1, ymax: 3
        )

    GgCharmAdaptation adaptation = new GgCharmAdapter().adapt(chart)
    assertTrue(adaptation.delegated)
    assertEquals(1, adaptation.charmChart.layers.size())
    assertEquals(1, adaptation.charmChart.annotations.size())
    assertTrue(adaptation.charmChart.annotations.first() instanceof CustomAnnotationSpec)

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('charm-annotation-custom'))
  }

  @Test
  void testAdapterDelegatesAnnotationLogticks() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [10, 2], [100, 3], [1000, 4]])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(sides: 'b', base: 10)

    GgCharmAdaptation adaptation = new GgCharmAdapter().adapt(chart)
    assertTrue(adaptation.delegated)
    assertEquals(1, adaptation.charmChart.annotations.size())
    assertTrue(adaptation.charmChart.annotations.first() instanceof LogticksAnnotationSpec)

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('charm-annotation-logticks'))
  }

  @Test
  void testAdapterDelegatesAnnotationRaster() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        annotation_raster(
            raster: [['red', 'green'], ['blue', 'yellow']],
            xmin: 1, xmax: 4, ymin: 1, ymax: 4
        )

    GgCharmAdaptation adaptation = new GgCharmAdapter().adapt(chart)
    assertTrue(adaptation.delegated)
    assertEquals(1, adaptation.charmChart.annotations.size())
    assertTrue(adaptation.charmChart.annotations.first() instanceof RasterAnnotationSpec)

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('charm-annotation-raster'))
  }

  @Test
  void testAdapterDelegatesAnnotationMapParity() {
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

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        annotation_map(map: mapData, fill: '#dddddd', color: '#333333')

    GgCharmAdaptation adaptation = new GgCharmAdapter().adapt(chart)
    assertTrue(adaptation.delegated)
    assertEquals(1, adaptation.charmChart.annotations.size())
    assertTrue(adaptation.charmChart.annotations.first() instanceof MapAnnotationSpec)

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('charm-annotation-map'))
    assertTrue(svg.contains('<path'))
  }

  @Test
  void testGgChartPlusAnnotateMapsToCharmAnnotationSpec() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        new Annotate('text', [x: 2, y: 2, label: 'center'])

    GgCharmAdaptation adaptation = new GgCharmAdapter().adapt(chart)
    assertTrue(adaptation.delegated)
    assertEquals(1, adaptation.charmChart.annotations.size())
    assertTrue(adaptation.charmChart.annotations.first() instanceof TextAnnotationSpec)

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('center'))
  }

  @Test
  void testAnnotationOrderPreservedWhenRasterBeforePoints() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        annotation_raster(
            raster: [['#eeeeee', '#dddddd'], ['#cccccc', '#bbbbbb']],
            xmin: 1, xmax: 3, ymin: 1, ymax: 3
        ) +
        geom_point()

    String svg = SvgWriter.toXml(chart.render())
    int rasterPos = svg.indexOf('charm-annotation-raster')
    int pointPos = svg.indexOf('<circle')
    assertTrue(rasterPos >= 0)
    assertTrue(pointPos >= 0)
    assertTrue(rasterPos < pointPos, 'Raster annotation should render behind points when added first')
  }

  @Test
  void testAnnotationOrderPreservedWhenRasterAfterPoints() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        annotation_raster(
            raster: [['#eeeeee', '#dddddd'], ['#cccccc', '#bbbbbb']],
            xmin: 1, xmax: 3, ymin: 1, ymax: 3
        )

    String svg = SvgWriter.toXml(chart.render())
    int rasterPos = svg.indexOf('charm-annotation-raster')
    int pointPos = svg.indexOf('<circle')
    assertTrue(rasterPos >= 0)
    assertTrue(pointPos >= 0)
    assertTrue(rasterPos > pointPos, 'Raster annotation should render above points when added last')
  }

  @Test
  void testAnnotationCustomClosureArityThreeReceivesScalesInCharmDelegation() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()
    Map<String, Object> captured = [:]

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        annotation_custom(
            grob: { G g, Map b, Map scales ->
              captured.scales = scales
              captured.xTransform = scales?.x?.transform(2)
              g.addRect()
                  .x([b.xmin, b.xmax].min() as int)
                  .y([b.ymin, b.ymax].min() as int)
                  .width((b.xmax - b.xmin).abs() as int)
                  .height((b.ymax - b.ymin).abs() as int)
                  .fill('red')
            },
            xmin: 1, xmax: 3, ymin: 1, ymax: 3
        )

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('charm-annotation-custom'))
    assertNotNull(captured.scales)
    assertNotNull(captured.xTransform)
  }

  @Test
  void testAnnotationCustomClosureArityFourReceivesCoordInCharmDelegation() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()
    Map<String, Object> captured = [:]

    GgChart chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        annotation_custom(
            grob: { G g, Map b, Map scales, Object coord ->
              captured.scales = scales
              captured.coord = coord
              captured.point = coord?.transform(2, 2, scales)
              g.addRect()
                  .x([b.xmin, b.xmax].min() as int)
                  .y([b.ymin, b.ymax].min() as int)
                  .width((b.xmax - b.xmin).abs() as int)
                  .height((b.ymax - b.ymin).abs() as int)
                  .fill('blue')
            },
            xmin: 1, xmax: 3, ymin: 1, ymax: 3
        )

    String svg = SvgWriter.toXml(chart.render())
    assertTrue(svg.contains('charm-annotation-custom'))
    assertNotNull(captured.scales)
    assertNotNull(captured.coord)
    assertTrue(captured.point instanceof List)
    assertEquals(2, (captured.point as List).size())
  }
}
