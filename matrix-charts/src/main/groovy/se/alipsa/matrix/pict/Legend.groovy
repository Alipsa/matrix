package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

import java.awt.Color
import java.awt.Font

/**
 * Configuration for a chart's legend display.
 *
 * <p>Controls visibility, position, direction, font, background color,
 * and an optional title for the legend. Used by {@link Chart} and
 * bridged to Charm's theme model via {@link CharmBridge}.</p>
 *
 * <p>Example usage:
 * <pre>
 * Legend legend = new Legend(visible: true, position: Style.Position.TOP)
 * chart.legend = legend
 * </pre>
 */
@CompileStatic
class Legend {

  /** Legend title text. */
  String title

  /** Whether the legend is visible. Defaults to {@code true}. */
  boolean visible = true

  /** Legend position relative to the plot area. Defaults to {@link Style.Position#RIGHT}. */
  Style.Position position = Style.Position.RIGHT

  /** Legend background color. */
  Color backgroundColor

  /** Legend font. */
  Font font

  /** Legend direction: vertical or horizontal key layout. Defaults to {@link Direction#VERTICAL}. */
  Direction direction = Direction.VERTICAL

  /** Controls whether legend keys are arranged vertically or horizontally. */
  static enum Direction {
    VERTICAL, HORIZONTAL
  }
}
