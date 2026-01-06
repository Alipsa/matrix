package se.alipsa.matrix.gg.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.PositionType

/**
 * Position adjustment specification with type and parameters.
 */
@CompileStatic
class Position {

  /** Position adjustment type. */
  final PositionType positionType

  /** Parameters for the position adjustment. */
  final Map params

  /**
   * Create a position specification.
   *
   * @param positionType the adjustment type
   * @param params optional parameters for the adjustment
   */
  Position(PositionType positionType, Map params = [:]) {
    this.positionType = positionType
    this.params = params ?: [:]
  }
}
