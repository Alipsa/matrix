package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Scale-domain type families.
 */
@CompileStatic
enum ScaleType {
  CONTINUOUS,
  DISCRETE,
  BINNED,
  DATE,
  TRANSFORM
}
