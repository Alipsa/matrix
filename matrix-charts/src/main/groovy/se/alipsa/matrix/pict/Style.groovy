package se.alipsa.matrix.pict

import java.awt.Color
import java.awt.Font

/**
 * Most of the styles are specified using java.awt classes (Because they are always accessible)
 * Of course, they will be translated to the Javafx or Swing equivalent when plotted (displayed).
 * <pre>
 * |------------------------------------------|
 * |            chart                         |
 * |   ------------------------------------   |
 * | c |          Title                   |   |
 * | h |   |---------------------------|  |   |
 * | a |   |  |---------------------|  |  | c |
 * | r |   |  |     Plot            |  |  | h |
 * | t |   |  |---------------------|  |  | a |
 * |   |   |          Legend           |  | r |
 * |   |   |---------------------------|  | t |
 * |   |                                  |   |
 * |   |----------------------------------|   |
 * |            chart                         |
 * |------------------------------------------|
 * </pre>
 * Title and Legend, and Plot can be positioned using clock
 * notation (1-12) as follows
 *
 * <pre>
 *   |-----------------------|
 *   | 11        12        1 |
 *   | 10                  2 |
 *   | 9                   3 |
 *   | 8                   4 |
 *   | 7          6        5 |
 *   |-----------------------|
 *  </pre>
 *  or in words
 *  <ol>
 *    <li>top right</li>
 *    <li>right top</li>
 *    <li>right center</li>
 *    <li>right bottom</li>
 *    <li>bottom right</li>
 *    <li>bottom center</li>
 *    <li>bottom left</li>
 *    <li>left bottom</li>
 *    <li>left center</li>
 *    <li>left top</li>
 *    <li>top left</li>
 *    <li>top center</li>
 *  </ol>
 *
 */
class Style {

  String css

  /** The back ground color of the plot area (where the actual chart is drawn) */
  Color plotBackgroundColor

  /** The background color of the area outside of the plot area */
  Color chartBackgroundColor

  /** The font used for the legend */
  Font legendFont

  /** whether to show the legend or not */
  Boolean legendVisible

  Color legendBackgroundColor

  Position legendPosition

  /** whether to show the title or not */
  Boolean titleVisible

  Boolean XAxisVisible
  Boolean YAxisVisible

  Map yLabels = [:]

  boolean isLegendVisible() {
    Boolean.TRUE == legendVisible
  }

  void setCss(String css) {
    this.css = css
  }

  String getCss() {
    return css
  }

  static enum Position {
    TOP, RIGHT, BOTTOM, LEFT
  }

  void setLegendPosition(String pos) {
    legendPosition = Position.valueOf(pos.toUpperCase())
  }
}
