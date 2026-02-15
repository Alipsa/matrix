package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed layer specification for Charm core.
 */
@CompileStatic
class LayerSpec extends Layer {

  /**
   * Creates a new layer spec.
   *
   * @param geom geometry type
   * @param stat stat type
   * @param aes layer aes
   * @param inheritAes inherit flag
   * @param position position mode
   * @param params layer params
   */
  LayerSpec(
      Geom geom,
      Stat stat = Stat.IDENTITY,
      Aes aes = null,
      boolean inheritAes = true,
      Position position = Position.IDENTITY,
      Map<String, Object> params = [:]
  ) {
    super(geom, stat, aes, inheritAes, position, params)
  }

  /**
   * Copies this layer spec.
   *
   * @return copied layer spec
   */
  @Override
  LayerSpec copy() {
    new LayerSpec(geom, stat, aes?.copy(), inheritAes, position, params)
  }
}
