package charm.render.geom

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.geom.Bin2dBuilder
import se.alipsa.matrix.charm.geom.PointBuilder
import se.alipsa.matrix.charm.geom.TextBuilder
import se.alipsa.matrix.charm.geom.TileBuilder
import se.alipsa.matrix.core.Matrix

/** Tests fill-scale resolution for layers with and without a fill mapping. */
class GeomFillScaleTest {

  @Test
  void unmappedLayerRetainsDefaultFillWhenAnotherLayerMapsFill() {
    Matrix data = Matrix.builder().columns([x: [1], y: [1], value: [5]]).types([Integer, Integer, Integer]).build()
    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x', y: 'y', fill: 'value'])
    spec.addLayer(new TileBuilder())
    spec.addLayer(new PointBuilder().inheritMapping(false).mapping([x: 'x', y: 'y']))
    spec.scale.fill(Scale.gradient('#000000', '#ffffff'))

    Svg svg = spec.build().render()
    assertEquals('#1f77b4', elementsWithClass(svg, 'charm-point').first().getAttribute('fill'))
  }

  @Test
  void bin2dUsesTheFillScaleForStatGeneratedCounts() {
    Matrix data = Matrix.builder().columns([
        x: [0, 0, 0, 1, 1, 2, 2, 2, 2],
        y: [0, 0, 1, 1, 1, 0, 0, 1, 2]
    ]).types([Integer, Integer]).build()
    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x', y: 'y'])
    spec.addLayer(new Bin2dBuilder().bins(3))

    List<String> fills = elementsWithClass(spec.build().render(), 'charm-tile')*.getAttribute('fill')*.toString()
    assertFalse(fills.isEmpty())
    assertFalse(fills.contains('#1f77b4'))
    assertTrue(fills.toSet().size() > 1)
  }

  @Test
  void textCanAutoContrastAgainstItsResolvedFill() {
    Matrix data = Matrix.builder().columns([
        x: [1, 2], y: [1, 2], label: ['low', 'high'], value: [0, 1]
    ]).types([Integer, Integer, String, Integer]).build()
    PlotSpec spec = Charts.plot(data)
    spec.mapping([x: 'x', y: 'y', label: 'label', fill: 'value'])
    spec.addLayer(new TextBuilder().autoContrastFill())
    spec.scale.fill(Scale.gradient('#000000', '#ffffff'))

    List<String> fills = elementsWithClass(spec.build().render(), 'charm-text')*.getAttribute('fill')*.toString()
    assertEquals(['#ffffff', '#000000'], fills)
  }

  private static List elementsWithClass(Svg svg, String cssClass) {
    svg.descendants().findAll { it.getAttribute('class')?.toString()?.split(' ')?.contains(cssClass) }
  }
}
