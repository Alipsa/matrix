package se.alipsa.matrix.gg.facet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Facet grid - forms a matrix of panels defined by row and column faceting variables.
 * Each panel shows data for a specific combination of row and column variable values.
 *
 * Usage:
 * - facet_grid(rows: 'var1', cols: 'var2') - row by var1, column by var2
 * - facet_grid(rows: 'var1') - rows only, single column
 * - facet_grid(cols: 'var2') - columns only, single row
 * - facet_grid(rows: ['var1', 'var2'], cols: 'var3') - multiple row variables
 *
 * Example:
 * ggplot(data, aes(x: 'x', y: 'y')) +
 *     geom_point() +
 *     facet_grid(rows: 'cyl', cols: 'gear')
 */
@CompileStatic
class FacetGrid extends Facet {

  /** Row faceting variable(s) */
  List<String> rows = []

  /** Column faceting variable(s) */
  List<String> cols = []

  /** Whether margins should be shown (all combinations including totals) */
  boolean margins = false

  FacetGrid() {}

  /**
   * Create a FacetGrid using ggplot2-style formula syntax.
   *
   * @param formula Formula string (e.g., "year ~ drv", "~ class", "cyl ~ .")
   */
  FacetGrid(String formula) {
    Map<String, List<String>> parsed = FormulaParser.parse(formula)
    this.rows = parsed.rows
    this.cols = parsed.cols
  }

  FacetGrid(Map params) {
    if (params.rows instanceof String) {
      this.rows = [params.rows as String]
    } else if (params.rows instanceof List) {
      this.rows = params.rows as List<String>
    }

    if (params.cols instanceof String) {
      this.cols = [params.cols as String]
    } else if (params.cols instanceof List) {
      this.cols = params.cols as List<String>
    }

    if (params.scales) this.scales = params.scales as String
    if (params.space) this.space = params.space as String
    if (params.labeller) {
      // Accept both String and Labeller objects
      if (params.labeller instanceof Labeller || params.labeller instanceof String) {
        this.labeller = params.labeller
      } else {
        this.labeller = params.labeller as String
      }
    }
    if (params.containsKey('strip')) this.strip = params.strip as boolean
    if (params.containsKey('margins')) this.margins = params.margins as boolean
    if (params.panelSpacing != null) this.panelSpacing = params.panelSpacing as int
  }

  @Override
  List<String> getFacetVariables() {
    List<String> vars = []
    vars.addAll(rows)
    vars.addAll(cols)
    return vars
  }

  @Override
  Map<String, Integer> computeLayout(Matrix data) {
    List<Object> rowValues = getUniqueValues(data, rows)
    List<Object> colValues = getUniqueValues(data, cols)

    int numRows = rowValues.isEmpty() ? 1 : rowValues.size()
    int numCols = colValues.isEmpty() ? 1 : colValues.size()

    return [nrow: numRows, ncol: numCols]
  }

  @Override
  List<Map<String, Object>> getPanelValues(Matrix data) {
    if ((rows.isEmpty() && cols.isEmpty()) || data == null || data.rowCount() == 0) {
      return []
    }

    List<Object> rowValues = getUniqueValues(data, rows)
    List<Object> colValues = getUniqueValues(data, cols)

    // Handle case where one dimension is empty
    if (rowValues.isEmpty()) rowValues = [null]
    if (colValues.isEmpty()) colValues = [null]

    List<Map<String, Object>> panels = []

    // Create a panel for each row/column combination
    for (Object rowVal : rowValues) {
      for (Object colVal : colValues) {
        Map<String, Object> panelKey = [:]

        // Add row variable values
        if (rowVal != null) {
          if (rows.size() == 1) {
            panelKey[rows[0]] = rowVal
          } else if (rowVal instanceof List) {
            List rowList = rowVal as List
            for (int i = 0; i < rows.size() && i < rowList.size(); i++) {
              panelKey[rows[i]] = rowList[i]
            }
          }
        }

        // Add column variable values
        if (colVal != null) {
          if (cols.size() == 1) {
            panelKey[cols[0]] = colVal
          } else if (colVal instanceof List) {
            List colList = colVal as List
            for (int i = 0; i < cols.size() && i < colList.size(); i++) {
              panelKey[cols[i]] = colList[i]
            }
          }
        }

        panels.add(panelKey)
      }
    }

    return panels
  }

  /**
   * Get unique values for a set of facet variables.
   * For multiple variables, returns unique combinations as Lists.
   */
  private List<Object> getUniqueValues(Matrix data, List<String> variables) {
    if (variables.isEmpty() || data == null || data.rowCount() == 0) {
      return []
    }

    // Check all variables exist
    for (String var : variables) {
      if (!data.columnNames().contains(var)) {
        return []
      }
    }

    Set<String> seen = new LinkedHashSet<>()
    List<Object> values = []

    if (variables.size() == 1) {
      // Single variable - return simple values
      String var = variables[0]
      data.each { row ->
        Object val = row[var]
        String key = val?.toString() ?: 'NA'
        if (!seen.contains(key)) {
          seen.add(key)
          values.add(val)
        }
      }
    } else {
      // Multiple variables - return Lists of values
      data.each { row ->
        List<Object> combo = []
        for (String var : variables) {
          combo.add(row[var])
        }
        String key = combo.collect { it?.toString() ?: 'NA' }.join('|')
        if (!seen.contains(key)) {
          seen.add(key)
          values.add(combo)
        }
      }
    }

    // Sort for consistent ordering
    values.sort { a, b ->
      String aStr = a instanceof List ? (a as List).collect { it?.toString() ?: '' }.join('|') : (a?.toString() ?: '')
      String bStr = b instanceof List ? (b as List).collect { it?.toString() ?: '' }.join('|') : (b?.toString() ?: '')
      aStr <=> bStr
    }

    return values
  }

  /**
   * Get the row index for a panel based on its values.
   */
  int getRowIndex(Map<String, Object> panelValues, Matrix data) {
    if (rows.isEmpty()) return 0

    List<Object> rowValues = getUniqueValues(data, rows)
    Object panelRowVal = rows.size() == 1 ? panelValues[rows[0]] :
        rows.collect { panelValues[it] }

    return rowValues.indexOf(panelRowVal)
  }

  /**
   * Get the column index for a panel based on its values.
   */
  int getColIndex(Map<String, Object> panelValues, Matrix data) {
    if (cols.isEmpty()) return 0

    List<Object> colValues = getUniqueValues(data, cols)
    Object panelColVal = cols.size() == 1 ? panelValues[cols[0]] :
        cols.collect { panelValues[it] }

    return colValues.indexOf(panelColVal)
  }

  /**
   * Get unique row values for labeling.
   */
  List<Object> getRowValues(Matrix data) {
    List<Object> values = getUniqueValues(data, rows)
    return values.isEmpty() ? [null] : values
  }

  /**
   * Get unique column values for labeling.
   */
  List<Object> getColValues(Matrix data) {
    List<Object> values = getUniqueValues(data, cols)
    return values.isEmpty() ? [null] : values
  }

  /**
   * Get the row label for a specific row index.
   */
  String getRowLabel(int rowIndex, Matrix data) {
    List<Object> rowVals = getRowValues(data)
    if (rowIndex < 0 || rowIndex >= rowVals.size() || rowVals[rowIndex] == null) {
      return ''
    }
    Object val = rowVals[rowIndex]

    // If labeller is a Labeller object, use it
    if (labeller instanceof Labeller) {
      if (rows.size() == 1) {
        return (labeller as Labeller).label(rows[0], val)
      } else {
        // Multiple row variables - create map
        Map<String, Object> valMap = [:]
        if (val instanceof List) {
          List valList = val as List
          for (int i = 0; i < rows.size() && i < valList.size(); i++) {
            valMap[rows[i]] = valList[i]
          }
        }
        return (labeller as Labeller).label(valMap)
      }
    }

    // String-based labelling (backward compatibility)
    if (labeller == 'both' && rows.size() == 1) {
      return "${rows[0]}: ${val}"
    }
    return val instanceof List ? (val as List).collect { it?.toString() ?: '' }.join(', ') : val.toString()
  }

  /**
   * Get the column label for a specific column index.
   */
  String getColLabel(int colIndex, Matrix data) {
    List<Object> colVals = getColValues(data)
    if (colIndex < 0 || colIndex >= colVals.size() || colVals[colIndex] == null) {
      return ''
    }
    Object val = colVals[colIndex]

    // If labeller is a Labeller object, use it
    if (labeller instanceof Labeller) {
      if (cols.size() == 1) {
        return (labeller as Labeller).label(cols[0], val)
      } else {
        // Multiple column variables - create map
        Map<String, Object> valMap = [:]
        if (val instanceof List) {
          List valList = val as List
          for (int i = 0; i < cols.size() && i < valList.size(); i++) {
            valMap[cols[i]] = valList[i]
          }
        }
        return (labeller as Labeller).label(valMap)
      }
    }

    // String-based labelling (backward compatibility)
    if (labeller == 'both' && cols.size() == 1) {
      return "${cols[0]}: ${val}"
    }
    return val instanceof List ? (val as List).collect { it?.toString() ?: '' }.join(', ') : val.toString()
  }
}
