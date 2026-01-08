package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

/**
 * Interface for coordinate transformations.
 * Transformations convert coordinates in one space to another space.
 */
@CompileStatic
interface Trans {
  /**
   * Forward transformation function.
   * @param x value to transform
   * @return transformed value
   */
  BigDecimal transform(Number x)

  /**
   * Inverse transformation function.
   * @param x transformed value
   * @return original value
   */
  BigDecimal inverse(Number x)

  /**
   * Name of the transformation.
   */
  String getName()

  /**
   * Breaks generation - some transformations may want custom break points.
   * @param limits data limits [min, max]
   * @param n approximate number of breaks
   * @return list of break points
   */
  List<BigDecimal> breaks(List<Number> limits, int n)
}
