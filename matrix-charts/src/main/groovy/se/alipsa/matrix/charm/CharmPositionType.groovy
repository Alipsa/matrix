package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Expanded position-adjustment types covering the full gg surface.
 *
 * Each value corresponds to a ggplot2 position adjustment family.
 */
@CompileStatic
enum CharmPositionType {
  IDENTITY,
  DODGE,
  DODGE2,
  STACK,
  FILL,
  JITTER,
  NUDGE
}
