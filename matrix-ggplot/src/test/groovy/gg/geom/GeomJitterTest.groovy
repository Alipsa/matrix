package gg.geom

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.gg.geom.GeomJitter
import se.alipsa.matrix.gg.layer.PositionType

class GeomJitterTest {

  @Test
  void testGeomJitterDefaults() {
    GeomJitter geom = new GeomJitter()

    assertEquals(PositionType.JITTER, geom.defaultPosition)
    assertEquals(['x', 'y'], geom.requiredAes)
  }
}
