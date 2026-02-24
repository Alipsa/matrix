package se.alipsa.matrix.gg.facet

import groovy.transform.CompileStatic

/**
 * Labeller class for formatting facet strip labels.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.facet.Labeller} instead.
 *             This class extends the charm package implementation for backward compatibility.
 */
@CompileStatic
class Labeller extends se.alipsa.matrix.charm.facet.Labeller {

  /**
   * Create a labeller with a specific labelling function.
   *
   * @param labelFunction Function that takes a map of variable->value and returns a label string
   * @param multiLine Whether to use multi-line labels
   */
  Labeller(Closure<String> labelFunction, boolean multiLine = true) {
    super(labelFunction, multiLine)
  }

  /**
   * Create a composite labeller with variable-specific labellers.
   *
   * @param variableLabellers Map of variable name -> labeller
   * @param defaultLabeller Default labeller for unspecified variables
   * @param multiLine Whether to use multi-line labels
   */
  Labeller(Map<String, ? extends se.alipsa.matrix.charm.facet.Labeller> variableLabellers,
           se.alipsa.matrix.charm.facet.Labeller defaultLabeller, boolean multiLine = true) {
    super(variableLabellers as Map<String, se.alipsa.matrix.charm.facet.Labeller>, defaultLabeller, multiLine)
  }
}
