package se.alipsa.groovy.charts

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
 * | h |----------------------------------|   |
 * | a |                                  | c |
 * | r |          Plot                    | h |
 * | t |                                  | a |
 * |   |                                  | r |
 * |   |----------------------------------| t |
 * |   |          Legend                  |   |
 * |   |----------------------------------|   |
 * |            chart                         |
 * |------------------------------------------|
 * </pre>
 */
class Style {

    /** The back ground color of the plot area (where the actual chart is drawn) */
    Color plotBackgroundColor

    /** The background color of the area outside of the plot area */
    Color chartBackgroundColor

    /** The font used for the legend */
    Font legendFont

    /** whether to show the legend or not */
    Boolean legendVisible

    /** whether to show the title or not */
    Boolean titleVisible
}
