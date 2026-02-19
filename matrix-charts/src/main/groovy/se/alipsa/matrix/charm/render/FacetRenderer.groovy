package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.ColumnExpr
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.facet.Labeller
import se.alipsa.matrix.core.Matrix

/**
 * Computes facet panels from chart facet spec.
 *
 * Supports NONE, WRAP (multi-variable, ncol/nrow, dir, labeller),
 * and GRID (multi-variable rows/cols, margins, labeller).
 */
@CompileStatic
class FacetRenderer {

  /**
   * Computes facet panels for a chart.
   *
   * @param chartData source matrix
   * @param facetType facet type
   * @param rows row facet columns
   * @param cols col facet columns
   * @param vars wrap facet columns
   * @param ncol wrap ncol
   * @param nrow wrap nrow
   * @param params extra facet parameters (labeller, scales, dir, drop, margins)
   * @return ordered panel specs
   */
  List<PanelSpec> computePanels(
      Matrix chartData,
      FacetType facetType,
      List<ColumnExpr> rows,
      List<ColumnExpr> cols,
      List<ColumnExpr> vars,
      Integer ncol,
      Integer nrow,
      Map<String, Object> params = [:]
  ) {
    if (facetType == FacetType.NONE) {
      return [defaultPanel(chartData)]
    }

    if (facetType == FacetType.WRAP) {
      return computeWrapPanels(chartData, vars, ncol, nrow, params)
    }

    computeGridPanels(chartData, rows, cols, params)
  }

  /**
   * Computes WRAP facet panels with multi-variable composite keys,
   * ncol/nrow, dir (horizontal/vertical fill), and labeller support.
   */
  private List<PanelSpec> computeWrapPanels(
      Matrix chartData,
      List<ColumnExpr> vars,
      Integer ncol,
      Integer nrow,
      Map<String, Object> params
  ) {
    if (vars == null || vars.isEmpty()) {
      return [defaultPanel(chartData)]
    }

    List<String> varNames = vars.collect { ColumnExpr expr -> expr.columnName() }
    Object labellerObj = params?.get('labeller')
    String dir = (params?.get('dir') ?: 'h') as String

    // Group by composite key from all vars
    LinkedHashMap<String, List<Integer>> grouped = new LinkedHashMap<>()
    LinkedHashMap<String, Map<String, Object>> facetValuesByKey = new LinkedHashMap<>()
    for (int i = 0; i < chartData.rowCount(); i++) {
      List<String> keyParts = []
      Map<String, Object> fv = [:]
      for (String varName : varNames) {
        Object val = chartData[i, varName]
        keyParts << String.valueOf(val)
        fv[varName] = val
      }
      String key = keyParts.join('|')
      grouped.computeIfAbsent(key) { [] as List<Integer> }.add(i)
      facetValuesByKey.putIfAbsent(key, fv)
    }
    if (grouped.isEmpty()) {
      return [defaultPanel(chartData)]
    }

    int count = grouped.size()
    int columns = resolveWrapColumns(count, ncol, nrow)
    int computedRows = (count / columns).ceil() as int

    List<PanelSpec> panels = []
    int idx = 0
    grouped.each { String key, List<Integer> rowIndexes ->
      Map<String, Object> fv = facetValuesByKey[key]
      String label = applyLabeller(labellerObj, fv, varNames)

      int panelRow, panelCol
      if (dir == 'v') {
        panelCol = idx.intdiv(computedRows)
        panelRow = idx % computedRows
      } else {
        panelRow = idx.intdiv(columns)
        panelCol = idx % columns
      }

      PanelSpec panel = new PanelSpec(
          row: panelRow,
          col: panelCol,
          label: label,
          colLabel: label,
          facetValues: fv,
          rowIndexes: new ArrayList<>(rowIndexes)
      )
      panels << panel
      idx++
    }
    panels
  }

  /**
   * Computes GRID facet panels with multi-variable rows/cols,
   * margins, and labeller support.
   */
  private List<PanelSpec> computeGridPanels(
      Matrix chartData,
      List<ColumnExpr> rows,
      List<ColumnExpr> cols,
      Map<String, Object> params
  ) {
    List<String> rowVarNames = (rows ?: []).collect { ColumnExpr expr -> expr.columnName() }
    List<String> colVarNames = (cols ?: []).collect { ColumnExpr expr -> expr.columnName() }
    Object labellerObj = params?.get('labeller')
    boolean margins = params?.get('margins') == true

    List<String> rowLevels = rowVarNames.isEmpty() ? [''] : uniqueCompositeValues(chartData, rowVarNames)
    List<String> colLevels = colVarNames.isEmpty() ? [''] : uniqueCompositeValues(chartData, colVarNames)
    if (rowLevels.isEmpty()) rowLevels = ['']
    if (colLevels.isEmpty()) colLevels = ['']

    List<PanelSpec> panels = []
    rowLevels.eachWithIndex { String rowLevel, int r ->
      colLevels.eachWithIndex { String colLevel, int c ->
        List<Integer> indexes = []
        Map<String, Object> facetValues = [:]

        for (int i = 0; i < chartData.rowCount(); i++) {
          boolean rowMatch = rowVarNames.isEmpty() || compositeMatch(chartData, i, rowVarNames, rowLevel)
          boolean colMatch = colVarNames.isEmpty() || compositeMatch(chartData, i, colVarNames, colLevel)
          if (rowMatch && colMatch) {
            indexes << i
          }
        }

        // Build facet values map for labeller
        if (!rowVarNames.isEmpty()) {
          List<String> parts = rowLevel.split('\\|').toList()
          rowVarNames.eachWithIndex { String varName, int vi ->
            if (vi < parts.size()) facetValues[varName] = parts[vi]
          }
        }
        if (!colVarNames.isEmpty()) {
          List<String> parts = colLevel.split('\\|').toList()
          colVarNames.eachWithIndex { String varName, int vi ->
            if (vi < parts.size()) facetValues[varName] = parts[vi]
          }
        }

        String rowLabel = ''
        if (!rowVarNames.isEmpty()) {
          Map<String, Object> rowFv = [:]
          List<String> parts = rowLevel.split('\\|').toList()
          rowVarNames.eachWithIndex { String varName, int vi ->
            if (vi < parts.size()) rowFv[varName] = parts[vi]
          }
          rowLabel = applyLabeller(labellerObj, rowFv, rowVarNames)
        }

        String colLabel = ''
        if (!colVarNames.isEmpty()) {
          Map<String, Object> colFv = [:]
          List<String> parts = colLevel.split('\\|').toList()
          colVarNames.eachWithIndex { String varName, int vi ->
            if (vi < parts.size()) colFv[varName] = parts[vi]
          }
          colLabel = applyLabeller(labellerObj, colFv, colVarNames)
        }

        String combinedLabel = [rowLabel, colLabel].findAll { String v -> v != null && !v.isEmpty() }.join(' | ')

        PanelSpec panel = new PanelSpec(
            row: r,
            col: c,
            label: combinedLabel,
            rowLabel: rowLabel,
            colLabel: colLabel,
            facetValues: facetValues,
            rowIndexes: indexes
        )
        panels << panel
      }
    }

    // Add margin panels if requested
    if (margins && (!rowVarNames.isEmpty() || !colVarNames.isEmpty())) {
      panels.addAll(computeMarginPanels(chartData, rowLevels, colLevels, rowVarNames, colVarNames, labellerObj))
    }

    panels.isEmpty() ? [defaultPanel(chartData)] : panels
  }

  /**
   * Computes margin panels for FacetGrid with margins=true.
   */
  private List<PanelSpec> computeMarginPanels(
      Matrix chartData,
      List<String> rowLevels,
      List<String> colLevels,
      List<String> rowVarNames,
      List<String> colVarNames,
      Object labellerObj
  ) {
    List<PanelSpec> marginPanels = []
    int nextRow = rowLevels.isEmpty() ? 1 : rowLevels.size()
    int nextCol = colLevels.isEmpty() ? 1 : colLevels.size()

    // Row margins (aggregate across all columns for each row level)
    if (!rowVarNames.isEmpty() && !colVarNames.isEmpty()) {
      rowLevels.eachWithIndex { String rowLevel, int r ->
        List<Integer> indexes = []
        for (int i = 0; i < chartData.rowCount(); i++) {
          if (compositeMatch(chartData, i, rowVarNames, rowLevel)) {
            indexes << i
          }
        }
        marginPanels << new PanelSpec(
            row: r, col: nextCol,
            label: '(all)',
            rowLabel: rowLevel, colLabel: '(all)',
            rowIndexes: indexes
        )
      }
    }

    // Column margins (aggregate across all rows for each column level)
    if (!rowVarNames.isEmpty() && !colVarNames.isEmpty()) {
      colLevels.eachWithIndex { String colLevel, int c ->
        List<Integer> indexes = []
        for (int i = 0; i < chartData.rowCount(); i++) {
          if (compositeMatch(chartData, i, colVarNames, colLevel)) {
            indexes << i
          }
        }
        marginPanels << new PanelSpec(
            row: nextRow, col: c,
            label: '(all)',
            rowLabel: '(all)', colLabel: colLevel,
            rowIndexes: indexes
        )
      }
    }

    // Global margin (all data)
    if (!rowVarNames.isEmpty() && !colVarNames.isEmpty()) {
      marginPanels << new PanelSpec(
          row: nextRow, col: nextCol,
          label: '(all)',
          rowLabel: '(all)', colLabel: '(all)',
          rowIndexes: (0..<chartData.rowCount()).collect { int idx -> idx }
      )
    }

    marginPanels
  }

  private static int resolveWrapColumns(int count, Integer ncol, Integer nrow) {
    if (ncol != null && nrow != null) {
      return ncol
    }
    if (ncol != null) {
      return ncol
    }
    if (nrow != null) {
      return (count / nrow).ceil() as int
    }
    (int) Math.ceil(Math.sqrt(count as double))
  }

  /**
   * Applies a labeller to facet values. Supports charm Labeller objects,
   * string-based labeller names ('value', 'both'), and null (defaults to 'value').
   */
  private static String applyLabeller(Object labeller, Map<String, Object> facetValues, List<String> varNames) {
    if (labeller instanceof Labeller) {
      return (labeller as Labeller).label(facetValues)
    }
    if (labeller == 'both') {
      return facetValues.collect { String k, Object v -> "${k}: ${v}" }.join(', ')
    }
    // Default: 'value' labelling
    facetValues.values().collect { it?.toString() ?: '' }.join(', ')
  }

  private static List<String> uniqueCompositeValues(Matrix matrix, List<String> varNames) {
    LinkedHashSet<String> values = new LinkedHashSet<>()
    for (int i = 0; i < matrix.rowCount(); i++) {
      List<String> parts = varNames.collect { String varName -> String.valueOf(matrix[i, varName]) }
      values << parts.join('|')
    }
    new ArrayList<>(values)
  }

  private static boolean compositeMatch(Matrix data, int rowIndex, List<String> varNames, String compositeKey) {
    List<String> parts = compositeKey.split('\\|').toList()
    for (int v = 0; v < varNames.size(); v++) {
      String actual = String.valueOf(data[rowIndex, varNames[v]])
      if (v < parts.size() && actual != parts[v]) {
        return false
      }
    }
    true
  }

  private static PanelSpec defaultPanel(Matrix data) {
    PanelSpec panel = new PanelSpec(row: 0, col: 0, label: null)
    panel.rowIndexes = (0..<data.rowCount()).collect { int idx -> idx }
    panel
  }
}
