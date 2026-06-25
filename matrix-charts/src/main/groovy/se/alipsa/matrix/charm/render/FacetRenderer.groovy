package se.alipsa.matrix.charm.render

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
@SuppressWarnings('DuplicateListLiteral')
@SuppressWarnings('DuplicateStringLiteral')
@SuppressWarnings('IfStatementBraces')
@SuppressWarnings('ImplementationAsType')
@SuppressWarnings('ParameterCount')
@SuppressWarnings('UnnecessaryCast')
@SuppressWarnings('UnnecessaryCollectCall')
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
    validateWrapDimensions(ncol, nrow)
    if (vars == null || vars.isEmpty()) {
      return [defaultPanel(chartData)]
    }

    List<String> varNames = vars.collect { ColumnExpr expr -> expr.columnName() }
    Object labellerObj = params?.get('labeller')
    String dir = (params?.get('dir') ?: 'h') as String

    // Group by composite key from all vars
    LinkedHashMap<List<Object>, List<Integer>> grouped = [:]
    LinkedHashMap<List<Object>, Map<String, Object>> facetValuesByKey = [:]
    for (int i = 0; i < chartData.rowCount(); i++) {
      List<Object> keyParts = []
      Map<String, Object> fv = [:]
      for (String varName : varNames) {
        Object val = chartData[i, varName]
        keyParts << val
        fv[varName] = val
      }
      List<Object> key = immutableTuple(keyParts)
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
    grouped.each { List<Object> key, List<Integer> rowIndexes ->
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

    List<List<Object>> rowLevels = rowVarNames.isEmpty() ? [[]] : uniqueCompositeValues(chartData, rowVarNames)
    List<List<Object>> colLevels = colVarNames.isEmpty() ? [[]] : uniqueCompositeValues(chartData, colVarNames)
    if (rowLevels.isEmpty()) rowLevels = [[]]
    if (colLevels.isEmpty()) colLevels = [[]]

    List<PanelSpec> panels = []
    rowLevels.eachWithIndex { List<Object> rowLevel, int r ->
      colLevels.eachWithIndex { List<Object> colLevel, int c ->
        List<Integer> indexes = []
        Map<String, Object> rowFacetValues = tupleToFacetValues(rowLevel, rowVarNames)
        Map<String, Object> colFacetValues = tupleToFacetValues(colLevel, colVarNames)
        Map<String, Object> facetValues = new LinkedHashMap<>(rowFacetValues)
        facetValues.putAll(colFacetValues)

        for (int i = 0; i < chartData.rowCount(); i++) {
          boolean rowMatch = rowVarNames.isEmpty() || compositeMatch(chartData, i, rowVarNames, rowLevel)
          boolean colMatch = colVarNames.isEmpty() || compositeMatch(chartData, i, colVarNames, colLevel)
          if (rowMatch && colMatch) {
            indexes << i
          }
        }

        String rowLabel = rowVarNames.isEmpty() ? '' : applyLabeller(labellerObj, rowFacetValues, rowVarNames)
        String colLabel = colVarNames.isEmpty() ? '' : applyLabeller(labellerObj, colFacetValues, colVarNames)

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
  @SuppressWarnings('UnusedPrivateMethodParameter')
  private List<PanelSpec> computeMarginPanels(
      Matrix chartData,
      List<List<Object>> rowLevels,
      List<List<Object>> colLevels,
      List<String> rowVarNames,
      List<String> colVarNames,
      Object labellerObj
  ) {
    List<PanelSpec> marginPanels = []
    int nextRow = rowLevels.isEmpty() ? 1 : rowLevels.size()
    int nextCol = colLevels.isEmpty() ? 1 : colLevels.size()

    // Row margins (aggregate across all columns for each row level)
    if (!rowVarNames.isEmpty() && !colVarNames.isEmpty()) {
      rowLevels.eachWithIndex { List<Object> rowLevel, int r ->
        List<Integer> indexes = []
        for (int i = 0; i < chartData.rowCount(); i++) {
          if (compositeMatch(chartData, i, rowVarNames, rowLevel)) {
            indexes << i
          }
        }
        Map<String, Object> facetValues = tupleToFacetValues(rowLevel, rowVarNames)
        String rowLabel = applyLabeller(labellerObj, facetValues, rowVarNames)
        marginPanels << new PanelSpec(
            row: r, col: nextCol,
            label: '(all)',
            rowLabel: rowLabel, colLabel: '(all)',
            facetValues: facetValues,
            rowIndexes: indexes
        )
      }
    }

    // Column margins (aggregate across all rows for each column level)
    if (!rowVarNames.isEmpty() && !colVarNames.isEmpty()) {
      colLevels.eachWithIndex { List<Object> colLevel, int c ->
        List<Integer> indexes = []
        for (int i = 0; i < chartData.rowCount(); i++) {
          if (compositeMatch(chartData, i, colVarNames, colLevel)) {
            indexes << i
          }
        }
        Map<String, Object> facetValues = tupleToFacetValues(colLevel, colVarNames)
        String colLabel = applyLabeller(labellerObj, facetValues, colVarNames)
        marginPanels << new PanelSpec(
            row: nextRow, col: c,
            label: '(all)',
            rowLabel: '(all)', colLabel: colLabel,
            facetValues: facetValues,
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
    (count as BigDecimal).sqrt().ceil() as int
  }

  private static void validateWrapDimensions(Integer ncol, Integer nrow) {
    if (ncol != null && ncol <= 0) {
      throw new IllegalArgumentException("ncol must be a positive integer, but was ${ncol}")
    }
    if (nrow != null && nrow <= 0) {
      throw new IllegalArgumentException("nrow must be a positive integer, but was ${nrow}")
    }
  }

  /**
   * Applies a labeller to facet values. Supports charm Labeller objects,
   * string-based labeller names ('value', 'both'), and null (defaults to 'value').
   */
  @SuppressWarnings('UnusedPrivateMethodParameter')
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

  private static List<List<Object>> uniqueCompositeValues(Matrix matrix, List<String> varNames) {
    LinkedHashSet<List<Object>> values = new LinkedHashSet<>()
    for (int i = 0; i < matrix.rowCount(); i++) {
      List<Object> parts = varNames.collect { String varName -> matrix[i, varName] }
      values << immutableTuple(parts)
    }
    new ArrayList<>(values)
  }

  private static boolean compositeMatch(
      Matrix data,
      int rowIndex,
      List<String> varNames,
      List<Object> compositeKey
  ) {
    for (int v = 0; v < varNames.size(); v++) {
      Object actual = data[rowIndex, varNames[v]]
      if (actual != compositeKey[v]) {
        return false
      }
    }
    true
  }

  private static List<Object> immutableTuple(List<Object> values) {
    Collections.unmodifiableList(new ArrayList<>(values))
  }

  private static Map<String, Object> tupleToFacetValues(List<Object> tuple, List<String> varNames) {
    Map<String, Object> facetValues = [:]
    varNames.eachWithIndex { String varName, int index ->
      facetValues[varName] = tuple[index]
    }
    facetValues
  }

  private static PanelSpec defaultPanel(Matrix data) {
    PanelSpec panel = new PanelSpec(row: 0, col: 0, label: null)
    panel.rowIndexes = (0..<data.rowCount()).collect { int idx -> idx }
    panel
  }

}
