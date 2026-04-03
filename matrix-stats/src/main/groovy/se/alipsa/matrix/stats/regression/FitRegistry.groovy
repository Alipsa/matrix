package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

/**
 * Registry of named fit methods. Provides built-in methods (lm, loess, gam)
 * and allows registration of custom methods.
 */
@CompileStatic
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
