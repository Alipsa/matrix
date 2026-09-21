package charm.render

import static org.junit.jupiter.api.Assertions.assertEquals
import static se.alipsa.matrix.charm.Charts.plot

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.render.CharmRenderer
import se.alipsa.matrix.charm.render.LayerPanelData
import se.alipsa.matrix.core.Matrix

class CharmRendererLayerDataTest {

  @Test
  void computesPostStatRowsForEveryLayer() {
    Matrix data = Matrix.builder().data(v: [1, 2, 2, 3, 3, 3]).build()
    Chart chart = plot(data) {
      mapping { x = 'v' }
      layers {
        geomHistogram().bins(3)
        geomPoint().mapping(x: 'v', y: 'v')
      }
    }.build()

    List<LayerPanelData> layers = new CharmRenderer().computeLayerData(chart)

    assertEquals([0, 1], layers*.layerIndex)
    assertEquals(3, layers[0].data.size())
    assertEquals(6G, layers[0].data.sum { it.y as BigDecimal } as BigDecimal)
    assertEquals(6, layers[1].data.size())
  }
}
