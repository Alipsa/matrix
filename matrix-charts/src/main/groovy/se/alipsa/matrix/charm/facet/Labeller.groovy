package se.alipsa.matrix.charm.facet

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
    multiLine ? values.join('\n') : values.join(', ')
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
    label([(varName): value])
  }

  // ---- Static factory methods ----

  /**
   * Creates a labeller that formats each strip as the facet value only.
   *
   * <p>For single-variable facets this produces e.g. {@code "setosa"}.
   * For multi-variable facets the values are joined with newlines (multiLine)
   * or commas (single-line).</p>
   *
   * @param multiLine whether to join multiple variables with newlines
   * @return a value-only labeller
   */
  static Labeller value(boolean multiLine = true) {
    new Labeller({ Map<String, Object> vals ->
      List<String> parts = vals.values().collect { it?.toString() ?: '' }
      multiLine ? parts.join('\n') : parts.join(', ')
    } as Closure<String>, multiLine)
  }

  /**
   * Creates a labeller that formats each strip as {@code "variable: value"}.
   *
   * <p>For example {@code "Species: setosa"}. Multiple variables are joined
   * with newlines (multiLine) or commas (single-line).</p>
   *
   * @param sep separator between variable name and value
   * @param multiLine whether to join multiple variables with newlines
   * @return a both-style labeller
   */
  static Labeller both(String sep = ': ', boolean multiLine = true) {
    new Labeller({ Map<String, Object> vals ->
      List<String> parts = vals.collect { String k, Object v -> "${k}${sep}${v}".toString() }
      multiLine ? parts.join('\n') : parts.join(', ')
    } as Closure<String>, multiLine)
  }

  /**
   * Creates a labeller backed by a custom closure.
   *
   * <p>The closure receives a {@code Map<String, Object>} of variable names
   * to values and must return a formatted label string.</p>
   *
   * @param fn labelling closure
   * @return a custom labeller
   */
  static Labeller label(Closure<String> fn) {
    new Labeller(fn)
  }
}
