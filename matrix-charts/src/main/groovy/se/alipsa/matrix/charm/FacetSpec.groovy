package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed facet specification for Charm core.
 */
@CompileStatic
class FacetSpec extends Facet {

  /**
   * Builder-style facet type setter.
   *
   * @param value facet type
   * @return this spec
   */
  FacetSpec type(FacetType value) {
    setType(value)
    this
  }

  /**
   * Copies this facet spec.
   *
   * @return copied facet spec
   */
  @Override
  FacetSpec copy() {
    new FacetSpec(
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
