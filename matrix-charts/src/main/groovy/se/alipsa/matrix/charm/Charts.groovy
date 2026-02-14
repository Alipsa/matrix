package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

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
   * Alias for {@link #plot(Matrix)} to support import-conflict ergonomics.
   *
   * @param data source matrix
   * @return mutable plot specification
   */
  static PlotSpec chart(Matrix data) {
    plot(data)
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
}
