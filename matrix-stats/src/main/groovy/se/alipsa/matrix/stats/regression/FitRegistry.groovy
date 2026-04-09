package se.alipsa.matrix.stats.regression

/**
 * Registry of named fit methods.
 *
 * <p>Built-in methods:
 * <ul>
 *   <li>{@code lm} - ordinary least squares</li>
 *   <li>{@code loess} - univariate local regression</li>
 *   <li>{@code gam} - additive model using spline-expanded smooth terms</li>
 * </ul>
 *
 * <p>Callers typically build a {@link se.alipsa.matrix.stats.formula.ModelFrameResult}
 * via {@code ModelFrame.of(...).evaluate()} and then dispatch to the chosen fit method
 * through this registry.
 */
final class FitRegistry {

  private static final FitRegistry INSTANCE = new FitRegistry()

  private final Map<String, FitMethod> methods = [:]

  private FitRegistry() {
    register('lm', new LmMethod())
    register('loess', new LoessMethod())
    register('gam', new GamMethod())
  }

  /**
   * Returns the singleton registry instance.
   *
   * @return the registry
   */
  static FitRegistry instance() {
    INSTANCE
  }

  /**
   * Registers a fit method under the given name, replacing any existing registration.
   *
   * @param name the method name (case-sensitive)
   * @param method the method implementation
   */
  void register(String name, FitMethod method) {
    methods[name] = method
  }

  /**
   * Looks up a fit method by name.
   *
   * @param name the method name
   * @return the fit method
   * @throws IllegalArgumentException if no method is registered under that name
   */
  FitMethod get(String name) {
    if (name == null) {
      throw new IllegalArgumentException('Fit method name must not be null')
    }
    FitMethod method = methods[name]
    if (method == null) {
      throw new IllegalArgumentException(
        "Unknown fit method '${name}'. Available: ${methods.keySet().sort()}"
      )
    }
    method
  }

  /**
   * Returns true if a method is registered under the given name.
   *
   * @param name the method name
   * @return true if registered
   */
  boolean contains(String name) {
    methods.containsKey(name)
  }

}
