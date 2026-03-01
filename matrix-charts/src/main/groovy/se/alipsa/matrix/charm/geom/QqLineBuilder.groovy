package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for QQ line layers.
 *
 * <p>Produces a {@code QQ_LINE / QQ_LINE} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'sample' }
 *   layers {
 *     geomQqLine().color('#cc0000').linetype('dashed')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class QqLineBuilder extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  QqLineBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  QqLineBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  QqLineBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  QqLineBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.QQ_LINE
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.QQ_LINE
  }
}
