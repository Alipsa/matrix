package se.alipsa.matrix.gg.facet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Facet wrap - wraps a 1D ribbon of panels into a 2D grid.
 * Useful for faceting by a single variable.
 *
 * Usage:
 * - facet_wrap('variable') - facet by a single variable
 * - facet_wrap('variable', ncol: 3) - specify number of columns
 * - facet_wrap('variable', nrow: 2) - specify number of rows
 * - facet_wrap(['var1', 'var2']) - facet by combination of variables
 *
 * Example:
 * ggplot(data, aes(x: 'x', y: 'y')) +
 *     geom_point() +
 *     facet_wrap('category', ncol: 2)
 */
@CompileStatic
class FacetWrap extends Facet {

  /** The faceting variable(s) */
  List<String> facets = []

  /** Number of columns (null for auto) */
  Integer ncol

  /** Number of rows (null for auto) */
  Integer nrow

  /** Direction to fill panels: 'h' (horizontal) or 'v' (vertical) */
  String dir = 'h'

  /** Whether to drop unused factor levels */
  boolean drop = true

  FacetWrap() {}

  FacetWrap(String facet) {
    this.facets = [facet]
  }

  FacetWrap(List<String> facets) {
    this.facets = facets
  }

  FacetWrap(Map params) {
    if (params.facets instanceof String) {
      this.facets = [params.facets as String]
    } else if (params.facets instanceof List) {
      this.facets = params.facets as List<String>
    }
    if (params.ncol != null) this.ncol = params.ncol as Integer
    if (params.nrow != null) this.nrow = params.nrow as Integer
    if (params.scales) this.scales = params.scales as String
    if (params.dir) this.dir = params.dir as String
    if (params.containsKey('drop')) this.drop = params.drop as boolean
    if (params.labeller) this.labeller = params.labeller as String
    if (params.containsKey('strip')) this.strip = params.strip as boolean
  }

  @Override
  List<String> getFacetVariables() {
    return facets
  }

  @Override
  Map<String, Integer> computeLayout(Matrix data) {
    List<Map<String, Object>> panels = getPanelValues(data)
    int n = panels.size()

    if (n == 0) {
      return [nrow: 1, ncol: 1]
    }

    int numRows, numCols

    if (ncol != null && nrow != null) {
      // Both specified
      numCols = ncol
      numRows = nrow
    } else if (ncol != null) {
      // Only ncol specified
      numCols = ncol
      numRows = Math.ceil(n / (double) numCols) as int
    } else if (nrow != null) {
      // Only nrow specified
      numRows = nrow
      numCols = Math.ceil(n / (double) numRows) as int
    } else {
      // Auto-compute: try to make it roughly square
      numCols = Math.ceil(Math.sqrt(n)) as int
      numRows = Math.ceil(n / (double) numCols) as int
    }

    return [nrow: numRows, ncol: numCols]
  }

  @Override
  List<Map<String, Object>> getPanelValues(Matrix data) {
    if (facets.isEmpty() || data == null || data.rowCount() == 0) {
      return []
    }

    // Get unique combinations of facet variables
    Set<String> seen = new LinkedHashSet<>()
    List<Map<String, Object>> panels = []

    data.each { row ->
      Map<String, Object> panelKey = [:]
      boolean valid = true

      for (String facetVar : facets) {
        if (!data.columnNames().contains(facetVar)) {
          valid = false
          break
        }
        panelKey[facetVar] = row[facetVar]
      }

      if (valid) {
        String key = panelKey.values().collect { it?.toString() ?: 'NA' }.join('|')
        if (!seen.contains(key)) {
          seen.add(key)
          panels.add(panelKey)
        }
      }
    }

    // Sort panels for consistent ordering
    panels.sort { Map<String, Object> a, Map<String, Object> b ->
      String aKey = a.values().collect { it?.toString() ?: '' }.join('|')
      String bKey = b.values().collect { it?.toString() ?: '' }.join('|')
      aKey <=> bKey
    }

    return panels
  }

  /**
   * Get the row and column position for a panel index.
   * @param index Panel index (0-based)
   * @param layout Layout map with nrow and ncol
   * @return Map with 'row' and 'col' (0-based)
   */
  Map<String, Integer> getPanelPosition(int index, Map<String, Integer> layout) {
    int numCols = layout.ncol

    if (dir == 'v') {
      // Fill vertically first
      int numRows = layout.nrow
      int col = index / numRows as int
      int row = index % numRows
      return [row: row, col: col]
    } else {
      // Fill horizontally first (default)
      int row = index / numCols as int
      int col = index % numCols
      return [row: row, col: col]
    }
  }
}
