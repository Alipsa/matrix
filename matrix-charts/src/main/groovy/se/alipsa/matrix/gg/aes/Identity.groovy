package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic

/**
 * Identity wrapper for constant values in aesthetic mappings.
 * Use I(value) in aes() to specify a constant instead of a column mapping.
 *
 * Example: aes(x: 'col1', color: I('red'))
 */
@CompileStatic
class Identity {
  final Object value

  Identity(Object value) {
    this.value = value
  }

  @Override
  String toString() {
    return "I($value)"
  }

  /**
   * Static factory method matching ggplot2's I() function.
   */
  static Identity of(Object value) {
    return new Identity(value)
  }
}