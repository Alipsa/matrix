package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

/**
 * Static DSL entry points for Charm chart specifications.
 */
@CompileStatic
class Charts {

  /**
   * Creates a plot specification for the supplied data.
   *
   * @param data source matrix
   * @return mutable plot specification
   */
  static PlotSpec plot(Matrix data) {
    new PlotSpec(data)
  }

  /**
   * Creates a plot specification from column-oriented map data.
   *
   * @param columns map of column name to values
   * @return mutable plot specification
   */
  static PlotSpec plot(Map<String, List> columns) {
    plot(toMatrix(columns))
  }

  /**
   * Creates a plot specification from iterable observations.
   * Supports iterables of maps, rows, and POJO/bean objects.
   *
   * @param observations iterable observations
   * @return mutable plot specification
   */
  static PlotSpec plot(Iterable<?> observations) {
    plot(toMatrix(observations))
  }

  /**
   * Creates and configures a plot specification in one call.
   *
   * @param data source matrix
   * @param configure plot configuration closure
   * @return mutable plot specification
   */
  static PlotSpec plot(
      Matrix data,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlotSpec) Closure<?> configure
  ) {
    PlotSpec spec = new PlotSpec(data)
    Closure<?> body = configure.rehydrate(spec, spec, spec)
    body.resolveStrategy = Closure.DELEGATE_ONLY
    body.call()
    spec
  }

  /**
   * Creates and configures a plot specification from column-oriented map data in one call.
   *
   * @param columns map of column name to values
   * @param configure plot configuration closure
   * @return mutable plot specification
   */
  static PlotSpec plot(
      Map<String, List> columns,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlotSpec) Closure<?> configure
  ) {
    plot(toMatrix(columns), configure)
  }

  /**
   * Creates and configures a plot specification from iterable observations in one call.
   *
   * @param observations iterable observations
   * @param configure plot configuration closure
   * @return mutable plot specification
   */
  static PlotSpec plot(
      Iterable<?> observations,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlotSpec) Closure<?> configure
  ) {
    plot(toMatrix(observations), configure)
  }

  /**
   * Alias for {@link #plot(Matrix)} to support import-conflict ergonomics.
   *
   * @param data source matrix
   * @return mutable plot specification
   */
  static PlotSpec chart(Matrix data) {
    plot(data)
  }

  /**
   * Alias for {@link #plot(Map)} to support import-conflict ergonomics.
   *
   * @param columns map of column name to values
   * @return mutable plot specification
   */
  static PlotSpec chart(Map<String, List> columns) {
    plot(columns)
  }

  /**
   * Alias for {@link #plot(Iterable)} to support import-conflict ergonomics.
   *
   * @param observations iterable observations
   * @return mutable plot specification
   */
  static PlotSpec chart(Iterable<?> observations) {
    plot(observations)
  }

  /**
   * Alias for {@link #plot(Matrix, Closure)} to support import-conflict ergonomics.
   *
   * @param data source matrix
   * @param configure plot configuration closure
   * @return mutable plot specification
   */
  static PlotSpec chart(
      Matrix data,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlotSpec) Closure<?> configure
  ) {
    plot(data, configure)
  }

  /**
   * Alias for {@link #plot(Map, Closure)} to support import-conflict ergonomics.
   *
   * @param columns map of column name to values
   * @param configure plot configuration closure
   * @return mutable plot specification
   */
  static PlotSpec chart(
      Map<String, List> columns,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlotSpec) Closure<?> configure
  ) {
    plot(columns, configure)
  }

  /**
   * Alias for {@link #plot(Iterable, Closure)} to support import-conflict ergonomics.
   *
   * @param observations iterable observations
   * @param configure plot configuration closure
   * @return mutable plot specification
   */
  static PlotSpec chart(
      Iterable<?> observations,
      @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlotSpec) Closure<?> configure
  ) {
    plot(observations, configure)
  }

  private static Matrix toMatrix(Map<String, List> columns) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException('columns cannot be null or empty')
    }
    Matrix.builder().columns(columns).build()
  }

  private static Matrix toMatrix(Iterable<?> observations) {
    if (observations == null) {
      throw new IllegalArgumentException('observations cannot be null')
    }
    List<?> list = observations instanceof List<?> ? (observations as List<?>) : observations.collect { Object it -> it } as List<?>
    if (list.isEmpty()) {
      throw new IllegalArgumentException('observations cannot be empty')
    }
    Object first = list.first()
    if (first instanceof Map) {
      return Matrix.builder().mapList(list as List<Map>).build()
    }
    if (first instanceof Row) {
      return Matrix.builder().rowList(list as List<Row>).build()
    }
    Matrix.builder().data(list).build()
  }
}
