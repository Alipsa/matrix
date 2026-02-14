package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Faceting configuration for Charm charts.
 */
@CompileStatic
class Facet {

  private FacetType type = FacetType.NONE
  private List<ColumnExpr> rows = []
  private List<ColumnExpr> cols = []
  private List<ColumnExpr> vars = []
  private Integer ncol
  private Integer nrow
  private Map<String, Object> params = [:]

  /**
   * Returns facet layout type.
   *
   * @return facet type
   */
  FacetType getType() {
    type
  }

  /**
   * Sets facet layout type.
   *
   * @param type facet type
   */
  void setType(FacetType type) {
    this.type = type ?: FacetType.NONE
  }

  /**
   * Returns row facet variables.
   *
   * @return row variables
   */
  List<ColumnExpr> getRows() {
    rows
  }

  /**
   * Sets row facet variables.
   *
   * @param rows row variables
   */
  void setRows(List<ColumnExpr> rows) {
    this.rows = rows == null ? [] : new ArrayList<>(rows)
  }

  /**
   * Returns column facet variables.
   *
   * @return column variables
   */
  List<ColumnExpr> getCols() {
    cols
  }

  /**
   * Sets column facet variables.
   *
   * @param cols column variables
   */
  void setCols(List<ColumnExpr> cols) {
    this.cols = cols == null ? [] : new ArrayList<>(cols)
  }

  /**
   * Returns wrap facet variables.
   *
   * @return wrap variables
   */
  List<ColumnExpr> getVars() {
    vars
  }

  /**
   * Sets wrap facet variables.
   *
   * @param vars wrap variables
   */
  void setVars(List<ColumnExpr> vars) {
    this.vars = vars == null ? [] : new ArrayList<>(vars)
  }

  /**
   * Returns wrap ncol.
   *
   * @return wrap ncol
   */
  Integer getNcol() {
    ncol
  }

  /**
   * Sets wrap ncol.
   *
   * @param ncol wrap ncol
   */
  void setNcol(Integer ncol) {
    this.ncol = ncol
  }

  /**
   * Returns wrap nrow.
   *
   * @return wrap nrow
   */
  Integer getNrow() {
    nrow
  }

  /**
   * Sets wrap nrow.
   *
   * @param nrow wrap nrow
   */
  void setNrow(Integer nrow) {
    this.nrow = nrow
  }

  /**
   * Returns extra facet parameters.
   *
   * @return parameter map
   */
  Map<String, Object> getParams() {
    params
  }

  /**
   * Sets extra facet parameters.
   *
   * @param params parameter map
   */
  void setParams(Map<String, Object> params) {
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
  }

  /**
   * Copies this facet spec.
   *
   * @return copied facet spec
   */
  Facet copy() {
    new Facet(
        type: type,
        rows: new ArrayList<>(rows),
        cols: new ArrayList<>(cols),
        vars: new ArrayList<>(vars),
        ncol: ncol,
        nrow: nrow,
        params: new LinkedHashMap<>(params)
    )
  }
}
