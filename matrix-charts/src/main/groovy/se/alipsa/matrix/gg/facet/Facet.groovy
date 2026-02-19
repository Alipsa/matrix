package se.alipsa.matrix.gg.facet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Base class for faceting specifications.
 * Faceting creates small multiples by splitting data into subsets
 * and creating a panel for each subset.
 */
@CompileStatic
abstract class Facet {

  /** Whether scales should be free or fixed across panels */
  String scales = 'fixed'  // 'fixed', 'free', 'free_x', 'free_y'

  /** Whether to use fixed space for panels or let them vary */
  String space = 'fixed'  // 'fixed', 'free', 'free_x', 'free_y'

  /** Labeller specification - can be a String ('value', 'both') or a Labeller object */
  Object labeller = 'value'

  /** Whether to include facet strips/labels */
  boolean strip = true

  /** Padding between panels in pixels */
  int panelSpacing = 5

  /**
   * Get the faceting variables.
   */
  abstract List<String> getFacetVariables()

  /**
   * Compute panel layout (number of rows and columns).
   * @param data The data matrix
   * @return Map with 'nrow' and 'ncol' keys
   */
  abstract Map<String, Integer> computeLayout(Matrix data)

  /**
   * Get unique panel values from the data.
   * @param data The data matrix
   * @return List of maps, each containing the facet variable values for a panel
   */
  abstract List<Map<String, Object>> getPanelValues(Matrix data)

  /**
   * Filter data for a specific panel.
   * @param data The full data matrix
   * @param panelValues Map of facet variable -> value for this panel
   * @return Filtered matrix for this panel
   */
  Matrix filterDataForPanel(Matrix data, Map<String, Object> panelValues) {
    if (panelValues.isEmpty()) return data

    Matrix result = data
    panelValues.each { String varName, Object value ->
      if (result.columnNames().contains(varName)) {
        result = result.subset { row ->
          row[varName] == value
        }
      }
    }
    return result
  }

  /**
   * Get the panel title/label for display.
   */
  String getPanelLabel(Map<String, Object> panelValues) {
    // If labeller is a Labeller object (either gg or charm), use it
    if (labeller instanceof se.alipsa.matrix.charm.facet.Labeller) {
      return (labeller as se.alipsa.matrix.charm.facet.Labeller).label(panelValues)
    }

    // Otherwise, use string-based labelling (backward compatibility)
    if (labeller == 'value') {
      return panelValues.values().collect { it?.toString() ?: '' }.join(', ')
    } else if (labeller == 'both') {
      return panelValues.collect { k, v -> "${k}: ${v}" }.join(', ')
    }
    return panelValues.values().collect { it?.toString() ?: '' }.join(', ')
  }

  /**
   * Check if x scales should be free (vary across panels).
   */
  boolean isFreeX() {
    return scales == 'free' || scales == 'free_x'
  }

  /**
   * Check if y scales should be free (vary across panels).
   */
  boolean isFreeY() {
    return scales == 'free' || scales == 'free_y'
  }
}
