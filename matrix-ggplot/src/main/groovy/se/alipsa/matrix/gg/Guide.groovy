package se.alipsa.matrix.gg

import groovy.transform.CompileStatic

/**
 * Guide specification for legends and colorbars.
 */
@CompileStatic
class Guide {

  /** Guide type, e.g. 'legend' or 'colorbar'. */
  String type

  /** Optional guide parameters. */
  Map params = [:]

  /**
   * Create a guide specification.
   *
   * @param type guide type ('legend' or 'colorbar')
   * @param params optional configuration values
   */
  Guide(String type, Map params = [:]) {
    this.type = type
    if (params) {
      this.params.putAll(params)
    }
  }
}
