package se.alipsa.matrix.charm.geom

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for 2D bin layers.
 *
 * <p>Produces a {@code BIN2D / BIN2D} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomBin2d().fill('#336699').bins(30)
 *   }
 * }
 * }</pre>
 */
class Bin2dBuilder extends LayerBuilder {

  private static final String BINS = 'bins'

  /**
   * Sets bin fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  Bin2dBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets bin outline colour.
   *
   * @param value colour value
   * @return this builder
   */
  Bin2dBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets bin outline colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  Bin2dBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets bin count (same for x and y).
   *
   * @param value bin count
   * @return this builder
   */
  Bin2dBuilder bins(int value) {
    params[BINS] = value
    this
  }

  /**
   * Sets bin counts for x and y separately.
   *
   * @param value list of [xBins, yBins]
   * @return this builder
   */
  Bin2dBuilder bins(List<Integer> value) {
    params[BINS] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.BIN2D
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.BIN2D
  }

}
