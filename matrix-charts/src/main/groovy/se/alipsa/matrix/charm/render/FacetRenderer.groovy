package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.ColumnExpr
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.core.Matrix

/**
 * Computes facet panels from chart facet spec.
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
   * @return ordered panel specs
   */
  List<PanelSpec> computePanels(
      Matrix chartData,
      FacetType facetType,
      List<ColumnExpr> rows,
      List<ColumnExpr> cols,
      List<ColumnExpr> vars,
      Integer ncol,
      Integer nrow
  ) {
    if (facetType == FacetType.NONE) {
      return [defaultPanel(chartData)]
    }

    if (facetType == FacetType.WRAP) {
      ColumnExpr expr = vars.isEmpty() ? null : vars.first()
      if (expr == null) {
        return [defaultPanel(chartData)]
      }
      LinkedHashMap<String, List<Integer>> grouped = new LinkedHashMap<>()
      for (int i = 0; i < chartData.rowCount(); i++) {
        String key = String.valueOf(chartData[i, expr.columnName()])
        grouped.computeIfAbsent(key) { [] as List<Integer> }.add(i)
      }
      if (grouped.isEmpty()) {
        return [defaultPanel(chartData)]
      }
      int columns = ncol ?: (int) Math.ceil(Math.sqrt(grouped.size() as double))
      List<PanelSpec> panels = []
      int idx = 0
      grouped.each { String key, List<Integer> rowIndexes ->
        PanelSpec panel = new PanelSpec(
            row: idx.intdiv(columns),
            col: idx % columns,
            label: key,
            rowIndexes: new ArrayList<>(rowIndexes)
        )
        panels << panel
        idx++
      }
      return panels
    }

    String rowColumn = rows.isEmpty() ? null : rows.first().columnName()
    String colColumn = cols.isEmpty() ? null : cols.first().columnName()
    List<String> rowLevels = rowColumn == null ? [''] : uniqueValues(chartData, rowColumn)
    List<String> colLevels = colColumn == null ? [''] : uniqueValues(chartData, colColumn)
    if (rowLevels.isEmpty()) {
      rowLevels = ['']
    }
    if (colLevels.isEmpty()) {
      colLevels = ['']
    }

    List<PanelSpec> panels = []
    rowLevels.eachWithIndex { String rowLevel, int r ->
      colLevels.eachWithIndex { String colLevel, int c ->
        List<Integer> indexes = []
        for (int i = 0; i < chartData.rowCount(); i++) {
          boolean rowMatch = rowColumn == null || String.valueOf(chartData[i, rowColumn]) == rowLevel
          boolean colMatch = colColumn == null || String.valueOf(chartData[i, colColumn]) == colLevel
          if (rowMatch && colMatch) {
            indexes << i
          }
        }
        PanelSpec panel = new PanelSpec(
            row: r,
            col: c,
            label: [rowLevel, colLevel].findAll { String v -> v != null && !v.isEmpty() }.join(' | '),
            rowIndexes: indexes
        )
        panels << panel
      }
    }
    panels.isEmpty() ? [defaultPanel(chartData)] : panels
  }

  private static List<String> uniqueValues(Matrix matrix, String columnName) {
    LinkedHashSet<String> values = new LinkedHashSet<>()
    for (int i = 0; i < matrix.rowCount(); i++) {
      values << String.valueOf(matrix[i, columnName])
    }
    new ArrayList<>(values)
  }

  private static PanelSpec defaultPanel(Matrix data) {
    PanelSpec panel = new PanelSpec(row: 0, col: 0, label: null)
    panel.rowIndexes = (0..<data.rowCount()).collect { int idx -> idx }
    panel
  }
}
