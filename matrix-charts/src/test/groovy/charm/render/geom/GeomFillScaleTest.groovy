package charm.render.geom

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.geom.PointBuilder
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

  private static List elementsWithClass(Svg svg, String cssClass) {
    svg.descendants().findAll { it.getAttribute('class')?.toString()?.split(' ')?.contains(cssClass) }
  }
}
