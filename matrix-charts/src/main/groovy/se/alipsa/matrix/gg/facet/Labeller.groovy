package se.alipsa.matrix.gg.facet

import groovy.transform.CompileStatic

/**
 * Labeller class for formatting facet strip labels.
 *
 * A labeller function takes a map of variable names to values and returns
 * a formatted label string. This class encapsulates labelling functions
 * and their configuration.
 *
 * Based on ggplot2's labeller system.
 */
@CompileStatic
class Labeller {

  /** The labelling function: Map<String, Object> -> String */
  Closure<String> labelFunction

  /** Whether to display multi-line labels (when multiple variables are present) */
  boolean multiLine = true

  /** Variable-specific labellers (for composite labellers) */
  Map<String, Labeller> variableLabellers = [:]

  /** Default labeller for variables not explicitly specified */
  Labeller defaultLabeller

  /**
   * Create a labeller with a specific labelling function.
   *
   * @param labelFunction Function that takes a map of variable->value and returns a label string
   * @param multiLine Whether to use multi-line labels
   */
  Labeller(Closure<String> labelFunction, boolean multiLine = true) {
    this.labelFunction = labelFunction
    this.multiLine = multiLine
  }

  /**
   * Create a composite labeller with variable-specific labellers.
   *
   * @param variableLabellers Map of variable name -> labeller
   * @param defaultLabeller Default labeller for unspecified variables
   * @param multiLine Whether to use multi-line labels
   */
  Labeller(Map<String, Labeller> variableLabellers, Labeller defaultLabeller, boolean multiLine = true) {
    this.variableLabellers = variableLabellers
    this.defaultLabeller = defaultLabeller
    this.multiLine = multiLine
  }

  /**
   * Apply this labeller to a panel's values.
   *
   * @param panelValues Map of variable names to their values for this panel
   * @return Formatted label string
   */
  String label(Map<String, Object> panelValues) {
    // If this is a composite labeller, delegate to variable-specific labellers
    if (!variableLabellers.isEmpty()) {
      List<String> parts = []
      panelValues.each { String varName, Object value ->
        Labeller varLabeller = variableLabellers[varName] ?: defaultLabeller
        if (varLabeller != null) {
          // Create a single-variable map for the labeller
          String part = varLabeller.label([(varName): value])
          parts.add(part)
        } else {
          // Fallback to value only
          parts.add(value?.toString() ?: '')
        }
      }
      return multiLine ? parts.join('\n') : parts.join(', ')
    }

    // Otherwise, use the labelling function
    if (labelFunction != null) {
      return labelFunction.call(panelValues)
    }

    // Fallback: just concatenate values
    List<String> values = panelValues.values().collect { it?.toString() ?: '' }
    return multiLine ? values.join('\n') : values.join(', ')
  }

  /**
   * Apply this labeller to a single variable value.
   * Convenience method for single-variable faceting.
   *
   * @param varName Variable name
   * @param value Variable value
   * @return Formatted label string
   */
  String label(String varName, Object value) {
    return label([(varName): value])
  }
}
