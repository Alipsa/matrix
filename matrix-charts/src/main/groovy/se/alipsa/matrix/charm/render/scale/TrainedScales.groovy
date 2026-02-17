package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic

/**
 * Container for all trained scales produced by {@link ScaleEngine}.
 *
 * Holds one scale per aesthetic channel: x, y, color, fill, and
 * optional size, shape, alpha, linetype, group.
 */
@CompileStatic
class TrainedScales {

  /** Trained x-axis scale. */
  CharmScale x

  /** Trained y-axis scale. */
  CharmScale y

  /** Trained color scale (for stroke/outline). */
  ColorCharmScale color

  /** Trained fill scale. */
  ColorCharmScale fill

  /** Trained size scale. */
  CharmScale size

  /** Trained shape scale. */
  CharmScale shape

  /** Trained alpha scale. */
  CharmScale alpha

  /** Trained linetype scale. */
  CharmScale linetype

  /** Trained group scale. */
  CharmScale group
}
