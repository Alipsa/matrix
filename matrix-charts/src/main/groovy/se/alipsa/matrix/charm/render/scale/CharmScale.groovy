package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Scale

/**
 * Abstract base for trained runtime scales in the Charm renderer.
 *
 * A CharmScale is produced by {@link ScaleEngine} from user-facing {@link Scale} specs
 * and data values. Once trained, it maps data values to pixel coordinates and provides
 * tick marks/labels for axis rendering.
 */
@CompileStatic
abstract class CharmScale {

  /** The user-facing scale configuration that produced this trained scale. */
  Scale scaleSpec

  /** Start of the rendered output range (pixels). */
  BigDecimal rangeStart

  /** End of the rendered output range (pixels). */
  BigDecimal rangeEnd

  /**
   * Optional display name for legend/guide compatibility.
   */
  String getName() {
    Object value = scaleSpec?.params?.get('name')
    value == null ? null : value.toString()
  }

  /**
   * Maps a data value to a rendered coordinate.
   *
   * @param value data value
   * @return pixel coordinate, or null if value cannot be mapped
   */
  abstract BigDecimal transform(Object value)

  /**
   * Returns tick values for axis rendering.
   *
   * @param count preferred number of ticks
   * @return list of data-space tick values
   */
  abstract List<Object> ticks(int count)

  /**
   * Returns display labels corresponding to ticks.
   *
   * @param count preferred number of ticks
   * @return list of formatted label strings
   */
  abstract List<String> tickLabels(int count)

  /**
   * Whether this is a discrete (categorical) scale.
   *
   * @return true if discrete
   */
  abstract boolean isDiscrete()
}
